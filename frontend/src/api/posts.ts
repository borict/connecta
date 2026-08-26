import { api, jsonPart, withQuery } from './client'
import type {
  CommentResponse,
  LikeCountResponse,
  LikedResponse,
  LikeResponse,
  PageResponse,
  PostResponse,
} from '../types/api'

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

export const COMMENT_PAGE_SIZE = 20

export function fetchComments(
  postId: string,
  page = 0,
  size = COMMENT_PAGE_SIZE,
): Promise<PageResponse<CommentResponse>> {
  return api.get<PageResponse<CommentResponse>>(withQuery(`/api/posts/${postId}/comments`, { page, size }))
}

export function createComment(postId: string, content: string): Promise<CommentResponse> {
  return api.post<CommentResponse>(`/api/posts/${postId}/comments`, { content })
}

export function deleteComment(commentId: string): Promise<void> {
  return api.delete(`/api/posts/comments/${commentId}`)
}

export function deletePost(postId: string): Promise<void> {
  return api.delete(`/api/posts/${postId}`)
}

export const USER_POSTS_PAGE_SIZE = 20

export function fetchUserPosts(
  userId: string,
  page = 0,
  size = USER_POSTS_PAGE_SIZE,
): Promise<PageResponse<PostResponse>> {
  return api.get<PageResponse<PostResponse>>(withQuery(`/api/posts/user/${userId}`, { page, size }))
}
