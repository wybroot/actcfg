package com.example.delivery.repository.harbor;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.repository.CreateResourceVersionRequest;
import com.example.delivery.repository.ResourceService;
import com.example.delivery.repository.ResourceVersionEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Harbor v2 API 集成服务。
 * 未配置 Harbor 时退化为"按传入坐标直接登记"——适用于离线/内网环境。
 */
@Service
public class HarborSyncService {

    private final HarborProperties props;
    private final ResourceService resourceService;

    public HarborSyncService(HarborProperties props, ResourceService resourceService) {
        this.props = props;
        this.resourceService = resourceService;
    }

    /**
     * 同步镜像到资源版本。
     * Harbor 已配置时调用 v2 API 获取 artifact digest；否则直接使用传入坐标登记。
     */
    public ResourceVersionEntity sync(Long resourceId, HarborSyncRequest req) {
        String imageRepo = props.isConfigured()
                ? props.baseUrl().replaceAll("/+$", "") + "/" + req.project() + "/" + req.repository()
                : req.project() + "/" + req.repository();

        String digest = null;
        if (props.isConfigured()) {
            digest = fetchDigest(req.project(), req.repository(), req.tag());
        }

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

    private String fetchDigest(String project, String repository, String tag) {
        try {
            String base = props.baseUrl().replaceAll("/+$", "");
            String encodedRepo = repository.replace("/", "%252F");
            String url = base + "/api/v2.0/projects/" + project
                    + "/repositories/" + encodedRepo + "/artifacts/" + tag;

            ArtifactResponse artifact = RestClient.create()
                    .get()
                    .uri(url)
                    .header("Authorization", basicAuth(props.username(), props.password()))
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
