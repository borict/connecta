import { Link } from 'react-router-dom'

export function RegisterPage() {
  return (
    <div className="container py-5" style={{ maxWidth: 440 }}>
      <h1 className="h3 text-center mb-4">
        <Link to="/" className="text-decoration-none text-dark">
          Connecta
        </Link>
      </h1>
      <div className="card shadow-sm">
        <div className="card-body p-4">
          <h2 className="h5 mb-3">Create an account</h2>
          <p className="text-secondary mb-3">Registration form comes in a later step.</p>
          <p className="mb-0">
            Already have an account? <Link to="/login">Log in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
