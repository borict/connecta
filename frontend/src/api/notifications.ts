import { api, withQuery } from './client'
import type { NotificationResponse, PageResponse, UnreadCountResponse } from '../types/api'

export const NOTIFICATION_PAGE_SIZE = 20

export function fetchNotifications(
  page = 0,
  size = NOTIFICATION_PAGE_SIZE,
): Promise<PageResponse<NotificationResponse>> {
  return api.get<PageResponse<NotificationResponse>>(withQuery('/api/notifications', { page, size }))
}

export function fetchUnreadCount(): Promise<UnreadCountResponse> {
  return api.get<UnreadCountResponse>('/api/notifications/unread-count')
}

export function markNotificationRead(id: string): Promise<NotificationResponse> {
  return api.put<NotificationResponse>(`/api/notifications/${encodeURIComponent(id)}/read`)
}

export function markAllNotificationsRead(): Promise<void> {
  return api.put<void>('/api/notifications/read-all')
}
