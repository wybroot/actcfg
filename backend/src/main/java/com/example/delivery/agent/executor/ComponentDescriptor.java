package com.example.delivery.agent.executor;

/**
 * 供执行计划推导的组件描述（由部署包解析层填充）。
 *
 * @param componentName 组件名
 * @param resourceType  资源类型（IMAGE / SQL / JAR / SCRIPT / CONFIG / PACKAGE），决定步骤类型
 * @param artifactRef   制品引用：镜像坐标或制品文件名/URL
 * @param configTemplate 渲染前配置模板（可空）
 * @param healthCheck   健康检查命令（可空）
 * @param deployOrder   部署顺序
 */
public record ComponentDescriptor(
        String componentName,
        String resourceType,
        String artifactRef,
        String configTemplate,
        String healthCheck,
        int deployOrder
) {}
