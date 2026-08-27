import { useSearchParams } from 'react-router-dom'
import { searchUsers } from '../api/users'
import { InfiniteScrollSentinel } from '../components/InfiniteScrollSentinel'
import { UserListItem } from '../components/UserListItem'
import { usePagedList } from '../lib/usePagedList'
import type { UserSummaryResponse } from '../types/api'

function userId(user: UserSummaryResponse): string {
  return user.id
}

export function SearchPage() {
  const [searchParams] = useSearchParams()
  const query = searchParams.get('q')?.trim() ?? ''
  const canSearch = query.length >= 2

  const { items: results, loading, loadingMore, error, loadMoreError, hasMore, loadMore } =
    usePagedList<UserSummaryResponse>({
      enabled: canSearch,
      resetKey: query,
      loadPage: (page) => searchUsers(query, page),
      getId: userId,
      fallbackError: 'Could not search people',
    })

  return (
    <>
      <h1 className="h4 mb-3">Search</h1>
      {!canSearch ? (
        <p className="text-secondary mb-0">Type at least 2 characters in the search box.</p>
      ) : loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Searching…</span>
          </div>
        </div>
      ) : error ? (
        <div className="alert alert-danger">{error}</div>
      ) : results.length === 0 ? (
        <p className="text-secondary mb-0">
          No people found for <span className="text-dark fw-medium">&ldquo;{query}&rdquo;</span>.
        </p>
      ) : (
        <>
          <p className="text-secondary mb-3">
            Results for <span className="text-dark fw-medium">&ldquo;{query}&rdquo;</span>
          </p>
          <ul className="list-unstyled mb-0">
            {results.map((user) => (
              <li key={user.id} className="border-bottom">
                <UserListItem user={user} />
              </li>
            ))}
          </ul>
          <InfiniteScrollSentinel
            disabled={!hasMore}
            loading={loadingMore}
            error={loadMoreError}
            onVisible={loadMore}
            onRetry={loadMore}
          />
        </>
      )}
    </>
  )
}
