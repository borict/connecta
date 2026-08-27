import { useCallback, useEffect, useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  fetchMessages,
  getOrCreateConversation,
  markConversationRead,
  MESSAGE_MAX_LENGTH,
  sendMessage,
} from '../api/conversations'
import { errorMessage } from '../api/errorMessage'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../components/Avatar'
import { formatPostTime } from '../lib/formatTime'
import { useMessageUnread } from '../messages/MessageUnreadContext'
import { useChatSocket } from '../messages/useChatSocket'
import type { ConversationResponse, MessageResponse } from '../types/api'

export function ChatPage() {
  const { userId } = useParams()
  const { user: me } = useAuth()
  const { refreshUnreadMessages } = useMessageUnread()
  const listRef = useRef<HTMLDivElement>(null)
  const [conversation, setConversation] = useState<ConversationResponse | null>(null)
  const [messages, setMessages] = useState<MessageResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [sendError, setSendError] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)

  const otherUserId = userId?.trim() ?? ''
  const conversationId = conversation?.conversationId ?? null

  const handleIncoming = useCallback(
    (message: MessageResponse) => {
      if (conversationId && message.conversationId !== conversationId) {
        return
      }
      setMessages((current) => (current.some((item) => item.id === message.id) ? current : [...current, message]))
      if (me && message.senderId !== me.id) {
        void markConversationRead(otherUserId).then(() => refreshUnreadMessages())
      }
    },
    [conversationId, me, otherUserId, refreshUnreadMessages],
  )

  const { connected } = useChatSocket(conversationId, handleIncoming)

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!otherUserId) {
        setError('Conversation not found')
        setLoading(false)
        return
      }
      if (me && otherUserId === me.id) {
        setError('You cannot message yourself')
        setConversation(null)
        setMessages([])
        setLoading(false)
        return
      }

      setError(null)
      setSendError(null)
      setLoading(true)
      try {
        const nextConversation = await getOrCreateConversation(otherUserId)
        const page = await fetchMessages(otherUserId)
        if (!cancelled) {
          setConversation(nextConversation)
          setMessages([...page.content].reverse())
        }
        try {
          await markConversationRead(otherUserId)
          await refreshUnreadMessages()
        } catch {
          // Chat still opens if the read receipt fails.
        }
      } catch (err) {
        if (!cancelled) {
          setError(errorMessage(err, 'Could not open chat'))
          setConversation(null)
          setMessages([])
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
  }, [otherUserId, me, refreshUnreadMessages])

  useLayoutEffect(() => {
    const list = listRef.current
    if (!list) {
      return
    }
    list.scrollTop = list.scrollHeight
  }, [messages, loading])

  async function handleSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const content = String(new FormData(event.currentTarget).get('content') ?? draft).trim()
    if (!content || !otherUserId || sending) {
      return
    }
    setSendError(null)
    setSending(true)
    try {
      const created = await sendMessage(otherUserId, content)
      setMessages((current) => (current.some((item) => item.id === created.id) ? current : [...current, created]))
      setDraft('')
    } catch (err) {
      setSendError(errorMessage(err, 'Could not send message'))
    } finally {
      setSending(false)
    }
  }

  const displayName = conversation?.otherDisplayName || conversation?.otherUsername || 'Chat'
  const username = conversation?.otherUsername
  const profilePath = username ? `/u/${encodeURIComponent(username)}` : null

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading chat…</span>
        </div>
      </div>
    )
  }

  if (error || !conversation) {
    return (
      <>
        <Link to="/messages" className="d-inline-block mb-3 text-decoration-none">
          ← Back to messages
        </Link>
        <div className="alert alert-danger mb-0">{error ?? 'Conversation not found'}</div>
      </>
    )
  }

  return (
    <div className="d-flex flex-column" style={{ minHeight: '70vh' }}>
      <div className="d-flex align-items-center gap-2 mb-3">
        <Link to="/messages" className="text-decoration-none flex-shrink-0">
          ← Messages
        </Link>
        <Avatar
          name={displayName}
          username={username ?? ''}
          src={conversation.otherProfilePictureUrl}
        />
        <div className="min-w-0">
          {profilePath ? (
            <Link to={profilePath} className="fw-semibold text-decoration-none text-reset text-truncate d-block">
              {displayName}
            </Link>
          ) : (
            <div className="fw-semibold text-truncate">{displayName}</div>
          )}
          <div className="d-flex align-items-center gap-2">
            {username ? <div className="text-secondary small">@{username}</div> : null}
            <span className={`small ${connected ? 'text-success' : 'text-secondary'}`}>
              {connected ? 'Live' : 'Connecting…'}
            </span>
          </div>
        </div>
      </div>
      <div ref={listRef} className="flex-grow-1 overflow-auto pe-1" style={{ maxHeight: '55vh' }}>
        {messages.length === 0 ? (
          <p className="text-secondary text-center py-5 mb-0">No messages yet. Say hello.</p>
        ) : (
          messages.map((message) => {
            const mine = Boolean(me && message.senderId === me.id)
            return (
              <div key={message.id} className={`d-flex mb-2 ${mine ? 'justify-content-end' : 'justify-content-start'}`}>
                <div
                  className={`rounded-3 px-3 py-2 ${mine ? 'bg-primary text-white' : 'bg-white border'}`}
                  style={{ maxWidth: '80%', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}
                >
                  {message.content}
                  <div className={`small mt-1 ${mine ? 'text-white-50' : 'text-secondary'}`}>
                    {formatPostTime(message.createdAt)}
                  </div>
                </div>
              </div>
            )
          })
        )}
      </div>
      {sendError ? <div className="alert alert-danger py-2 mt-3 mb-0">{sendError}</div> : null}
      <form className="mt-3" onSubmit={handleSend}>
        <div className="input-group">
          <textarea
            className="form-control"
            name="content"
            rows={2}
            maxLength={MESSAGE_MAX_LENGTH}
            placeholder="Write a message…"
            aria-label="Write a message"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            disabled={sending}
          />
          <button className="btn btn-primary" type="submit" disabled={sending || !draft.trim()}>
            {sending ? 'Sending…' : 'Send'}
          </button>
        </div>
      </form>
    </div>
  )
}
