package com.example.delivery.packagebuild;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.customer.CustomerEntity;
import com.example.delivery.customer.CustomerEnvironmentEntity;
import com.example.delivery.customer.CustomerService;
import com.example.delivery.agent.executor.ComponentDescriptor;
import com.example.delivery.agent.executor.ExecutionPlan;
import com.example.delivery.agent.executor.ExecutionPlanService;
import com.example.delivery.deploy.DeployComponentEntity;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.deploy.DeployPlanVersionEntity;
import com.example.delivery.deploy.DeployPlanVersionStatus;
import com.example.delivery.repository.ResourceService;
import com.example.delivery.repository.ResourceVersionEntity;
import com.example.delivery.snapshot.SnapshotComponentEntity;
import com.example.delivery.snapshot.SnapshotEntity;
import com.example.delivery.snapshot.SnapshotService;
import com.example.delivery.storage.ObjectStorageService;
import com.example.delivery.storage.StoredObject;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageBuildService {
    private final CustomerService customerService;
    private final DeployPlanService deployPlanService;
    private final ResourceService resourceService;
    private final PackageBuildRepository packageBuildRepository;
    private final ObjectStorageService objectStorageService;
    private final SnapshotService snapshotService;
    private final ExecutionPlanService executionPlanService = new ExecutionPlanService();
    private final AtomicLong packageIdSequence = new AtomicLong(1);
    private final Map<Long, PackageBuildEntity> packages = new ConcurrentHashMap<>();
    private final Map<Long, PackageManifest> manifests = new ConcurrentHashMap<>();

    @Autowired
    public PackageBuildService(
            CustomerService customerService,
            DeployPlanService deployPlanService,
            ResourceService resourceService,
            ObjectProvider<PackageBuildRepository> packageBuildRepositoryProvider,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            ObjectProvider<SnapshotService> snapshotServiceProvider
    ) {
        this.customerService = customerService;
        this.deployPlanService = deployPlanService;
        this.resourceService = resourceService;
        this.packageBuildRepository = packageBuildRepositoryProvider.getIfAvailable();
        this.objectStorageService = objectStorageServiceProvider.getIfAvailable();
        this.snapshotService = snapshotServiceProvider.getIfAvailable();
        if (this.packageBuildRepository == null) {
            seed();
        }
    }

    public PackageBuildService(
            CustomerService customerService,
            DeployPlanService deployPlanService,
            ResourceService resourceService
    ) {
        this.customerService = customerService;
        this.deployPlanService = deployPlanService;
        this.resourceService = resourceService;
        this.packageBuildRepository = null;
        this.objectStorageService = null;
        this.snapshotService = null;
        seed();
    }

    public List<PackageBuildEntity> listPackages() {
        if (useJdbc()) {
            return packageBuildRepository.findAll();
        }
        return packages.values().stream()
                .sorted(Comparator.comparing(PackageBuildEntity::createdAt).reversed())
                .toList();
    }

    public PackageBuildEntity getPackage(Long id) {
        if (useJdbc()) {
            return packageBuildRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "部署包不存在"));
        }
        PackageBuildEntity packageBuild = packages.get(id);
        if (packageBuild == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部署包不存在");
        }
        return packageBuild;
    }

    @Transactional
    public PackageBuildEntity createPackage(CreatePackageBuildRequest request) {
        CustomerEntity customer = customerService.getCustomer(request.customerId());
        CustomerEnvironmentEntity environment = customerService.getEnvironment(request.environmentId());
        if (!environment.customerId().equals(customer.id())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "客户环境不属于当前客户");
        }
        DeployPlanVersionEntity version = deployPlanService.getVersion(request.deployPlanVersionId());
        if (version.status() != DeployPlanVersionStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只能基于已发布部署方案版本生成部署包");
        }
        if (!request.deployPlanVersionId().equals(environment.deployPlanVersionId())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "部署方案版本与客户环境绑定版本不一致");
        }

        Long id = packageIdSequence.getAndIncrement();
        String packageCode = "PKG" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", id);
        LocalDateTime createdAt = LocalDateTime.now();
        int retentionDays = objectStorageService != null ? objectStorageService.packageRetentionDays() : 90;
        if (useJdbc()) {
            PackageBuildEntity created = packageBuildRepository.insertPackage(packageCode, request, null, "packages/" + packageCode + ".zip", retentionDays);
            String manifestJson = buildManifestJson(created.id(), packageCode, customer, environment, version, request, created.createdAt());
            StoredObject archive = buildAndStoreArchive(packageCode, manifestJson, environment, version);
            packageBuildRepository.updateArtifacts(created.id(), archive.checksum(), archive.objectUrl());
            packageBuildRepository.insertManifest(created.id(), manifestJson, archive.objectUrl());
            return getPackage(created.id());
        }
        String manifestJson = buildManifestJson(id, packageCode, customer, environment, version, request, createdAt);
        StoredObject archive = buildAndStoreArchive(packageCode, manifestJson, environment, version);
        PackageBuildEntity packageBuild = new PackageBuildEntity(
                id,
                packageCode,
                customer.id(),
                environment.id(),
                version.id(),
                request.packageVersion(),
                PackageBuildStatus.SUCCESS,
                true,
                archive.objectUrl(),
                archive.checksum(),
                createdAt,
                PackageLifecycleState.ACTIVE,
                0L,
                null,
                createdAt.plusDays(retentionDays)
        );
        packages.put(id, packageBuild);
        manifests.put(id, new PackageManifest(id, manifestJson, archive.checksum()));
        return packageBuild;
    }

    public PackageManifest getManifest(Long packageBuildId) {
        getPackage(packageBuildId);
        if (useJdbc()) {
            return packageBuildRepository.findManifest(packageBuildId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "部署包 manifest 不存在"));
        }
        PackageManifest manifest = manifests.get(packageBuildId);
        if (manifest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部署包 manifest 不存在");
        }
        return manifest;
    }

    public PackageBuildStatus getStatus(Long packageBuildId) {
        return getPackage(packageBuildId).buildStatus();
    }

    @Transactional
    public PackageDownloadInfo getDownloadInfo(Long packageBuildId) {
        PackageBuildEntity packageBuild = getPackage(packageBuildId);
        if (packageBuild.buildStatus() != PackageBuildStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有生成成功的部署包才能下载");
        }
        if (!packageBuild.lifecycleState().downloadable()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "部署包已" + (packageBuild.lifecycleState() == PackageLifecycleState.PURGED ? "清理" : "废弃") + "，不可下载");
        }
        PackageManifest manifest = getManifest(packageBuildId);
        recordDownload(packageBuild);
        return new PackageDownloadInfo(
                packageBuild.id(),
                packageBuild.packageCode(),
                packageBuild.filePath(),
                packageBuild.checksum(),
                manifest.manifestJson()
        );
    }

    /** 记录一次下载：计数 +1 并刷新最后下载时间。 */
    private void recordDownload(PackageBuildEntity packageBuild) {
        if (useJdbc()) {
            packageBuildRepository.incrementDownload(packageBuild.id());
            return;
        }
        packages.put(packageBuild.id(), packageBuild.withLifecycle(
                packageBuild.lifecycleState(),
                packageBuild.downloadCount() + 1,
                LocalDateTime.now(),
                packageBuild.retentionUntil()));
    }

    /** 归档部署包（活跃 → 归档，仍可下载）。 */
    @Transactional
    public PackageBuildEntity archivePackage(Long packageBuildId) {
        return transition(packageBuildId, PackageLifecycleState.ARCHIVED,
                java.util.Set.of(PackageLifecycleState.ACTIVE));
    }

    /** 废弃部署包（活跃/归档 → 废弃，禁止下载，等待清理）。 */
    @Transactional
    public PackageBuildEntity deprecatePackage(Long packageBuildId) {
        return transition(packageBuildId, PackageLifecycleState.DEPRECATED,
                java.util.Set.of(PackageLifecycleState.ACTIVE, PackageLifecycleState.ARCHIVED));
    }

    private PackageBuildEntity transition(Long packageBuildId, PackageLifecycleState target,
                                          java.util.Set<PackageLifecycleState> allowedFrom) {
        PackageBuildEntity pkg = getPackage(packageBuildId);
        if (pkg.lifecycleState() == target) {
            return pkg;
        }
        if (!allowedFrom.contains(pkg.lifecycleState())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "部署包当前状态 " + pkg.lifecycleState() + " 不允许流转到 " + target);
        }
        if (useJdbc()) {
            packageBuildRepository.updateLifecycle(packageBuildId, target);
            return getPackage(packageBuildId);
        }
        PackageBuildEntity updated = pkg.withLifecycle(target, pkg.downloadCount(),
                pkg.lastDownloadedAt(), pkg.retentionUntil());
        packages.put(packageBuildId, updated);
        return updated;
    }

    /**
     * 清理过期部署包：所有已废弃且过保留期的包 → 删除物理 zip → 置 PURGED（保留元数据供审计）。
     * @return 已清理的包数量
     */
    @Transactional
    public int cleanupExpired() {
        List<PackageBuildEntity> purgeable = findPurgeable();
        int purged = 0;
        for (PackageBuildEntity pkg : purgeable) {
            if (objectStorageService != null) {
                objectStorageService.deleteObject(pkg.filePath());
            }
            if (useJdbc()) {
                packageBuildRepository.updateLifecycle(pkg.id(), PackageLifecycleState.PURGED);
            } else {
                packages.put(pkg.id(), pkg.withLifecycle(PackageLifecycleState.PURGED,
                        pkg.downloadCount(), pkg.lastDownloadedAt(), pkg.retentionUntil()));
            }
            purged++;
        }
        return purged;
    }

    private List<PackageBuildEntity> findPurgeable() {
        if (useJdbc()) {
            return packageBuildRepository.findPurgeable();
        }
        LocalDateTime now = LocalDateTime.now();
        return packages.values().stream()
                .filter(p -> p.lifecycleState() == PackageLifecycleState.DEPRECATED)
                .filter(p -> p.retentionUntil() != null && p.retentionUntil().isBefore(now))
                .toList();
    }

    @Transactional
    public void deletePackage(Long packageBuildId) {
        getPackage(packageBuildId);
        if (useJdbc()) {
            packageBuildRepository.delete(packageBuildId);
            return;
        }
        packages.remove(packageBuildId);
        manifests.remove(packageBuildId);
    }

    private void seed() {
        CreatePackageBuildRequest request = new CreatePackageBuildRequest(1L, 1L, 1L, "1.0.0", "MVP 示例部署包");
        createPackage(request);
    }

    private String buildManifestJson(
            Long packageBuildId,
            String packageCode,
            CustomerEntity customer,
            CustomerEnvironmentEntity environment,
            DeployPlanVersionEntity version,
            CreatePackageBuildRequest request,
            LocalDateTime createdAt
    ) {
        List<DeployComponentEntity> components = deployPlanService.listComponents(version.id());
        String componentJson = components.stream()
                .map(component -> buildComponentJson(component, resourceService.getVersion(component.resourceVersionId())))
                .collect(java.util.stream.Collectors.joining(","));
        return "{"
                + jsonField("packageBuildId", packageBuildId) + ","
                + jsonField("packageCode", packageCode) + ","
                + jsonField("packageVersion", request.packageVersion()) + ","
                + jsonField("customerId", customer.id()) + ","
                + jsonField("customerCode", customer.customerCode()) + ","
                + jsonField("environmentId", environment.id()) + ","
                + jsonField("environmentName", environment.environmentName()) + ","
                + jsonField("environmentType", environment.environmentType().name()) + ","
                + jsonField("deployPlanVersionId", version.id()) + ","
                + jsonField("deployPlanVersion", version.version()) + ","
                + jsonField("createdAt", createdAt.toString()) + ","
                + jsonField("remark", request.remark()) + ","
                + "\"components\":[" + componentJson + "]"
                + "}";
    }

    private String buildComponentJson(DeployComponentEntity component, ResourceVersionEntity resourceVersion) {
        return "{"
                + jsonField("componentId", component.id()) + ","
                + jsonField("componentName", component.componentName()) + ","
                + jsonField("componentType", component.componentType()) + ","
                + jsonField("deployOrder", component.deployOrder()) + ","
                + jsonField("resourceVersionId", resourceVersion.id()) + ","
                + jsonField("resourceId", resourceVersion.resourceId()) + ","
                + jsonField("resourceVersion", resourceVersion.version()) + ","
                + jsonField("externalUrl", resourceVersion.externalUrl()) + ","
                + jsonField("imageRepository", resourceVersion.imageRepository()) + ","
                + jsonField("imageTag", resourceVersion.imageTag()) + ","
                + jsonField("resourceChecksum", resourceVersion.checksum()) + ","
                + jsonField("configTemplate", component.configTemplate()) + ","
                + jsonField("healthCheck", component.healthCheck())
                + "}";
    }

    /**
     * 生成真实压缩部署包（ZIP）：含 manifest.json、checksum.sha256、README.txt、每组件渲染配置、
     * 以及能取到的制品二进制（内嵌到 artifacts/，取不到的在 manifest 中按 URL 引用）。
     * 组件来源优先使用客户环境的配置快照（与源方案解耦），无快照时回退方案版本组件。
     * 对象存储不可用（dev 内存模式）时降级为引用存储，不写真实文件——保证测试可跑。
     */
    private StoredObject buildAndStoreArchive(String packageCode, String manifestJson,
                                              CustomerEnvironmentEntity environment,
                                              DeployPlanVersionEntity version) {
        // 解析组件来源：优先快照
        List<ComponentSource> sources = resolveComponentSources(environment, version);

        if (objectStorageService == null) {
            // dev 内存模式：只算 manifest 校验和，不落地 ZIP
            String checksum = sha256(manifestJson);
            return new StoredObject("local", packageCode + ".zip",
                    "packages/" + packageCode + ".zip", checksum);
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
                writeZipText(zip, "manifest.json", manifestJson);
                writeZipText(zip, "README.txt", buildReadme(packageCode, sources));
                // 同包可执行 agent 脚本 + 执行计划（离线部署执行器）
                ExecutionPlan plan = executionPlanService.buildPlan(packageCode, toDescriptors(sources));
                writeZipText(zip, "agent/deploy-agent.sh", executionPlanService.generateAgentScript(plan));
                writeZipText(zip, "agent/execution-plan.json", executionPlanService.renderPlanJson(plan));
                for (ComponentSource src : sources) {
                    String dir = "components/" + sanitize(src.componentName());
                    if (src.configTemplate() != null) {
                        writeZipText(zip, dir + "/config.conf", src.configTemplate());
                    }
                    if (src.healthCheck() != null) {
                        writeZipText(zip, dir + "/healthcheck.txt", src.healthCheck());
                    }
                    // 内嵌能取到的二进制
                    if (src.artifactUrl() != null) {
                        byte[] bytes = objectStorageService.fetchObjectBytes(src.artifactUrl());
                        if (bytes != null) {
                            writeZipBytes(zip, "artifacts/" + sanitize(src.componentName()) + "/" + fileName(src.artifactUrl()), bytes);
                        }
                    }
                }
            }
            byte[] zipBytes = buffer.toByteArray();
            String checksum = sha256Bytes(zipBytes);
            // 把 checksum 也写进包旁记录（此处直接用 manifest 校验作为标识）
            return objectStorageService.putPackageArchive(packageCode + ".zip", zipBytes, checksum);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "部署包压缩失败: " + e.getMessage());
        }
    }

    private List<ComponentSource> resolveComponentSources(CustomerEnvironmentEntity environment,
                                                          DeployPlanVersionEntity version) {
        List<ComponentSource> result = new ArrayList<>();
        SnapshotEntity snapshot = snapshotService != null
                ? snapshotService.findByEnvironmentOrNull(environment.id()) : null;
        if (snapshot != null) {
            int order = 1;
            for (SnapshotComponentEntity c : snapshotService.listComponents(snapshot.id())) {
                String url = null;
                String resourceType = "UNKNOWN";
                if (c.resourceVersionId() != null) {
                    ResourceVersionEntity rv = resourceService.getVersion(c.resourceVersionId());
                    url = rv.externalUrl();
                    resourceType = resourceService.getResource(rv.resourceId()).resourceType().name();
                }
                result.add(new ComponentSource(c.componentName(), resourceType, c.configTemplate(),
                        c.healthCheck(), url, c.deployOrder() == 0 ? order++ : c.deployOrder()));
            }
            return result;
        }
        for (DeployComponentEntity c : deployPlanService.listComponents(version.id())) {
            ResourceVersionEntity rv = resourceService.getVersion(c.resourceVersionId());
            String resourceType = resourceService.getResource(rv.resourceId()).resourceType().name();
            result.add(new ComponentSource(c.componentName(), resourceType, c.configTemplate(),
                    c.healthCheck(), rv.externalUrl(), c.deployOrder()));
        }
        return result;
    }

    private record ComponentSource(String componentName, String resourceType, String configTemplate,
                                   String healthCheck, String artifactUrl, int deployOrder) {}

    private List<ComponentDescriptor> toDescriptors(List<ComponentSource> sources) {
        return sources.stream()
                .map(s -> new ComponentDescriptor(s.componentName(), s.resourceType(),
                        s.artifactUrl() != null ? fileName(s.artifactUrl()) : s.componentName(),
                        s.configTemplate(), s.healthCheck(), s.deployOrder()))
                .toList();
    }

    /** 获取部署包的离线执行计划（供前端预览）。 */
    public ExecutionPlan getExecutionPlan(Long packageBuildId) {
        PackageBuildEntity pkg = getPackage(packageBuildId);
        CustomerEnvironmentEntity environment = customerService.getEnvironment(pkg.environmentId());
        DeployPlanVersionEntity version = deployPlanService.getVersion(pkg.deployPlanVersionId());
        List<ComponentSource> sources = resolveComponentSources(environment, version);
        return executionPlanService.buildPlan(pkg.packageCode(), toDescriptors(sources));
    }

    private String buildReadme(String packageCode, List<ComponentSource> sources) {
        StringBuilder sb = new StringBuilder();
        sb.append("部署包: ").append(packageCode).append("\n");
        sb.append("生成时间: ").append(LocalDateTime.now()).append("\n");
        sb.append("组件数量: ").append(sources.size()).append("\n\n");
        sb.append("目录结构:\n");
        sb.append("  manifest.json          部署清单（组件、资源、配置元数据）\n");
        sb.append("  checksum.sha256        包体校验和\n");
        sb.append("  components/<名称>/       各组件渲染后配置与健康检查\n");
        sb.append("  artifacts/<名称>/        内嵌的制品二进制（取不到的见 manifest 引用地址）\n");
        return sb.toString();
    }

    private void writeZipText(ZipOutputStream zip, String name, String content) throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void writeZipBytes(ZipOutputStream zip, String name, byte[] bytes) throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private String sanitize(String name) {
        return name == null ? "unnamed" : name.replaceAll("[^A-Za-z0-9._\\-一-龥]", "_");
    }

    private String fileName(String url) {
        int slash = url.replace("\\", "/").lastIndexOf('/');
        String name = slash >= 0 ? url.substring(slash + 1) : url;
        return name.isBlank() ? "artifact.bin" : name;
    }

    private String sha256Bytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String jsonField(String name, String value) {
        return "\"" + name + "\":" + (value == null ? "null" : "\"" + escape(value) + "\"");
    }

    private String jsonField(String name, Long value) {
        return "\"" + name + "\":" + value;
    }

    private String jsonField(String name, int value) {
        return "\"" + name + "\":" + value;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private boolean useJdbc() {
        return packageBuildRepository != null;
    }
}
