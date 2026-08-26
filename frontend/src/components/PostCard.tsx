import { useState } from 'react'
import { Link } from 'react-router-dom'
import { deletePost } from '../api/posts'
import { errorMessage } from '../api/errorMessage'
import { useAuth } from '../auth/AuthContext'
import { formatPostTime } from '../lib/formatTime'
import type { FeedPostDto } from '../types/api'
import { Avatar } from './Avatar'
import { CommentThread } from './CommentThread'
import { LikeButton } from './LikeButton'

type PostCardProps = {
  post: FeedPostDto
  onDeleted?: (postId: string) => void
}

export function PostCard({ post, onDeleted }: PostCardProps) {
  const { user } = useAuth()
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const displayName = post.authorDisplayName || post.authorUsername || 'Unknown'
  const username = post.authorUsername
  const profilePath = username ? `/u/${encodeURIComponent(username)}` : null
  const isOwn = Boolean(user && post.authorId === user.id)

  const authorBlock = (
    <>
      <Avatar name={displayName} username={username ?? ''} src={post.authorProfilePictureUrl} />
      <div className="min-w-0">
        <div className="fw-semibold text-truncate">{displayName}</div>
        {username ? <div className="text-secondary small text-truncate">@{username}</div> : null}
      </div>
    </>
  )

  async function handleDelete() {
    if (deleting) {
      return
    }
    setError(null)
    setDeleting(true)
    try {
      await deletePost(post.id)
      onDeleted?.(post.id)
    } catch (err) {
      setError(errorMessage(err, 'Could not delete post'))
      setDeleting(false)
    }
  }

  return (
    <article className="card shadow-sm mb-3">
      <div className="card-body">
        {error ? <div className="alert alert-danger py-2">{error}</div> : null}
        <header className="d-flex align-items-center gap-2 mb-3">
          {profilePath ? (
            <Link to={profilePath} className="d-flex align-items-center gap-2 text-decoration-none text-reset min-w-0">
              {authorBlock}
            </Link>
          ) : (
            <div className="d-flex align-items-center gap-2 min-w-0">{authorBlock}</div>
          )}
          <div className="ms-auto d-flex align-items-center gap-2 flex-shrink-0">
            <time className="text-secondary small text-nowrap" dateTime={post.createdAt}>
              {formatPostTime(post.createdAt)}
            </time>
            {isOwn ? (
              <button
                type="button"
                className="btn btn-link btn-sm text-secondary text-decoration-none p-0"
                onClick={() => void handleDelete()}
                disabled={deleting}
                aria-label="Delete post"
              >
                {deleting ? 'Deleting…' : 'Delete'}
              </button>
            ) : null}
          </div>
        </header>
        <p className="mb-0" style={{ whiteSpace: 'pre-wrap' }}>
          {post.content}
        </p>
        {post.imageUrl ? (
          <img src={post.imageUrl} alt="" className="mt-3 rounded w-100" style={{ maxHeight: 520, objectFit: 'cover' }} />
        ) : null}
        <footer className="d-flex flex-wrap gap-3 mt-3 small align-items-center">
          <LikeButton postId={post.id} initialCount={post.likeCount} />
          <CommentThread postId={post.id} initialCount={post.commentCount} />
        </footer>
      </div>
    </article>
  )
}
