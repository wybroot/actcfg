package com.example.delivery.deploy;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.repository.ResourceService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DeployPlanService {
    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";

    private final DeployPlanRepository deployPlanRepository;
    private final ResourceService resourceService;
    private final AtomicLong planIdSequence = new AtomicLong(1);
    private final AtomicLong versionIdSequence = new AtomicLong(1);
    private final AtomicLong componentIdSequence = new AtomicLong(1);
    private final Map<Long, DeployPlanEntity> plans = new ConcurrentHashMap<>();
    private final Map<Long, DeployPlanVersionEntity> versions = new ConcurrentHashMap<>();
    private final Map<Long, DeployComponentEntity> components = new ConcurrentHashMap<>();

    @Autowired
    public DeployPlanService(ResourceService resourceService, ObjectProvider<DeployPlanRepository> deployPlanRepositoryProvider) {
        this.resourceService = resourceService;
        this.deployPlanRepository = deployPlanRepositoryProvider.getIfAvailable();
        if (this.deployPlanRepository == null) {
            seed();
        }
    }

    public DeployPlanService(ResourceService resourceService) {
        this.resourceService = resourceService;
        this.deployPlanRepository = null;
        seed();
    }

    public List<DeployPlanEntity> listPlans() {
        if (useJdbc()) {
            return deployPlanRepository.findAllActivePlans();
        }
        return plans.values().stream()
                .filter(plan -> !isDeleted(plan))
                .sorted(Comparator.comparing(DeployPlanEntity::createdAt).reversed())
                .toList();
    }

    public DeployPlanEntity getPlan(Long id) {
        if (useJdbc()) {
            return deployPlanRepository.findActivePlanById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "部署方案不存在"));
        }
        DeployPlanEntity plan = plans.get(id);
        if (plan == null || isDeleted(plan)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部署方案不存在");
        }
        return plan;
    }

    @Transactional
    public DeployPlanEntity createPlan(CreateDeployPlanRequest request) {
        ensurePlanCodeUnique(request.planCode());
        if (useJdbc()) {
            return deployPlanRepository.insertPlan(request, ENABLED);
        }
        DeployPlanEntity plan = new DeployPlanEntity(
                planIdSequence.getAndIncrement(),
                request.planCode(),
                request.planName(),
                null,
                ENABLED,
                LocalDateTime.now()
        );
        plans.put(plan.id(), plan);
        return plan;
    }

    public List<DeployPlanVersionEntity> listVersions(Long planId) {
        getPlan(planId);
        if (useJdbc()) {
            return deployPlanRepository.findVersionsByPlanId(planId);
        }
        return versions.values().stream()
                .filter(version -> version.planId().equals(planId))
                .sorted(Comparator.comparing(DeployPlanVersionEntity::createdAt).reversed())
                .map(this::normalizeVersion)
                .toList();
    }

    @Transactional
    public DeployPlanVersionEntity createVersion(Long planId, CreateDeployPlanVersionRequest request) {
        getPlan(planId);
        ensureVersionUnique(planId, request.version());
        if (useJdbc()) {
            return deployPlanRepository.insertVersion(planId, request);
        }
        DeployPlanVersionEntity version = new DeployPlanVersionEntity(
                versionIdSequence.getAndIncrement(),
                planId,
                request.version(),
                DeployPlanVersionStatus.DRAFT,
                true,
                LocalDateTime.now()
        );
        versions.put(version.id(), version);
        return version;
    }

    @Transactional
    public DeployPlanVersionEntity publishVersion(Long versionId) {
        DeployPlanVersionEntity current = getVersion(versionId);
        if (current.status() != DeployPlanVersionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有草稿版本才能发布");
        }
        if (useJdbc()) {
            return deployPlanRepository.publishVersion(current);
        }
        DeployPlanVersionEntity published = new DeployPlanVersionEntity(
                current.id(),
                current.planId(),
                current.version(),
                DeployPlanVersionStatus.PUBLISHED,
                false,
                current.createdAt()
        );
        versions.put(versionId, published);
        DeployPlanEntity plan = getPlan(current.planId());
        plans.put(plan.id(), new DeployPlanEntity(
                plan.id(),
                plan.planCode(),
                plan.planName(),
                versionId,
                plan.status(),
                plan.createdAt()
        ));
        return published;
    }

    public List<DeployComponentEntity> listComponents(Long versionId) {
        DeployPlanVersionEntity version = getVersion(versionId);
        if (useJdbc()) {
            return deployPlanRepository.findComponentsByVersionId(version.id());
        }
        return components.values().stream()
                .filter(component -> component.planVersionId().equals(version.id()))
                .sorted(Comparator.comparingInt(DeployComponentEntity::deployOrder))
                .toList();
    }

    @Transactional
    public DeployComponentEntity createComponent(Long versionId, CreateDeployComponentRequest request) {
        DeployPlanVersionEntity version = getVersion(versionId);
        if (!version.canEdit()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "已发布版本不能修改组件");
        }
        resourceService.requireEnabledVersion(request.resourceVersionId());
        if (useJdbc()) {
            return deployPlanRepository.insertComponent(versionId, request);
        }
        DeployComponentEntity component = new DeployComponentEntity(
                componentIdSequence.getAndIncrement(),
                versionId,
                request.componentName(),
                request.componentType(),
                request.resourceVersionId(),
                request.deployOrder(),
                request.configTemplate(),
                request.healthCheck()
        );
        components.put(component.id(), component);
        return component;
    }

    public DeployPlanVersionEntity getVersion(Long versionId) {
        if (useJdbc()) {
            return deployPlanRepository.findVersionById(versionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "部署方案版本不存在"));
        }
        DeployPlanVersionEntity version = versions.get(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部署方案版本不存在");
        }
        return normalizeVersion(version);
    }

    private DeployPlanVersionEntity normalizeVersion(DeployPlanVersionEntity version) {
        return new DeployPlanVersionEntity(
                version.id(),
                version.planId(),
                version.version(),
                version.status(),
                version.canEdit(),
                version.createdAt()
        );
    }

    private void ensurePlanCodeUnique(String planCode) {
        boolean exists = useJdbc()
                ? deployPlanRepository.existsPlanCode(planCode)
                : plans.values().stream().anyMatch(plan -> plan.planCode().equals(planCode));
        if (exists) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "部署方案编码已存在");
        }
    }

    private void ensureVersionUnique(Long planId, String version) {
        boolean exists = useJdbc()
                ? deployPlanRepository.existsVersion(planId, version)
                : versions.values().stream().anyMatch(item -> item.planId().equals(planId) && item.version().equals(version));
        if (exists) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "部署方案版本已存在");
        }
    }

    private boolean isDeleted(DeployPlanEntity plan) {
        return StringUtils.hasText(plan.status()) && DISABLED.equals(plan.status()) && plan.currentVersionId() == null;
    }

    private boolean useJdbc() {
        return deployPlanRepository != null;
    }

    private void seed() {
        LocalDateTime now = LocalDateTime.now();
        DeployPlanEntity plan = new DeployPlanEntity(
                planIdSequence.getAndIncrement(),
                "PLAN-001",
                "标准单机部署方案",
                1L,
                ENABLED,
                now
        );
        plans.put(plan.id(), plan);

        DeployPlanVersionEntity published = new DeployPlanVersionEntity(
                versionIdSequence.getAndIncrement(),
                plan.id(),
                "1.0.0",
                DeployPlanVersionStatus.PUBLISHED,
                false,
                now
        );
        versions.put(published.id(), published);

        DeployPlanVersionEntity draft = new DeployPlanVersionEntity(
                versionIdSequence.getAndIncrement(),
                plan.id(),
                "1.1.0",
                DeployPlanVersionStatus.DRAFT,
                true,
                now
        );
        versions.put(draft.id(), draft);

        components.put(componentIdSequence.getAndIncrement(), new DeployComponentEntity(
                1L,
                published.id(),
                "应用服务",
                "APP",
                1L,
                1,
                "server.port=${app.port}",
                "GET /actuator/health"
        ));
    }
}
