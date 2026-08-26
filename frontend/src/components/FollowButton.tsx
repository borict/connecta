import { useState } from 'react'
import { errorMessage } from '../api/errorMessage'
import { followUser, unfollowUser } from '../api/social'

type FollowButtonProps = {
  userId: string
  following: boolean
  pending: boolean
  onChanged: () => Promise<void>
}

export function FollowButton({ userId, following, pending, onChanged }: FollowButtonProps) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleClick() {
    if (busy) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      if (following || pending) {
        await unfollowUser(userId)
      } else {
        await followUser(userId)
      }
      await onChanged()
    } catch (err) {
      setError(errorMessage(err, 'Could not update follow'))
    } finally {
      setBusy(false)
    }
  }

  const label = following ? 'Unfollow' : pending ? 'Requested' : 'Follow'
  const className =
    following || pending ? 'btn btn-outline-secondary btn-sm' : 'btn btn-primary btn-sm'

  return (
    <div>
      <button
        type="button"
        className={className}
        onClick={() => void handleClick()}
        disabled={busy}
        aria-label={pending ? 'Cancel follow request' : label}
        aria-pressed={following || pending}
      >
        {busy ? 'Saving…' : label}
      </button>
      {error ? <div className="text-danger small mt-1">{error}</div> : null}
    </div>
  )
}
