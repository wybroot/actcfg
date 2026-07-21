package com.example.delivery.agent.executor;

/**
 * 单个部署执行步骤。
 *
 * @param order    执行顺序（从 1 递增）
 * @param stepCode 步骤编码（与 agent 上报 stepCode 对齐，如 CHECK_ENV / LOAD_IMAGE_应用服务）
 * @param stepName 中文步骤名
 * @param type     步骤类型
 * @param target   操作目标（镜像坐标 / 制品文件名 / 组件名等）
 * @param detail   附加说明（健康检查命令、配置文件路径等）
 */
public record DeployStep(
        int order,
        String stepCode,
        String stepName,
        DeployStepType type,
        String target,
        String detail
) {}
