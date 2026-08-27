import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  banAdminUser,
  deactivateAdminUser,
  fetchAdminUsers,
  restoreAdminUser,
  unbanAdminUser,
} from '../api/admin'
import { errorMessage } from '../api/errorMessage'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../components/Avatar'
import { InfiniteScrollSentinel } from '../components/InfiniteScrollSentinel'
import { formatPostTime } from '../lib/formatTime'
import { usePagedList } from '../lib/usePagedList'
import type { AdminUserResponse } from '../types/api'

function adminUserId(user: AdminUserResponse): string {
  return user.id
}

export function AdminPage() {
  const { user: me } = useAuth()
  const [busyId, setBusyId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { items: users, setItems: setUsers, loading, loadingMore, error, loadMoreError, hasMore, loadMore } =
    usePagedList<AdminUserResponse>({
      resetKey: 'admin-users',
      loadPage: (page) => fetchAdminUsers(page),
      getId: adminUserId,
      fallbackError: 'Could not load users',
    })

  async function runAction(
    userId: string,
    action: (id: string) => Promise<AdminUserResponse>,
    confirmMessage?: string,
  ) {
    if (busyId) {
      return
    }
    if (confirmMessage && !window.confirm(confirmMessage)) {
      return
    }
    setActionError(null)
    setBusyId(userId)
    try {
      const updated = await action(userId)
      setUsers((current) => current.map((user) => (user.id === updated.id ? updated : user)))
    } catch (err) {
      setActionError(errorMessage(err, 'Could not update user'))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <h1 className="h4 mb-3">Admin</h1>
      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading users…</span>
          </div>
        </div>
      ) : error ? (
        <div className="alert alert-danger">{error}</div>
      ) : (
        <>
          {actionError ? <div className="alert alert-danger py-2">{actionError}</div> : null}
          {users.length === 0 ? (
            <p className="text-secondary mb-0">No users found.</p>
          ) : (
            <>
              <div>
                {users.map((user) => (
                  <AdminUserRow
                    key={user.id}
                    user={user}
                    isSelf={Boolean(me && user.id === me.id)}
                    busy={busyId === user.id}
                    disabled={Boolean(busyId)}
                    onBan={() =>
                      void runAction(user.id, banAdminUser, `Ban @${user.username}? They will not be able to log in.`)
                    }
                    onUnban={() => void runAction(user.id, unbanAdminUser)}
                    onDeactivate={() =>
                      void runAction(
                        user.id,
                        deactivateAdminUser,
                        `Deactivate @${user.username}? Their account will be turned off.`,
                      )
                    }
                    onRestore={() => void runAction(user.id, restoreAdminUser)}
                  />
                ))}
              </div>
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

type AdminUserRowProps = {
  user: AdminUserResponse
  isSelf: boolean
  busy: boolean
  disabled: boolean
  onBan: () => void
  onUnban: () => void
  onDeactivate: () => void
  onRestore: () => void
}

function AdminUserRow({
  user,
  isSelf,
  busy,
  disabled,
  onBan,
  onUnban,
  onDeactivate,
  onRestore,
}: AdminUserRowProps) {
  const profilePath = `/u/${encodeURIComponent(user.username)}`

  return (
    <div className="border-bottom py-3">
      <div className="d-flex align-items-start gap-2">
        <Link to={profilePath} className="flex-shrink-0">
          <Avatar name={user.displayName} username={user.username} src={null} />
        </Link>
        <div className="min-w-0 flex-grow-1">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <Link to={profilePath} className="fw-semibold text-decoration-none text-reset text-truncate">
              {user.displayName}
            </Link>
            {isSelf ? <span className="badge text-bg-light border">You</span> : null}
            {user.role === 'ADMIN' ? <span className="badge text-bg-primary">Admin</span> : null}
            {user.isBanned ? <span className="badge text-bg-danger">Banned</span> : null}
            {user.isActive ? null : <span className="badge text-bg-secondary">Inactive</span>}
            {user.isPrivate ? <span className="badge text-bg-light border">Private</span> : null}
          </div>
          <div className="text-secondary small">@{user.username}</div>
          <div className="text-secondary small text-truncate">{user.email}</div>
          <div className="text-secondary small">Joined {formatPostTime(user.createdAt)}</div>
          {isSelf ? (
            <p className="text-secondary small mb-0 mt-2">You cannot change your own account here.</p>
          ) : (
            <div className="d-flex flex-wrap gap-2 mt-2">
              {user.isBanned ? (
                <button type="button" className="btn btn-outline-secondary btn-sm" onClick={onUnban} disabled={disabled}>
                  {busy ? 'Saving…' : 'Unban'}
                </button>
              ) : (
                <button type="button" className="btn btn-outline-danger btn-sm" onClick={onBan} disabled={disabled}>
                  {busy ? 'Saving…' : 'Ban'}
                </button>
              )}
              {user.isActive ? (
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={onDeactivate}
                  disabled={disabled}
                >
                  {busy ? 'Saving…' : 'Deactivate'}
                </button>
              ) : (
                <button type="button" className="btn btn-outline-primary btn-sm" onClick={onRestore} disabled={disabled}>
                  {busy ? 'Saving…' : 'Restore'}
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
