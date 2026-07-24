package com.example.delivery.agent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 执行日志实时推送（SSE）。按任务 ID 维护订阅者，任务上报状态/日志时广播。
 * 私有化内网场景下前端可实时观看部署进度，无需轮询。
 */
@Service
public class AgentLogStreamService {
    /** 每个任务的活跃 SSE 连接列表。 */
    private final Map<Long, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    /** 订阅某任务的日志流。连接保持到超时或客户端断开。 */
    public SseEmitter subscribe(Long taskId) {
        // 30 分钟超时，足够单次部署
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        List<SseEmitter> list = subscribers.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        emitter.onCompletion(() -> remove(taskId, emitter));
        emitter.onTimeout(() -> remove(taskId, emitter));
        emitter.onError(e -> remove(taskId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("stream opened for task " + taskId));
        } catch (IOException e) {
            remove(taskId, emitter);
        }
        return emitter;
    }

    /** 向订阅该任务的所有连接推送一条日志事件。 */
    public void publish(Long taskId, AgentLogEvent event) {
        List<SseEmitter> list = subscribers.get(taskId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("log").data(event));
                if (event.finished()) {
                    emitter.complete();
                    remove(taskId, emitter);
                }
            } catch (IOException | IllegalStateException e) {
                remove(taskId, emitter);
            }
        }
    }

    private void remove(Long taskId, SseEmitter emitter) {
        List<SseEmitter> list = subscribers.get(taskId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                subscribers.remove(taskId);
            }
        }
    }

    /** 当前某任务的订阅连接数（测试/诊断用）。 */
    public int subscriberCount(Long taskId) {
        List<SseEmitter> list = subscribers.get(taskId);
        return list == null ? 0 : list.size();
    }

    /**
     * 推送的日志事件。
     *
     * @param taskId    任务 ID
     * @param stepCode  步骤编码
     * @param stepName  步骤名
     * @param status    任务/步骤状态
     * @param logLevel  日志级别
     * @param content   日志内容
     * @param finished  是否为终态（终态推送后关闭连接）
     */
    public record AgentLogEvent(
            Long taskId,
            String stepCode,
            String stepName,
            String status,
            String logLevel,
            String content,
            boolean finished
    ) {}
}
