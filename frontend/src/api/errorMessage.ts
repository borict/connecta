import { ApiError } from '../api/client'

export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (error instanceof ApiError && error.message) {
    return error.message
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}
