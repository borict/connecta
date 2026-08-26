import type { UserMeResponse } from '../types/api'

const TOKEN_KEY = 'connecta.token'
const USER_KEY = 'connecta.user'

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAccessToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getStoredUser(): UserMeResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as UserMeResponse
  } catch {
    return null
  }
}

export function setSession(token: string, user: UserMeResponse): void {
  setAccessToken(token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAccessToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
