import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './Navbar.css'

const AUTH_NAV_LINKS = [
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'My Portfolios', to: '/portfolios' },
  { label: 'Compare Funds', to: '/compare' },
  { label: 'Saved Charts', to: '/charts' },
]

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const { isAuthenticated, user, logout } = useAuth()

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <nav className={`navbar${scrolled ? ' navbar--scrolled' : ''}`}>
      <div className="navbar__inner">
        {/* Left: GS monogram + wordmark */}
        <Link to="/" className="navbar__brand">
          <div className="navbar__monogram">GS</div>
          <div className="navbar__wordmark">
            <span className="navbar__wordmark-main">Goldman Sachs</span>
            <span className="navbar__wordmark-sub">Asset Management</span>
          </div>
        </Link>

        {/* Right: nav links */}
        <ul className="navbar__links">
          {isAuthenticated ? (
            <>
              {AUTH_NAV_LINKS.map(({ label, to }) => (
                <li key={to}>
                  <Link to={to} className="navbar__link">{label}</Link>
                </li>
              ))}
              <li>
                <Link to="/predictor" className="navbar__link navbar__link--cta">
                  Get Started
                </Link>
              </li>
              {user?.displayName && (
                <li>
                  <span className="navbar__display-name">{user.displayName}</span>
                </li>
              )}
              <li>
                <button className="navbar__link navbar__link--logout" onClick={logout}>
                  Logout
                </button>
              </li>
            </>
          ) : (
            <>
              <li>
                <Link to="/login" className="navbar__link">Login</Link>
              </li>
              <li>
                <Link to="/predictor" className="navbar__link navbar__link--cta">
                  Get Started
                </Link>
              </li>
            </>
          )}
        </ul>
      </div>

      {/* Gold accent underline */}
      <div className="navbar__accent" />
    </nav>
  )
}
