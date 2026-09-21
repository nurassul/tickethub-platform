package dev.project.statistic.websockets.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.statistic.api.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsWebSocketHandler extends TextWebSocketHandler {

    private final StatisticsService statisticsService;
    private final ObjectMapper objectMapper;

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("New client connected to WebSockets: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("Client disconnected: {}", session.getId());
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastMetrics() {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            var metrics = Map.of(
                    "totalBookings", statisticsService.getTotalBookings(),
                    "totalTickets", statisticsService.getTotalTickets(),
                    "timestamp", Instant.now().toString()
            );

            String jsonMessage = objectMapper.writeValueAsString(metrics);
            TextMessage message = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("Error while sending metrics", e);
        }
    }


}
