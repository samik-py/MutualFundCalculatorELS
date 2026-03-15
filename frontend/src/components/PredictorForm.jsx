import { useState, useRef, useEffect } from 'react'
import { fetchFunds, calculateFutureValue } from '../services/api'
import './PredictorForm.css'

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(value)
}

function AnimatedCounter({ target, duration = 1200 }) {
  const [display, setDisplay] = useState(0)
  const frameRef = useRef(null)
  const prevTarget = useRef(null)

  if (prevTarget.current !== target) {
    prevTarget.current = target
    cancelAnimationFrame(frameRef.current)
    const startVal = display
    const startTime = performance.now()
    const tick = (now) => {
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      setDisplay(Math.round(startVal + (target - startVal) * eased))
      if (progress < 1) frameRef.current = requestAnimationFrame(tick)
    }
    frameRef.current = requestAnimationFrame(tick)
  }

  return <>{formatCurrency(display)}</>
}

export default function PredictorForm() {
  const [funds, setFunds] = useState([])
  const [selectedFund, setSelectedFund] = useState('')
  const [amount, setAmount] = useState('')
  const [years, setYears] = useState(10)
  const [result, setResult] = useState(null)
  const [hasCalculated, setHasCalculated] = useState(false)
  const [loading, setLoading] = useState(false)
  const [fundsLoading, setFundsLoading] = useState(true)

  useEffect(() => {
    fetchFunds()
      .then((data) => {
        setFunds(data)
        if (data.length > 0) setSelectedFund(data[0].fundId)
      })
      .catch(() => {})
      .finally(() => setFundsLoading(false))
  }, [])

  const handleCalculate = async () => {
    const principal = parseFloat(amount)
    if (!principal || principal <= 0 || !selectedFund) return
    setLoading(true)
    try {
      const data = await calculateFutureValue(selectedFund, principal, years)
      const gainPct = ((data.gain / principal) * 100).toFixed(1)
      setResult({
        fv: data.futureValue,
        gain: data.gain,
        gainPct,
        annualReturn: (data.annualReturn * 100).toFixed(2),
      })
      setHasCalculated(true)
    } finally {
      setLoading(false)
    }
  }

  const sliderStyle = {
    background: `linear-gradient(to right, var(--gold) 0%, var(--gold) ${((years - 1) / 29) * 100}%, rgba(255,255,255,0.12) ${((years - 1) / 29) * 100}%, rgba(255,255,255,0.12) 100%)`,
  }

  const selectedFundObj = funds.find(f => f.fundId === selectedFund)

  return (
    <section id="calculator" className="predictor-section">
      <div className="section">
        <div className="predictor-header reveal">
          <span className="predictor-eyebrow">Goldman Sachs · Quantitative Research</span>
          <h2 className="predictor-title">Investment Projector</h2>
          <p className="predictor-subtitle">
            Model your portfolio's future value using live CAPM returns from Newton Analytics,
            FRED, and Yahoo Finance.
          </p>
        </div>

        <div className="predictor-card reveal reveal-delay-1">
          {/* ── Fund Selector ── */}
          <div className="field-group">
            <label className="field-label">Select Mutual Fund</label>
            <div className="select-wrapper">
              <select
                className="field-select"
                value={selectedFund}
                disabled={fundsLoading}
                onChange={(e) => {
                  setSelectedFund(e.target.value)
                  setHasCalculated(false)
                }}
              >
                {fundsLoading && <option>Loading funds…</option>}
                {funds.map((f) => (
                  <option key={f.fundId} value={f.fundId}>
                    {f.name} ({f.ticker})
                  </option>
                ))}
              </select>
              <svg className="select-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M6 9l6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            {selectedFundObj && (
              <span className="field-hint">
                Ticker: <strong>{selectedFundObj.ticker}</strong> · Return computed via CAPM using live beta from Newton Analytics
              </span>
            )}
          </div>

          {/* ── Investment Amount ── */}
          <div className="field-group">
            <label className="field-label">Initial Investment</label>
            <div className="input-wrapper">
              <span className="input-prefix">$</span>
              <input
                className="field-input"
                type="number"
                min="0"
                step="1000"
                placeholder="10,000"
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value)
                  setHasCalculated(false)
                }}
              />
            </div>
          </div>

          {/* ── Time Horizon Slider ── */}
          <div className="field-group">
            <div className="slider-header">
              <label className="field-label">Time Horizon</label>
              <span className="slider-value">
                <strong>{years}</strong> {years === 1 ? 'year' : 'years'}
              </span>
            </div>
            <input
              className="field-range"
              type="range"
              min="1"
              max="30"
              value={years}
              style={sliderStyle}
              onChange={(e) => {
                setYears(parseInt(e.target.value))
                setHasCalculated(false)
              }}
            />
            <div className="slider-ticks">
              <span>1yr</span>
              <span>5yr</span>
              <span>10yr</span>
              <span>20yr</span>
              <span>30yr</span>
            </div>
          </div>

          {/* ── Calculate Button ── */}
          <button
            className={`calc-button ${!amount || parseFloat(amount) <= 0 || loading ? 'calc-button--disabled' : ''}`}
            onClick={handleCalculate}
            disabled={!amount || parseFloat(amount) <= 0 || loading}
          >
            {loading ? (
              <span>Computing CAPM return…</span>
            ) : (
              <>
                <span>Calculate Future Value</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="calc-button__icon">
                  <path d="M13 7l5 5m0 0l-5 5m5-5H6" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </>
            )}
          </button>

          {/* ── Result Panel ── */}
          {hasCalculated && result && (
            <div className="result-panel">
              <div className="result-panel__inner">
                <div className="result-main">
                  <span className="result-label">Projected Value in {years} years</span>
                  <span className="result-value">
                    <AnimatedCounter target={Math.round(result.fv)} />
                  </span>
                </div>
                <div className="result-stats">
                  <div className="result-stat">
                    <span className="result-stat__label">Total Gain</span>
                    <span className="result-stat__value result-stat__value--positive">
                      +<AnimatedCounter target={Math.round(result.gain)} />
                    </span>
                  </div>
                  <div className="result-stat">
                    <span className="result-stat__label">Return on Investment</span>
                    <span className="result-stat__value result-stat__value--positive">
                      +{result.gainPct}%
                    </span>
                  </div>
                  <div className="result-stat">
                    <span className="result-stat__label">CAPM Annual Return</span>
                    <span className="result-stat__value">{result.annualReturn}%</span>
                  </div>
                </div>
                <p className="result-disclaimer">
                  * CAPM return derived from live beta (Newton Analytics), Rf (FRED DGS10), and Rm (S&P 500 5yr CAGR).
                  Projections do not guarantee future performance.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
