import { api, withQuery } from './client'
import type {
  FollowResponse,
  FollowStateResponse,
  FollowStatsResponse,
  FollowUserResponse,
  PageResponse,
} from '../types/api'

export const FOLLOW_PAGE_SIZE = 20

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

export function fetchFollowers(
  userId: string,
  page = 0,
  size = FOLLOW_PAGE_SIZE,
): Promise<PageResponse<FollowUserResponse>> {
  return api.get<PageResponse<FollowUserResponse>>(withQuery(`/api/social/${userId}/followers`, { page, size }))
}

export function fetchFollowing(
  userId: string,
  page = 0,
  size = FOLLOW_PAGE_SIZE,
): Promise<PageResponse<FollowUserResponse>> {
  return api.get<PageResponse<FollowUserResponse>>(withQuery(`/api/social/${userId}/following`, { page, size }))
}

export function fetchIncomingRequests(
  page = 0,
  size = FOLLOW_PAGE_SIZE,
): Promise<PageResponse<FollowUserResponse>> {
  return api.get<PageResponse<FollowUserResponse>>(withQuery('/api/social/me/requests', { page, size }))
}

export function acceptFollowRequest(followerId: string): Promise<FollowResponse> {
  return api.post<FollowResponse>(`/api/social/${followerId}/accept`)
}

export function rejectFollowRequest(followerId: string): Promise<void> {
  return api.post(`/api/social/${followerId}/reject`)
}
