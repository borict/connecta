import { api, withQuery } from './client'
import type { AdminUserResponse, PageResponse } from '../types/api'

export const ADMIN_USER_PAGE_SIZE = 20

export function fetchAdminUsers(
  page = 0,
  size = ADMIN_USER_PAGE_SIZE,
): Promise<PageResponse<AdminUserResponse>> {
  return api.get<PageResponse<AdminUserResponse>>(withQuery('/api/admin/users', { page, size }))
}

export function banAdminUser(userId: string): Promise<AdminUserResponse> {
  return api.patch<AdminUserResponse>(`/api/admin/users/${encodeURIComponent(userId)}/ban`)
}

export function unbanAdminUser(userId: string): Promise<AdminUserResponse> {
  return api.patch<AdminUserResponse>(`/api/admin/users/${encodeURIComponent(userId)}/unban`)
}

export function deactivateAdminUser(userId: string): Promise<AdminUserResponse> {
  return api.patch<AdminUserResponse>(`/api/admin/users/${encodeURIComponent(userId)}/deactivate`)
}

export function restoreAdminUser(userId: string): Promise<AdminUserResponse> {
  return api.patch<AdminUserResponse>(`/api/admin/users/${encodeURIComponent(userId)}/restore`)
}
