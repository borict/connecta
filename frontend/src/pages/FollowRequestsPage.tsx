import { useState } from 'react'
import { errorMessage } from '../api/errorMessage'
import { acceptFollowRequest, fetchIncomingRequests, rejectFollowRequest } from '../api/social'
import { InfiniteScrollSentinel } from '../components/InfiniteScrollSentinel'
import { UserListItem } from '../components/UserListItem'
import { usePagedList } from '../lib/usePagedList'
import type { FollowUserResponse } from '../types/api'

function requestId(request: FollowUserResponse): string {
  return request.userId
}

export function FollowRequestsPage() {
  const [busyId, setBusyId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const {
    items: requests,
    setItems: setRequests,
    loading,
    loadingMore,
    error,
    loadMoreError,
    hasMore,
    loadMore,
  } = usePagedList<FollowUserResponse>({
    resetKey: 'requests',
    loadPage: (page) => fetchIncomingRequests(page),
    getId: requestId,
    fallbackError: 'Could not load follow requests',
  })

  async function handleAccept(followerId: string) {
    if (busyId) {
      return
    }
    setActionError(null)
    setBusyId(followerId)
    try {
      await acceptFollowRequest(followerId)
      setRequests((current) => current.filter((request) => request.userId !== followerId))
    } catch (err) {
      setActionError(errorMessage(err, 'Could not accept request'))
    } finally {
      setBusyId(null)
    }
  }

  async function handleReject(followerId: string) {
    if (busyId) {
      return
    }
    setActionError(null)
    setBusyId(followerId)
    try {
      await rejectFollowRequest(followerId)
      setRequests((current) => current.filter((request) => request.userId !== followerId))
    } catch (err) {
      setActionError(errorMessage(err, 'Could not reject request'))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <h1 className="h4 mb-3">Follow requests</h1>
      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading follow requests…</span>
          </div>
        </div>
      ) : error ? (
        <div className="alert alert-danger">{error}</div>
      ) : (
        <>
          {actionError ? <div className="alert alert-danger py-2">{actionError}</div> : null}
          {requests.length === 0 ? (
            <p className="text-secondary mb-0">No follow requests.</p>
          ) : (
            <>
              <ul className="list-unstyled mb-0">
                {requests.map((request) => {
                  const busy = busyId === request.userId
                  return (
                    <li key={request.userId} className="border-bottom">
                      <UserListItem
                        user={request}
                        actions={
                          <>
                            <button
                              type="button"
                              className="btn btn-primary btn-sm"
                              onClick={() => void handleAccept(request.userId)}
                              disabled={Boolean(busyId)}
                            >
                              {busy ? 'Saving…' : 'Accept'}
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-secondary btn-sm"
                              onClick={() => void handleReject(request.userId)}
                              disabled={Boolean(busyId)}
                            >
                              Reject
                            </button>
                          </>
                        }
                      />
                    </li>
                  )
                })}
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
      )}
    </>
  )
}
