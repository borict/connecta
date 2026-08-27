import { useCallback, useEffect, useRef, useState } from 'react'
import { errorMessage } from '../api/errorMessage'
import type { PageResponse } from '../types/api'
import { hasNextPage, mergeUnique } from './pagedList'

type UsePagedListOptions<T> = {
  enabled?: boolean
  resetKey: string
  loadPage: (page: number) => Promise<PageResponse<T>>
  getId: (item: T) => string
  fallbackError: string
  onLoaded?: (page: PageResponse<T>, mode: 'replace' | 'append') => void
}

export function usePagedList<T>(options: UsePagedListOptions<T>) {
  const { enabled = true, resetKey, loadPage, getId, fallbackError, onLoaded } = options
  const [items, setItems] = useState<T[]>([])
  const [nextPage, setNextPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(enabled)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loadMoreError, setLoadMoreError] = useState<string | null>(null)
  const loadingMoreRef = useRef(false)
  const loadPageRef = useRef(loadPage)
  const getIdRef = useRef(getId)
  const onLoadedRef = useRef(onLoaded)
  loadPageRef.current = loadPage
  getIdRef.current = getId
  onLoadedRef.current = onLoaded
  const query = enabled ? resetKey : ''
  const [activeQuery, setActiveQuery] = useState(query)
  if (query !== activeQuery) {
    setActiveQuery(query)
    setLoading(enabled)
    setError(null)
    setLoadMoreError(null)
    if (!enabled) {
      setItems([])
      setNextPage(1)
      setHasMore(false)
    }
  }

  useEffect(() => {
    if (!enabled) {
      setItems([])
      setNextPage(1)
      setHasMore(false)
      setLoading(false)
      setError(null)
      setLoadMoreError(null)
      return
    }

    let cancelled = false
    setError(null)
    setLoadMoreError(null)
    setLoading(true)

    void loadPageRef
      .current(0)
      .then((page) => {
        if (cancelled) {
          return
        }
        setItems(page.content)
        setNextPage(1)
        setHasMore(hasNextPage(page))
        onLoadedRef.current?.(page, 'replace')
      })
      .catch((err: unknown) => {
        if (cancelled) {
          return
        }
        setError(errorMessage(err, fallbackError))
        setItems([])
        setHasMore(false)
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [enabled, resetKey, fallbackError])

  const loadMore = useCallback(() => {
    if (!enabled || loading || loadingMoreRef.current || !hasMore) {
      return
    }
    loadingMoreRef.current = true
    setLoadingMore(true)
    setLoadMoreError(null)
    const pageToLoad = nextPage
    void loadPageRef
      .current(pageToLoad)
      .then((page) => {
        setItems((current) => mergeUnique(current, page.content, getIdRef.current))
        setNextPage(pageToLoad + 1)
        setHasMore(hasNextPage(page))
        onLoadedRef.current?.(page, 'append')
      })
      .catch((err: unknown) => {
        setLoadMoreError(errorMessage(err, fallbackError))
      })
      .finally(() => {
        loadingMoreRef.current = false
        setLoadingMore(false)
      })
  }, [enabled, fallbackError, hasMore, loading, nextPage])

  return {
    items,
    setItems,
    loading,
    loadingMore,
    error,
    loadMoreError,
    hasMore,
    loadMore,
  }
}
