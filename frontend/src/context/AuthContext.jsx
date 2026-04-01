import { createContext, useContext, useState, useEffect } from 'react'
import { AUTH_EXPIRED_EVENT } from '../services/authFetch'

const JWT_KEY = 'jwt'
const USER_KEY = 'user'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(() => localStorage.getItem(JWT_KEY))
  const [user, setUserState] = useState(() => {
    const stored = localStorage.getItem(USER_KEY)
    return stored ? JSON.parse(stored) : null
  })

  // Evict expired tokens on mount
  useEffect(() => {
    const stored = localStorage.getItem(JWT_KEY)
    if (!stored) return
    try {
      const payload = JSON.parse(atob(stored.split('.')[1]))
      if (payload.exp * 1000 < Date.now()) _clear()
    } catch {
      _clear()
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const handleAuthExpired = () => {
      _clear()
    }

    const handleStorage = (event) => {
      if (event.key === JWT_KEY || event.key === USER_KEY) {
        try {
          const nextToken = localStorage.getItem(JWT_KEY)
          const nextUser = localStorage.getItem(USER_KEY)
          setTokenState(nextToken)
          setUserState(nextUser ? JSON.parse(nextUser) : null)
        } catch {
          _clear()
        }
      }
    }

    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
    window.addEventListener('storage', handleStorage)
    return () => {
      window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
      window.removeEventListener('storage', handleStorage)
    }
  }, [])

  function _persist(data) {
    const userObj = { email: data.email, displayName: data.displayName }
    localStorage.setItem(JWT_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(userObj))
    setTokenState(data.token)
    setUserState(userObj)
  }

  function _clear() {
    localStorage.removeItem(JWT_KEY)
    localStorage.removeItem(USER_KEY)
    setTokenState(null)
    setUserState(null)
  }

  async function login(email, password) {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || 'Invalid credentials')
    }
    _persist(await res.json())
  }

  async function register(email, password, displayName) {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, displayName }),
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || 'Registration failed')
    }
    _persist(await res.json())
  }

  function logout() {
    _clear()
  }

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
