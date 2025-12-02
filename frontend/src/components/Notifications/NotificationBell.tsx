import React, { useState, useEffect } from 'react';
import { BellIcon } from '@heroicons/react/24/outline';
import { notificationService } from '../../services/notificationService';
import type { Notification } from '../../services/notificationService';
import { NotificationPanel } from './NotificationPanel';
import { cn } from '../../theme';

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
        className={cn(
          'relative p-2.5 rounded-lg transition-all duration-200',
          'text-gray-400 hover:text-gray-200',
          'hover:bg-gray-800/50',
          showPanel && 'bg-gray-800/70 text-gray-200'
        )}
        aria-label="Notifications"
        title="Notifications"
      >
        <BellIcon className="w-5 h-5" />

        {/* Unread Badge */}
        {unreadCount > 0 && (
          <span className={cn(
            'absolute -top-1 -right-1',
            'flex items-center justify-center',
            'min-w-[20px] h-5 px-1.5',
            'text-[10px] font-bold text-white',
            'bg-gradient-to-br from-red-500 to-red-600',
            'rounded-full shadow-lg',
            'animate-pulse'
          )}>
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {/* Notification Panel */}
      {showPanel && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-40 bg-black/20 backdrop-blur-sm"
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
