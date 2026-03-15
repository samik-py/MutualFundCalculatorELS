import { useEffect } from 'react'
import Navbar from '../components/Navbar'
import TickerTape from '../components/TickerTape'
import PortfolioBuilder from '../components/PortfolioBuilder'
import './PortfolioPage.css'

function useScrollReveal() {
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => entries.forEach(e => { if (e.isIntersecting) e.target.classList.add('visible') }),
      { threshold: 0.08, rootMargin: '0px 0px -40px 0px' }
    )
    document.querySelectorAll('.reveal').forEach(el => observer.observe(el))
    return () => observer.disconnect()
  }, [])
}

export default function PortfolioPage() {
  useScrollReveal()

  return (
    <>
      <Navbar />
      <TickerTape />
      <main>
        <section className="pp-hero">
          <div className="section">
            <div className="pp-hero__inner reveal">
              <span className="predictor-eyebrow">Goldman Sachs · Portfolio Construction</span>
              <h1 className="pp-hero__title">Portfolio Builder</h1>
              <p className="pp-hero__subtitle">
                Construct two custom portfolios from our 21-fund catalog, assign allocations,
                and compare their CAPM-projected growth trajectories over any time horizon.
              </p>
              <div className="pp-methodology">
                <div className="pp-method-item">
                  <span className="pp-method-icon">&#9632;</span>
                  <span>Weights are normalized to 100% — partial allocations are supported</span>
                </div>
                <div className="pp-method-item">
                  <span className="pp-method-icon">&#9632;</span>
                  <span>Expected returns computed via CAPM using live Rf and fund beta from Newton Analytics</span>
                </div>
                <div className="pp-method-item">
                  <span className="pp-method-icon">&#9632;</span>
                  <span>Portfolio return is a weighted average of constituent fund CAPM returns</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="pp-builder-section">
          <div className="section" style={{ maxWidth: 1100 }}>
            <div className="reveal reveal-delay-1">
              <PortfolioBuilder />
            </div>
          </div>
        </section>

        {/* Footer */}
        <footer className="app-footer">
          <div className="app-footer__gs">
            <span className="app-footer__monogram">GS</span>
            <span className="app-footer__brand">Goldman Sachs Asset Management</span>
          </div>
          <span className="app-footer__copy">
            © {new Date().getFullYear()} Goldman Sachs Group, Inc. · For illustrative purposes only · Not financial advice
          </span>
        </footer>
      </main>
    </>
  )
}
