import { Link } from 'react-router-dom'
import type { FollowUserResponse } from '../types/api'
import { Avatar } from './Avatar'

type UserListItemProps = {
  user: FollowUserResponse
}

export function UserListItem({ user }: UserListItemProps) {
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

  if (!profilePath) {
    return <div className="d-flex align-items-center gap-2 py-2">{body}</div>
  }

  return (
    <Link to={profilePath} className="d-flex align-items-center gap-2 py-2 text-decoration-none text-reset">
      {body}
    </Link>
  )
}
