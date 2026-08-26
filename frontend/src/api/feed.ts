import { api, withQuery } from './client'
import type { FeedPostDto, PageResponse } from '../types/api'

export const FEED_PAGE_SIZE = 20

export function fetchFeed(page = 0, size = FEED_PAGE_SIZE): Promise<PageResponse<FeedPostDto>> {
  return api.get<PageResponse<FeedPostDto>>(withQuery('/api/feed', { page, size }))
}
