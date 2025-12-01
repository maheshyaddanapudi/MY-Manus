import React, { useState, useEffect } from 'react';
import { notificationService } from '../../services/notificationService';
import type { Notification } from '../../services/notificationService';
import { NotificationPanel } from './NotificationPanel';

export const NotificationBell: React.FC = () => {
  const [unreadCount, setUnreadCount] = useState(0);
  const [showPanel, setShowPanel] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);

  useEffect(() => {
    loadUnreadCount();

    // Poll for updates every 10 seconds
    const interval = setInterval(loadUnreadCount, 10000);

    return () => clearInterval(interval);
  }, []);

  const loadUnreadCount = async () => {
    const count = await notificationService.getUnreadCount();
    setUnreadCount(count);
  };

  const loadNotifications = async () => {
    const notifs = await notificationService.getNotifications();
    setNotifications(notifs);
  };

  const handleBellClick = async () => {
    setShowPanel(!showPanel);
    if (!showPanel) {
      await loadNotifications();
    }
  };

  const handleMarkAllRead = async () => {
    await notificationService.markAllAsRead();
    setUnreadCount(0);
    await loadNotifications();
  };

  const handleNotificationClick = async (notification: Notification) => {
    if (!notification.isRead) {
      await notificationService.markAsRead(notification.id);
      setUnreadCount(Math.max(0, unreadCount - 1));
    }

    // Navigate if action URL exists
    if (notification.actionUrl) {
      window.location.href = notification.actionUrl;
    }

    setShowPanel(false);
  };

  return (
    <div className="relative">
      {/* Bell Button */}
      <button
        onClick={handleBellClick}
        className="relative p-2 text-gray-300 hover:text-white transition-colors"
        aria-label="Notifications"
      >
        <svg
          className="w-6 h-6"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>

        {/* Unread Badge */}
        {unreadCount > 0 && (
          <span className="absolute top-0 right-0 flex items-center justify-center w-5 h-5 text-xs font-bold text-white bg-red-500 rounded-full">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {/* Notification Panel */}
      {showPanel && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-40"
            onClick={() => setShowPanel(false)}
          />

          {/* Panel */}
          <div className="absolute right-0 mt-2 z-50">
            <NotificationPanel
              notifications={notifications}
              onClose={() => setShowPanel(false)}
              onMarkAllRead={handleMarkAllRead}
              onNotificationClick={handleNotificationClick}
            />
          </div>
        </>
      )}
    </div>
  );
};
