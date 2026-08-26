import { useParams } from 'react-router-dom'

export function ProfilePage() {
  const { username } = useParams()

  return (
    <>
      <h1 className="h4 mb-3">{username ? `@${username}` : 'Profile'}</h1>
      <p className="text-secondary mb-0">This profile will appear here.</p>
    </>
  )
}
