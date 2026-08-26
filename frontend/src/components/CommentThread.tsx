import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { createComment, deleteComment, fetchComments } from '../api/posts'
import { errorMessage } from '../api/errorMessage'
import { useAuth } from '../auth/AuthContext'
import { formatPostTime } from '../lib/formatTime'
import type { CommentResponse } from '../types/api'
import { Avatar } from './Avatar'

const MAX_CONTENT = 500

type CommentThreadProps = {
  postId: string
  initialCount: number
}

export function CommentThread({ postId, initialCount }: CommentThreadProps) {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const [count, setCount] = useState(initialCount)
  const [comments, setComments] = useState<CommentResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [draft, setDraft] = useState('')

  useEffect(() => {
    setCount(initialCount)
  }, [initialCount])

  useEffect(() => {
    if (!open) {
      return
    }

    let cancelled = false

    async function load() {
      setError(null)
      setLoading(true)
      try {
        const page = await fetchComments(postId)
        if (!cancelled) {
          setComments(page.content)
          setCount(page.totalElements)
        }
      } catch (err) {
        if (!cancelled) {
          setError(errorMessage(err, 'Could not load comments'))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [open, postId])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = String(new FormData(event.currentTarget).get('content') ?? draft).trim()
    if (!trimmed) {
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      const created = await createComment(postId, trimmed)
      setComments((current) => [created, ...current])
      setCount((current) => current + 1)
      setDraft('')
    } catch (err) {
      setError(errorMessage(err, 'Could not add comment'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(commentId: string) {
    if (deletingId) {
      return
    }
    setError(null)
    setDeletingId(commentId)
    try {
      await deleteComment(commentId)
      setComments((current) => current.filter((comment) => comment.id !== commentId))
      setCount((current) => Math.max(0, current - 1))
    } catch (err) {
      setError(errorMessage(err, 'Could not delete comment'))
    } finally {
      setDeletingId(null)
    }
  }

  function toggleOpen() {
    if (!open) {
      setLoading(true)
    }
    setOpen((current) => !current)
  }

  const remaining = MAX_CONTENT - draft.length
  const inputId = `comment-${postId}`

  return (
    <>
      <button
        type="button"
        className={`btn btn-link btn-sm text-decoration-none p-0 ${open ? 'text-primary' : 'text-secondary'}`}
        onClick={toggleOpen}
        aria-expanded={open}
        aria-label={open ? 'Hide comments' : 'Show comments'}
      >
        <i className={`bi ${open ? 'bi-chat-fill' : 'bi-chat'} me-1`} aria-hidden="true" />
        {count}
      </button>
      {open ? (
        <div className="w-100 mt-1 pt-3 border-top">
          {error ? <div className="alert alert-danger py-2 small">{error}</div> : null}
          {loading ? (
            <div className="d-flex justify-content-center py-3">
              <div className="spinner-border spinner-border-sm text-primary" role="status">
                <span className="visually-hidden">Loading comments…</span>
              </div>
            </div>
          ) : comments.length === 0 ? (
            <p className="text-secondary small mb-3">No comments yet.</p>
          ) : (
            <ul className="list-unstyled mb-3">
              {comments.map((comment) => {
                const displayName = comment.authorDisplayName || comment.authorUsername || 'Unknown'
                const username = comment.authorUsername
                const profilePath = username ? `/u/${encodeURIComponent(username)}` : null
                const isOwn = Boolean(user && comment.authorId === user.id)

                return (
                  <li key={comment.id} className="d-flex gap-2 mb-3">
                    {profilePath ? (
                      <Link to={profilePath} className="flex-shrink-0">
                        <Avatar
                          name={displayName}
                          username={username ?? ''}
                          src={comment.authorProfilePictureUrl}
                          size={28}
                        />
                      </Link>
                    ) : (
                      <Avatar name={displayName} username={username ?? ''} src={comment.authorProfilePictureUrl} size={28} />
                    )}
                    <div className="min-w-0 flex-grow-1">
                      <div className="d-flex align-items-baseline gap-2">
                        {profilePath ? (
                          <Link to={profilePath} className="fw-semibold small text-decoration-none text-reset text-truncate">
                            {displayName}
                          </Link>
                        ) : (
                          <span className="fw-semibold small text-truncate">{displayName}</span>
                        )}
                        <time className="text-secondary small text-nowrap" dateTime={comment.createdAt}>
                          {formatPostTime(comment.createdAt)}
                        </time>
                        {isOwn ? (
                          <button
                            type="button"
                            className="btn btn-link btn-sm text-secondary text-decoration-none p-0 ms-auto"
                            onClick={() => void handleDelete(comment.id)}
                            disabled={deletingId === comment.id}
                            aria-label="Delete comment"
                          >
                            Delete
                          </button>
                        ) : null}
                      </div>
                      <p className="mb-0 small" style={{ whiteSpace: 'pre-wrap' }}>
                        {comment.content}
                      </p>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
          <form onSubmit={handleSubmit}>
            <label className="form-label visually-hidden" htmlFor={inputId}>
              Write a comment
            </label>
            <div className="d-flex gap-2 align-items-start">
              <textarea
                id={inputId}
                name="content"
                className="form-control form-control-sm"
                rows={2}
                maxLength={MAX_CONTENT}
                placeholder="Write a comment…"
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                required
                disabled={submitting}
              />
              <button className="btn btn-primary btn-sm" type="submit" disabled={submitting}>
                {submitting ? 'Posting…' : 'Comment'}
              </button>
            </div>
            <div className={`form-text ${remaining <= 20 ? 'text-danger' : ''}`}>{remaining}</div>
          </form>
        </div>
      ) : null}
    </>
  )
}
