import { useState, useEffect } from 'react'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer
} from 'recharts'
import { fetchFunds, comparePortfolios } from '../services/api'
import './PortfolioBuilder.css'

function formatCurrency(v) {
  if (v >= 1_000_000) return `$${(v / 1_000_000).toFixed(2)}M`
  if (v >= 1_000) return `$${(v / 1_000).toFixed(1)}K`
  return `$${Math.round(v)}`
}

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip__year">Year {label}</div>
      {payload.map(p => (
        <div key={p.dataKey} className="chart-tooltip__row">
          <span className="chart-tooltip__dot" style={{ background: p.color }} />
          <span className="chart-tooltip__name">{p.name}</span>
          <span className="chart-tooltip__value">{formatCurrency(p.value)}</span>
        </div>
      ))}
    </div>
  )
}

const EMPTY_HOLDING = () => ({ fundId: '', weight: 20 })

function PortfolioPanel({ name, setName, holdings, setHoldings, allFunds, color }) {
  const totalWeight = holdings.reduce((s, h) => s + (h.weight || 0), 0)
  const pct = Math.min(100, Math.round(totalWeight))

  const addHolding = () => setHoldings(h => [...h, EMPTY_HOLDING()])
  const removeHolding = (i) => setHoldings(h => h.filter((_, idx) => idx !== i))
  const updateHolding = (i, field, val) =>
    setHoldings(h => h.map((item, idx) => idx === i ? { ...item, [field]: val } : item))

  return (
    <div className="pb-panel" style={{ '--panel-color': color }}>
      <div className="pb-panel__name-row">
        <div className="pb-panel__color-dot" />
        <input
          className="pb-panel__name-input"
          value={name}
          onChange={e => setName(e.target.value)}
          placeholder="Portfolio name"
          maxLength={30}
        />
      </div>

      {/* Weight indicator */}
      <div className="pb-weight-bar">
        <div
          className={`pb-weight-fill ${pct === 100 ? 'pb-weight-fill--ok' : pct > 100 ? 'pb-weight-fill--over' : ''}`}
          style={{ width: `${pct}%` }}
        />
      </div>
      <div className={`pb-weight-label ${pct === 100 ? 'pb-weight-label--ok' : ''}`}>
        {totalWeight.toFixed(0)}% allocated {pct === 100 ? '✓' : pct > 100 ? '(over 100%)' : ''}
      </div>

      {/* Holdings */}
      <div className="pb-holdings">
        {holdings.map((h, i) => (
          <div key={i} className="pb-holding">
            <div className="select-wrapper pb-holding__select">
              <select
                className="field-select"
                value={h.fundId}
                onChange={e => updateHolding(i, 'fundId', e.target.value)}
              >
                <option value="">— select fund —</option>
                {allFunds.map(f => (
                  <option key={f.fundId} value={f.fundId}>{f.name} ({f.ticker})</option>
                ))}
              </select>
              <svg className="select-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M6 9l6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <div className="pb-holding__weight">
              <input
                className="pb-weight-input"
                type="number" min="1" max="100" step="1"
                value={h.weight}
                onChange={e => updateHolding(i, 'weight', Math.max(1, Math.min(100, Number(e.target.value))))}
              />
              <span className="pb-weight-pct">%</span>
            </div>
            <button className="pb-remove" onClick={() => removeHolding(i)} title="Remove">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" width="14" height="14">
                <path d="M6 18L18 6M6 6l12 12" strokeLinecap="round" />
              </svg>
            </button>
          </div>
        ))}
      </div>

      <button className="pb-add" onClick={addHolding} disabled={holdings.length >= 10}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" width="14" height="14">
          <path d="M12 5v14M5 12h14" strokeLinecap="round" />
        </svg>
        Add Fund
      </button>
    </div>
  )
}

