import './HeroSection.css'

export default function HeroSection() {
  const handleScroll = () => {
    document.getElementById('calculator')?.scrollIntoView({ behavior: 'smooth' })
  }

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
          <button className="hero__cta" onClick={handleScroll}>
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
    </section>
  )
}
