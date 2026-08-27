import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { errorMessage } from '../api/errorMessage'
import { fetchPost } from '../api/posts'
import { PostCard } from '../components/PostCard'
import type { FeedPostDto } from '../types/api'

export function PostPage() {
  const { postId } = useParams()
  const [post, setPost] = useState<FeedPostDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      const id = postId?.trim()
      if (!id) {
        setError('Post not found')
        setPost(null)
        setLoading(false)
        return
      }

      setError(null)
      setLoading(true)
      try {
        const next = await fetchPost(id)
        if (!cancelled) {
          setPost(next)
        }
      } catch (err) {
        if (cancelled) {
          return
        }
        if (err instanceof ApiError && err.status === 404) {
          setError('Post not found')
        } else {
          setError(errorMessage(err, 'Could not load post'))
        }
        setPost(null)
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
  }, [postId])

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading post…</span>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <>
        <Link to="/notifications" className="d-inline-block mb-3 text-decoration-none">
          ← Back to notifications
        </Link>
        <div className="alert alert-danger mb-0">{error}</div>
      </>
    )
  }

  if (!post) {
    return (
      <>
        <Link to="/notifications" className="d-inline-block mb-3 text-decoration-none">
          ← Back to notifications
        </Link>
        <p className="text-secondary mb-0">This post was deleted.</p>
      </>
    )
  }

  return (
    <>
      <Link to="/notifications" className="d-inline-block mb-3 text-decoration-none">
        ← Back to notifications
      </Link>
      <PostCard post={post} onDeleted={() => setPost(null)} />
    </>
  )
}
