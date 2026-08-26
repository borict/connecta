import { api } from './client'
import { setSession } from './token'
import type { LoginRequest, LoginResponse } from '../types/api'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/api/auth/login', request)
  setSession(response.token, response.user)
  return response
}
