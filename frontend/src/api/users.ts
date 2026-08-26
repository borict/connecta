import { api } from './client'
import type { UserProfileResponse } from '../types/api'

export function fetchUserByUsername(username: string): Promise<UserProfileResponse> {
  return api.get<UserProfileResponse>(`/api/users/by-username/${encodeURIComponent(username)}`)
}
