import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchCurrentUser } from '../api/auth'
import { clearAccessToken, getAccessToken, getStoredUser, setSession } from '../api/token'
import type { UserMeResponse } from '../types/api'

type AuthContextValue = {
  user: UserMeResponse | null
  loading: boolean
  establishSession: (user: UserMeResponse) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [user, setUser] = useState<UserMeResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function bootstrap() {
      const token = getAccessToken()
      if (!token) {
        if (!cancelled) {
          setUser(null)
          setLoading(false)
        }
        return
      }

      try {
        const me = await fetchCurrentUser()
        if (cancelled) {
          return
        }
        setSession(token, me)
        setUser(me)
      } catch (error) {
        if (cancelled) {
          return
        }
        if (error instanceof ApiError && error.status === 401) {
          setUser(null)
        } else {
          setUser(getStoredUser())
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void bootstrap()
    return () => {
      cancelled = true
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      establishSession(nextUser: UserMeResponse) {
        setUser(nextUser)
      },
      logout() {
        clearAccessToken()
        setUser(null)
        navigate('/login', { replace: true })
      },
    }),
    [user, loading, navigate],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return value
}
