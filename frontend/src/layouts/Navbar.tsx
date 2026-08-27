import { useEffect, useState, type FormEvent } from 'react'
import { NavLink, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../components/Avatar'

function navClassName({ isActive }: { isActive: boolean }): string {
  return isActive ? 'nav-link active' : 'nav-link'
}

function iconNavClassName({ isActive }: { isActive: boolean }): string {
  return isActive ? 'nav-link px-2 active' : 'nav-link px-2'
}

export function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [query, setQuery] = useState('')

  useEffect(() => {
    setQuery(searchParams.get('q') ?? '')
  }, [searchParams])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formQuery = String(new FormData(event.currentTarget).get('q') ?? '').trim()
    if (formQuery.length < 2) {
      return
    }
    setQuery(formQuery)
    navigate(`/search?q=${encodeURIComponent(formQuery)}`)
  }

  if (!user) {
    return null
  }

  const profilePath = `/u/${encodeURIComponent(user.username)}`

  return (
    <nav className="navbar navbar-expand-lg bg-white border-bottom sticky-top">
      <div className="container" style={{ maxWidth: 1040 }}>
        <NavLink className="navbar-brand fw-semibold me-lg-3" to="/">
          Connecta
        </NavLink>
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#mainNav"
          aria-controls="mainNav"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon" />
        </button>
        <div className="collapse navbar-collapse" id="mainNav">
          <form className="d-flex my-2 my-lg-0 mx-lg-auto" style={{ maxWidth: 360, width: '100%' }} onSubmit={handleSearch} role="search">
            <div className="input-group">
              <input
                className="form-control"
                type="search"
                name="q"
                placeholder="Search people"
                aria-label="Search people"
                minLength={2}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
              <button className="btn btn-outline-secondary" type="submit" aria-label="Search">
                <i className="bi bi-search" />
              </button>
            </div>
          </form>
          <ul className="navbar-nav ms-lg-3 mb-2 mb-lg-0 align-items-lg-center">
            <li className="nav-item">
              <NavLink className={navClassName} to="/" end>
                Home
              </NavLink>
            </li>
            <li className="nav-item">
              <NavLink className={iconNavClassName} to="/requests" title="Follow requests" aria-label="Follow requests">
                <i className="bi bi-person-plus fs-5" />
              </NavLink>
            </li>
            <li className="nav-item">
              <NavLink className={iconNavClassName} to="/messages" title="Messages" aria-label="Messages">
                <i className="bi bi-chat-dots fs-5" />
              </NavLink>
            </li>
            <li className="nav-item">
              <NavLink className={iconNavClassName} to="/notifications" title="Notifications" aria-label="Notifications">
                <i className="bi bi-bell fs-5" />
              </NavLink>
            </li>
            <li className="nav-item dropdown">
              <button
                className="btn nav-link dropdown-toggle d-flex align-items-center gap-2"
                type="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
                aria-label="Account menu"
              >
                <Avatar name={user.displayName} username={user.username} src={user.profilePictureUrl} />
                <span className="d-lg-none">{user.displayName}</span>
              </button>
              <ul className="dropdown-menu dropdown-menu-end">
                <li>
                  <NavLink className="dropdown-item" to={profilePath}>
                    View profile
                  </NavLink>
                </li>
                <li>
                  <NavLink className="dropdown-item" to="/settings">
                    Edit profile
                  </NavLink>
                </li>
                <li>
                  <NavLink className="dropdown-item" to="/requests">
                    Follow requests
                  </NavLink>
                </li>
                <li>
                  <hr className="dropdown-divider" />
                </li>
                <li>
                  <button className="dropdown-item" type="button" onClick={logout}>
                    Log out
                  </button>
                </li>
              </ul>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  )
}
