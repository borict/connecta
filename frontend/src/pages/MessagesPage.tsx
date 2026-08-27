import { fetchConversations } from '../api/conversations'
import { useAuth } from '../auth/AuthContext'
import { ConversationItem } from '../components/ConversationItem'
import { InfiniteScrollSentinel } from '../components/InfiniteScrollSentinel'
import { usePagedList } from '../lib/usePagedList'
import type { ConversationResponse } from '../types/api'

function conversationId(conversation: ConversationResponse): string {
  return conversation.conversationId
}

export function MessagesPage() {
  const { user } = useAuth()
  const { items: conversations, loading, loadingMore, error, loadMoreError, hasMore, loadMore } =
    usePagedList<ConversationResponse>({
      resetKey: 'inbox',
      loadPage: (page) => fetchConversations(page),
      getId: conversationId,
      fallbackError: 'Could not load messages',
    })

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
          <InfiniteScrollSentinel
            disabled={!hasMore}
            loading={loadingMore}
            error={loadMoreError}
            onVisible={loadMore}
            onRetry={loadMore}
          />
        </div>
      )}
    </>
  )
}
