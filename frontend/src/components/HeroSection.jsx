import { useNavigate } from 'react-router-dom'
import './HeroSection.css'

const STATS = [
  { value: '21', label: 'Institutional Funds' },
  { value: '30yr', label: 'Max Projection Horizon' },
  { value: '2,000', label: 'Monte Carlo Simulations' },
  { value: 'Live', label: 'CAPM Market Data' },
]

const CAPABILITIES = [
  {
    title: 'Investment Projector',
    desc: 'Project future value across 21 institutional funds using live CAPM returns. Inputs are sourced daily from FRED and Yahoo Finance — not estimates.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" aria-hidden="true">
        <path d="M3 17l4-8 4 4 4-6 4 10" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M3 21h18" strokeLinecap="round" />
      </svg>
    ),
  },
  {
    title: 'Fund Comparison',
    desc: 'Compare up to 5 funds side-by-side over a 1–30 year horizon. CAPM annual return and final projected value shown per fund.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" aria-hidden="true">
        <path d="M4 20V10M9 20V4M14 20V12M19 20V7" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    title: 'Monte Carlo Simulation',
    desc: 'Run up to 2,000 Geometric Brownian Motion simulations — the model behind Black-Scholes — to map outcome distributions across the 5th to 95th percentile.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" aria-hidden="true">
        <path d="M2 12c2-4 4-6 6-4s3 6 5 6 4-5 6-5" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M2 18h20" strokeLinecap="round" opacity=".35" />
        <path d="M2 6h20" strokeLinecap="round" opacity=".35" />
      </svg>
    ),
  },
  {
    title: 'Portfolio Management',
    desc: 'Build custom portfolios, log holdings with cost basis and share count, and track real-time gain/loss, return %, and total value across every position.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" aria-hidden="true">
        <rect x="2" y="7" width="20" height="14" rx="2" />
        <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2" strokeLinecap="round" />
        <path d="M12 12v4M10 14h4" strokeLinecap="round" />
      </svg>
    ),
  },
  {
    title: 'Crypto Suite',
    desc: 'Full cryptocurrency desk powered by Coinbase — live prices, tax lot optimization, stress testing across market scenarios, on-chain metrics, and fee auditing.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" aria-hidden="true">
        <path d="M12 2L2 7l10 5 10-5-10-5z" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M2 17l10 5 10-5" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M2 12l10 5 10-5" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    title: 'AI Portfolio Optimizer',
    desc: 'Describe your investment goals in plain English. Gemini AI classifies your risk profile and returns a personalized fund allocation with projected annual returns.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" aria-hidden="true">
        <circle cx="12" cy="12" r="9" />
        <path d="M9 9.5c0-1.4 1.3-2.5 3-2.5s3 1.1 3 2.5c0 1.8-2 2.5-3 3.5" strokeLinecap="round" />
        <circle cx="12" cy="17" r=".5" fill="currentColor" />
      </svg>
    ),
  },
]

const SOURCES = [
  { label: 'FRED API', sub: 'Risk-Free Rate (DGS10)' },
  { label: 'Yahoo Finance', sub: 'S&P 500 5-Year CAGR' },
  { label: 'Coinbase', sub: 'Live Crypto Prices' },
  { label: 'Gemini AI', sub: 'Portfolio Intelligence' },
]

const STEPS = [
  {
    n: '01',
    title: 'Create your account',
    desc: 'Sign up in seconds. No credit card, brokerage connection, or minimum balance required.',
  },
  {
    n: '02',
    title: 'Build and compare',
    desc: 'Run projections, compare funds head-to-head, simulate thousands of market outcomes, and manage your portfolio holdings.',
  },
  {
    n: '03',
    title: 'Get AI-driven insights',
    desc: 'The AI optimizer reads your risk profile and returns a tailored fund allocation — backed by live CAPM data, not static models.',
  },
]