export default function PortfolioBuilder() {
  const [allFunds, setAllFunds] = useState([])
  const [nameA, setNameA] = useState('Growth Portfolio')
  const [nameB, setNameB] = useState('Conservative Portfolio')
  const [holdingsA, setHoldingsA] = useState([
    { fundId: 'qqq', weight: 40 },
    { fundId: 'fidelity-growth', weight: 35 },
    { fundId: 'vanguard-500', weight: 25 },
  ])
  const [holdingsB, setHoldingsB] = useState([
    { fundId: 'vanguard-500', weight: 40 },
    { fundId: 'pimco-total', weight: 35 },
    { fundId: 'schwab-total', weight: 25 },
  ])
  const [amount, setAmount] = useState(50000)
  const [years, setYears] = useState(20)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchFunds().then(setAllFunds).catch(() => {})
  }, [])

  const validHoldings = (holdings) =>
    holdings.filter(h => h.fundId && h.weight > 0)

  const canCompare =
    validHoldings(holdingsA).length > 0 &&
    validHoldings(holdingsB).length > 0 &&
    !loading

  const handleCompare = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await comparePortfolios(
        validHoldings(holdingsA), validHoldings(holdingsB),
        amount, years, nameA, nameB
      )
      // Build chart rows
      const rows = res.portfolioA.map((ptA, i) => ({
        year: ptA.year,
        [res.nameA]: Math.round(ptA.value),
        [res.nameB]: Math.round(res.portfolioB[i].value),
      }))
      setResult({ rows, nameA: res.nameA, nameB: res.nameB, returnA: res.annualReturnA, returnB: res.annualReturnB })
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const sliderStyle = {
    background: `linear-gradient(to right, var(--gold) 0%, var(--gold) ${((years - 1) / 29) * 100}%, rgba(255,255,255,0.12) ${((years - 1) / 29) * 100}%, rgba(255,255,255,0.12) 100%)`,
  }

  const finalA = result?.rows?.[result.rows.length - 1]?.[result.nameA]
  const finalB = result?.rows?.[result.rows.length - 1]?.[result.nameB]

  return (
    <div className="pb-wrapper">
      {/* Dual portfolio panels */}
      <div className="pb-panels">
        <PortfolioPanel
          name={nameA} setName={setNameA}
          holdings={holdingsA} setHoldings={setHoldingsA}
          allFunds={allFunds} color="#d1a153"
        />
        <PortfolioPanel
          name={nameB} setName={setNameB}
          holdings={holdingsB} setHoldings={setHoldingsB}
          allFunds={allFunds} color="#67e8f9"
        />
      </div>

      {/* Shared controls */}
      <div className="pb-shared-controls">
        <div className="pb-shared-row">
          <div className="mc-field">
            <label className="field-label">Initial Investment</label>
            <div className="input-wrapper">
              <span className="input-prefix">$</span>
              <input
                className="field-input"
                type="number" min="1000" step="5000"
                value={amount}
                onChange={e => setAmount(Math.max(1000, Number(e.target.value)))}
              />
            </div>
          </div>
          <div className="mc-field mc-field--grow">
            <div className="slider-header">
              <label className="field-label">Time Horizon</label>
              <span className="slider-value"><strong>{years}</strong> yr</span>
            </div>
            <input
              className="field-range" type="range" min="1" max="30" value={years}
              style={sliderStyle}
              onChange={e => setYears(Number(e.target.value))}
            />
            <div className="slider-ticks">
              <span>1yr</span><span>5yr</span><span>10yr</span><span>20yr</span><span>30yr</span>
            </div>
          </div>
        </div>

        <button
          className={`calc-button ${!canCompare ? 'calc-button--disabled' : ''}`}
          onClick={handleCompare} disabled={!canCompare}
        >
          {loading ? 'Computing...' : 'Compare Portfolios'}
        </button>
        {error && <p className="mc-error">{error}</p>}
      </div>

      {/* Result chart */}
      {result && (
        <div className="pb-chart-area">
          <div className="pb-chart-header">
            <h3 className="mc-chart-title">Portfolio Comparison — {years}-Year Projection</h3>
          </div>

          {/* Stats */}
          <div className="pb-stats">
            {[
              { name: result.nameA, ret: result.returnA, final: finalA, color: '#d1a153' },
              { name: result.nameB, ret: result.returnB, final: finalB, color: '#67e8f9' },
            ].map(({ name, ret, final, color }) => (
              <div key={name} className="pb-stat-card">
                <div className="pb-stat-dot" style={{ background: color }} />
                <div className="pb-stat-body">
                  <div className="pb-stat-name">{name}</div>
                  <div className="pb-stat-return">
                    CAPM Return: <strong style={{ color }}>{(ret * 100).toFixed(2)}%/yr</strong>
                  </div>
                  <div className="pb-stat-fv" style={{ color }}>
                    {formatCurrency(final)} in {years}yr
                  </div>
                </div>
              </div>
            ))}
          </div>

          <ResponsiveContainer width="100%" height={340}>
            <LineChart data={result.rows} margin={{ top: 8, right: 24, left: 16, bottom: 8 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
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
              <Legend wrapperStyle={{ paddingTop: 16, fontSize: 11, color: 'rgba(232,234,240,0.6)' }} />
              <Line type="monotone" dataKey={result.nameA} stroke="#d1a153" strokeWidth={2.5} dot={false} activeDot={{ r: 4 }} />
              <Line type="monotone" dataKey={result.nameB} stroke="#67e8f9" strokeWidth={2.5} dot={false} activeDot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>

          <p className="result-disclaimer" style={{ marginTop: 16 }}>
            * Projections use CAPM expected returns. Weights are normalized to 100%. Results are illustrative only.
          </p>
        </div>
      )}
    </div>
  )
}

