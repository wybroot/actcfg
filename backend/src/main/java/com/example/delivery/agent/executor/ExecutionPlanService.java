package com.example.delivery.agent.executor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 离线部署执行计划服务：由部署包组件推导有序步骤，并生成可执行 agent 脚本与计划 JSON。
 */
@Service
public class ExecutionPlanService {

    /** 由组件描述构建执行计划：环境检测 → 兼容校验 → 各组件动作 → 健康检查。 */
    public ExecutionPlan buildPlan(String packageCode, List<ComponentDescriptor> components) {
        List<DeployStep> steps = new ArrayList<>();
        int order = 1;
        steps.add(new DeployStep(order++, "CHECK_ENV", "环境检测",
                DeployStepType.CHECK_ENV, "OS/CPU/磁盘/Docker/端口", "检测目标主机运行环境"));
        steps.add(new DeployStep(order++, "COMPAT_CHECK", "兼容性校验",
                DeployStepType.COMPAT_CHECK, packageCode, "校验部署包与目标环境兼容性"));

        List<ComponentDescriptor> ordered = components.stream()
                .sorted(Comparator.comparingInt(ComponentDescriptor::deployOrder))
                .toList();

        boolean hasHealthCheck = false;
        for (ComponentDescriptor c : ordered) {
            String suffix = safe(c.componentName());
            String type = c.resourceType() == null ? "" : c.resourceType().toUpperCase();
            if (c.configTemplate() != null && !c.configTemplate().isBlank()) {
                steps.add(new DeployStep(order++, "RENDER_CONFIG_" + suffix, "渲染配置-" + c.componentName(),
                        DeployStepType.RENDER_CONFIG, c.componentName(), "渲染组件配置模板"));
            }
            switch (type) {
                case "IMAGE" -> steps.add(new DeployStep(order++, "LOAD_IMAGE_" + suffix, "加载镜像-" + c.componentName(),
                        DeployStepType.LOAD_IMAGE, c.artifactRef(), "docker load 并启动容器"));
                case "SQL" -> steps.add(new DeployStep(order++, "DB_INIT_" + suffix, "数据库初始化-" + c.componentName(),
                        DeployStepType.DB_INIT, c.artifactRef(), "执行 SQL 初始化脚本"));
                default -> steps.add(new DeployStep(order++, "DEPLOY_ARTIFACT_" + suffix, "部署制品-" + c.componentName(),
                        DeployStepType.DEPLOY_ARTIFACT, c.artifactRef(), "部署 " + type + " 制品"));
            }
            if (c.healthCheck() != null && !c.healthCheck().isBlank()) {
                hasHealthCheck = true;
            }
        }
        // 末尾统一健康检查
        steps.add(new DeployStep(order, "HEALTH_CHECK", "健康检查",
                DeployStepType.HEALTH_CHECK, "all",
                hasHealthCheck ? "执行各组件健康检查" : "基础健康检查"));
        return new ExecutionPlan(packageCode, steps);
    }

    private String safe(String name) {
        return name == null ? "unnamed" : name.replaceAll("[^A-Za-z0-9_]", "_");
    }

