package com.example.delivery.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AgentLogStreamServiceTests {

    @Test
    void subscribeRegistersEmitter() {
        AgentLogStreamService svc = new AgentLogStreamService();
        SseEmitter emitter = svc.subscribe(1L);

        assertNotNull(emitter);
        assertEquals(1, svc.subscriberCount(1L));
    }

    @Test
    void publishToTaskWithoutSubscribersIsNoop() {
        AgentLogStreamService svc = new AgentLogStreamService();
        // 无订阅者时推送不应抛异常
        svc.publish(99L, new AgentLogStreamService.AgentLogEvent(
                99L, "DEPLOY", "部署", "RUNNING", "INFO", "进行中", false));
        assertEquals(0, svc.subscriberCount(99L));
    }

    @Test
    void finishedEventCompletesAndClearsSubscribers() {
        AgentLogStreamService svc = new AgentLogStreamService();
        svc.subscribe(2L);
        assertEquals(1, svc.subscriberCount(2L));

        // 终态事件推送后连接关闭，订阅者清空
        svc.publish(2L, new AgentLogStreamService.AgentLogEvent(
                2L, "HEALTH_CHECK", "健康检查", "SUCCESS", "INFO", "完成", true));
        assertEquals(0, svc.subscriberCount(2L));
    }
}
