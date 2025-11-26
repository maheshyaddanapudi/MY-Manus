/**
 * Notification service for managing browser and in-app notifications
 */

export interface Notification {
  id: number;
  userId: string;
  sessionId: string;
  type: 'TASK_COMPLETED' | 'TASK_FAILED' | 'AGENT_WAITING' | 'PLAN_ADJUSTED' | 'TOOL_ERROR' | 'SYSTEM' | 'INFO';
  title: string;
  message: string;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  isRead: boolean;
  browserNotification: boolean;
  actionUrl?: string;
  createdAt: string;
  readAt?: string;
}

class NotificationService {
  private permission: NotificationPermission = 'default';
  private userId: string = 'default-user'; // TODO: Get from auth context

  /**
   * Initialize notification service
   */
  async initialize(): Promise<void> {
    // Check if browser supports notifications
    if (!('Notification' in window)) {
      console.warn('This browser does not support desktop notifications');
      return;
    }

    // Request permission if not granted
    if (Notification.permission === 'default') {
      this.permission = await Notification.requestPermission();
    } else {
      this.permission = Notification.permission;
    }

    console.log('Notification permission:', this.permission);
  }

  /**
   * Show browser notification
   */
  async showBrowserNotification(notification: Notification): Promise<void> {
    if (!notification.browserNotification) {
      return;
    }

    if (this.permission !== 'granted') {
      console.warn('Notification permission not granted');
      return;
    }

    try {
      const browserNotif = new Notification(notification.title, {
        body: notification.message,
        icon: '/logo192.png', // Update with your app icon
        badge: '/logo192.png',
        tag: `notification-${notification.id}`,
        requireInteraction: notification.priority === 'HIGH' || notification.priority === 'URGENT',
        silent: notification.priority === 'LOW',
      });

      // Handle click
      browserNotif.onclick = () => {
        window.focus();
        if (notification.actionUrl) {
          window.location.href = notification.actionUrl;
        }
        browserNotif.close();
        this.markAsRead(notification.id);
      };

      // Auto-close after 10 seconds for normal priority
      if (notification.priority === 'NORMAL' || notification.priority === 'LOW') {
        setTimeout(() => browserNotif.close(), 10000);
      }
    } catch (error) {
      console.error('Failed to show browser notification:', error);
    }
  }

  /**
   * Get all notifications
   */
  async getNotifications(): Promise<Notification[]> {
    try {
      const response = await fetch(`/api/notifications?userId=${this.userId}`);
      if (!response.ok) throw new Error('Failed to fetch notifications');
      return await response.json();
    } catch (error) {
      console.error('Error fetching notifications:', error);
      return [];
    }
  }

  /**
   * Get unread notifications
   */
  async getUnreadNotifications(): Promise<Notification[]> {
    try {
      const response = await fetch(`/api/notifications/unread?userId=${this.userId}`);
      if (!response.ok) throw new Error('Failed to fetch unread notifications');
      return await response.json();
    } catch (error) {
      console.error('Error fetching unread notifications:', error);
      return [];
    }
  }

  /**
   * Get unread count
   */
  async getUnreadCount(): Promise<number> {
    try {
      const response = await fetch(`/api/notifications/unread/count?userId=${this.userId}`);
      if (!response.ok) throw new Error('Failed to fetch unread count');
      const data = await response.json();
      return data.count;
    } catch (error) {
      console.error('Error fetching unread count:', error);
      return 0;
    }
  }

  /**
   * Mark notification as read
   */
  async markAsRead(notificationId: number): Promise<void> {
    try {
      await fetch(`/api/notifications/${notificationId}/read`, {
        method: 'POST',
      });
    } catch (error) {
      console.error('Error marking notification as read:', error);
    }
  }

  /**
   * Mark all notifications as read
   */
  async markAllAsRead(): Promise<number> {
    try {
      const response = await fetch(`/api/notifications/read-all?userId=${this.userId}`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error('Failed to mark all as read');
      const data = await response.json();
      return data.markedCount;
    } catch (error) {
      console.error('Error marking all as read:', error);
      return 0;
    }
  }

  /**
   * Set user ID (for multi-user support)
   */
  setUserId(userId: string): void {
    this.userId = userId;
  }

  /**
   * Play notification sound
   */
  playSound(priority: Notification['priority']): void {
    // Create audio context and play beep
    // Different sounds for different priorities
    const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);

    // Set frequency based on priority
    switch (priority) {
      case 'URGENT':
        oscillator.frequency.value = 800;
        gainNode.gain.value = 0.3;
        break;
      case 'HIGH':
        oscillator.frequency.value = 600;
        gainNode.gain.value = 0.2;
        break;
      default:
        oscillator.frequency.value = 400;
        gainNode.gain.value = 0.1;
    }

    oscillator.start();
    oscillator.stop(audioContext.currentTime + 0.2);
  }
}

export const notificationService = new NotificationService();
