import { api } from './client'
import type { FollowResponse, FollowStateResponse, FollowStatsResponse } from '../types/api'

export function fetchFollowStats(userId: string): Promise<FollowStatsResponse> {
  return api.get<FollowStatsResponse>(`/api/social/${userId}/stats`)
}

export function fetchFollowState(userId: string): Promise<FollowStateResponse> {
  return api.get<FollowStateResponse>(`/api/social/${userId}/is-following`)
}

export function followUser(userId: string): Promise<FollowResponse> {
  return api.post<FollowResponse>(`/api/social/${userId}`)
}

export function unfollowUser(userId: string): Promise<void> {
  return api.delete(`/api/social/${userId}`)
}
