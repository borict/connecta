import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { errorMessage } from '../api/errorMessage'
import { searchUsers } from '../api/users'
import { UserListItem } from '../components/UserListItem'
import type { UserSummaryResponse } from '../types/api'

export function SearchPage() {
  const [searchParams] = useSearchParams()
  const query = searchParams.get('q')?.trim() ?? ''
  const [results, setResults] = useState<UserSummaryResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (query.length < 2) {
      setResults([])
      setError(null)
      setLoading(false)
      return
    }

    let cancelled = false

    async function load() {
      setError(null)
      setLoading(true)
      try {
        const page = await searchUsers(query)
        if (!cancelled) {
          setResults(page.content)
        }
      } catch (err) {
        if (!cancelled) {
          setError(errorMessage(err, 'Could not search people'))
          setResults([])
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [query])

  return (
    <>
      <h1 className="h4 mb-3">Search</h1>
      {query.length < 2 ? (
        <p className="text-secondary mb-0">Type at least 2 characters in the search box.</p>
      ) : loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Searching…</span>
          </div>
        </div>
      ) : error ? (
        <div className="alert alert-danger">{error}</div>
      ) : results.length === 0 ? (
        <p className="text-secondary mb-0">
          No people found for <span className="text-dark fw-medium">&ldquo;{query}&rdquo;</span>.
        </p>
      ) : (
        <>
          <p className="text-secondary mb-3">
            Results for <span className="text-dark fw-medium">&ldquo;{query}&rdquo;</span>
          </p>
          <ul className="list-unstyled mb-0">
            {results.map((user) => (
              <li key={user.id} className="border-bottom">
                <UserListItem user={user} />
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  )
}
