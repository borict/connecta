import { NavLink, Outlet } from 'react-router-dom'

function navClassName({ isActive }: { isActive: boolean }): string {
  return isActive ? 'nav-link active' : 'nav-link'
}

export function AppLayout() {
  return (
    <>
      <nav className="navbar navbar-expand-lg bg-white border-bottom sticky-top">
        <div className="container" style={{ maxWidth: 960 }}>
          <NavLink className="navbar-brand fw-semibold" to="/">
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
            <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
              <li className="nav-item">
                <NavLink className={navClassName} to="/" end>
                  Home
                </NavLink>
              </li>
              <li className="nav-item">
                <NavLink className={navClassName} to="/login">
                  Log in
                </NavLink>
              </li>
              <li className="nav-item">
                <NavLink className={navClassName} to="/register">
                  Register
                </NavLink>
              </li>
            </ul>
          </div>
        </div>
      </nav>
      <main className="container py-4" style={{ maxWidth: 640 }}>
        <Outlet />
      </main>
    </>
  )
}
