package com.project.performanceTrack.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    // Map of userId -> their active SSE connection
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Called when frontend connects to GET /api/v1/notifications/stream
    public SseEmitter createEmitter(Integer userId) {
        // 30 minute timeout (then frontend reconnects)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // Clean up when connection ends
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        emitters.put(userId, emitter);
        log.debug("SSE connection opened for userId: {}", userId);
        return emitter;
    }

    // Called by NotificationService whenever a notification is saved
    public void sendToUser(Integer userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
            } catch (IOException e) {
                emitters.remove(userId);
                log.debug("SSE connection lost for userId: {}", userId);
            }
        }
    }
}
