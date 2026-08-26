import { useEffect, useState } from 'react'
import { fetchLikeCount, fetchLiked, likePost, unlikePost } from '../api/posts'

type LikeButtonProps = {
  postId: string
  initialCount: number
}

export function LikeButton({ postId, initialCount }: LikeButtonProps) {
  const [liked, setLiked] = useState(false)
  const [count, setCount] = useState(initialCount)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    setCount(initialCount)
  }, [initialCount])

  useEffect(() => {
    let cancelled = false

    async function loadLiked() {
      try {
        const response = await fetchLiked(postId)
        if (!cancelled) {
          setLiked(response.liked)
        }
      } catch {
        if (!cancelled) {
          setLiked(false)
        }
      }
    }

    void loadLiked()
    return () => {
      cancelled = true
    }
  }, [postId])

  async function toggleLike() {
    if (busy) {
      return
    }
    setBusy(true)
    try {
      if (liked) {
        await unlikePost(postId)
        const { count: nextCount } = await fetchLikeCount(postId)
        setLiked(false)
        setCount(nextCount)
      } else {
        const response = await likePost(postId)
        setLiked(response.liked)
        setCount(response.count)
      }
    } catch {
      // Keep current heart state; the next load or click retries.
    } finally {
      setBusy(false)
    }
  }

  return (
    <button
      type="button"
      className={`btn btn-link btn-sm text-decoration-none p-0 ${liked ? 'text-danger' : 'text-secondary'}`}
      onClick={toggleLike}
      disabled={busy}
      aria-pressed={liked}
      aria-label={liked ? 'Unlike' : 'Like'}
    >
      <i className={`bi ${liked ? 'bi-heart-fill' : 'bi-heart'} me-1`} aria-hidden="true" />
      {count}
    </button>
  )
}
