import { createContext, useCallback, useContext, useEffect, useMemo, useState, type Dispatch, type ReactNode, type SetStateAction } from 'react'
import { useLocation } from 'react-router-dom'
import { fetchUnreadCount } from '../api/notifications'

type UnreadCountContextValue = {
  unreadCount: number
  setUnreadCount: Dispatch<SetStateAction<number>>
  refreshUnreadCount: () => Promise<void>
}

const UnreadCountContext = createContext<UnreadCountContextValue | null>(null)

export function UnreadCountProvider({ children }: { children: ReactNode }) {
  const location = useLocation()
  const [unreadCount, setUnreadCount] = useState(0)

  const refreshUnreadCount = useCallback(async () => {
    try {
      const response = await fetchUnreadCount()
      setUnreadCount(response.unreadCount)
    } catch {
      // Keep the last known count on a transient failure.
    }
  }, [])

  useEffect(() => {
    void refreshUnreadCount()
  }, [refreshUnreadCount, location.pathname])

  const value = useMemo<UnreadCountContextValue>(
    () => ({ unreadCount, setUnreadCount, refreshUnreadCount }),
    [unreadCount, refreshUnreadCount],
  )

  return <UnreadCountContext.Provider value={value}>{children}</UnreadCountContext.Provider>
}

export function useUnreadCount(): UnreadCountContextValue {
  const value = useContext(UnreadCountContext)
  if (!value) {
    throw new Error('useUnreadCount must be used within UnreadCountProvider')
  }
  return value
}
