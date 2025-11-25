package ai.mymanus.service;

import ai.mymanus.model.Notification;
import ai.mymanus.repository.NotificationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing user notifications
 * Supports both in-app and browser notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter notificationsSent;
    private Counter notificationsRead;

    /**
     * Initialize metrics
     */
    @jakarta.annotation.PostConstruct
    public void initMetrics() {
        notificationsSent = Counter.builder("notifications.sent")
            .description("Total notifications sent")
            .tag("type", "all")
            .register(meterRegistry);

        notificationsRead = Counter.builder("notifications.read")
            .description("Total notifications marked as read")
            .register(meterRegistry);
    }

    /**
     * Send a notification to a user
     */
    @Transactional
    public Notification sendNotification(Notification notification) {
        log.info("📬 Sending notification: {} to user: {} (session: {})",
            notification.getType(), notification.getUserId(), notification.getSessionId());

        // Save to database
        notification = notificationRepository.save(notification);

        // Send via WebSocket for real-time delivery
        messagingTemplate.convertAndSend(
            "/topic/notifications/" + notification.getSessionId(),
            notification
        );

        // Also send to user topic if userId is set
        if (notification.getUserId() != null) {
            messagingTemplate.convertAndSend(
                "/user/" + notification.getUserId() + "/notifications",
                notification
            );
        }

        // Increment metrics
        notificationsSent.increment();

        Counter.builder("notifications.sent.by.type")
            .tag("type", notification.getType().toString())
            .register(meterRegistry)
            .increment();

        return notification;
    }

    /**
     * Send task completion notification
     */
    public Notification notifyTaskCompleted(String sessionId, String userId, String taskDescription) {
        return sendNotification(Notification.builder()
            .sessionId(sessionId)
            .userId(userId)
            .type(Notification.NotificationType.TASK_COMPLETED)
            .title("Task Completed")
            .message(taskDescription)
            .priority(Notification.Priority.NORMAL)
            .browserNotification(true)
            .actionUrl("/sessions/" + sessionId)
            .build());
    }

    /**
     * Send task failure notification
     */
    public Notification notifyTaskFailed(String sessionId, String userId, String errorMessage) {
        return sendNotification(Notification.builder()
            .sessionId(sessionId)
            .userId(userId)
            .type(Notification.NotificationType.TASK_FAILED)
            .title("Task Failed")
            .message(errorMessage)
            .priority(Notification.Priority.HIGH)
            .browserNotification(true)
            .actionUrl("/sessions/" + sessionId)
            .build());
    }

    /**
     * Send agent waiting notification
     */
    public Notification notifyAgentWaiting(String sessionId, String userId, String question) {
        return sendNotification(Notification.builder()
            .sessionId(sessionId)
            .userId(userId)
            .type(Notification.NotificationType.AGENT_WAITING)
            .title("Agent Waiting for Input")
            .message(question)
            .priority(Notification.Priority.HIGH)
            .browserNotification(true)
            .actionUrl("/sessions/" + sessionId)
            .build());
    }

    /**
     * Send plan adjusted notification
     */
    public Notification notifyPlanAdjusted(String sessionId, String userId, String adjustmentDescription) {
        return sendNotification(Notification.builder()
            .sessionId(sessionId)
            .userId(userId)
            .type(Notification.NotificationType.PLAN_ADJUSTED)
            .title("Plan Adjusted")
            .message(adjustmentDescription)
            .priority(Notification.Priority.NORMAL)
            .browserNotification(false)  // In-app only
            .actionUrl("/sessions/" + sessionId)
            .build());
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread count
     */
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
            notificationsRead.increment();
            log.debug("Marked notification {} as read", notificationId);
        });
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public int markAllAsRead(String userId) {
        int count = notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        log.info("Marked {} notifications as read for user: {}", count, userId);
        return count;
    }

    /**
     * Delete old read notifications (scheduled cleanup)
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deleted = notificationRepository.deleteOldReadNotifications(cutoffDate);
        log.info("Cleaned up {} old notifications", deleted);
    }
}
