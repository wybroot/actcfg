package com.example.delivery.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.repository.ResourceService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SnapshotServiceTests {
    private SnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        DeployPlanService deployPlanService = new DeployPlanService(new ResourceService());
        snapshotService = new SnapshotService(deployPlanService);
    }

    @Test
    void createSnapshotDeepCopiesComponents() {
        // 方案版本 1 (PLAN-001 已发布版本) 含一个组件
        SnapshotEntity snapshot = snapshotService.createSnapshot(1L, 1L, 1L);

        assertEquals("ACTIVE", snapshot.status());
        List<SnapshotComponentEntity> components = snapshotService.listComponents(snapshot.id());
        assertFalse(components.isEmpty());
        assertEquals("应用服务", components.get(0).componentName());
    }

    @Test
    void editingSnapshotConfigDoesNotAffectOtherEnvironment() {
        // 两个环境各自基于同一方案版本生成独立快照
        SnapshotEntity snapA = snapshotService.createSnapshot(1L, 100L, 1L);
        SnapshotEntity snapB = snapshotService.createSnapshot(2L, 200L, 1L);
        assertNotEquals(snapA.id(), snapB.id());

        Long compA = snapshotService.listComponents(snapA.id()).get(0).id();

        // 改 A 的配置
        snapshotService.updateComponentConfig(compA, "server.port=9999");

        // A 变了，B 不受影响
        assertEquals("server.port=9999", snapshotService.listComponents(snapA.id()).get(0).configTemplate());
        assertNotEquals("server.port=9999", snapshotService.listComponents(snapB.id()).get(0).configTemplate());
    }

    @Test
    void rebindingReplacesSnapshot() {
        SnapshotEntity first = snapshotService.createSnapshot(1L, 300L, 1L);
        SnapshotEntity second = snapshotService.createSnapshot(1L, 300L, 1L);

        // 同环境重绑，getByEnvironment 返回最新 ACTIVE 快照
        SnapshotEntity active = snapshotService.getByEnvironment(300L);
        assertEquals(second.id(), active.id());
        assertNotEquals(first.id(), active.id());
    }
}
