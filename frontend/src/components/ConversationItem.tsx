import { Link } from 'react-router-dom'
import { Avatar } from './Avatar'
import { formatPostTime } from '../lib/formatTime'
import type { ConversationResponse } from '../types/api'

type ConversationItemProps = {
  conversation: ConversationResponse
  currentUserId: string
}

export function ConversationItem({ conversation, currentUserId }: ConversationItemProps) {
  const displayName = conversation.otherDisplayName || conversation.otherUsername || 'Unknown'
  const username = conversation.otherUsername ?? ''
  const unread = conversation.unreadCount > 0
  const last = conversation.lastMessage
  const preview = last
    ? `${last.senderId === currentUserId ? 'You: ' : ''}${last.content}`
    : 'No messages yet'
  const time = conversation.lastMessageAt ? formatPostTime(conversation.lastMessageAt) : ''
  const chatPath = `/messages/${encodeURIComponent(conversation.otherUserId)}`

  return (
    <Link
      to={chatPath}
      className={`d-flex align-items-start gap-2 py-3 border-bottom text-decoration-none text-reset ${unread ? 'bg-primary-subtle' : ''}`}
    >
      <Avatar name={displayName} username={username} src={conversation.otherProfilePictureUrl} size={44} />
      <div className="min-w-0 flex-grow-1">
        <div className="d-flex align-items-baseline justify-content-between gap-2">
          <div className="fw-semibold text-truncate">{displayName}</div>
          {time ? <div className="text-secondary small flex-shrink-0">{time}</div> : null}
        </div>
        {username ? <div className="text-secondary small text-truncate">@{username}</div> : null}
        <div className={`small text-truncate ${unread ? 'fw-semibold text-dark' : 'text-secondary'}`}>
          {preview}
        </div>
      </div>
      {unread ? (
        <span className="badge rounded-pill bg-danger flex-shrink-0 mt-1">
          {conversation.unreadCount > 99 ? '99+' : conversation.unreadCount}
        </span>
      ) : null}
    </Link>
  )
}
