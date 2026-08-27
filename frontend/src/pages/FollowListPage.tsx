import { useEffect, useState } from 'react'
import { Link, NavLink, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { errorMessage } from '../api/errorMessage'
import { fetchFollowers, fetchFollowing } from '../api/social'
import { fetchUserByUsername } from '../api/users'
import { UserListItem } from '../components/UserListItem'
import type { FollowUserResponse } from '../types/api'

type FollowListKind = 'followers' | 'following'

type FollowListPageProps = {
  kind: FollowListKind
}

function navClassName({ isActive }: { isActive: boolean }): string {
  return isActive ? 'nav-link active' : 'nav-link'
}

export function FollowListPage({ kind }: FollowListPageProps) {
  const { username } = useParams()
  const [users, setUsers] = useState<FollowUserResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const handle = username?.trim() ?? ''
  const profilePath = handle ? `/u/${encodeURIComponent(handle)}` : '/'
  const title = kind === 'followers' ? 'Followers' : 'Following'

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!handle) {
        setError('User not found')
        setLoading(false)
        return
      }

      setError(null)
      setLoading(true)
      try {
        const profile = await fetchUserByUsername(handle)
        const page = kind === 'followers' ? await fetchFollowers(profile.id) : await fetchFollowing(profile.id)
        if (!cancelled) {
          setUsers(page.content)
        }
      } catch (err) {
        if (cancelled) {
          return
        }
        if (err instanceof ApiError && err.status === 404) {
          setError('User not found')
        } else {
          setError(errorMessage(err, `Could not load ${kind}`))
        }
        setUsers([])
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
  }, [handle, kind])

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
      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading {kind}…</span>
          </div>
        </div>
      ) : error ? (
        <div className="alert alert-danger">{error}</div>
      ) : users.length === 0 ? (
        <p className="text-secondary mb-0">
          {kind === 'followers' ? 'No followers yet.' : 'Not following anyone yet.'}
        </p>
      ) : (
        <ul className="list-unstyled mb-0">
          {users.map((user) => (
            <li key={user.userId} className="border-bottom">
              <UserListItem user={user} />
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
