import { useEffect, useState } from 'react'
import { errorMessage } from '../api/errorMessage'
import { fetchConversations } from '../api/conversations'
import { useAuth } from '../auth/AuthContext'
import { ConversationItem } from '../components/ConversationItem'
import type { ConversationResponse } from '../types/api'

export function MessagesPage() {
  const { user } = useAuth()
  const [conversations, setConversations] = useState<ConversationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setError(null)
      setLoading(true)
      try {
        const page = await fetchConversations()
        if (!cancelled) {
          setConversations(page.content)
        }
      } catch (err) {
        if (!cancelled) {
          setError(errorMessage(err, 'Could not load messages'))
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

  return (
    <>
      <h1 className="h4 mb-3">Messages</h1>
      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading messages…</span>
          </div>
        </div>
      ) : error ? (
        <div className="alert alert-danger">{error}</div>
      ) : conversations.length === 0 ? (
        <p className="text-secondary mb-0">No messages yet.</p>
      ) : (
        <div>
          {conversations.map((conversation) => (
            <ConversationItem
              key={conversation.conversationId}
              conversation={conversation}
              currentUserId={user?.id ?? ''}
            />
          ))}
        </div>
      )}
    </>
  )
}
