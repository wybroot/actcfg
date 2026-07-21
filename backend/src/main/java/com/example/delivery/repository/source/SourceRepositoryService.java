package com.example.delivery.repository.source;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.security.crypto.SecretCipher;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 源仓库管理：CRUD + 连接测试。密码用 {@link SecretCipher} 加密入库，对外脱敏，内部同步时解密。
 */
@Service
public class SourceRepositoryService {
    private static final String ENABLED = "ENABLED";
    private static final String MASK = "******";

    private final SourceRepositoryRepository repository;
    private final SecretCipher secretCipher;
    private final AtomicLong idSequence = new AtomicLong(1);
    private final Map<Long, SourceRepositoryEntity> store = new ConcurrentHashMap<>();

    @Autowired
    public SourceRepositoryService(ObjectProvider<SourceRepositoryRepository> repositoryProvider,
                                   ObjectProvider<SecretCipher> secretCipherProvider) {
        this.repository = repositoryProvider.getIfAvailable();
        SecretCipher cipher = secretCipherProvider.getIfAvailable();
        this.secretCipher = cipher != null ? cipher : new SecretCipher(null);
    }

    public SourceRepositoryService() {
        this.repository = null;
        this.secretCipher = new SecretCipher(null);
    }

    private boolean useJdbc() {
        return repository != null;
    }

    private String defaultStatus(String status) {
        return status == null || status.isBlank() ? ENABLED : status;
    }

    // ---- 对外读取（脱敏）----

    /** 列出全部源仓库，密码脱敏为掩码。 */
    public List<SourceRepositoryEntity> listRepositories() {
        List<SourceRepositoryEntity> all = useJdbc()
                ? repository.findAllActive()
                : store.values().stream()
                        .filter(r -> !r.deleted())
                        .sorted(Comparator.comparing(SourceRepositoryEntity::createdAt).reversed())
                        .toList();
        return all.stream().map(this::mask).toList();
    }

    /** 单个源仓库，密码脱敏。 */
    public SourceRepositoryEntity getRepository(Long id) {
        return mask(getRaw(id));
    }

    /** 内部读取：保留存储原值（含密文），不脱敏。 */
    private SourceRepositoryEntity getRaw(Long id) {
        if (useJdbc()) {
            return repository.findActiveById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "源仓库不存在"));
        }
        SourceRepositoryEntity r = store.get(id);
        if (r == null || r.deleted()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "源仓库不存在");
        }
        return r;
    }

    private SourceRepositoryEntity mask(SourceRepositoryEntity r) {
        String masked = (r.password() != null && !r.password().isBlank()) ? MASK : null;
        return new SourceRepositoryEntity(r.id(), r.repoCode(), r.repoName(), r.repoType(),
                r.baseUrl(), r.username(), masked, r.description(), r.status(),
                r.createdAt(), r.updatedAt(), r.deleted());
    }

    // ---- 写操作 ----

    @Transactional
    public SourceRepositoryEntity create(CreateSourceRepositoryRequest req) {
        ensureCodeUnique(req.repoCode());
        String passwordEnc = secretCipher.encrypt(req.password());
        SourceRepositoryType type = req.repoType() == null ? SourceRepositoryType.HARBOR : req.repoType();
        if (useJdbc()) {
            return mask(repository.insert(req, passwordEnc, defaultStatus(req.status())));
        }
        LocalDateTime now = LocalDateTime.now();
        SourceRepositoryEntity entity = new SourceRepositoryEntity(
                idSequence.getAndIncrement(), req.repoCode(), req.repoName(), type,
                req.baseUrl(), req.username(), passwordEnc, req.description(),
                defaultStatus(req.status()), now, now, false);
        store.put(entity.id(), entity);
        return mask(entity);
    }

    @Transactional
    public SourceRepositoryEntity update(Long id, UpdateSourceRepositoryRequest req) {
        SourceRepositoryEntity current = getRaw(id);
        // 密码留空表示不改，沿用原密文；非空则加密覆盖
        String passwordEnc = (req.password() == null || req.password().isBlank())
                ? current.password()
                : secretCipher.encrypt(req.password());
        SourceRepositoryType type = req.repoType() == null ? current.repoType() : req.repoType();
        if (useJdbc()) {
            return mask(repository.update(id, req, passwordEnc, defaultStatus(req.status())));
        }
        SourceRepositoryEntity updated = new SourceRepositoryEntity(
                current.id(), current.repoCode(), req.repoName(), type,
                req.baseUrl(), req.username(), passwordEnc, req.description(),
                defaultStatus(req.status()), current.createdAt(), LocalDateTime.now(), false);
        store.put(id, updated);
        return mask(updated);
    }

    @Transactional
    public void delete(Long id) {
        SourceRepositoryEntity current = getRaw(id);
        if (useJdbc()) {
            repository.softDelete(id);
            return;
        }
        store.put(id, new SourceRepositoryEntity(current.id(), current.repoCode(), current.repoName(),
                current.repoType(), current.baseUrl(), current.username(), current.password(),
                current.description(), current.status(), current.createdAt(), LocalDateTime.now(), true));
    }

    private void ensureCodeUnique(String repoCode) {
        boolean exists = useJdbc()
                ? repository.existsByCode(repoCode)
                : store.values().stream().anyMatch(r -> !r.deleted() && r.repoCode().equals(repoCode));
        if (exists) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "源仓库编码已存在");
        }
    }

    // ---- 供同步使用：解密凭证 ----

    /** 解密后的连接凭证，仅内部（HarborSyncService）使用。 */
    public record Credentials(String baseUrl, String username, String password, SourceRepositoryType type) {}

    public Credentials getCredentials(Long id) {
        SourceRepositoryEntity r = getRaw(id);
        if (!ENABLED.equals(r.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "源仓库已停用，无法使用");
        }
        return new Credentials(r.baseUrl(), r.username(), secretCipher.decrypt(r.password()), r.repoType());
    }

    // ---- 连接测试 ----

    /**
     * 测试源仓库连通性。HARBOR 用解密凭证调 /api/v2.0/health（无需认证也返回 200，认证错误会体现在状态码）。
     * 只判断"能否连通并鉴权"，不抛异常给上层——返回结构化结果。
     */
    public TestResult testConnection(Long id) {
        SourceRepositoryEntity r = getRaw(id);
        String password = secretCipher.decrypt(r.password());
        String base = r.baseUrl() == null ? "" : r.baseUrl().replaceAll("/+$", "");
        try {
            String url = base + "/api/v2.0/health";
            RestClient.create().get().uri(url)
                    .header("Authorization", basicAuth(r.username(), password))
                    .retrieve().toBodilessEntity();
            return new TestResult(true, "连接成功");
        } catch (RestClientException e) {
            return new TestResult(false, "连接失败: " + e.getMessage());
        } catch (Exception e) {
            return new TestResult(false, "连接异常: " + e.getMessage());
        }
    }

    /** 连接测试结果。 */
    public record TestResult(boolean success, String message) {}

    private static String basicAuth(String user, String pass) {
        if (user == null || pass == null) {
            return "";
        }
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
    }
}