export default function HeroSection() {
  const navigate = useNavigate()

  return (
    <div className="lp">

      {/* ── Hero ── */}
      <section className="lp__hero">
        <div className="lp__hero-inner reveal">
          <div className="lp__eyebrow">
            <span className="lp__eyebrow-line" />
            <span className="lp__eyebrow-text">Goldman Sachs · Asset Management</span>
            <span className="lp__eyebrow-line" />
          </div>

          <h1 className="lp__title">
            Institutional-Grade<br />
            <em>Fund Intelligence</em>
          </h1>

          <p className="lp__subtitle">
            CAPM projections. AI portfolio optimization. Live market data.<br />
            Precision analytics built for the serious investor.
          </p>

          <div className="lp__actions">
            <button className="lp__cta lp__cta--primary" onClick={() => navigate('/register')} type="button">
              Create Account
            </button>
            <button className="lp__cta lp__cta--secondary" onClick={() => navigate('/predictor')} type="button">
              Explore Tools
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
                <path d="M5 12h14M12 5l7 7-7 7" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </div>

          <dl className="lp__stats" aria-label="Platform statistics">
            {STATS.map((s, i) => (
              <div key={s.label} className="lp__stat-group">
                {i > 0 && <div className="lp__stat-sep" aria-hidden="true" />}
                <div className="lp__stat reveal" style={{ '--delay': `${i * 0.08}s` }}>
                  <dd className="lp__stat-value">{s.value}</dd>
                  <dt className="lp__stat-label">{s.label}</dt>
                </div>
              </div>
            ))}
          </dl>
        </div>

        <div className="lp__scroll-cue" aria-hidden="true">
          <span className="lp__scroll-dot" />
        </div>
      </section>

      {/* ── Platform Capabilities ── */}
      <section className="lp__section lp__section--capabilities">
        <div className="lp__section-inner">
          <div className="lp__section-header reveal">
            <span className="lp__section-eyebrow">Platform Capabilities</span>
            <h2 className="lp__section-title">Every tool you need, in one place</h2>
            <p className="lp__section-sub">
              Six integrated modules covering the full investment analysis lifecycle — from single-fund projection to AI-optimized portfolio construction.
            </p>
          </div>

          <div className="lp__caps-grid">
            {CAPABILITIES.map((cap, i) => (
              <div key={cap.title} className="lp__cap reveal" style={{ '--delay': `${(i % 3) * 0.07}s` }}>
                <div className="lp__cap-icon" aria-hidden="true">{cap.icon}</div>
                <h3 className="lp__cap-title">{cap.title}</h3>
                <p className="lp__cap-desc">{cap.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Data Sources ── */}
      <section className="lp__section lp__section--sources">
        <div className="lp__section-inner">
          <div className="lp__sources-row reveal">
            <span className="lp__sources-label">Powered by real data from</span>
            <div className="lp__sources-list">
              {SOURCES.map((src) => (
                <div key={src.label} className="lp__source">
                  <span className="lp__source-name">{src.label}</span>
                  <span className="lp__source-sub">{src.sub}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ── How It Works ── */}
      <section className="lp__section lp__section--how">
        <div className="lp__section-inner">
          <div className="lp__section-header reveal">
            <span className="lp__section-eyebrow">How It Works</span>
            <h2 className="lp__section-title">From signup to insight in minutes</h2>
          </div>

          <div className="lp__steps">
            {STEPS.map((step, i) => (
              <div key={step.n} className="lp__step reveal" style={{ '--delay': `${i * 0.1}s` }}>
                <div className="lp__step-number" aria-hidden="true">{step.n}</div>
                <div className="lp__step-connector" aria-hidden="true" />
                <h3 className="lp__step-title">{step.title}</h3>
                <p className="lp__step-desc">{step.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Final CTA ── */}
      <section className="lp__section lp__section--cta">
        <div className="lp__cta-banner reveal">
          <div className="lp__eyebrow lp__eyebrow--cta">
            <span className="lp__eyebrow-line" />
            <span className="lp__eyebrow-text">Get Started Today</span>
            <span className="lp__eyebrow-line" />
          </div>
          <h2 className="lp__cta-banner-title">
            Start making institutional-grade<br />investment decisions.
          </h2>
          <p className="lp__cta-banner-sub">
            Free to use. No brokerage account required. Live market data from day one.
          </p>
          <div className="lp__actions">
            <button className="lp__cta lp__cta--primary" onClick={() => navigate('/register')} type="button">
              Create Free Account
            </button>
            <button className="lp__cta lp__cta--secondary" onClick={() => navigate('/login')} type="button">
              Sign In
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
                <path d="M5 12h14M12 5l7 7-7 7" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </div>
        </div>
      </section>

    </div>
  )
}
