import { api, jsonPart } from './client'
import { setSession } from './token'
import type { LoginRequest, LoginResponse, RegisterRequest, UserMeResponse } from '../types/api'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/api/auth/login', request)
  setSession(response.token, response.user)
  return response
}

export async function register(
  data: RegisterRequest,
  profilePicture?: File | null,
): Promise<UserMeResponse> {
  const formData = new FormData()
  formData.append('data', jsonPart(data), 'data.json')
  if (profilePicture && profilePicture.size > 0) {
    formData.append('profilePicture', profilePicture)
  }
  return api.postMultipart<UserMeResponse>('/api/auth/register', formData)
}

export async function registerAndLogin(
  data: RegisterRequest,
  profilePicture?: File | null,
): Promise<LoginResponse> {
  await register(data, profilePicture)
  return login({
    usernameOrEmail: data.username,
    password: data.password,
  })
}

export function fetchCurrentUser(): Promise<UserMeResponse> {
  return api.get<UserMeResponse>('/api/users/me')
}
