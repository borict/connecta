import { useInfiniteScroll } from '../lib/useInfiniteScroll'

type InfiniteScrollSentinelProps = {
  disabled: boolean
  loading?: boolean
  error?: string | null
  onVisible: () => void
  onRetry?: () => void
  root?: Element | null
}

export function InfiniteScrollSentinel({
  disabled,
  loading = false,
  error = null,
  onVisible,
  onRetry,
  root = null,
}: InfiniteScrollSentinelProps) {
  const ref = useInfiniteScroll(onVisible, { disabled: disabled || Boolean(error), root })

  if (error) {
    return (
      <div className="text-center py-3">
        <p className="text-secondary small mb-2">{error}</p>
        {onRetry ? (
          <button type="button" className="btn btn-outline-secondary btn-sm" onClick={onRetry}>
            Try again
          </button>
        ) : null}
      </div>
    )
  }

  if (disabled && !loading) {
    return null
  }

  return (
    <div ref={ref} className="d-flex justify-content-center py-3">
      {loading ? (
        <div className="spinner-border spinner-border-sm text-primary" role="status">
          <span className="visually-hidden">Loading more…</span>
        </div>
      ) : null}
    </div>
  )
}
