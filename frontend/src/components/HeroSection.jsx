import { useNavigate } from 'react-router-dom'
import './HeroSection.css'

export default function HeroSection() {
  const navigate = useNavigate()

  return (
    <section className="hero">
      <div className="hero__inner reveal">
        <div className="hero__eyebrow">
          <span className="hero__line" />
          <span className="hero__eyebrow-text">Goldman Sachs · Asset Management</span>
          <span className="hero__line" />
        </div>

        <h1 className="hero__title">
          Mutual Fund<br />
          <em>Investment Predictor</em>
        </h1>

        <p className="hero__subtitle">
          Institutional-grade forecasting built on Goldman Sachs research methodologies.<br />
          Precision analytics for the serious investor.
        </p>

        <div className="hero__actions">
          <button className="hero__cta" onClick={() => navigate('/predictor')}>
            <span>Get Started</span>
            <svg className="hero__cta-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M12 5v14M5 12l7 7 7-7" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        </div>

        <div className="hero__stats">
          <div className="hero__stat reveal reveal-delay-1">
            <span className="hero__stat-value">500+</span>
            <span className="hero__stat-label">Funds Tracked</span>
          </div>
          <div className="hero__stat-sep" />
          <div className="hero__stat reveal reveal-delay-2">
            <span className="hero__stat-value">30yr</span>
            <span className="hero__stat-label">Projection Horizon</span>
          </div>
          <div className="hero__stat-sep" />
          <div className="hero__stat reveal reveal-delay-3">
            <span className="hero__stat-value">Real-Time</span>
            <span className="hero__stat-label">Market Analysis</span>
          </div>
        </div>
      </div>

      <div className="hero__scroll-indicator">
        <span className="hero__scroll-dot" />
      </div>

      {/* ── About / Project Description ── */}
      <div className="hero__about reveal">
        <div className="hero__about-header">
          <span className="hero__line" />
          <span className="hero__eyebrow-text">About This Project</span>
          <span className="hero__line" />
        </div>

        <p className="hero__about-body">
          This tool was built to make institutional-quality investment analysis accessible to everyone.
          Enter a principal amount, choose a mutual fund, and set your time horizon — our engine projects
          your future value using real compound-growth models. The AI Portfolio Optimizer then reads your
          risk profile and recommends an allocation strategy aligned with your goals.
        </p>

        <div className="hero__features">
          <div className="hero__feature reveal reveal-delay-1">
            <div className="hero__feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4">
                <path d="M3 17l4-8 4 4 4-6 4 10" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
            <h3 className="hero__feature-title">Future Value Calculator</h3>
            <p className="hero__feature-desc">
              Projects investment growth over up to 30 years using per-fund compound annual return rates.
              Calculates total gain and ROI alongside the projected value.
            </p>
          </div>

          <div className="hero__feature reveal reveal-delay-2">
            <div className="hero__feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4">
                <circle cx="12" cy="12" r="9"/>
                <path d="M12 8v4l3 3" strokeLinecap="round"/>
              </svg>
            </div>
            <h3 className="hero__feature-title">AI Portfolio Optimizer</h3>
            <p className="hero__feature-desc">
              Describe your risk appetite and goals in plain language. The optimizer classifies your
              profile and returns a tailored fund allocation with expected annual returns.
            </p>
          </div>

          <div className="hero__feature reveal reveal-delay-3">
            <div className="hero__feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <path d="M3 9h18M9 21V9" strokeLinecap="round"/>
              </svg>
            </div>
            <h3 className="hero__feature-title">Live Market Ticker</h3>
            <p className="hero__feature-desc">
              Real-time feed of major indices, top mutual funds, treasuries, and commodities — keeping
              market context front and center as you plan.
            </p>
          </div>
        </div>

        <div className="hero__stack">
          <span className="hero__stack-label">Built with</span>
          <span className="hero__stack-pill">React 19</span>
          <span className="hero__stack-pill">Spring Boot 3</span>
          <span className="hero__stack-pill">Java 17</span>
          <span className="hero__stack-pill">Vite</span>
        </div>

        <button className="hero__cta hero__cta--bottom" onClick={() => navigate('/predictor')}>
          <span>Launch the Predictor</span>
          <svg className="hero__cta-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M5 12h14M12 5l7 7-7 7" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>
    </section>
  )
}
