import type { NotificationResponse, UserSummaryResponse } from '../types/api'

export function notificationPath(
  notification: NotificationResponse,
  actor: UserSummaryResponse | undefined,
): string | null {
  switch (notification.type) {
    case 'LIKE':
    case 'COMMENT':
      return notification.resourceId ? `/posts/${encodeURIComponent(notification.resourceId)}` : null
    case 'FOLLOW':
      return actor?.username ? `/u/${encodeURIComponent(actor.username)}` : null
    case 'MESSAGE':
      return '/messages'
    default:
      return null
  }
}

export function notificationText(
  notification: NotificationResponse,
  actor: UserSummaryResponse | undefined,
): string {
  const name = actor?.displayName?.trim() || actor?.username?.trim()
  if (!name) {
    return notification.message
  }
  switch (notification.type) {
    case 'LIKE':
      return `${name} liked your post`
    case 'COMMENT':
      return `${name} commented on your post`
    case 'FOLLOW':
      return `${name} started following you`
    case 'MESSAGE':
      return `${name} sent you a message`
    default:
      return notification.message
  }
}
