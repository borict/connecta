import { Avatar } from './Avatar'
import { formatPostTime } from '../lib/formatTime'
import { notificationText } from '../lib/notificationPath'
import type { NotificationResponse, UserSummaryResponse } from '../types/api'

type NotificationItemProps = {
  notification: NotificationResponse
  actor?: UserSummaryResponse
  busy?: boolean
  onOpen: (notification: NotificationResponse) => void
}

export function NotificationItem({ notification, actor, busy, onOpen }: NotificationItemProps) {
  const displayName = actor?.displayName || actor?.username || 'Someone'
  const username = actor?.username ?? ''

  return (
    <button
      type="button"
      className={`w-100 list-group-item list-group-item-action d-flex align-items-start gap-2 border-0 border-bottom rounded-0 py-3 ${
        notification.read ? '' : 'bg-primary-subtle'
      }`}
      onClick={() => onOpen(notification)}
      disabled={busy}
    >
      <Avatar name={displayName} username={username} src={actor?.profilePictureUrl} />
      <div className="min-w-0 flex-grow-1 text-start">
        <div className={notification.read ? '' : 'fw-semibold'}>
          {notificationText(notification, actor)}
        </div>
        <div className="text-secondary small">{formatPostTime(notification.createdAt)}</div>
      </div>
      {notification.read ? null : (
        <span
          className="bg-primary rounded-circle flex-shrink-0 mt-2"
          style={{ width: 8, height: 8 }}
          aria-label="Unread"
        />
      )}
    </button>
  )
}
