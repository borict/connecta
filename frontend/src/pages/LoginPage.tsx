import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { errorMessage } from '../api/errorMessage'

export function LoginPage() {
  const navigate = useNavigate()
  const [usernameOrEmail, setUsernameOrEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login({
        usernameOrEmail: usernameOrEmail.trim(),
        password,
      })
      navigate('/', { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not log in'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="container py-5" style={{ maxWidth: 440 }}>
      <h1 className="h3 text-center mb-4">
        <Link to="/" className="text-decoration-none text-dark">
          Connecta
        </Link>
      </h1>
      <div className="card shadow-sm">
        <div className="card-body p-4">
          <h2 className="h5 mb-3">Log in</h2>
          {error ? <div className="alert alert-danger py-2">{error}</div> : null}
          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="usernameOrEmail">
                Username or email
              </label>
              <input
                id="usernameOrEmail"
                className="form-control"
                autoComplete="username"
                value={usernameOrEmail}
                onChange={(event) => setUsernameOrEmail(event.target.value)}
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                type="password"
                className="form-control"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </div>
            <button className="btn btn-primary w-100" type="submit" disabled={submitting}>
              {submitting ? 'Logging in…' : 'Log in'}
            </button>
          </form>
          <p className="mb-0 mt-3">
            No account? <Link to="/register">Register</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
