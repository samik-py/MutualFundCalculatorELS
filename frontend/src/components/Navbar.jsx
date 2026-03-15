import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import './Navbar.css'

const NAV_LINKS = [
  { label: 'Predictor', to: '/predictor' },
  { label: 'Compare', to: '/compare' },
  { label: 'Portfolio', to: '/portfolio' },
]

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const location = useLocation()

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
          {NAV_LINKS.map(({ label, to }) => (
            <li key={to}>
              <Link
                to={to}
                className={`navbar__link${location.pathname === to ? ' navbar__link--active' : ''}`}
              >
                {label}
              </Link>
            </li>
          ))}
          <li>
            <Link to="/predictor" className="navbar__link navbar__link--cta">
              Get Started
            </Link>
          </li>
        </ul>
      </div>

      {/* Gold accent underline */}
      <div className="navbar__accent" />
    </nav>
  )
}
