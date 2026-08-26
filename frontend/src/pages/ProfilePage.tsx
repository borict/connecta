import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { errorMessage } from '../api/errorMessage'
import { fetchUserPosts } from '../api/posts'
import { fetchFollowStats } from '../api/social'
import { fetchUserByUsername } from '../api/users'
import { Avatar } from '../components/Avatar'
import { PostCard } from '../components/PostCard'
import type { FeedPostDto, FollowStatsResponse, Gender, UserProfileResponse } from '../types/api'
import { isLimitedProfile, isPublicProfile } from '../types/api'

function genderLabel(gender: Gender | null): string | null {
  if (gender === 'MALE') {
    return 'Male'
  }
  if (gender === 'FEMALE') {
    return 'Female'
  }
  if (gender === 'OTHER') {
    return 'Other'
  }
  return null
}

function countLabel(count: number, singular: string, plural: string): string {
  return `${count} ${count === 1 ? singular : plural}`
}

export function ProfilePage() {
  const { username } = useParams()
  const [profile, setProfile] = useState<UserProfileResponse | null>(null)
  const [stats, setStats] = useState<FollowStatsResponse | null>(null)
  const [posts, setPosts] = useState<FeedPostDto[]>([])
  const [postTotal, setPostTotal] = useState(0)
  const [privatePosts, setPrivatePosts] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setError(null)
      setProfile(null)
      setStats(null)
      setPosts([])
      setPostTotal(0)
      setPrivatePosts(false)
      setLoading(true)

      const handle = username?.trim()
      if (!handle) {
        setError('User not found')
        setLoading(false)
        return
      }

      try {
        const user = await fetchUserByUsername(handle)
        const limited = isLimitedProfile(user)
        const [nextStats, nextPosts] = await Promise.all([
          fetchFollowStats(user.id).catch(() => null),
          limited
            ? Promise.resolve<'private'>('private')
            : fetchUserPosts(user.id).catch((err) => {
                if (err instanceof ApiError && err.status === 403) {
                  return 'private' as const
                }
                throw err
              }),
        ])
        if (cancelled) {
          return
        }
        setProfile(user)
        setStats(nextStats)
        if (nextPosts === 'private') {
          setPrivatePosts(true)
          setPosts([])
          setPostTotal(0)
        } else {
          setPrivatePosts(false)
          setPosts(nextPosts.content)
          setPostTotal(nextPosts.totalElements)
        }
      } catch (err) {
        if (cancelled) {
          return
        }
        if (err instanceof ApiError && err.status === 404) {
          setError('User not found')
        } else {
          setError(errorMessage(err, 'Could not load profile'))
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
  }, [username])

  function handleDeleted(postId: string) {
    setPosts((current) => current.filter((post) => post.id !== postId))
    setPostTotal((current) => Math.max(0, current - 1))
  }

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading profile…</span>
        </div>
      </div>
    )
  }

  if (error || !profile) {
    return <div className="alert alert-danger">{error ?? 'User not found'}</div>
  }

  const publicProfile = isPublicProfile(profile) ? profile : null
  const gender = publicProfile ? genderLabel(publicProfile.gender) : null
  const bio = publicProfile?.bio
  const location = publicProfile?.location

  return (
    <>
      <header className="d-flex gap-3 mb-4">
        <Avatar
          name={profile.displayName}
          username={profile.username}
          src={profile.profilePictureUrl}
          size={88}
        />
        <div className="min-w-0 flex-grow-1">
          <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
            <h1 className="h4 mb-0 text-truncate">{profile.displayName}</h1>
            {profile.isPrivate ? <span className="badge text-bg-light border">Private</span> : null}
          </div>
          <div className="text-secondary mb-2">@{profile.username}</div>
          <div className="d-flex flex-wrap gap-3 small mb-2">
            {!publicProfile ? null : <span>{countLabel(postTotal, 'post', 'posts')}</span>}
            {stats ? (
              <>
                <span>{countLabel(stats.followers, 'follower', 'followers')}</span>
                <span>{countLabel(stats.following, 'following', 'following')}</span>
              </>
            ) : null}
          </div>
          {bio ? <p className="mb-2" style={{ whiteSpace: 'pre-wrap' }}>{bio}</p> : null}
          {location || gender ? (
            <p className="text-secondary small mb-0">
              {location ? (
                <>
                  <i className="bi bi-geo-alt me-1" aria-hidden="true" />
                  {location}
                </>
              ) : null}
              {location && gender ? <span className="mx-2">·</span> : null}
              {gender}
            </p>
          ) : null}
        </div>
      </header>
      {privatePosts ? (
        <div className="card shadow-sm">
          <div className="card-body text-center py-5">
            <i className="bi bi-lock fs-3 text-secondary" aria-hidden="true" />
            <p className="fw-semibold mb-1 mt-2">This account is private</p>
            <p className="text-secondary small mb-0">Only accepted followers can see their posts.</p>
          </div>
        </div>
      ) : posts.length === 0 ? (
        <p className="text-secondary mb-0">No posts yet.</p>
      ) : (
        posts.map((post) => <PostCard key={post.id} post={post} onDeleted={handleDeleted} />)
      )}
    </>
  )
}
