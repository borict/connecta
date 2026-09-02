import { fetchExplore } from '../api/explore'
import { InfiniteScrollSentinel } from '../components/InfiniteScrollSentinel'
import { PostCard } from '../components/PostCard'
import { usePagedList } from '../lib/usePagedList'
import type { FeedPostDto } from '../types/api'

function postId(post: FeedPostDto): string {
  return post.id
}

export function ExplorePage() {
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
    resetKey: 'explore',
    loadPage: (page) => fetchExplore(page),
    getId: postId,
    fallbackError: 'Could not load Explore',
  })

  function handleDeleted(postIdToRemove: string) {
    setPosts((current) => current.filter((post) => post.id !== postIdToRemove))
  }

  return (
    <>
      <h1 className="h4 mb-3">Explore</h1>
      <p className="text-secondary mb-3">Public posts from people you do not follow.</p>
      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading Explore…</span>
          </div>
        </div>
      ) : (
        <>
          {error ? <div className="alert alert-danger">{error}</div> : null}
          {!error && posts.length === 0 ? (
            <p className="text-secondary mb-0">
              Nothing to discover yet. Search for people or check back when someone new posts.
            </p>
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
