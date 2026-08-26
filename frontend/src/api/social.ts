import { api } from './client'
import type { FollowStatsResponse } from '../types/api'

export function fetchFollowStats(userId: string): Promise<FollowStatsResponse> {
  return api.get<FollowStatsResponse>(`/api/social/${userId}/stats`)
}
