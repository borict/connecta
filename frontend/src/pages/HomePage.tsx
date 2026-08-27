import { fetchFeed } from '../api/feed'
import { InfiniteScrollSentinel } from '../components/InfiniteScrollSentinel'
import { PostCard } from '../components/PostCard'
import { PostComposer } from '../components/PostComposer'
import { usePagedList } from '../lib/usePagedList'
import type { FeedPostDto } from '../types/api'

function postId(post: FeedPostDto): string {
  return post.id
}

export function HomePage() {
  const {
    items: posts,
    setItems: setPosts,
    loading,
    loadingMore,
    error,
    loadMoreError,
    hasMore,
    loadMore,
  } = usePagedList<FeedPostDto>({
    resetKey: 'feed',
    loadPage: (page) => fetchFeed(page),
    getId: postId,
    fallbackError: 'Could not load feed',
  })

  function handleCreated(post: FeedPostDto) {
    setPosts((current) => [post, ...current.filter((item) => item.id !== post.id)])
  }

  function handleDeleted(postIdToRemove: string) {
    setPosts((current) => current.filter((post) => post.id !== postIdToRemove))
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
          {!error ? (
            <InfiniteScrollSentinel
              disabled={!hasMore}
              loading={loadingMore}
              error={loadMoreError}
              onVisible={loadMore}
              onRetry={loadMore}
            />
          ) : null}
        </>
      )}
    </>
  )
}
