import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Navbar from '../components/Navbar'
import './AuthPage.css'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register(email, password, displayName)
      navigate('/predictor', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <Navbar />
      <main id="main-content" className="auth-page">
        <div className="auth-card">
          <p className="auth-eyebrow">Goldman Sachs · Portal</p>
          <h1 className="auth-title">Create account</h1>
          <p className="auth-subtitle">Start your investment journey today</p>

          {error && (
            <div id="register-error" className="auth-error" role="alert" aria-live="assertive">
              {error}
            </div>
          )}

          <form className="auth-form" onSubmit={handleSubmit} noValidate aria-describedby={error ? 'register-error' : undefined}>
            <div className="auth-field">
              <label className="auth-label" htmlFor="displayName">Display Name</label>
              <input
                id="displayName"
                className="auth-input"
                type="text"
                autoComplete="name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="John Smith"
              />
            </div>

            <div className="auth-field">
              <label className="auth-label" htmlFor="email">Email</label>
              <input
                id="email"
                className="auth-input"
                type="email"
                autoComplete="email"
                required
                aria-required="true"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
              />
            </div>

            <div className="auth-field">
              <label className="auth-label" htmlFor="password">Password</label>
              <input
                id="password"
                className="auth-input"
                type="password"
                autoComplete="new-password"
                required
                aria-required="true"
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Minimum 8 characters"
                aria-describedby="password-hint"
              />
              <span id="password-hint" className="sr-only">Minimum 8 characters required</span>
            </div>

            <button className="auth-btn" type="submit" disabled={loading} aria-busy={loading}>
              {loading ? 'Creating account…' : 'Create Account'}
            </button>
          </form>

          <p className="auth-switch">
            Already have an account?{' '}
            <Link to="/login" className="auth-switch-link">Sign in</Link>
          </p>
        </div>
      </main>
    </>
  )
}
