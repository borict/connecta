import { api, withQuery } from './client'
import type { ConversationResponse, PageResponse } from '../types/api'

export const CONVERSATION_PAGE_SIZE = 20
export const CONVERSATION_UNREAD_PAGE_SIZE = 50

export function fetchConversations(
  page = 0,
  size = CONVERSATION_PAGE_SIZE,
): Promise<PageResponse<ConversationResponse>> {
  return api.get<PageResponse<ConversationResponse>>(withQuery('/api/conversations', { page, size }))
}

export function totalConversationUnread(conversations: ConversationResponse[]): number {
  return conversations.reduce((sum, conversation) => sum + (conversation.unreadCount || 0), 0)
}