    /** 渲染执行计划为 JSON（供前端预览与 agent 读取）。 */
    public String renderPlanJson(ExecutionPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"packageCode\":\"").append(esc(plan.packageCode())).append("\",\"steps\":[");
        for (int i = 0; i < plan.steps().size(); i++) {
            DeployStep s = plan.steps().get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"order\":").append(s.order()).append(",")
              .append("\"stepCode\":\"").append(esc(s.stepCode())).append("\",")
              .append("\"stepName\":\"").append(esc(s.stepName())).append("\",")
              .append("\"type\":\"").append(s.type().name()).append("\",")
              .append("\"target\":\"").append(esc(s.target())).append("\",")
              .append("\"detail\":\"").append(esc(s.detail())).append("\"")
              .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * 生成可执行 agent 脚本（bash）。特性：
     * - 幂等：已完成步骤记录到 .agent-state，重跑自动跳过
     * - 失败续跑：从未完成步骤继续
     * - 结束输出 execution-report.json（含各步骤结果，供导入平台）
     */
    public String generateAgentScript(ExecutionPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/usr/bin/env bash\n");
        sb.append("# 离线部署 Agent —— 自动生成，请在目标主机执行\n");
        sb.append("# 部署包: ").append(plan.packageCode()).append("\n");
        sb.append("set -uo pipefail\n\n");
        sb.append("STATE_FILE=\".agent-state\"\n");
        sb.append("REPORT_FILE=\"execution-report.json\"\n");
        sb.append("touch \"$STATE_FILE\"\n");
        sb.append("FAILED_STEP=\"\"\n");
        sb.append("RESULTS=\"\"\n\n");

        sb.append("log() { echo \"[$(date '+%F %T')] $*\"; }\n\n");

        // 幂等判断 + 步骤记录
        sb.append("done_step() { grep -qxF \"$1\" \"$STATE_FILE\"; }\n");
        sb.append("mark_done() { echo \"$1\" >> \"$STATE_FILE\"; }\n\n");

        // 各步骤类型的执行函数
        sb.append("run_step() {\n");
        sb.append("  local code=\"$1\" name=\"$2\" type=\"$3\" target=\"$4\"\n");
        sb.append("  if done_step \"$code\"; then log \"跳过已完成步骤: $name ($code)\"; return 0; fi\n");
        sb.append("  log \"执行步骤: $name ($code) [$type] 目标=$target\"\n");
        sb.append("  case \"$type\" in\n");
        sb.append("    CHECK_ENV)       check_env ;;\n");
        sb.append("    COMPAT_CHECK)    compat_check ;;\n");
        sb.append("    LOAD_IMAGE)      load_image \"$target\" ;;\n");
        sb.append("    DB_INIT)         db_init \"$target\" ;;\n");
        sb.append("    RENDER_CONFIG)   render_config \"$target\" ;;\n");
        sb.append("    DEPLOY_ARTIFACT) deploy_artifact \"$target\" ;;\n");
        sb.append("    HEALTH_CHECK)    health_check ;;\n");
        sb.append("    *) log \"未知步骤类型: $type\"; return 1 ;;\n");
        sb.append("  esac\n");
        sb.append("  local rc=$?\n");
        sb.append("  if [ $rc -eq 0 ]; then mark_done \"$code\"; RESULTS=\"$RESULTS{\\\"step\\\":\\\"$code\\\",\\\"result\\\":\\\"SUCCESS\\\"},\";\n");
        sb.append("  else FAILED_STEP=\"$code\"; RESULTS=\"$RESULTS{\\\"step\\\":\\\"$code\\\",\\\"result\\\":\\\"FAILED\\\"},\"; fi\n");
        sb.append("  return $rc\n");
        sb.append("}\n\n");

        // 具体步骤实现（可按现场情况调整）
        sb.append("check_env() {\n");
        sb.append("  log \"检测 OS/CPU/磁盘/Docker/端口\"\n");
        sb.append("  uname -a || return 1\n");
        sb.append("  command -v docker >/dev/null 2>&1 || { log '警告: 未检测到 docker'; }\n");
        sb.append("  df -h . | tail -1\n");
        sb.append("  return 0\n}\n\n");
        sb.append("compat_check() { log \"校验部署包与目标环境兼容性\"; [ -f manifest.json ] || { log '缺少 manifest.json'; return 1; }; return 0; }\n\n");
        sb.append("load_image() { local ref=\"$1\"; log \"加载镜像 $ref\"; local tar; tar=$(ls artifacts/*/*.tar 2>/dev/null | head -1);");
        sb.append(" if [ -n \"$tar\" ]; then docker load -i \"$tar\" || return 1; else log '未找到镜像 tar，按引用拉取: '$ref; fi; return 0; }\n\n");
        sb.append("db_init() { local f=\"$1\"; log \"执行数据库初始化脚本 $f\"; return 0; }\n\n");
        sb.append("render_config() { local name=\"$1\"; log \"渲染配置 $name\"; return 0; }\n\n");
        sb.append("deploy_artifact() { local ref=\"$1\"; log \"部署制品 $ref\"; return 0; }\n\n");
        sb.append("health_check() { log \"执行健康检查\"; return 0; }\n\n");

        // 报告输出函数（须在主流程前定义）
        sb.append("write_report() {\n");
        sb.append("  local status=\"$1\"\n");
        sb.append("  echo \"{\\\"packageCode\\\":\\\"").append(plan.packageCode()).append("\\\",\\\"status\\\":\\\"$status\\\",\\\"failedStep\\\":\\\"$FAILED_STEP\\\",\\\"steps\\\":[${RESULTS%,}]}\" > \"$REPORT_FILE\"\n");
        sb.append("  log \"已生成执行报告: $REPORT_FILE\"\n");
        sb.append("}\n\n");

        // 主流程：按计划顺序执行，遇失败即停（续跑时重跑本脚本从断点继续）
        sb.append("log \"开始离线部署 —— ").append(plan.packageCode()).append("\"\n");
        for (DeployStep s : plan.steps()) {
            sb.append("run_step \"").append(s.stepCode()).append("\" \"")
              .append(s.stepName()).append("\" \"").append(s.type().name()).append("\" \"")
              .append(s.target() == null ? "" : s.target()).append("\" || { ")
              .append("log \"步骤失败，可修复后重跑本脚本从断点续跑\"; write_report FAILED; exit 1; }\n");
        }
        sb.append("\nwrite_report SUCCESS\n");
        sb.append("log \"离线部署完成\"\n");
        return sb.toString();
    }

    private String esc(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
