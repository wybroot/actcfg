package com.example.delivery.snapshot;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.deploy.DeployComponentEntity;
import com.example.delivery.deploy.DeployPlanEntity;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.deploy.DeployPlanVersionEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户配置快照服务：绑定部署方案版本时深拷贝方案组件为独立副本，后续在快照上编辑与源方案解耦。
 */
@Service
public class SnapshotService {

    private final DeployPlanService deployPlanService;
    private final SnapshotRepository snapshotRepository;

    private final AtomicLong snapshotIdSeq = new AtomicLong(1);
    private final AtomicLong componentIdSeq = new AtomicLong(1);
    private final Map<Long, SnapshotEntity> snapshots = new ConcurrentHashMap<>();
    private final Map<Long, SnapshotComponentEntity> components = new ConcurrentHashMap<>();

    @Autowired
    public SnapshotService(DeployPlanService deployPlanService,
                           ObjectProvider<SnapshotRepository> snapshotRepositoryProvider) {
        this.deployPlanService = deployPlanService;
        this.snapshotRepository = snapshotRepositoryProvider.getIfAvailable();
    }

    /** 测试用构造：内存模式。 */
    public SnapshotService(DeployPlanService deployPlanService) {
        this.deployPlanService = deployPlanService;
        this.snapshotRepository = null;
    }

    private boolean useJdbc() {
        return snapshotRepository != null;
    }

    /**
     * 绑定时调用：深拷贝方案版本的组件生成新快照；同环境已有 ACTIVE 快照则先停用。
     */
    @Transactional
    public SnapshotEntity createSnapshot(Long customerId, Long environmentId, Long planVersionId) {
        DeployPlanVersionEntity version = deployPlanService.getVersion(planVersionId);
        DeployPlanEntity plan = deployPlanService.getPlan(version.planId());
        List<DeployComponentEntity> planComponents = deployPlanService.listComponents(planVersionId);

        if (useJdbc()) {
            snapshotRepository.deactivateByEnvironment(environmentId);
            SnapshotEntity saved = snapshotRepository.insertSnapshot(new SnapshotEntity(
                    null, customerId, environmentId, planVersionId,
                    plan.planName(), version.version(), "ACTIVE", null));
            for (DeployComponentEntity c : planComponents) {
                snapshotRepository.insertComponent(saved.id(), toSnapshotComponent(null, saved.id(), c));
            }
            return saved;
        }

        // 内存：停用旧快照
        snapshots.values().stream()
                .filter(s -> s.environmentId().equals(environmentId) && "ACTIVE".equals(s.status()))
                .toList()
                .forEach(s -> snapshots.put(s.id(), replaceStatus(s, "REPLACED")));

        long snapshotId = snapshotIdSeq.getAndIncrement();
        SnapshotEntity snapshot = new SnapshotEntity(
                snapshotId, customerId, environmentId, planVersionId,
                plan.planName(), version.version(), "ACTIVE", LocalDateTime.now());
        snapshots.put(snapshotId, snapshot);
        for (DeployComponentEntity c : planComponents) {
            long cid = componentIdSeq.getAndIncrement();
            components.put(cid, toSnapshotComponent(cid, snapshotId, c));
        }
        return snapshot;
    }

    public SnapshotEntity getByEnvironment(Long environmentId) {
        if (useJdbc()) {
            return snapshotRepository.findByEnvironmentId(environmentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "该环境尚未生成配置快照"));
        }
        return snapshots.values().stream()
                .filter(s -> s.environmentId().equals(environmentId) && "ACTIVE".equals(s.status()))
                .max(Comparator.comparing(SnapshotEntity::id))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "该环境尚未生成配置快照"));
    }

    /** 环境无快照时返回 null（不抛异常），供发布中心回退到方案版本。 */
    public SnapshotEntity findByEnvironmentOrNull(Long environmentId) {
        try {
            return getByEnvironment(environmentId);
        } catch (BusinessException e) {
            return null;
        }
    }

    public List<SnapshotComponentEntity> listComponents(Long snapshotId) {
        if (useJdbc()) {
            return snapshotRepository.findComponents(snapshotId);
        }
        return components.values().stream()
                .filter(c -> c.snapshotId().equals(snapshotId))
                .sorted(Comparator.comparingInt(SnapshotComponentEntity::deployOrder))
                .toList();
    }

    @Transactional
    public SnapshotComponentEntity updateComponentConfig(Long componentId, String configTemplate) {
        if (useJdbc()) {
            SnapshotComponentEntity current = snapshotRepository.findComponentById(componentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "快照组件不存在"));
            snapshotRepository.updateComponentConfig(componentId, configTemplate);
            return snapshotRepository.findComponentById(componentId).orElse(current);
        }
        SnapshotComponentEntity current = components.get(componentId);
        if (current == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "快照组件不存在");
        }
        SnapshotComponentEntity updated = new SnapshotComponentEntity(
                current.id(), current.snapshotId(), current.componentName(), current.componentType(),
                current.resourceVersionId(), current.deployOrder(), configTemplate, current.healthCheck());
        components.put(componentId, updated);
        return updated;
    }

    private SnapshotComponentEntity toSnapshotComponent(Long id, Long snapshotId, DeployComponentEntity c) {
        return new SnapshotComponentEntity(
                id, snapshotId, c.componentName(), c.componentType(),
                c.resourceVersionId(), c.deployOrder(), c.configTemplate(), c.healthCheck());
    }

    private SnapshotEntity replaceStatus(SnapshotEntity s, String status) {
        return new SnapshotEntity(s.id(), s.customerId(), s.environmentId(), s.sourcePlanVersionId(),
                s.planName(), s.versionLabel(), status, s.createdAt());
    }

    // 供发布中心使用：返回 List，便于外部无快照时判空
    public List<SnapshotComponentEntity> listComponentsSafe(Long snapshotId) {
        return snapshotId == null ? new ArrayList<>() : listComponents(snapshotId);
    }
}
