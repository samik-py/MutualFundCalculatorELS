import { useState, useEffect } from 'react'
import {
  ComposedChart, Area, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer
} from 'recharts'
import { fetchFunds, runMonteCarlo } from '../services/api'
import './MonteCarloChart.css'

function formatCurrency(v) {
  if (v >= 1_000_000) return `$${(v / 1_000_000).toFixed(2)}M`
  if (v >= 1_000) return `$${(v / 1_000).toFixed(1)}K`
  return `$${Math.round(v)}`
}

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  const p5   = payload.find(p => p.name === 'p5')?.value
  const p25  = payload.find(p => p.name === 'p25_delta')
  const p50  = payload.find(p => p.name === 'Median')?.value
  const p75  = payload.find(p => p.name === 'p75_delta')
  const p95_delta = payload.find(p => p.name === 'p95_delta')

  const low  = p5 ?? 0
  const q1   = low + (p25?.value ?? 0)
  const med  = p50
  const q3   = q1 + (p75?.value ?? 0)
  const high = q3 + (p95_delta?.value ?? 0)

  return (
    <div className="mc-tooltip">
      <div className="mc-tooltip__year">Year {label}</div>
      {[
        { label: '95th pct.', val: high, color: 'rgba(209,161,83,0.5)' },
        { label: '75th pct.', val: q3, color: 'rgba(209,161,83,0.7)' },
        { label: 'Median', val: med, color: '#d1a153' },
        { label: '25th pct.', val: q1, color: 'rgba(209,161,83,0.7)' },
        { label: '5th pct.', val: low, color: 'rgba(209,161,83,0.5)' },
      ].map(({ label: l, val, color }) => val != null && (
        <div key={l} className="mc-tooltip__row">
          <span className="mc-tooltip__dot" style={{ background: color }} />
          <span className="mc-tooltip__label">{l}</span>
          <span className="mc-tooltip__val">{formatCurrency(val)}</span>
        </div>
      ))}
    </div>
  )
}

