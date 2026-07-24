package com.example.delivery.repository.harbor;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.repository.CreateResourceVersionRequest;
import com.example.delivery.repository.ResourceService;
import com.example.delivery.repository.ResourceVersionEntity;
import com.example.delivery.repository.source.SourceRepositoryService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Harbor v2 API 集成服务。镜像来源按优先级解析：
 * 1. 请求带 sourceRepositoryId → 用该源仓库（前端管理）的地址与解密凭证
 * 2. 否则回退全局 app.harbor 配置
 * 3. 都没有 → 按传入坐标直接登记（不校验 digest），适用于离线/内网
 * 无论哪种，都只同步元数据（digest + 坐标），不下载镜像层。
 */
@Service
public class HarborSyncService {

    private final HarborProperties props;
    private final ResourceService resourceService;
    private final SourceRepositoryService sourceRepositoryService;

    public HarborSyncService(HarborProperties props, ResourceService resourceService,
                             ObjectProvider<SourceRepositoryService> sourceRepositoryServiceProvider) {
        this.props = props;
        this.resourceService = resourceService;
        this.sourceRepositoryService = sourceRepositoryServiceProvider.getIfAvailable();
    }

    /** 解析后的连接信息：baseUrl 为空表示无可用源，走"直接登记"降级路径。 */
    private record Connection(String baseUrl, String username, String password) {
        boolean usable() {
            return baseUrl != null && !baseUrl.isBlank();
        }
    }

    private Connection resolveConnection(Long sourceRepositoryId) {
        if (sourceRepositoryId != null && sourceRepositoryService != null) {
            SourceRepositoryService.Credentials c = sourceRepositoryService.getCredentials(sourceRepositoryId);
            return new Connection(c.baseUrl(), c.username(), c.password());
        }
        if (props.isConfigured()) {
            return new Connection(props.baseUrl(), props.username(), props.password());
        }
        return new Connection(null, null, null);
    }

    /**
     * 同步镜像到资源版本。连接可用时调 Harbor v2 API 获取 digest；否则直接用传入坐标登记。
     */
    public ResourceVersionEntity sync(Long resourceId, HarborSyncRequest req) {
        Connection conn = resolveConnection(req.sourceRepositoryId());

        String imageRepo = conn.usable()
                ? conn.baseUrl().replaceAll("/+$", "") + "/" + req.project() + "/" + req.repository()
                : req.project() + "/" + req.repository();

        String digest = conn.usable() ? fetchDigest(conn, req.project(), req.repository(), req.tag()) : null;

        String ver = (req.version() != null && !req.version().isBlank()) ? req.version() : req.tag();

        CreateResourceVersionRequest createReq = new CreateResourceVersionRequest(
                ver,
                null,          // externalUrl
                imageRepo,     // imageRepository
                req.tag(),     // imageTag
                digest,        // checksum (Harbor digest)
                req.releaseNote(),
                null
        );
        return resourceService.createVersion(resourceId, createReq);
    }

    // ---- Harbor API ----

    private String fetchDigest(Connection conn, String project, String repository, String tag) {
        try {
            String base = conn.baseUrl().replaceAll("/+$", "");
            String encodedRepo = repository.replace("/", "%252F");
            String url = base + "/api/v2.0/projects/" + project
                    + "/repositories/" + encodedRepo + "/artifacts/" + tag;

            ArtifactResponse artifact = RestClient.create()
                    .get()
                    .uri(url)
                    .header("Authorization", basicAuth(conn.username(), conn.password()))
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(ArtifactResponse.class);

            return artifact != null ? artifact.digest() : null;
        } catch (RestClientException e) {
            // Harbor 可达性问题不阻塞登记——降级为无 digest
            return null;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Harbor 查询失败: " + e.getMessage());
        }
    }

    private static String basicAuth(String user, String pass) {
        if (user == null || pass == null) return "";
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
    }

    // ---- Harbor API 响应映射（只取需要的字段）----

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArtifactResponse(
            @JsonProperty("digest") String digest,
            @JsonProperty("tags")   List<TagItem> tags
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TagItem(@JsonProperty("name") String name) {}
}
