export function AuthLoadingScreen() {
  return (
    <div className="d-flex min-vh-100 justify-content-center align-items-center">
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">Loading…</span>
      </div>
    </div>
  )
}
