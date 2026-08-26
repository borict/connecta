import { useSearchParams } from 'react-router-dom'

export function SearchPage() {
  const [searchParams] = useSearchParams()
  const query = searchParams.get('q')?.trim() ?? ''

  return (
    <>
      <h1 className="h4 mb-3">Search</h1>
      {query.length >= 2 ? (
        <p className="text-secondary mb-0">
          Results for <span className="text-dark fw-medium">&ldquo;{query}&rdquo;</span> will appear here.
        </p>
      ) : (
        <p className="text-secondary mb-0">Type at least 2 characters in the search box.</p>
      )}
    </>
  )
}
