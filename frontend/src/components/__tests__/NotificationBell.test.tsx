import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NotificationBell } from '../Notifications/NotificationBell';
import { notificationService } from '../../services/notificationService';

// Mock notification service
vi.mock('../../services/notificationService', () => ({
  notificationService: {
    getUnreadCount: vi.fn(),
    getNotifications: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}));

// Mock NotificationPanel to simplify testing
vi.mock('../Notifications/NotificationPanel', () => ({
  NotificationPanel: ({ notifications, onMarkAllRead, onNotificationClick }: any) => (
    <div data-testid="notification-panel">
      <button onClick={onMarkAllRead}>Mark All Read</button>
      {notifications.map((notif: any) => (
        <div key={notif.id} onClick={() => onNotificationClick(notif)}>
          {notif.title}
        </div>
      ))}
    </div>
  ),
}));

describe('NotificationBell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.clearAllTimers();
    
    // Default mocks
    (notificationService.getUnreadCount as any).mockResolvedValue(0);
    (notificationService.getNotifications as any).mockResolvedValue([]);
    (notificationService.markAsRead as any).mockResolvedValue(undefined);
    (notificationService.markAllAsRead as any).mockResolvedValue(undefined);
  });

  it('renders notification bell button', () => {
    render(<NotificationBell />);
    
    const button = screen.getByLabelText('Notifications');
    expect(button).toBeInTheDocument();
  });

  it('loads unread count on mount', async () => {
    (notificationService.getUnreadCount as any).mockResolvedValue(5);
    
    render(<NotificationBell />);
    
    await waitFor(() => {
      expect(notificationService.getUnreadCount).toHaveBeenCalled();
    });
    
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('does not show badge when unread count is 0', async () => {
    (notificationService.getUnreadCount as any).mockResolvedValue(0);
    
    render(<NotificationBell />);
    
    await waitFor(() => {
      expect(notificationService.getUnreadCount).toHaveBeenCalled();
    });
    
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('shows notification panel when bell is clicked', async () => {
    const user = userEvent.setup();
    const mockNotifications = [
      { id: '1', title: 'Test Notification', message: 'Test', isRead: false, createdAt: '2024-01-01T10:00:00Z' },
    ];
    
    (notificationService.getNotifications as any).mockResolvedValue(mockNotifications);
    
    render(<NotificationBell />);
    
    const button = screen.getByLabelText('Notifications');
    await user.click(button);
    
    await waitFor(() => {
      expect(screen.getByTestId('notification-panel')).toBeInTheDocument();
    });
    
    expect(notificationService.getNotifications).toHaveBeenCalled();
  });

  it('hides notification panel when bell is clicked again', async () => {
    const user = userEvent.setup();
    
    render(<NotificationBell />);
    
    const button = screen.getByLabelText('Notifications');
    
    // Open panel
    await user.click(button);
    await waitFor(() => {
      expect(screen.getByTestId('notification-panel')).toBeInTheDocument();
    });
    
    // Close panel
    await user.click(button);
    await waitFor(() => {
      expect(screen.queryByTestId('notification-panel')).not.toBeInTheDocument();
    });
  });

  it('marks all notifications as read', async () => {
    const user = userEvent.setup();
    const mockNotifications = [
      { id: '1', title: 'Test 1', message: 'Test', isRead: false, createdAt: '2024-01-01T10:00:00Z' },
      { id: '2', title: 'Test 2', message: 'Test', isRead: false, createdAt: '2024-01-01T10:00:00Z' },
    ];
    
    (notificationService.getUnreadCount as any).mockResolvedValue(2);
    (notificationService.getNotifications as any).mockResolvedValue(mockNotifications);
    
    render(<NotificationBell />);
    
    // Open panel
    const button = screen.getByLabelText('Notifications');
    await user.click(button);
    
    await waitFor(() => {
      expect(screen.getByTestId('notification-panel')).toBeInTheDocument();
    });
    
    // Mark all as read
    const markAllButton = screen.getByText('Mark All Read');
    await user.click(markAllButton);
    
    await waitFor(() => {
      expect(notificationService.markAllAsRead).toHaveBeenCalled();
    });
  });

  it('marks individual notification as read when clicked', async () => {
    const user = userEvent.setup();
    const mockNotifications = [
      { id: '1', title: 'Test Notification', message: 'Test', isRead: false, createdAt: '2024-01-01T10:00:00Z' },
    ];
    
    (notificationService.getUnreadCount as any).mockResolvedValue(1);
    (notificationService.getNotifications as any).mockResolvedValue(mockNotifications);
    
    render(<NotificationBell />);
    
    // Open panel
    const button = screen.getByLabelText('Notifications');
    await user.click(button);
    
    await waitFor(() => {
      expect(screen.getByText('Test Notification')).toBeInTheDocument();
    });
    
    // Click notification
    await user.click(screen.getByText('Test Notification'));
    
    await waitFor(() => {
      expect(notificationService.markAsRead).toHaveBeenCalledWith('1');
    });
  });

  it('does not mark already read notification as read again', async () => {
    const user = userEvent.setup();
    const mockNotifications = [
      { id: '1', title: 'Read Notification', message: 'Test', isRead: true, createdAt: '2024-01-01T10:00:00Z' },
    ];
    
    (notificationService.getNotifications as any).mockResolvedValue(mockNotifications);
    
    render(<NotificationBell />);
    
    // Open panel
    const button = screen.getByLabelText('Notifications');
    await user.click(button);
    
    await waitFor(() => {
      expect(screen.getByText('Read Notification')).toBeInTheDocument();
    });
    
    // Click notification
    await user.click(screen.getByText('Read Notification'));
    
    // Should not call markAsRead for already read notification
    expect(notificationService.markAsRead).not.toHaveBeenCalled();
  });

  it('closes panel after clicking a notification', async () => {
    const user = userEvent.setup();
    const mockNotifications = [
      { id: '1', title: 'Test Notification', message: 'Test', isRead: false, createdAt: '2024-01-01T10:00:00Z' },
    ];
    
    (notificationService.getNotifications as any).mockResolvedValue(mockNotifications);
    
    render(<NotificationBell />);
    
    // Open panel
    const button = screen.getByLabelText('Notifications');
    await user.click(button);
    
    await waitFor(() => {
      expect(screen.getByTestId('notification-panel')).toBeInTheDocument();
    });
    
    // Click notification
    await user.click(screen.getByText('Test Notification'));
    
    // Panel should close
    await waitFor(() => {
      expect(screen.queryByTestId('notification-panel')).not.toBeInTheDocument();
    });
  });
});
