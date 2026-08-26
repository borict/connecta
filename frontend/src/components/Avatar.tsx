type AvatarProps = {
  name: string
  username: string
  src?: string | null
  size?: number
}

function initials(name: string, username: string): string {
  const source = name.trim() || username.trim() || '?'
  const parts = source.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase()
  }
  return source.slice(0, 2).toUpperCase()
}

export function Avatar({ name, username, src, size = 32 }: AvatarProps) {
  const dimension = { width: size, height: size, fontSize: Math.max(11, size * 0.38) }

  if (src) {
    return (
      <img
        src={src}
        alt={name || username}
        className="rounded-circle object-fit-cover"
        style={dimension}
      />
    )
  }

  return (
    <span
      className="rounded-circle bg-secondary text-white d-inline-flex align-items-center justify-content-center fw-semibold"
      style={dimension}
      aria-hidden="true"
    >
      {initials(name, username)}
    </span>
  )
}
