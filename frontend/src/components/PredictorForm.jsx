import { useState, useRef } from 'react'
import './PredictorForm.css'

// Hardcoded fund data with assumed annual returns
const FUNDS = [
  { id: 'vanguard-500', label: 'Vanguard 500 Index (VFIAX)', return: 0.107 },
  { id: 'fidelity-growth', label: 'Fidelity Growth Company (FDGRX)', return: 0.138 },
  { id: 'trowe-bluechip', label: 'T. Rowe Price Blue Chip (TRBCX)', return: 0.121 },
  { id: 'schwab-total', label: 'Schwab Total Market (SWTSX)', return: 0.104 },
  { id: 'pimco-total', label: 'PIMCO Total Return (PTTRX)', return: 0.042 },
]

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(value)
}

function AnimatedCounter({ target, duration = 1200 }) {
  const [display, setDisplay] = useState(0)
  const startRef = useRef(null)
  const frameRef = useRef(null)

  // Kick off animation whenever target changes
  const prevTarget = useRef(null)
  if (prevTarget.current !== target) {
    prevTarget.current = target
    cancelAnimationFrame(frameRef.current)
    const startVal = display
    const startTime = performance.now()

    const tick = (now) => {
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      // Ease out cubic
      const eased = 1 - Math.pow(1 - progress, 3)
      const current = Math.round(startVal + (target - startVal) * eased)
      setDisplay(current)
      if (progress < 1) {
        frameRef.current = requestAnimationFrame(tick)
      }
    }

    frameRef.current = requestAnimationFrame(tick)
  }

  return <>{formatCurrency(display)}</>
}

export default function PredictorForm() {
  const [selectedFund, setSelectedFund] = useState(FUNDS[0].id)
  const [amount, setAmount] = useState('')
  const [years, setYears] = useState(10)
  const [result, setResult] = useState(null)
  const [hasCalculated, setHasCalculated] = useState(false)

  const fund = FUNDS.find((f) => f.id === selectedFund)

  const handleCalculate = () => {
    const principal = parseFloat(amount)
    if (!principal || principal <= 0) return

    const fv = principal * Math.pow(1 + fund.return, years)
    const gain = fv - principal
    const gainPct = ((gain / principal) * 100).toFixed(1)

    setResult({ fv, gain, gainPct, annualReturn: (fund.return * 100).toFixed(1) })
    setHasCalculated(true)
  }

  // Track/thumb gradient for range input
  const sliderStyle = {
    background: `linear-gradient(to right, var(--gold) 0%, var(--gold) ${((years - 1) / 29) * 100}%, rgba(255,255,255,0.12) ${((years - 1) / 29) * 100}%, rgba(255,255,255,0.12) 100%)`,
  }

  return (
    <section id="calculator" className="predictor-section">
      <div className="section">
        <div className="predictor-header reveal">
          <span className="predictor-eyebrow">Goldman Sachs · Quantitative Research</span>
          <h2 className="predictor-title">Investment Projector</h2>
          <p className="predictor-subtitle">
            Model your portfolio's future value using historical fund performance data.
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
                onChange={(e) => {
                  setSelectedFund(e.target.value)
                  setHasCalculated(false)
                }}
              >
                {FUNDS.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.label}
                  </option>
                ))}
              </select>
              <svg className="select-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M6 9l6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <span className="field-hint">
              Assumed annual return: <strong>{(fund.return * 100).toFixed(1)}%</strong>
            </span>
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
            className={`calc-button ${!amount || parseFloat(amount) <= 0 ? 'calc-button--disabled' : ''}`}
            onClick={handleCalculate}
            disabled={!amount || parseFloat(amount) <= 0}
          >
            <span>Calculate Future Value</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="calc-button__icon">
              <path d="M13 7l5 5m0 0l-5 5m5-5H6" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
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
                    <span className="result-stat__label">Avg. Annual Return</span>
                    <span className="result-stat__value">{result.annualReturn}%</span>
                  </div>
                </div>
                <p className="result-disclaimer">
                  * Projections are based on historical average returns and do not guarantee future performance.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
