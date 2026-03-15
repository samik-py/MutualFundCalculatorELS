import { useEffect } from 'react'
import Navbar from '../components/Navbar'
import TickerTape from '../components/TickerTape'
import FundCompareChart from '../components/FundCompareChart'
import MonteCarloChart from '../components/MonteCarloChart'
import GroundTruth from '../components/GroundTruth'
import './ComparePage.css'

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

export default function ComparePage() {
  useScrollReveal()

  return (
    <>
      <Navbar />
      <TickerTape />
      <main>
        {/* ── Page Header ── */}
        <section className="cp-hero">
          <div className="section">
            <div className="cp-hero__inner reveal">
              <span className="predictor-eyebrow">Goldman Sachs · Quantitative Research</span>
              <h1 className="cp-hero__title">Fund Analytics &amp; Comparison</h1>
              <p className="cp-hero__subtitle">
                Compare projected returns across our full 21-fund catalog, visualize
                confidence intervals with Monte Carlo simulation, and examine the
                live market parameters that drive every CAPM projection.
              </p>
            </div>
          </div>
        </section>

        {/* ── Fund Comparison Chart ── */}
        <section className="cp-section">
          <div className="section" style={{ maxWidth: 1100 }}>
            <div className="cp-section-header reveal">
              <span className="cp-section-num">01</span>
              <div>
                <h2 className="cp-section-title">Fund Comparison</h2>
                <p className="cp-section-sub">
                  Select up to 10 funds and visualize their CAPM-projected growth trajectories side-by-side.
                </p>
              </div>
            </div>
            <div className="reveal reveal-delay-1">
              <FundCompareChart />
            </div>
          </div>
        </section>

        {/* ── Monte Carlo ── */}
        <section className="cp-section">
          <div className="section-divider" />
          <div className="section" style={{ maxWidth: 1100 }}>
            <div className="cp-section-header reveal">
              <span className="cp-section-num">02</span>
              <div>
                <h2 className="cp-section-title">Monte Carlo Predictive Model</h2>
                <p className="cp-section-sub">
                  Statistical confidence bands using Geometric Brownian Motion. Run thousands of
                  simulated futures to understand the range of outcomes for any fund.
                </p>
              </div>
            </div>
            <div className="reveal reveal-delay-1">
              <MonteCarloChart />
            </div>
          </div>
        </section>

        {/* ── Ground Truth ── */}
        <GroundTruth />

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
