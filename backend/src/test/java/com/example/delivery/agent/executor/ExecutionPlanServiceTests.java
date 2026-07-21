package com.example.delivery.agent.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExecutionPlanServiceTests {
    private ExecutionPlanService service;

    @BeforeEach
    void setUp() {
        service = new ExecutionPlanService();
    }

    @Test
    void buildPlanOrdersStepsByType() {
        List<ComponentDescriptor> components = List.of(
                new ComponentDescriptor("应用服务", "IMAGE", "harbor/app:1.0", "port=8080", "GET /health", 1),
                new ComponentDescriptor("初始化脚本", "SQL", "init.sql", null, null, 2),
                new ComponentDescriptor("配置包", "CONFIG", "conf.tar", "k=v", null, 3)
        );

        ExecutionPlan plan = service.buildPlan("PKG-TEST-001", components);

        // 首两步固定为环境检测、兼容校验
        assertEquals(DeployStepType.CHECK_ENV, plan.steps().get(0).type());
        assertEquals(DeployStepType.COMPAT_CHECK, plan.steps().get(1).type());
        // 末步为健康检查
        assertEquals(DeployStepType.HEALTH_CHECK, plan.steps().get(plan.steps().size() - 1).type());

        // 含镜像加载与数据库初始化步骤
        assertTrue(plan.steps().stream().anyMatch(s -> s.type() == DeployStepType.LOAD_IMAGE));
        assertTrue(plan.steps().stream().anyMatch(s -> s.type() == DeployStepType.DB_INIT));
        // CONFIG 类组件走部署制品
        assertTrue(plan.steps().stream().anyMatch(s -> s.type() == DeployStepType.DEPLOY_ARTIFACT));
        // 有配置模板的组件生成渲染步骤
        assertTrue(plan.steps().stream().anyMatch(s -> s.type() == DeployStepType.RENDER_CONFIG));

        // order 从 1 连续递增
        for (int i = 0; i < plan.steps().size(); i++) {
            assertEquals(i + 1, plan.steps().get(i).order());
        }
    }

    @Test
    void generatedScriptContainsStepCodesAndIdempotency() {
        List<ComponentDescriptor> components = List.of(
                new ComponentDescriptor("应用服务", "IMAGE", "harbor/app:1.0", null, null, 1)
        );
        ExecutionPlan plan = service.buildPlan("PKG-TEST-002", components);
        String script = service.generateAgentScript(plan);

        assertTrue(script.startsWith("#!/usr/bin/env bash"));
        // 幂等 + 续跑机制
        assertTrue(script.contains(".agent-state"));
        assertTrue(script.contains("done_step"));
        assertTrue(script.contains("execution-report.json"));
        // 各步骤 code 出现在脚本
        for (DeployStep s : plan.steps()) {
            assertTrue(script.contains(s.stepCode()), "脚本应包含步骤 " + s.stepCode());
        }
    }

    @Test
    void renderPlanJsonIsValidShape() {
        ExecutionPlan plan = service.buildPlan("PKG-TEST-003", List.of());
        String json = service.renderPlanJson(plan);

        assertTrue(json.contains("\"packageCode\":\"PKG-TEST-003\""));
        assertTrue(json.contains("\"steps\":["));
        // 空组件也有环境检测+兼容校验+健康检查 3 步
        assertEquals(3, plan.steps().size());
        assertFalse(json.contains("\"type\":\"LOAD_IMAGE\""));
    }
}
