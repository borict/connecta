import { api, jsonPart } from './client'
import type { PostResponse } from '../types/api'

export async function createPost(content: string, image?: File | null): Promise<PostResponse> {
  const formData = new FormData()
  formData.append('data', jsonPart({ content }), 'data.json')
  if (image && image.size > 0) {
    formData.append('image', image)
  }
  return api.postMultipart<PostResponse>('/api/posts', formData)
}
