import { api, jsonPart, withQuery } from './client'
import type { PageResponse, UpdateProfileRequest, UserMeResponse, UserProfileResponse, UserSummaryResponse } from '../types/api'

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

export function updateMe(
  data: UpdateProfileRequest,
  profilePicture?: File | null,
): Promise<UserMeResponse> {
  const formData = new FormData()
  formData.append('data', jsonPart(data), 'data.json')
  if (profilePicture && profilePicture.size > 0) {
    formData.append('profilePicture', profilePicture)
  }
  return api.putMultipart<UserMeResponse>('/api/users/me', formData)
}

export function fetchUsersByIds(ids: string[]): Promise<UserSummaryResponse[]> {
  const unique = [...new Set(ids.filter((id) => id.length > 0))]
  if (unique.length === 0) {
    return Promise.resolve([])
  }
  return api.get<UserSummaryResponse[]>(withQuery('/api/users/batch', { ids: unique.join(',') }))
}
