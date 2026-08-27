import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/errorMessage'
import { fetchNotifications, markAllNotificationsRead, markNotificationRead } from '../api/notifications'
import { fetchUsersByIds } from '../api/users'
import { NotificationItem } from '../components/NotificationItem'
import { notificationPath } from '../lib/notificationPath'
import { useUnreadCount } from '../notifications/UnreadCountContext'
import type { NotificationResponse, UserSummaryResponse } from '../types/api'

export function NotificationsPage() {
  const navigate = useNavigate()
  const { setUnreadCount, refreshUnreadCount } = useUnreadCount()
  const [notifications, setNotifications] = useState<NotificationResponse[]>([])
  const [actors, setActors] = useState<Record<string, UserSummaryResponse>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [markingAll, setMarkingAll] = useState(false)

  const unreadOnPage = notifications.some((notification) => !notification.read)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setError(null)
      setLoading(true)
      try {
        const page = await fetchNotifications()
        const actorIds = page.content.map((item) => item.actorId)
        let actorMap: Record<string, UserSummaryResponse> = {}
        try {
          const users = await fetchUsersByIds(actorIds)
          actorMap = Object.fromEntries(users.map((user) => [user.id, user]))
        } catch {
          actorMap = {}
        }
        if (!cancelled) {
          setNotifications(page.content)
          setActors(actorMap)
        }
      } catch (err) {
        if (!cancelled) {
          setError(errorMessage(err, 'Could not load notifications'))
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

  async function handleOpen(notification: NotificationResponse) {
    if (busyId || markingAll) {
      return
    }
    setActionError(null)
    const path = notificationPath(notification, actors[notification.actorId])
    if (!notification.read) {
      setBusyId(notification.id)
      try {
        const updated = await markNotificationRead(notification.id)
        setNotifications((current) =>
          current.map((item) => (item.id === updated.id ? updated : item)),
        )
        setUnreadCount((count) => Math.max(0, count - 1))
      } catch (err) {
        setActionError(errorMessage(err, 'Could not mark notification as read'))
        setBusyId(null)
        return
      }
      setBusyId(null)
    }
    if (path) {
      navigate(path)
    }
  }

  async function handleMarkAll() {
    if (markingAll || busyId || !unreadOnPage) {
      return
    }
    setActionError(null)
    setMarkingAll(true)
    try {
      await markAllNotificationsRead()
      setNotifications((current) => current.map((item) => ({ ...item, read: true })))
      setUnreadCount(0)
      await refreshUnreadCount()
    } catch (err) {
      setActionError(errorMessage(err, 'Could not mark notifications as read'))
    } finally {
      setMarkingAll(false)
    }
  }

  return (
    <>
      <div className="d-flex align-items-center justify-content-between gap-2 mb-3">
        <h1 className="h4 mb-0">Notifications</h1>
        {unreadOnPage ? (
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            onClick={() => void handleMarkAll()}
            disabled={markingAll || Boolean(busyId)}
          >
            {markingAll ? 'Marking…' : 'Mark all as read'}
          </button>
        ) : null}
      </div>
      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading notifications…</span>
          </div>
        </div>
      ) : error ? (
        <div className="alert alert-danger">{error}</div>
      ) : (
        <>
          {actionError ? <div className="alert alert-danger py-2">{actionError}</div> : null}
          {notifications.length === 0 ? (
            <p className="text-secondary mb-0">No notifications yet.</p>
          ) : (
            <div className="list-group list-group-flush">
              {notifications.map((notification) => (
                <NotificationItem
                  key={notification.id}
                  notification={notification}
                  actor={actors[notification.actorId]}
                  busy={busyId === notification.id || markingAll}
                  onOpen={(item) => void handleOpen(item)}
                />
              ))}
            </div>
          )}
        </>
      )}
    </>
  )
}
