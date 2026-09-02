import { api, withQuery } from './client'
import { FEED_PAGE_SIZE } from './feed'
import type { FeedPostDto, PageResponse } from '../types/api'

export function fetchExplore(page = 0, size = FEED_PAGE_SIZE): Promise<PageResponse<FeedPostDto>> {
  return api.get<PageResponse<FeedPostDto>>(withQuery('/api/explore', { page, size }))
}
