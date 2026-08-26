import { api, jsonPart } from './client'
import type { LikeCountResponse, LikedResponse, LikeResponse, PostResponse } from '../types/api'

export async function createPost(content: string, image?: File | null): Promise<PostResponse> {
  const formData = new FormData()
  formData.append('data', jsonPart({ content }), 'data.json')
  if (image && image.size > 0) {
    formData.append('image', image)
  }
  return api.postMultipart<PostResponse>('/api/posts', formData)
}

export function likePost(postId: string): Promise<LikeResponse> {
  return api.post<LikeResponse>(`/api/posts/${postId}/likes`)
}

export function unlikePost(postId: string): Promise<void> {
  return api.delete(`/api/posts/${postId}/likes`)
}

export function fetchLiked(postId: string): Promise<LikedResponse> {
  return api.get<LikedResponse>(`/api/posts/${postId}/liked`)
}

export function fetchLikeCount(postId: string): Promise<LikeCountResponse> {
  return api.get<LikeCountResponse>(`/api/posts/${postId}/likes/count`)
}
