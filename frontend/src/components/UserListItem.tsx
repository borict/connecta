import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'
import { Avatar } from './Avatar'

export type UserListPerson = {
  username: string | null
  displayName: string | null
  profilePictureUrl: string | null
}

type UserListItemProps = {
  user: UserListPerson
  actions?: ReactNode
}

export function UserListItem({ user, actions }: UserListItemProps) {
  const displayName = user.displayName || user.username || 'Unknown'
  const username = user.username
  const profilePath = username ? `/u/${encodeURIComponent(username)}` : null

  const body = (
    <>
      <Avatar name={displayName} username={username ?? ''} src={user.profilePictureUrl} />
      <div className="min-w-0">
        <div className="fw-semibold text-truncate">{displayName}</div>
        {username ? <div className="text-secondary small text-truncate">@{username}</div> : null}
      </div>
    </>
  )

  return (
    <div className="d-flex align-items-center gap-2 py-2">
      {profilePath ? (
        <Link to={profilePath} className="d-flex align-items-center gap-2 text-decoration-none text-reset min-w-0 flex-grow-1">
          {body}
        </Link>
      ) : (
        <div className="d-flex align-items-center gap-2 min-w-0 flex-grow-1">{body}</div>
      )}
      {actions ? <div className="flex-shrink-0 d-flex gap-2">{actions}</div> : null}
    </div>
  )
}
