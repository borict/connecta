import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { AuthLoadingScreen } from './AuthLoadingScreen'

export function GuestRoute() {
  const { user, loading } = useAuth()

  if (loading) {
    return <AuthLoadingScreen />
  }
  if (user) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
