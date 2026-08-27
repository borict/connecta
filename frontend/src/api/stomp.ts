import { Client } from '@stomp/stompjs'
import { getWsBaseUrl } from './client'
import type { MessageResponse } from '../types/api'

export const CHAT_SEND_DESTINATION = '/app/chat.send'

export function conversationTopic(conversationId: string): string {
  return `/topic/conversations.${conversationId}`
}

export function createChatStompClient(token: string): Client {
  return new Client({
    brokerURL: `${getWsBaseUrl()}/ws?token=${encodeURIComponent(token)}`,
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })
}

export function parseChatMessage(body: string | undefined): MessageResponse | null {
  if (!body) {
    return null
  }
  try {
    const payload = JSON.parse(body) as Record<string, unknown>
    const id = asUuidString(payload.id)
    const conversationId = asUuidString(payload.conversationId)
    const senderId = asUuidString(payload.senderId)
    const content = typeof payload.content === 'string' ? payload.content : null
    const createdAt = asIsoDate(payload.createdAt)
    if (!id || !conversationId || !senderId || content == null || !createdAt) {
      return null
    }
    return { id, conversationId, senderId, content, createdAt }
  } catch {
    return null
  }
}

function asUuidString(value: unknown): string | null {
  if (typeof value === 'string' && value.length > 0) {
    return value
  }
  return null
}

function asIsoDate(value: unknown): string | null {
  if (typeof value === 'string' && value.length > 0) {
    return value
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? null : date.toISOString()
  }
  if (Array.isArray(value) && value.length >= 3 && value.every((part) => typeof part === 'number')) {
    const [year, month, day, hour = 0, minute = 0, second = 0, nano = 0] = value
    const date = new Date(Date.UTC(year, month - 1, day, hour, minute, second, Math.floor(nano / 1e6)))
    return Number.isNaN(date.getTime()) ? null : date.toISOString()
  }
  return null
}
