import { useEffect, useState, useRef } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './Navbar.css'

const AUTH_NAV_LINKS = [
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'Crypto Suite', to: '/crypto' },
  { label: 'My Portfolios', to: '/portfolios' },
  { label: 'Compare Funds', to: '/compare' },
  { label: 'Saved Charts', to: '/charts' },
]

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const { isAuthenticated, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const dropdownRef = useRef(null)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => {
    const onClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setProfileOpen(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  useEffect(() => {
    setProfileOpen(false)
  }, [location.pathname])

  return (
    <>
      {/* Skip navigation — first focusable element on every page */}
      <a href="#main-content" className="skip-link">Skip to main content</a>

      <nav
        className={`navbar${scrolled ? ' navbar--scrolled' : ''}`}
        aria-label="Main navigation"
      >
        <div className="navbar__inner">
          {/* Left: GS monogram + wordmark */}
          <Link to="/" className="navbar__brand" aria-label="Goldman Sachs Asset Management — home">
            <div className="navbar__monogram" aria-hidden="true">GS</div>
            <div className="navbar__wordmark">
              <span className="navbar__wordmark-main">Goldman Sachs</span>
              <span className="navbar__wordmark-sub">Asset Management</span>
            </div>
          </Link>

          {/* Center: main nav links */}
          {isAuthenticated && (
            <ul className="navbar__links navbar__links--center" role="list">
              {AUTH_NAV_LINKS.map(({ label, to }) => (
                <li key={to}>
                  <Link
                    to={to}
                    className="navbar__link"
                    aria-current={location.pathname === to ? 'page' : undefined}
                  >
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          )}

          {/* Right: actions */}
          <ul className="navbar__links navbar__links--right" role="list">
            {isAuthenticated ? (
              <>
                <li>
                  <Link
                    to="/predictor"
                    className="navbar__link navbar__link--cta"
                    aria-current={location.pathname === '/predictor' ? 'page' : undefined}
                  >
                    Investment Tools
                  </Link>
                </li>
                <li className="navbar__profile-wrap" ref={dropdownRef}>
                  <button
                    className={`navbar__profile-btn${profileOpen ? ' navbar__profile-btn--open' : ''}`}
                    onClick={() => setProfileOpen((o) => !o)}
                    aria-label="Account menu"
                    aria-expanded={profileOpen}
                    type="button"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
                      <circle cx="12" cy="8" r="4" />
                      <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" strokeLinecap="round" />
                    </svg>
                  </button>
                  {profileOpen && (
                    <div className="navbar__dropdown" role="menu">
                      <button
                        className="navbar__dropdown-item"
                        role="menuitem"
                        type="button"
                        onClick={() => navigate('/profile')}
                      >
                        Profile Settings
                      </button>
                      <div className="navbar__dropdown-sep" />
                      <button
                        className="navbar__dropdown-item navbar__dropdown-item--danger"
                        role="menuitem"
                        type="button"
                        onClick={logout}
                      >
                        Logout
                      </button>
                    </div>
                  )}
                </li>
              </>
            ) : (
              <>
                <li>
                  <Link
                    to="/predictor"
                    className="navbar__link navbar__link--cta"
                    aria-current={location.pathname === '/predictor' ? 'page' : undefined}
                  >
                    Investment Tools
                  </Link>
                </li>
                <li>
                  <Link
                    to="/login"
                    className="navbar__link"
                    aria-current={location.pathname === '/login' ? 'page' : undefined}
                  >
                    Login
                  </Link>
                </li>
              </>
            )}
          </ul>
        </div>

        {/* Gold accent underline — decorative */}
        <div className="navbar__accent" aria-hidden="true" />
      </nav>
    </>
  )
}
