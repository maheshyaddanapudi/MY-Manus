import React from 'react';
import { XMarkIcon } from '@heroicons/react/24/outline';
import type { Notification } from '../../services/notificationService';
import { cn, getBadgeClasses } from '../../theme';

interface NotificationPanelProps {
  notifications: Notification[];
  onClose: () => void;
  onMarkAllRead: () => void;
  onNotificationClick: (notification: Notification) => void;
}

export const NotificationPanel: React.FC<NotificationPanelProps> = ({
  notifications,
  onClose,
  onMarkAllRead,
  onNotificationClick,
}) => {
  const getTypeIcon = (type: Notification['type']) => {
    switch (type) {
      case 'TASK_COMPLETED':
        return '✅';
      case 'TASK_FAILED':
        return '❌';
      case 'AGENT_WAITING':
        return '⏸️';
      case 'PLAN_ADJUSTED':
        return '🔄';
      case 'TOOL_ERROR':
        return '⚠️';
      case 'SYSTEM':
        return '🔧';
      default:
        return 'ℹ️';
    }
  };

  const getPriorityColor = (priority: Notification['priority']) => {
    switch (priority) {
      case 'URGENT':
        return 'border-red-500';
      case 'HIGH':
        return 'border-orange-500';
      case 'LOW':
        return 'border-gray-600';
      default:
        return 'border-blue-500';
    }
  };

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  return (
    <div className="w-96 bg-gray-800/95 backdrop-blur-xl border border-gray-700/50 rounded-xl shadow-2xl overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-gray-700/50 bg-gray-800/60">
        <div>
          <h3 className="text-base font-semibold text-gray-200 flex items-center gap-2">
            🔔 Notifications
          </h3>
          {unreadCount > 0 && (
            <p className={cn(getBadgeClasses('info'), 'text-xs mt-1')}>
              {unreadCount} unread
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {unreadCount > 0 && (
            <button
              onClick={onMarkAllRead}
              className="text-xs text-blue-400 hover:text-blue-300 transition-colors px-2 py-1 rounded hover:bg-blue-500/10"
            >
              Mark all read
            </button>
          )}
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-200 p-1 rounded-lg hover:bg-gray-700/50 transition-all"
          >
            <XMarkIcon className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Notifications List */}
      <div className="max-h-96 overflow-y-auto custom-scrollbar">
        {notifications.length === 0 ? (
          <div className="p-12 text-center">
            <div className="text-6xl mb-4 opacity-30">🔔</div>
            <p className="text-gray-400 font-medium">No notifications yet</p>
            <p className="text-sm text-gray-600 mt-2">You'll see updates here</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-700/30">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                onClick={() => onNotificationClick(notification)}
                className={cn(
                  'p-4 cursor-pointer transition-all duration-200 border-l-4',
                  notification.isRead
                    ? 'bg-gray-800/40 hover:bg-gray-800/60'
                    : 'bg-gray-800/60 hover:bg-gray-800/80',
                  getPriorityColor(notification.priority)
                )}
              >
                <div className="flex items-start gap-3">
                  {/* Icon */}
                  <div className="flex-shrink-0 text-2xl mt-0.5">
                    {getTypeIcon(notification.type)}
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <p className={cn(
                        'text-sm font-medium leading-snug',
                        notification.isRead ? 'text-gray-300' : 'text-gray-100'
                      )}>
                        {notification.title}
                      </p>
                      {!notification.isRead && (
                        <span className="flex-shrink-0 w-2 h-2 bg-blue-500 rounded-full mt-1.5 animate-pulse" />
                      )}
                    </div>
                    <p className="text-sm text-gray-400 mt-1.5 leading-relaxed line-clamp-2">
                      {notification.message}
                    </p>
                    <p className="text-xs text-gray-500 mt-2 flex items-center gap-1.5">
                      <span className="w-1 h-1 rounded-full bg-gray-600"></span>
                      {formatTime(notification.createdAt)}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Footer */}
      {notifications.length > 0 && (
        <div className="p-3 border-t border-gray-700/50 text-center bg-gray-800/40">
          <button className="text-sm text-blue-400 hover:text-blue-300 transition-colors px-3 py-1.5 rounded-lg hover:bg-blue-500/10">
            View all notifications →
          </button>
        </div>
      )}
    </div>
  );
};
