import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { CONVERSATION_UNREAD_PAGE_SIZE, fetchConversations, totalConversationUnread } from '../api/conversations'

type MessageUnreadContextValue = {
  unreadMessages: number
  refreshUnreadMessages: () => Promise<void>
}

const MessageUnreadContext = createContext<MessageUnreadContextValue | null>(null)

export function MessageUnreadProvider({ children }: { children: ReactNode }) {
  const location = useLocation()
  const [unreadMessages, setUnreadMessages] = useState(0)

  const refreshUnreadMessages = useCallback(async () => {
    try {
      const page = await fetchConversations(0, CONVERSATION_UNREAD_PAGE_SIZE)
      setUnreadMessages(totalConversationUnread(page.content))
    } catch {
      // Keep the last known count on a transient failure.
    }
  }, [])

  useEffect(() => {
    void refreshUnreadMessages()
  }, [refreshUnreadMessages, location.pathname])

  const value = useMemo<MessageUnreadContextValue>(
    () => ({ unreadMessages, refreshUnreadMessages }),
    [unreadMessages, refreshUnreadMessages],
  )

  return <MessageUnreadContext.Provider value={value}>{children}</MessageUnreadContext.Provider>
}

export function useMessageUnread(): MessageUnreadContextValue {
  const value = useContext(MessageUnreadContext)
  if (!value) {
    throw new Error('useMessageUnread must be used within MessageUnreadProvider')
  }
  return value
}
