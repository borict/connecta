import { useCallback, useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { getAccessToken } from '../api/token'
import {
  CHAT_SEND_DESTINATION,
  conversationTopic,
  createChatStompClient,
  parseChatMessage,
} from '../api/stomp'
import type { MessageResponse } from '../types/api'

export function useChatSocket(
  conversationId: string | null,
  onMessage: (message: MessageResponse) => void,
): { connected: boolean; sendLive: (content: string) => boolean } {
  const [connected, setConnected] = useState(false)
  const clientRef = useRef<Client | null>(null)
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  useEffect(() => {
    if (!conversationId) {
      return
    }
    const token = getAccessToken()
    if (!token) {
      return
    }

    const client = createChatStompClient(token)
    client.onConnect = () => {
      client.subscribe(conversationTopic(conversationId), (frame) => {
        const payload = parseChatMessage(frame.body)
        if (payload && payload.conversationId === conversationId) {
          onMessageRef.current(payload)
        }
      })
      setConnected(true)
    }
    client.onDisconnect = () => setConnected(false)
    client.onStompError = () => setConnected(false)
    client.onWebSocketClose = () => setConnected(false)
    clientRef.current = client
    client.activate()

    return () => {
      setConnected(false)
      if (clientRef.current === client) {
        clientRef.current = null
      }
      void client.deactivate()
    }
  }, [conversationId])

  const sendLive = useCallback(
    (content: string): boolean => {
      const client = clientRef.current
      if (!client?.connected || !conversationId) {
        return false
      }
      client.publish({
        destination: CHAT_SEND_DESTINATION,
        body: JSON.stringify({ conversationId, content }),
        headers: {
          'content-type': 'application/json',
          Authorization: `Bearer ${getAccessToken() ?? ''}`,
        },
      })
      return true
    },
    [conversationId],
  )

  return { connected, sendLive }
}
