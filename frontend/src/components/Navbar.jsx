import { useEffect, useState } from 'react'
import './Navbar.css'

const NAV_LINKS = ['Markets', 'Insights', 'Funds', 'About']

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <nav className={`navbar${scrolled ? ' navbar--scrolled' : ''}`}>
      <div className="navbar__inner">
        {/* Left: GS monogram + wordmark */}
        <div className="navbar__brand">
          <div className="navbar__monogram">GS</div>
          <div className="navbar__wordmark">
            <span className="navbar__wordmark-main">Goldman Sachs</span>
            <span className="navbar__wordmark-sub">Asset Management</span>
          </div>
        </div>

        {/* Right: nav links */}
        <ul className="navbar__links">
          {NAV_LINKS.map((link) => (
            <li key={link}>
              <a href="#" className="navbar__link">{link}</a>
            </li>
          ))}
          <li>
            <a href="#calculator" className="navbar__link navbar__link--cta">
              Get Started
            </a>
          </li>
        </ul>
      </div>

      {/* Gold accent underline */}
      <div className="navbar__accent" />
    </nav>
  )
}
