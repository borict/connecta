import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerAndLogin } from '../api/auth'
import { errorMessage } from '../api/errorMessage'
import { useAuth } from '../auth/AuthContext'
import type { Gender, RegisterRequest } from '../types/api'

function dateYearsAgo(years: number): string {
  const date = new Date()
  date.setFullYear(date.getFullYear() - years)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function optionalText(value: string): string | undefined {
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : undefined
}

export function RegisterPage() {
  const navigate = useNavigate()
  const { establishSession } = useAuth()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')
  const [bio, setBio] = useState('')
  const [location, setLocation] = useState('')
  const [gender, setGender] = useState<Gender | ''>('')
  const [isPrivate, setIsPrivate] = useState(false)
  const [profilePicture, setProfilePicture] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const maxBirthDate = dateYearsAgo(15)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const data: RegisterRequest = {
        username: username.trim(),
        email: email.trim(),
        password,
        displayName: displayName.trim(),
        dateOfBirth,
        bio: optionalText(bio),
        location: optionalText(location),
        gender: gender || undefined,
        isPrivate,
      }
      const response = await registerAndLogin(data, profilePicture)
      establishSession(response.user)
      navigate('/', { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not register'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="container py-5" style={{ maxWidth: 480 }}>
      <h1 className="h3 text-center mb-4">
        <Link to="/" className="text-decoration-none text-dark">
          Connecta
        </Link>
      </h1>
      <div className="card shadow-sm">
        <div className="card-body p-4">
          <h2 className="h5 mb-3">Create an account</h2>
          {error ? <div className="alert alert-danger py-2">{error}</div> : null}
          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="username">
                Username
              </label>
              <input
                id="username"
                className="form-control"
                autoComplete="username"
                minLength={3}
                maxLength={50}
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                className="form-control"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
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
                autoComplete="new-password"
                minLength={8}
                maxLength={100}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="displayName">
                Display name
              </label>
              <input
                id="displayName"
                className="form-control"
                autoComplete="name"
                maxLength={100}
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="dateOfBirth">
                Date of birth
              </label>
              <input
                id="dateOfBirth"
                type="date"
                className="form-control"
                autoComplete="bday"
                max={maxBirthDate}
                value={dateOfBirth}
                onChange={(event) => setDateOfBirth(event.target.value)}
                required
              />
              <div className="form-text">You must be at least 15 years old.</div>
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="bio">
                Bio <span className="text-secondary">(optional)</span>
              </label>
              <textarea
                id="bio"
                className="form-control"
                rows={2}
                maxLength={100}
                value={bio}
                onChange={(event) => setBio(event.target.value)}
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="location">
                Location <span className="text-secondary">(optional)</span>
              </label>
              <input
                id="location"
                className="form-control"
                autoComplete="address-level2"
                maxLength={100}
                value={location}
                onChange={(event) => setLocation(event.target.value)}
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="gender">
                Gender <span className="text-secondary">(optional)</span>
              </label>
              <select
                id="gender"
                className="form-select"
                value={gender}
                onChange={(event) => setGender(event.target.value as Gender | '')}
              >
                <option value="">Prefer not to say</option>
                <option value="FEMALE">Female</option>
                <option value="MALE">Male</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="profilePicture">
                Profile picture <span className="text-secondary">(optional)</span>
              </label>
              <input
                id="profilePicture"
                type="file"
                className="form-control"
                accept="image/jpeg,image/png,image/webp"
                onChange={(event) => setProfilePicture(event.target.files?.[0] ?? null)}
              />
              <div className="form-text">JPEG, PNG, or WebP. Max 5MB.</div>
            </div>
            <div className="form-check mb-3">
              <input
                id="isPrivate"
                className="form-check-input"
                type="checkbox"
                checked={isPrivate}
                onChange={(event) => setIsPrivate(event.target.checked)}
              />
              <label className="form-check-label" htmlFor="isPrivate">
                Private profile
              </label>
            </div>
            <button className="btn btn-primary w-100" type="submit" disabled={submitting}>
              {submitting ? 'Creating account…' : 'Create account'}
            </button>
          </form>
          <p className="mb-0 mt-3">
            Already have an account? <Link to="/login">Log in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
