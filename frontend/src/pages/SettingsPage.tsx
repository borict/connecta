import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/errorMessage'
import { updateMe } from '../api/users'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../components/Avatar'
import type { Gender, UpdateProfileRequest } from '../types/api'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

function isGender(value: string): value is Gender {
  return value === 'FEMALE' || value === 'MALE' || value === 'OTHER'
}

export function SettingsPage() {
  const navigate = useNavigate()
  const { user, establishSession } = useAuth()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [bio, setBio] = useState(user?.bio ?? '')
  const [location, setLocation] = useState(user?.location ?? '')
  const [gender, setGender] = useState<Gender | ''>(user?.gender ?? '')
  const [isPrivate, setIsPrivate] = useState(user?.isPrivate ?? false)
  const [profilePicture, setProfilePicture] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const previewUrl = profilePicture ? URL.createObjectURL(profilePicture) : null

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    }
  }, [previewUrl])

  if (!user) {
    return null
  }

  const profilePath = `/u/${encodeURIComponent(user.username)}`

  function handleImageChange(file: File | null) {
    setError(null)
    if (!file) {
      setProfilePicture(null)
      return
    }
    if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
      setError('Profile picture must be JPEG, PNG, or WebP')
      setProfilePicture(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      return
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setError('Profile picture must be at most 5MB')
      setProfilePicture(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      return
    }
    setProfilePicture(file)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const nextDisplayName = String(form.get('displayName') ?? displayName).trim()
    const nextBio = String(form.get('bio') ?? bio)
    const nextLocation = String(form.get('location') ?? location)
    const nextGender = String(form.get('gender') ?? gender)
    const privateBox = event.currentTarget.elements.namedItem('isPrivate')
    const nextPrivate = privateBox instanceof HTMLInputElement ? privateBox.checked : isPrivate
    const fileFromForm = form.get('profilePicture')
    const nextPicture =
      fileFromForm instanceof File && fileFromForm.size > 0 ? fileFromForm : profilePicture

    if (!nextDisplayName) {
      setError('Display name is required')
      return
    }

    if (nextPicture && !ALLOWED_IMAGE_TYPES.has(nextPicture.type)) {
      setError('Profile picture must be JPEG, PNG, or WebP')
      return
    }
    if (nextPicture && nextPicture.size > MAX_IMAGE_BYTES) {
      setError('Profile picture must be at most 5MB')
      return
    }

    const data: UpdateProfileRequest = {
      displayName: nextDisplayName,
      bio: nextBio,
      location: nextLocation,
      isPrivate: nextPrivate,
    }
    if (isGender(nextGender)) {
      data.gender = nextGender
    }

    setError(null)
    setSubmitting(true)
    try {
      const updated = await updateMe(data, nextPicture)
      establishSession(updated)
      navigate(profilePath)
    } catch (err) {
      setError(errorMessage(err, 'Could not update profile'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <h1 className="h4 mb-3">Edit profile</h1>
      {error ? <div className="alert alert-danger py-2">{error}</div> : null}
      <form onSubmit={handleSubmit}>
        <div className="d-flex align-items-center gap-3 mb-3">
          <Avatar
            name={displayName || user.displayName}
            username={user.username}
            src={previewUrl ?? user.profilePictureUrl}
            size={72}
          />
          <div className="min-w-0">
            <div className="fw-semibold text-truncate">{user.displayName}</div>
            <div className="text-secondary small">@{user.username}</div>
          </div>
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="username">
            Username
          </label>
          <input id="username" className="form-control" value={user.username} disabled />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="email">
            Email
          </label>
          <input id="email" className="form-control" value={user.email} disabled />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="displayName">
            Display name
          </label>
          <input
            id="displayName"
            name="displayName"
            className="form-control"
            autoComplete="name"
            maxLength={100}
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="bio">
            Bio
          </label>
          <textarea
            id="bio"
            name="bio"
            className="form-control"
            rows={3}
            maxLength={100}
            value={bio}
            onChange={(event) => setBio(event.target.value)}
          />
          <div className="form-text">{bio.length}/100</div>
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="location">
            Location
          </label>
          <input
            id="location"
            name="location"
            className="form-control"
            autoComplete="address-level2"
            maxLength={100}
            value={location}
            onChange={(event) => setLocation(event.target.value)}
          />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="gender">
            Gender
          </label>
          <select
            id="gender"
            name="gender"
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
            Profile picture
          </label>
          <input
            id="profilePicture"
            name="profilePicture"
            ref={fileInputRef}
            type="file"
            className="form-control"
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => handleImageChange(event.target.files?.[0] ?? null)}
          />
          <div className="form-text">JPEG, PNG, or WebP. Max 5MB. Leave empty to keep the current photo.</div>
        </div>
        <div className="form-check mb-4">
          <input
            id="isPrivate"
            name="isPrivate"
            className="form-check-input"
            type="checkbox"
            checked={isPrivate}
            onChange={(event) => setIsPrivate(event.target.checked)}
          />
          <label className="form-check-label" htmlFor="isPrivate">
            Private profile
          </label>
        </div>
        <div className="d-flex gap-2">
          <button className="btn btn-primary" type="submit" disabled={submitting}>
            {submitting ? 'Saving…' : 'Save changes'}
          </button>
          <Link className="btn btn-outline-secondary" to={profilePath}>
            Cancel
          </Link>
        </div>
      </form>
    </>
  )
}
