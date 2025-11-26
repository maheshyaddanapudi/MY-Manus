package ai.mymanus.controller;

import ai.mymanus.model.Notification;
import ai.mymanus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for notification management
 *
 * User ID Handling:
 * - Development Mode (auth.enabled=false): Uses "default-user" or query param
 * - Production Mode (auth.enabled=true): Extracts from Security Context
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @Value("${auth.enabled:false}")
    private boolean authEnabled;

    /**
     * Get all notifications for current user
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(required = false) String userId) {

        String effectiveUserId = getUserId(userId);
        List<Notification> notifications = notificationService.getUserNotifications(effectiveUserId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @RequestParam(required = false) String userId) {

        String effectiveUserId = getUserId(userId);
        List<Notification> notifications = notificationService.getUnreadNotifications(effectiveUserId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestParam(required = false) String userId) {

        String effectiveUserId = getUserId(userId);
        long count = notificationService.getUnreadCount(effectiveUserId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark notification as read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @RequestParam(required = false) String userId) {

        String effectiveUserId = getUserId(userId);
        int count = notificationService.markAllAsRead(effectiveUserId);
        return ResponseEntity.ok(Map.of("markedCount", count));
    }

    /**
     * Send test notification (for development)
     */
    @PostMapping("/test")
    public ResponseEntity<Notification> sendTestNotification(
            @RequestParam String sessionId,
            @RequestParam(required = false) String userId) {

        String effectiveUserId = getUserId(userId);

        Notification notification = notificationService.sendNotification(
            Notification.builder()
                .sessionId(sessionId)
                .userId(effectiveUserId)
                .type(Notification.NotificationType.INFO)
                .title("Test Notification")
                .message("This is a test notification from MY-Manus")
                .priority(Notification.Priority.NORMAL)
                .browserNotification(true)
                .build()
        );

        return ResponseEntity.ok(notification);
    }

    /**
     * Get effective user ID based on authentication mode
     *
     * Development Mode (auth.enabled=false):
     * - Returns query param userId if provided
     * - Otherwise returns "default-user"
     *
     * Production Mode (auth.enabled=true):
     * - Extracts userId from SecurityContext
     * - Falls back to "default-user" if not authenticated
     */
    private String getUserId(String requestUserId) {
        // In production mode with auth enabled, extract from Security Context
        if (authEnabled) {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null && authentication.isAuthenticated()
                        && !"anonymousUser".equals(authentication.getPrincipal())) {

                    // Extract username from authentication
                    String userId = authentication.getName();
                    log.debug("Extracted userId from Security Context: {}", userId);
                    return userId;
                }
            } catch (Exception e) {
                log.warn("Failed to extract userId from Security Context: {}", e.getMessage());
            }
        }

        // Development mode: use query param or default
        String effectiveUserId = requestUserId != null ? requestUserId : "default-user";
        log.debug("Using userId: {} (auth.enabled={})", effectiveUserId, authEnabled);

        return effectiveUserId;
    }
}
