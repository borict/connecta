import { clearAccessToken, getAccessToken } from './token'
import type { ApiErrorResponse } from '../types/api'

const DEFAULT_API_BASE_URL = 'http://localhost:8080'

export class ApiError extends Error {
  readonly status: number
  readonly body: ApiErrorResponse | null

  constructor(status: number, message: string, body: ApiErrorResponse | null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

export function getApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL?.trim()
  const base = configured && configured.length > 0 ? configured : DEFAULT_API_BASE_URL
  return base.replace(/\/$/, '')
}

export function getWsBaseUrl(): string {
  return getApiBaseUrl().replace(/^http/i, 'ws')
}

export function jsonPart(value: unknown): Blob {
  return new Blob([JSON.stringify(value)], { type: 'application/json' })
}

export function withQuery(
  path: string,
  params: Record<string, string | number | boolean | undefined | null>,
): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) {
      continue
    }
    search.set(key, String(value))
  }
  const query = search.toString()
  if (!query) {
    return path
  }
  return `${path}${path.includes('?') ? '&' : '?'}${query}`
}

async function parseBody(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text) {
    return null
  }
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function toApiError(status: number, body: unknown, fallback: string): ApiError {
  if (body && typeof body === 'object' && 'message' in body) {
    const apiBody = body as ApiErrorResponse
    return new ApiError(status, apiBody.message || fallback, apiBody)
  }
  if (typeof body === 'string' && body.length > 0) {
    return new ApiError(status, body, null)
  }
  return new ApiError(status, fallback, null)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = getAccessToken()
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers,
  })

  if (response.status === 401) {
    clearAccessToken()
  }

  if (response.status === 204 || response.status === 205) {
    return undefined as T
  }

  const body = await parseBody(response)
  if (!response.ok) {
    throw toApiError(response.status, body, response.statusText || 'Request failed')
  }
  return body as T
}

function jsonHeaders(headers?: HeadersInit): Headers {
  const next = new Headers(headers)
  if (!next.has('Content-Type')) {
    next.set('Content-Type', 'application/json')
  }
  return next
}

export const api = {
  get<T>(path: string): Promise<T> {
    return request<T>(path, { method: 'GET' })
  },

  post<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, {
      method: 'POST',
      headers: jsonHeaders(),
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  },

  put<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, {
      method: 'PUT',
      headers: jsonHeaders(),
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  },

  patch<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, {
      method: 'PATCH',
      headers: jsonHeaders(),
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  },

  delete(path: string): Promise<void> {
    return request<void>(path, { method: 'DELETE' })
  },

  postMultipart<T>(path: string, formData: FormData): Promise<T> {
    return request<T>(path, { method: 'POST', body: formData })
  },

  putMultipart<T>(path: string, formData: FormData): Promise<T> {
    return request<T>(path, { method: 'PUT', body: formData })
  },
}
