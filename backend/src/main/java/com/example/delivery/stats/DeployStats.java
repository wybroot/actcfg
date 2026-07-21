package com.example.delivery.stats;

import java.util.List;

/**
 * 部署成功率统计与失败归因。
 *
 * @param totalTasks      纳入统计的任务总数（已结束：SUCCESS/FAILED/CANCELED/SKIPPED）
 * @param successCount    成功任务数
 * @param failedCount     失败任务数
 * @param canceledCount   取消任务数
 * @param successRate     成功率（成功 / 已结束，保留两位小数；无数据为 0）
 * @param topFailedSteps  失败步骤 Top 排行（含各步骤失败次数）
 * @param topFailReasons  失败原因 Top 排行（含各原因出现次数）
 */
public record DeployStats(
        long totalTasks,
        long successCount,
        long failedCount,
        long canceledCount,
        double successRate,
        List<Attribution> topFailedSteps,
        List<Attribution> topFailReasons
) {
    /** 归因条目：某个失败步骤/原因及其出现次数。 */
    public record Attribution(String label, long count) {}
}
