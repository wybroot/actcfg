package com.example.delivery.stats;

import java.util.Map;

/**
 * Dashboard 概览统计。
 *
 * @param customerCount   客户数
 * @param resourceCount   制品数
 * @param packageCount    部署包数
 * @param agentTaskCount  Agent 任务总数
 * @param taskStatusCounts Agent 任务各状态计数
 */
public record StatsOverview(
        int customerCount,
        int resourceCount,
        int packageCount,
        int agentTaskCount,
        Map<String, Long> taskStatusCounts
) {}
