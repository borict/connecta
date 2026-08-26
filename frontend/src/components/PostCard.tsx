import { Link } from 'react-router-dom'
import { Avatar } from './Avatar'
import { LikeButton } from './LikeButton'
import { formatPostTime } from '../lib/formatTime'
import type { FeedPostDto } from '../types/api'

type PostCardProps = {
  post: FeedPostDto
}

export function PostCard({ post }: PostCardProps) {
  const displayName = post.authorDisplayName || post.authorUsername || 'Unknown'
  const username = post.authorUsername
  const profilePath = username ? `/u/${encodeURIComponent(username)}` : null

  const authorBlock = (
    <>
      <Avatar name={displayName} username={username ?? ''} src={post.authorProfilePictureUrl} />
      <div className="min-w-0">
        <div className="fw-semibold text-truncate">{displayName}</div>
        {username ? <div className="text-secondary small text-truncate">@{username}</div> : null}
      </div>
    </>
  )

  return (
    <article className="card shadow-sm mb-3">
      <div className="card-body">
        <header className="d-flex align-items-center gap-2 mb-3">
          {profilePath ? (
            <Link to={profilePath} className="d-flex align-items-center gap-2 text-decoration-none text-reset min-w-0">
              {authorBlock}
            </Link>
          ) : (
            <div className="d-flex align-items-center gap-2 min-w-0">{authorBlock}</div>
          )}
          <time className="text-secondary small ms-auto text-nowrap" dateTime={post.createdAt}>
            {formatPostTime(post.createdAt)}
          </time>
        </header>
        <p className="mb-0" style={{ whiteSpace: 'pre-wrap' }}>
          {post.content}
        </p>
        {post.imageUrl ? (
          <img src={post.imageUrl} alt="" className="mt-3 rounded w-100" style={{ maxHeight: 520, objectFit: 'cover' }} />
        ) : null}
        <footer className="d-flex gap-3 mt-3 small align-items-center">
          <LikeButton postId={post.id} initialCount={post.likeCount} />
          <span className="text-secondary">
            <i className="bi bi-chat me-1" aria-hidden="true" />
            {post.commentCount}
          </span>
        </footer>
      </div>
    </article>
  )
}