export default function MonteCarloChart() {
  const [allFunds, setAllFunds] = useState([])
  const [fundId, setFundId] = useState('vanguard-500')
  const [amount, setAmount] = useState(10000)
  const [years, setYears] = useState(20)
  const [simulations, setSimulations] = useState(500)
  const [chartData, setChartData] = useState(null)
  const [meta, setMeta] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchFunds().then(setAllFunds).catch(() => {})
  }, [])

  const handleRun = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await runMonteCarlo(fundId, amount, years, simulations)
      // Build stacked area data: p5 base, then deltas for bands
      // Stack order: p5 | p25-p5 (inner low) | p75-p25 (iqr) | p95-p75 (inner high)
      const byPct = {}
      res.series.forEach(s => {
        byPct[s.percentile] = s.data.map(d => d.value)
      })
      const rows = Array.from({ length: years + 1 }, (_, i) => ({
        year: i,
        p5: Math.round(byPct[5][i]),
        p25_delta: Math.round(byPct[25][i] - byPct[5][i]),
        p75_delta: Math.round(byPct[75][i] - byPct[25][i]),
        p95_delta: Math.round(byPct[95][i] - byPct[75][i]),
        Median: Math.round(byPct[50][i]),
      }))
      setChartData(rows)
      setMeta({
        baseReturn: res.baseReturn,
        vol: res.estimatedVolatility,
        beta: res.estimatedBeta,
        p5_final: formatCurrency(byPct[5][years]),
        median_final: formatCurrency(byPct[50][years]),
        p95_final: formatCurrency(byPct[95][years]),
      })
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const sliderStyle = (val, max) => ({
    background: `linear-gradient(to right, var(--gold) 0%, var(--gold) ${((val - 1) / (max - 1)) * 100}%, rgba(255,255,255,0.12) ${((val - 1) / (max - 1)) * 100}%, rgba(255,255,255,0.12) 100%)`,
  })

  return (
    <div className="mc-wrapper">
      <div className="mc-intro">
        <h3 className="mc-intro__title">Monte Carlo Predictive Model</h3>
        <p className="mc-intro__body">
          Uses Geometric Brownian Motion (GBM) — the same statistical model underpinning
          Black-Scholes option pricing — to simulate thousands of possible futures for your
          investment. Volatility is derived from each fund's beta × market historical vol (16.5%).
          The result is a confidence band, not a single-path prediction.
        </p>
      </div>

      <div className="mc-controls">
        {/* Fund select */}
        <div className="field-group">
          <label className="field-label">Fund</label>
          <div className="select-wrapper">
            <select className="field-select" value={fundId} onChange={e => setFundId(e.target.value)}>
              {allFunds.map(f => (
                <option key={f.fundId} value={f.fundId}>{f.name} ({f.ticker})</option>
              ))}
            </select>
            <svg className="select-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M6 9l6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </div>
        </div>

        <div className="mc-row">
          {/* Amount */}
          <div className="mc-field">
            <label className="field-label">Initial Investment</label>
            <div className="input-wrapper">
              <span className="input-prefix">$</span>
              <input
                className="field-input"
                type="number" min="100" step="1000"
                value={amount}
                onChange={e => setAmount(Math.max(100, Number(e.target.value)))}
              />
            </div>
          </div>

          {/* Years */}
          <div className="mc-field mc-field--grow">
            <div className="slider-header">
              <label className="field-label">Horizon</label>
              <span className="slider-value"><strong>{years}</strong> yr</span>
            </div>
            <input
              className="field-range" type="range" min="1" max="30" value={years}
              style={sliderStyle(years, 30)}
              onChange={e => setYears(Number(e.target.value))}
            />
          </div>

          {/* Simulations */}
          <div className="mc-field">
            <div className="slider-header">
              <label className="field-label">Simulations</label>
              <span className="slider-value"><strong>{simulations}</strong></span>
            </div>
            <input
              className="field-range" type="range" min="100" max="2000" step="100" value={simulations}
              style={sliderStyle(simulations, 2000)}
              onChange={e => setSimulations(Number(e.target.value))}
            />
          </div>
        </div>

        <button
          className={`calc-button ${loading ? 'calc-button--disabled' : ''}`}
          onClick={handleRun} disabled={loading}
        >
          {loading ? 'Running simulation...' : `Run ${simulations.toLocaleString()} Simulations`}
        </button>
        {error && <p className="mc-error">{error}</p>}
      </div>

      {/* Chart */}
      {chartData && meta && (
        <div className="mc-chart-area">
          <div className="mc-chart-header">
            <div>
              <h3 className="mc-chart-title">Return Distribution — {years}-Year Outlook</h3>
              <p className="mc-chart-subtitle">
                CAPM base return {(meta.baseReturn * 100).toFixed(2)}% ·
                Estimated beta {meta.beta.toFixed(2)} ·
                Annual volatility {(meta.vol * 100).toFixed(1)}%
              </p>
            </div>
          </div>

          {/* Confidence summary */}
          <div className="mc-summary">
            <div className="mc-summary-item">
              <span className="mc-summary-label">Pessimistic (5th pct)</span>
              <span className="mc-summary-val mc-summary-val--low">{meta.p5_final}</span>
            </div>
            <div className="mc-summary-item mc-summary-item--main">
              <span className="mc-summary-label">Expected (Median)</span>
              <span className="mc-summary-val mc-summary-val--med">{meta.median_final}</span>
            </div>
            <div className="mc-summary-item">
              <span className="mc-summary-label">Optimistic (95th pct)</span>
              <span className="mc-summary-val mc-summary-val--high">{meta.p95_final}</span>
            </div>
          </div>

          <ResponsiveContainer width="100%" height={360}>
            <ComposedChart data={chartData} margin={{ top: 8, right: 24, left: 16, bottom: 8 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis
                dataKey="year"
                tick={{ fill: 'rgba(232,234,240,0.45)', fontSize: 11 }}
                tickLine={false}
                axisLine={{ stroke: 'rgba(255,255,255,0.08)' }}
                tickFormatter={v => `Yr ${v}`}
              />
              <YAxis
                tick={{ fill: 'rgba(232,234,240,0.45)', fontSize: 11 }}
                tickLine={false}
                axisLine={false}
                tickFormatter={formatCurrency}
                width={72}
              />
              <Tooltip content={<CustomTooltip />} />

              {/* Stacked bands: p5 (transparent base) → p25-p5 → p75-p25 → p95-p75 */}
              <Area stackId="band" type="monotone" dataKey="p5"
                fill="transparent" stroke="rgba(209,161,83,0.3)"
                strokeWidth={1} strokeDasharray="4 3"
                name="5th pct" dot={false} activeDot={false} legendType="none"
              />
              <Area stackId="band" type="monotone" dataKey="p25_delta"
                fill="rgba(209,161,83,0.08)" stroke="none"
                name="25th–5th" dot={false} activeDot={false} legendType="none"
              />
              <Area stackId="band" type="monotone" dataKey="p75_delta"
                fill="rgba(209,161,83,0.18)" stroke="none"
                name="IQR (25th–75th)" dot={false} activeDot={false} legendType="none"
              />
              <Area stackId="band" type="monotone" dataKey="p95_delta"
                fill="rgba(209,161,83,0.08)" stroke="rgba(209,161,83,0.3)"
                strokeWidth={1} strokeDasharray="4 3"
                name="95th pct" dot={false} activeDot={false} legendType="none"
              />

              {/* Median line */}
              <Line type="monotone" dataKey="Median"
                stroke="#d1a153" strokeWidth={2.5}
                dot={false} activeDot={{ r: 4, fill: '#d1a153' }}
              />
            </ComposedChart>
          </ResponsiveContainer>

          <div className="mc-legend">
            <span className="mc-legend-item">
              <span className="mc-legend-band mc-legend-band--outer" />
              Outer band (5th – 95th percentile)
            </span>
            <span className="mc-legend-item">
              <span className="mc-legend-band mc-legend-band--inner" />
              IQR (25th – 75th percentile)
            </span>
            <span className="mc-legend-item">
              <span className="mc-legend-line" />
              Median (50th percentile)
            </span>
          </div>

          <p className="result-disclaimer" style={{ marginTop: 16 }}>
            * Monte Carlo simulations use Geometric Brownian Motion. Results represent statistical
            distributions, not forecasts. Past volatility does not guarantee future behavior.
          </p>
        </div>
      )}
    </div>
  )
}
