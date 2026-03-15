import { useState, useEffect } from 'react'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts'
import { fetchFunds, compareFunds } from '../services/api'
import './FundCompareChart.css'

const CHART_COLORS = [
  '#d1a153', '#67e8f9', '#a78bfa', '#5de89e', '#f97316',
  '#ec4899', '#38bdf8', '#facc15', '#fb923c', '#4ade80',
]

const CATEGORIES = [
  { label: 'Large-Cap Blend', ids: ['vanguard-500', 'fxaix', 'ivv', 'spy', 'voog'] },
  { label: 'Large-Cap Growth', ids: ['fidelity-growth', 'trowe-bluechip', 'fcntx', 'agthx', 'qqq', 'arkk', 'xlk'] },
  { label: 'Total Market',    ids: ['schwab-total', 'vti', 'vtsax'] },
  { label: 'Balanced',        ids: ['vwelx', 'prwcx'] },
  { label: 'International',   ids: ['dodfx'] },
  { label: 'Fixed Income',    ids: ['pimco-total', 'agg', 'bnd'] },
]

function formatCurrency(v) {
  if (v >= 1_000_000) return `$${(v / 1_000_000).toFixed(2)}M`
  if (v >= 1_000) return `$${(v / 1_000).toFixed(1)}K`
  return `$${v.toFixed(0)}`
}

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip__year">Year {label}</div>
      {payload.map((p) => (
        <div key={p.dataKey} className="chart-tooltip__row">
          <span className="chart-tooltip__dot" style={{ background: p.color }} />
          <span className="chart-tooltip__name">{p.name}</span>
          <span className="chart-tooltip__value">{formatCurrency(p.value)}</span>
        </div>
      ))}
    </div>
  )
}

export default function FundCompareChart() {
  const [allFunds, setAllFunds] = useState([])
  const [selected, setSelected] = useState(['vanguard-500', 'qqq', 'pimco-total'])
  const [amount, setAmount] = useState(10000)
  const [years, setYears] = useState(20)
  const [chartData, setChartData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchFunds().then(setAllFunds).catch(() => {})
  }, [])

  const toggle = (id) => {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : prev.length < 10 ? [...prev, id] : prev
    )
  }

  const handleCompare = async () => {
    if (selected.length === 0) return
    setLoading(true)
    setError(null)
    try {
      const res = await compareFunds(selected, amount, years)
      // Transform to recharts format: [{year, FundName: value, ...}, ...]
      const maxYear = res.funds[0].projection.length
      const rows = Array.from({ length: maxYear }, (_, i) => {
        const row = { year: i }
        res.funds.forEach((f) => { row[f.name] = Math.round(f.projection[i].value) })
        return row
      })
      setChartData({ rows, funds: res.funds })
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const sliderStyle = {
    background: `linear-gradient(to right, var(--gold) 0%, var(--gold) ${((years - 1) / 29) * 100}%, rgba(255,255,255,0.12) ${((years - 1) / 29) * 100}%, rgba(255,255,255,0.12) 100%)`,
  }

  return (
    <div className="fcc-wrapper">
      {/* Controls */}
      <div className="fcc-controls">
        {/* Fund selector */}
        <div className="fcc-field">
          <label className="field-label">Select Funds to Compare <span className="fcc-count">({selected.length}/10)</span></label>
          <div className="fcc-categories">
            {CATEGORIES.map((cat) => {
              const catFunds = allFunds.filter((f) => cat.ids.includes(f.fundId))
              if (catFunds.length === 0) return null
              return (
                <div key={cat.label} className="fcc-category">
                  <div className="fcc-category__label">{cat.label}</div>
                  <div className="fcc-chips">
                    {catFunds.map((f) => (
                      <button
                        key={f.fundId}
                        className={`fcc-chip ${selected.includes(f.fundId) ? 'fcc-chip--on' : ''}`}
                        onClick={() => toggle(f.fundId)}
                        title={f.ticker}
                      >
                        {f.ticker}
                      </button>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div className="fcc-row">
          {/* Amount */}
          <div className="fcc-field fcc-field--sm">
            <label className="field-label">Initial Investment</label>
            <div className="input-wrapper">
              <span className="input-prefix">$</span>
              <input
                className="field-input"
                type="number"
                min="100"
                step="1000"
                value={amount}
                onChange={(e) => setAmount(Math.max(100, Number(e.target.value)))}
              />
            </div>
          </div>

          {/* Years */}
          <div className="fcc-field fcc-field--grow">
            <div className="slider-header">
              <label className="field-label">Time Horizon</label>
              <span className="slider-value"><strong>{years}</strong> yr</span>
            </div>
            <input
              className="field-range"
              type="range" min="1" max="30" value={years}
              style={sliderStyle}
              onChange={(e) => setYears(Number(e.target.value))}
            />
            <div className="slider-ticks">
              <span>1yr</span><span>5yr</span><span>10yr</span><span>20yr</span><span>30yr</span>
            </div>
          </div>
        </div>

        <button
          className={`calc-button ${selected.length === 0 || loading ? 'calc-button--disabled' : ''}`}
          onClick={handleCompare}
          disabled={selected.length === 0 || loading}
        >
          {loading ? 'Calculating...' : `Compare ${selected.length} Fund${selected.length !== 1 ? 's' : ''}`}
        </button>
        {error && <p className="fcc-error">{error}</p>}
      </div>

      {/* Chart */}
      {chartData && (
        <div className="fcc-chart-area">
          <div className="fcc-chart-header">
            <span className="fcc-chart-title">Projected Growth — {years}-Year Horizon</span>
            <span className="fcc-chart-subtitle">Initial: {formatCurrency(amount)}</span>
          </div>

          <ResponsiveContainer width="100%" height={380}>
            <LineChart data={chartData.rows} margin={{ top: 8, right: 24, left: 16, bottom: 8 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
              <XAxis
                dataKey="year"
                tick={{ fill: 'rgba(232,234,240,0.45)', fontSize: 11 }}
                tickLine={false}
                axisLine={{ stroke: 'rgba(255,255,255,0.08)' }}
                tickFormatter={(v) => `Yr ${v}`}
              />
              <YAxis
                tick={{ fill: 'rgba(232,234,240,0.45)', fontSize: 11 }}
                tickLine={false}
                axisLine={false}
                tickFormatter={formatCurrency}
                width={70}
              />
              <Tooltip content={<CustomTooltip />} />
              <Legend
                wrapperStyle={{ paddingTop: 16, fontSize: 11, color: 'rgba(232,234,240,0.6)' }}
              />
              {chartData.funds.map((f, i) => (
                <Line
                  key={f.fundId}
                  type="monotone"
                  dataKey={f.name}
                  stroke={CHART_COLORS[i % CHART_COLORS.length]}
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
              ))}
            </LineChart>
          </ResponsiveContainer>

          {/* Return table */}
          <div className="fcc-return-table">
            <div className="fcc-return-header">
              <span>Fund</span>
              <span>CAPM Annual Return</span>
              <span>Projected in {years}yr</span>
            </div>
            {chartData.funds.map((f, i) => (
              <div key={f.fundId} className="fcc-return-row">
                <span className="fcc-return-name">
                  <span className="fcc-return-dot" style={{ background: CHART_COLORS[i % CHART_COLORS.length] }} />
                  {f.name}
                </span>
                <span className="fcc-return-rate">
                  {(f.annualReturn * 100).toFixed(2)}%
                </span>
                <span className="fcc-return-fv">
                  {formatCurrency(f.projection[f.projection.length - 1].value)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
