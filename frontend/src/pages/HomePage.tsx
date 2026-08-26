import { useEffect, useState } from 'react'
import { fetchFeed } from '../api/feed'
import { errorMessage } from '../api/errorMessage'
import { PostCard } from '../components/PostCard'
import { PostComposer } from '../components/PostComposer'
import type { FeedPostDto } from '../types/api'

export function HomePage() {
  const [posts, setPosts] = useState<FeedPostDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setError(null)
      setLoading(true)
      try {
        const page = await fetchFeed(0)
        if (!cancelled) {
          setPosts(page.content)
        }
      } catch (err) {
        if (!cancelled) {
          setError(errorMessage(err, 'Could not load feed'))
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
  }, [])

  function handleCreated(post: FeedPostDto) {
    setPosts((current) => [post, ...current])
  }

  function handleDeleted(postId: string) {
    setPosts((current) => current.filter((post) => post.id !== postId))
  }

  return (
    <>
      <h1 className="h4 mb-3">Home</h1>
      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading feed…</span>
          </div>
        </div>
      ) : (
        <>
          <PostComposer onCreated={handleCreated} />
          {error ? <div className="alert alert-danger">{error}</div> : null}
          {!error && posts.length === 0 ? (
            <p className="text-secondary mb-0">Your feed is empty. Follow people or create a post.</p>
          ) : null}
          {!error ? posts.map((post) => <PostCard key={post.id} post={post} onDeleted={handleDeleted} />) : null}
        </>
      )}
    </>
  )
}
