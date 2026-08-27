import type { PageResponse } from '../types/api'

export function hasNextPage(page: PageResponse<unknown>): boolean {
  return page.page + 1 < page.totalPages
}

export function mergeUnique<T>(current: T[], incoming: T[], getId: (item: T) => string): T[] {
  if (incoming.length === 0) {
    return current
  }
  const seen = new Set(current.map(getId))
  const extra = incoming.filter((item) => !seen.has(getId(item)))
  return extra.length === 0 ? current : [...current, ...extra]
}
