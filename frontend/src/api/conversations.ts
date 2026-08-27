import { api, withQuery } from './client'
import type { ConversationResponse, MessageResponse, PageResponse } from '../types/api'

export const CONVERSATION_PAGE_SIZE = 20
export const CONVERSATION_UNREAD_PAGE_SIZE = 50
export const MESSAGE_PAGE_SIZE = 20
export const MESSAGE_MAX_LENGTH = 2000

export function fetchConversations(
  page = 0,
  size = CONVERSATION_PAGE_SIZE,
): Promise<PageResponse<ConversationResponse>> {
  return api.get<PageResponse<ConversationResponse>>(withQuery('/api/conversations', { page, size }))
}

export function totalConversationUnread(conversations: ConversationResponse[]): number {
  return conversations.reduce((sum, conversation) => sum + (conversation.unreadCount || 0), 0)
}

export function getOrCreateConversation(userId: string): Promise<ConversationResponse> {
  return api.post<ConversationResponse>(`/api/conversations/users/${encodeURIComponent(userId)}`)
}

export function fetchMessages(
  userId: string,
  page = 0,
  size = MESSAGE_PAGE_SIZE,
): Promise<PageResponse<MessageResponse>> {
  return api.get<PageResponse<MessageResponse>>(
    withQuery(`/api/conversations/users/${encodeURIComponent(userId)}/messages`, { page, size }),
  )
}

export function sendMessage(userId: string, content: string): Promise<MessageResponse> {
  return api.post<MessageResponse>(`/api/conversations/users/${encodeURIComponent(userId)}/messages`, { content })
}

export function markConversationRead(userId: string): Promise<void> {
  return api.put<void>(`/api/conversations/users/${encodeURIComponent(userId)}/read`)
}
