package ai.mymanus.controller;

import ai.mymanus.model.Notification;
import ai.mymanus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for notification management
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for current user
     * TODO: Extract userId from security context in production
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(required = false) String userId) {

        // For now, use userId from query param
        // In production, get from SecurityContext
        String effectiveUserId = userId != null ? userId : "default-user";

        List<Notification> notifications = notificationService.getUserNotifications(effectiveUserId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @RequestParam(required = false) String userId) {

        String effectiveUserId = userId != null ? userId : "default-user";
        List<Notification> notifications = notificationService.getUnreadNotifications(effectiveUserId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestParam(required = false) String userId) {

        String effectiveUserId = userId != null ? userId : "default-user";
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

        String effectiveUserId = userId != null ? userId : "default-user";
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

        String effectiveUserId = userId != null ? userId : "default-user";

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
}
