import { api, withQuery } from './client'
import type { PageResponse, UserProfileResponse, UserSummaryResponse } from '../types/api'

export const USER_SEARCH_PAGE_SIZE = 20

export function fetchUserByUsername(username: string): Promise<UserProfileResponse> {
  return api.get<UserProfileResponse>(`/api/users/by-username/${encodeURIComponent(username)}`)
}

export function searchUsers(
  query: string,
  page = 0,
  size = USER_SEARCH_PAGE_SIZE,
): Promise<PageResponse<UserSummaryResponse>> {
  return api.get<PageResponse<UserSummaryResponse>>(withQuery('/api/users/search', { q: query, page, size }))
}
