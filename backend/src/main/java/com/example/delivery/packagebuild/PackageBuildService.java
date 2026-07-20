package com.example.delivery.packagebuild;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.customer.CustomerEntity;
import com.example.delivery.customer.CustomerEnvironmentEntity;
import com.example.delivery.customer.CustomerService;
import com.example.delivery.deploy.DeployComponentEntity;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.deploy.DeployPlanVersionEntity;
import com.example.delivery.deploy.DeployPlanVersionStatus;
import com.example.delivery.repository.ResourceService;
import com.example.delivery.repository.ResourceVersionEntity;
import com.example.delivery.storage.ObjectStorageService;
import com.example.delivery.storage.StoredObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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
    private final AtomicLong packageIdSequence = new AtomicLong(1);
    private final Map<Long, PackageBuildEntity> packages = new ConcurrentHashMap<>();
    private final Map<Long, PackageManifest> manifests = new ConcurrentHashMap<>();

    @Autowired
    public PackageBuildService(
            CustomerService customerService,
            DeployPlanService deployPlanService,
            ResourceService resourceService,
            ObjectProvider<PackageBuildRepository> packageBuildRepositoryProvider,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this.customerService = customerService;
        this.deployPlanService = deployPlanService;
        this.resourceService = resourceService;
        this.packageBuildRepository = packageBuildRepositoryProvider.getIfAvailable();
        this.objectStorageService = objectStorageServiceProvider.getIfAvailable();
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
        if (useJdbc()) {
            PackageBuildEntity created = packageBuildRepository.insertPackage(packageCode, request, null, "packages/" + packageCode + "/package.txt");
            String manifestJson = buildManifestJson(created.id(), packageCode, customer, environment, version, request, created.createdAt());
            String checksum = sha256(manifestJson);
            storePackageText(packageCode + "/manifest.json", manifestJson, "application/json", checksum);
            StoredObject checksumObject = storePackageText(packageCode + "/checksum.sha256", checksum, "text/plain", checksum);
            StoredObject packageObject = storePackageText(packageCode + "/package.txt", "MVP package placeholder: " + packageCode, "text/plain", checksum);
            packageBuildRepository.updateArtifacts(created.id(), checksum, packageObject.objectUrl());
            packageBuildRepository.insertManifest(created.id(), manifestJson, checksumObject.objectUrl());
            return getPackage(created.id());
        }
        String manifestJson = buildManifestJson(id, packageCode, customer, environment, version, request, createdAt);
        String checksum = sha256(manifestJson);
        PackageBuildEntity packageBuild = new PackageBuildEntity(
                id,
                packageCode,
                customer.id(),
                environment.id(),
                version.id(),
                request.packageVersion(),
                PackageBuildStatus.SUCCESS,
                true,
                "packages/" + packageCode + ".zip",
                checksum,
                createdAt
        );
        packages.put(id, packageBuild);
        manifests.put(id, new PackageManifest(id, manifestJson, checksum));
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

    public PackageDownloadInfo getDownloadInfo(Long packageBuildId) {
        PackageBuildEntity packageBuild = getPackage(packageBuildId);
        if (packageBuild.buildStatus() != PackageBuildStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有生成成功的部署包才能下载");
        }
        PackageManifest manifest = getManifest(packageBuildId);
        return new PackageDownloadInfo(
                packageBuild.id(),
                packageBuild.packageCode(),
                packageBuild.filePath(),
                packageBuild.checksum(),
                manifest.manifestJson()
        );
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

    private StoredObject storePackageText(String objectName, String content, String contentType, String checksum) {
        if (objectStorageService == null) {
            return new StoredObject("local", objectName, objectName, checksum);
        }
        return objectStorageService.putPackageText(objectName, content, contentType, checksum);
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
