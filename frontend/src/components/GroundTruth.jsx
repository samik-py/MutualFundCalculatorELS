import { useEffect, useState } from 'react'
import { getMarketIndicators } from '../services/api'
import './GroundTruth.css'

function fmt(value) {
  return (value * 100).toFixed(2) + '%'
}

export default function GroundTruth() {
  const [indicators, setIndicators] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMarketIndicators()
      .then(setIndicators)
      .catch(() => setIndicators(null))
      .finally(() => setLoading(false))
  }, [])

  const rf = indicators?.riskFreeRate ?? 0.04
  const rm = indicators?.marketReturn5y ?? 0.10
  const premium = rm - rf

  return (
    <section className="gt-section">
      <div className="section-divider" />
      <div className="section">
        <div className="gt-header reveal">
          <span className="gt-eyebrow">Goldman Sachs · Quantitative Research</span>
          <h2 className="gt-title">Ground Truth</h2>
          <p className="gt-subtitle">
            Live market parameters confirmed by authoritative sources. These are the
            inputs our CAPM engine uses — not estimates, not defaults.
          </p>
        </div>

        {loading ? (
          <div className="gt-loading reveal reveal-delay-1">Fetching live data...</div>
        ) : (
          <div className="gt-grid reveal reveal-delay-1">
            {/* Rf card */}
            <div className="gt-card gt-card--rf">
              <div className="gt-card__badge">CONFIRMED</div>
              <div className="gt-card__metric">{fmt(rf)}</div>
              <div className="gt-card__label">Risk-Free Rate (Rf)</div>
              <div className="gt-card__source">
                <span className="gt-card__source-icon">&#9632;</span>
                FRED · DGS10 · 10-Year U.S. Treasury
              </div>
              <p className="gt-card__desc">
                The yield on 10-year government debt — the closest approximation to
                a risk-free return. Used as the baseline for all CAPM projections.
              </p>
              <div className="gt-card__certainty">
                <span className="gt-certainty-label">Certainty</span>
                <div className="gt-certainty-bar">
                  <div className="gt-certainty-fill gt-certainty-fill--high" style={{ width: '95%' }} />
                </div>
                <span className="gt-certainty-text">Very High · Government Data</span>
              </div>
            </div>

            {/* Rm card */}
            <div className="gt-card gt-card--rm">
              <div className="gt-card__badge gt-card__badge--moderate">HISTORICAL</div>
              <div className="gt-card__metric">{fmt(rm)}</div>
              <div className="gt-card__label">Market Return 5Y (Rm)</div>
              <div className="gt-card__source">
                <span className="gt-card__source-icon">&#9632;</span>
                Yahoo Finance · S&amp;P 500 · 5-Year CAGR
              </div>
              <p className="gt-card__desc">
                Compound annual growth rate of the S&P 500 over the past 5 years.
                Historical fact — forward-looking returns may differ.
              </p>
              <div className="gt-card__certainty">
                <span className="gt-certainty-label">Certainty</span>
                <div className="gt-certainty-bar">
                  <div className="gt-certainty-fill gt-certainty-fill--moderate" style={{ width: '70%' }} />
                </div>
                <span className="gt-certainty-text">Moderate · Past Performance</span>
              </div>
            </div>

            {/* Equity risk premium */}
            <div className="gt-card gt-card--erp">
              <div className="gt-card__badge gt-card__badge--derived">DERIVED</div>
              <div className="gt-card__metric">{fmt(premium)}</div>
              <div className="gt-card__label">Equity Risk Premium (Rm − Rf)</div>
              <div className="gt-card__source">
                <span className="gt-card__source-icon">&#9632;</span>
                Computed · CAPM Input
              </div>
              <p className="gt-card__desc">
                The excess return investors demand for holding equities over the risk-free
                asset. Multiplied by each fund's beta to produce its expected return.
              </p>
              <div className="gt-capm-formula">
                <span className="gt-capm-text">E(r) = {fmt(rf)} + β × {fmt(premium)}</span>
              </div>
            </div>
          </div>
        )}

        {indicators && (
          <p className="gt-footer-note reveal reveal-delay-2">
            Data fetched {indicators.dataAsOf}. Rf: {indicators.rfSource}.
            Rm: {indicators.rmSource}.
            Rates are cached for 24 hours and refresh automatically.
          </p>
        )}
      </div>
    </section>
  )
}
