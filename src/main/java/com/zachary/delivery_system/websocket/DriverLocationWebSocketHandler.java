package com.zachary.delivery_system.websocket;

import com.zachary.delivery_system.dto.Location.DriverLocationUpdateMessage;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.security.JwtService;
import com.zachary.delivery_system.service.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverLocationWebSocketHandler extends TextWebSocketHandler {

    private static final Set<String> MAP_ROLES =
            Set.of("DISPATCHER", "ADMIN");

    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final AppUserService appUserService;

    // A session is added only after its first message supplies a valid JWT.
    private final Map<String, WebSocketSession> authenticatedSessions =
            new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) throws Exception {
        if (authenticatedSessions.containsKey(session.getId())) {
            // This connection is server-push only after authentication.
            return;
        }

        AuthenticationMessage authenticationMessage;

        try {
            authenticationMessage = objectMapper.readValue(
                    message.getPayload(),
                    AuthenticationMessage.class
            );
        } catch (RuntimeException exception) {
            reject(session, "The authentication message is invalid");
            return;
        }

        if (!"AUTH".equals(authenticationMessage.type())
                || authenticationMessage.token() == null) {
            reject(session, "Authentication is required");
            return;
        }

        AppUser user = authenticate(authenticationMessage.token());

        if (user == null) {
            reject(session, "The token is invalid or expired");
            return;
        }

        String role = user.getRole() == null
                ? ""
                : user.getRole().toUpperCase(Locale.ROOT);

        if (!MAP_ROLES.contains(role)) {
            reject(
                    session,
                    "Only dispatchers and administrators can view live locations"
            );
            return;
        }

        authenticatedSessions.put(session.getId(), session);
        sendJson(session, Map.of("type", "AUTHENTICATED"));
    }

    public void broadcastLocation(DriverLocationUpdateMessage location) {
        String payload;

        try {
            payload = objectMapper.writeValueAsString(
                    Map.of(
                            "type", "LOCATION_UPDATE",
                            "payload", location
                    )
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Could not serialize a location WebSocket message",
                    exception
            );
        }

        TextMessage message = new TextMessage(payload);

        authenticatedSessions.forEach((sessionId, session) -> {
            if (!session.isOpen()) {
                authenticatedSessions.remove(sessionId);
                return;
            }

            try {
                // Spring WebSocket sessions do not allow concurrent sends.
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException exception) {
                authenticatedSessions.remove(sessionId);
                log.warn(
                        "Could not push a driver location to WebSocket session {}",
                        sessionId,
                        exception
                );
            }
        });
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        authenticatedSessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) throws Exception {
        authenticatedSessions.remove(session.getId());

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private AppUser authenticate(String token) {
        try {
            String username = jwtService.extractUsername(token);
            AppUser user = appUserService.lambdaQuery()
                    .eq(AppUser::getUsername, username)
                    .one();

            return user != null && jwtService.isTokenValid(token, username)
                    ? user
                    : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void reject(WebSocketSession session, String reason)
            throws IOException {
        sendJson(
                session,
                Map.of(
                        "type", "AUTHENTICATION_ERROR",
                        "message", reason
                )
        );
        session.close(CloseStatus.POLICY_VIOLATION);
    }

    private void sendJson(WebSocketSession session, Object value)
            throws IOException {
        String payload = objectMapper.writeValueAsString(value);

        synchronized (session) {
            session.sendMessage(new TextMessage(payload));
        }
    }

    private record AuthenticationMessage(String type, String token) {
    }
}
