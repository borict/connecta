import { useEffect, useState } from 'react'
import { Link, NavLink, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { errorMessage } from '../api/errorMessage'
import { fetchFollowers, fetchFollowing } from '../api/social'
import { fetchUserByUsername } from '../api/users'
import { InfiniteScrollSentinel } from '../components/InfiniteScrollSentinel'
import { UserListItem } from '../components/UserListItem'
import { usePagedList } from '../lib/usePagedList'
import type { FollowUserResponse } from '../types/api'

type FollowListKind = 'followers' | 'following'

type FollowListPageProps = {
  kind: FollowListKind
}

function navClassName({ isActive }: { isActive: boolean }): string {
  return isActive ? 'nav-link active' : 'nav-link'
}

function followUserId(user: FollowUserResponse): string {
  return user.userId
}

export function FollowListPage({ kind }: FollowListPageProps) {
  const { username } = useParams()
  const [profileId, setProfileId] = useState<string | null>(null)
  const [profileError, setProfileError] = useState<string | null>(null)
  const [resolving, setResolving] = useState(true)

  const handle = username?.trim() ?? ''
  const profilePath = handle ? `/u/${encodeURIComponent(handle)}` : '/'
  const title = kind === 'followers' ? 'Followers' : 'Following'

  useEffect(() => {
    let cancelled = false

    async function resolveProfile() {
      if (!handle) {
        setProfileError('User not found')
        setProfileId(null)
        setResolving(false)
        return
      }
      setProfileError(null)
      setResolving(true)
      try {
        const profile = await fetchUserByUsername(handle)
        if (!cancelled) {
          setProfileId(profile.id)
        }
      } catch (err) {
        if (cancelled) {
          return
        }
        setProfileId(null)
        if (err instanceof ApiError && err.status === 404) {
          setProfileError('User not found')
        } else {
          setProfileError(errorMessage(err, `Could not load ${kind}`))
        }
      } finally {
        if (!cancelled) {
          setResolving(false)
        }
      }
    }

    void resolveProfile()
    return () => {
      cancelled = true
    }
  }, [handle, kind])

  const { items: users, loading, loadingMore, error, loadMoreError, hasMore, loadMore } =
    usePagedList<FollowUserResponse>({
      enabled: Boolean(profileId),
      resetKey: `${kind}:${profileId ?? ''}`,
      loadPage: (page) =>
        kind === 'followers' ? fetchFollowers(profileId as string, page) : fetchFollowing(profileId as string, page),
      getId: followUserId,
      fallbackError: `Could not load ${kind}`,
    })

  const busy = resolving || loading
  const displayError = profileError ?? error

  return (
    <>
      <div className="mb-3">
        <Link to={profilePath} className="small text-decoration-none">
          ← Back to profile
        </Link>
      </div>
      <h1 className="h4 mb-1">{title}</h1>
      {handle ? <p className="text-secondary mb-3">@{handle}</p> : null}
      <ul className="nav nav-underline mb-3">
        <li className="nav-item">
          <NavLink className={navClassName} to={`${profilePath}/followers`}>
            Followers
          </NavLink>
        </li>
        <li className="nav-item">
          <NavLink className={navClassName} to={`${profilePath}/following`}>
            Following
          </NavLink>
        </li>
      </ul>
      {busy ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading {kind}…</span>
          </div>
        </div>
      ) : displayError ? (
        <div className="alert alert-danger">{displayError}</div>
      ) : users.length === 0 ? (
        <p className="text-secondary mb-0">
          {kind === 'followers' ? 'No followers yet.' : 'Not following anyone yet.'}
        </p>
      ) : (
        <>
          <ul className="list-unstyled mb-0">
            {users.map((user) => (
              <li key={user.userId} className="border-bottom">
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
