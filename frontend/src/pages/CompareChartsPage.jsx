import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTheme } from '../context/ThemeContext'
import Navbar from '../components/Navbar'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import { calculateFutureValue } from '../services/api'
import { saveChart, getSavedChart } from '../services/chartApi'
import '../components/PredictorForm.css'
import './CompareChartsPage.css'

const FUNDS = [
  { id: 'vanguard-500', label: 'Vanguard 500 Index (VFIAX)' },
  { id: 'fidelity-growth', label: 'Fidelity Growth Company (FDGRX)' },
  { id: 'trowe-bluechip', label: 'T. Rowe Price Blue Chip (TRBCX)' },
  { id: 'schwab-total', label: 'Schwab Total Market (SWTSX)' },
  { id: 'pimco-total', label: 'PIMCO Total Return (PTTRX)' },
]

const LINE_COLORS = ['#54779F', '#7399C6', '#8CAED5', '#A8BFDC', '#C3D4E8']
const YEAR_TICKS = [
  { value: 1, label: '1yr', align: 'start' },
  { value: 5, label: '5yr', align: 'center' },
  { value: 10, label: '10yr', align: 'center' },
  { value: 20, label: '20yr', align: 'center' },
  { value: 30, label: '30yr', align: 'end' },
]

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
    minimumFractionDigits: 0,
  }).format(value)
}

export default function CompareChartsPage() {
  const { theme } = useTheme()
  const isLight = theme === 'light'
  const chartTickColor = isLight ? 'rgba(13,31,51,0.8)' : 'rgba(255,255,255,0.9)'
  const chartGridColor = isLight ? 'rgba(0,0,0,0.08)' : 'rgba(255,255,255,0.1)'
  const chartAxisColor = isLight ? 'rgba(0,0,0,0.15)' : 'rgba(255,255,255,0.2)'
  const chartAxisLabelColor = isLight ? 'rgba(13,31,51,0.6)' : 'rgba(255,255,255,0.7)'
  const tooltipStyle = {
    background: isLight ? 'rgba(240,244,249,0.97)' : 'rgba(10,30,60,0.95)',
    border: isLight ? '1px solid rgba(0,0,0,0.12)' : '1px solid rgba(255,255,255,0.15)',
    borderRadius: '8px',
    color: isLight ? '#0d1f33' : '#e8eaf0',
  }
  const sliderTrack = isLight ? 'rgba(0,0,0,0.12)' : 'rgba(255,255,255,0.12)'

  const [searchParams] = useSearchParams()
  const [selectedIds, setSelectedIds] = useState(() => FUNDS.slice(0, 2).map((f) => f.id))
  const [amount, setAmount] = useState('10000')
  const [years, setYears] = useState(10)
  const [chartData, setChartData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [saveModalOpen, setSaveModalOpen] = useState(false)
  const [saveTitle, setSaveTitle] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)
  const [shareStatus, setShareStatus] = useState('')

  const runGenerate = async (fundIdList, principal, yearsVal) => {
    const selected = FUNDS.filter((f) => fundIdList.includes(f.id))
    if (selected.length < 2 || selected.length > 5 || !principal || principal <= 0) return
    setLoading(true)
    setError(null)
    try {
      const annualReturns = {}
      for (const fund of selected) {
        const res = await calculateFutureValue(fund.id, principal, yearsVal)
        annualReturns[fund.id] = res.annualReturn
      }
      const data = []
      for (let y = 0; y <= yearsVal; y++) {
        const point = { year: y }
        selected.forEach((f) => {
          point[f.id] = Math.round(principal * Math.pow(1 + annualReturns[f.id], y))
        })
        data.push(point)
      }
      setChartData({ data, selectedFunds: selected })
    } catch (e) {
      setError(e.message)
      setChartData(null)
    } finally {
      setLoading(false)
    }
  }

  const chartIdParam = searchParams.get('chartId')
  const sharedFundIdsParam = searchParams.get('fundIds')
  const sharedAmountParam = searchParams.get('amount')
  const sharedYearsParam = searchParams.get('years')

  useEffect(() => {
    let cancelled = false

    ;(async () => {
      if (chartIdParam) {
        try {
          const saved = await getSavedChart(Number(chartIdParam))
          if (cancelled) return
          const ids = saved.fundIds.split(',').map((s) => s.trim()).filter(Boolean)
          setSelectedIds(ids)
          setAmount(String(saved.amount))
          setYears(saved.timeHorizon)
          await runGenerate(ids, saved.amount, saved.timeHorizon)
        } catch (e) {
          if (!cancelled) setError(e.message)
        }
        return
      }

      if (sharedFundIdsParam && sharedAmountParam && sharedYearsParam) {
        try {
          const ids = sharedFundIdsParam
            .split(',')
            .map((s) => s.trim())
            .filter((id) => FUNDS.some((f) => f.id === id))
          const parsedAmount = Number(sharedAmountParam)
          const parsedYears = Number(sharedYearsParam)
          if (ids.length >= 2 && ids.length <= 5 && parsedAmount > 0 && parsedYears >= 1 && parsedYears <= 30) {
            if (cancelled) return
            setSelectedIds(ids)
            setAmount(String(parsedAmount))
            setYears(parsedYears)
            await runGenerate(ids, parsedAmount, parsedYears)
          }
        } catch (e) {
          if (!cancelled) setError(e.message)
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [chartIdParam, sharedFundIdsParam, sharedAmountParam, sharedYearsParam])

  const toggleFund = (id) => {
    setSelectedIds((prev) => {
      const next = prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
      if (next.length > 5) return prev
      return next
    })
  }

  const selectedFunds = FUNDS.filter((f) => selectedIds.includes(f.id))
  const canGenerate = selectedFunds.length >= 2 && selectedFunds.length <= 5 && amount && parseFloat(amount) > 0

  const sliderStyle = {
    background: `linear-gradient(to right, var(--gold) 0%, var(--gold) ${((years - 1) / 29) * 100}%, ${sliderTrack} ${((years - 1) / 29) * 100}%, ${sliderTrack} 100%)`,
  }

  const handleGenerate = async () => {
    if (!canGenerate) return
    const principal = parseFloat(amount)
    if (!principal || principal <= 0) return
    await runGenerate(selectedIds, principal, years)
  }

  const handleOpenSaveModal = () => {
    setSaveTitle('')
    setSaveError(null)
    setSaveModalOpen(true)
  }

  const handleSaveChart = async (e) => {
    e.preventDefault()
    const title = saveTitle.trim()
    if (!title) return
    setSaving(true)
    setSaveError(null)
    try {
      await saveChart({
        title,
        fundIds: selectedIds.join(','),
        timeHorizon: years,
        amount: parseFloat(amount),
      })
      setSaveModalOpen(false)
      setSaveTitle('')
    } catch (e) {
      setSaveError(e.message)
    } finally {
      setSaving(false)
    }
  }

  const handleShareChart = async () => {
    if (!chartData) return
    const baseUrl = `${window.location.origin}/compare`
    const shareUrl = `${baseUrl}?fundIds=${encodeURIComponent(selectedIds.join(','))}&amount=${encodeURIComponent(amount)}&years=${encodeURIComponent(String(years))}`

    try {
      await navigator.clipboard.writeText(shareUrl)
      setShareStatus('Share link copied')
      window.setTimeout(() => setShareStatus(''), 2500)
    } catch {
      setShareStatus(shareUrl)
    }
  }

  return (
    <>
      <Navbar />
      <main id="main-content" className="compare-charts-page">
        <div className="compare-charts-section">
          <header className="compare-charts-header">
            <h1 className="compare-charts-title">Compare Fund Growth</h1>
            <p className="compare-charts-subtitle">
              Select 2-5 funds, set your investment and horizon, then generate a projected growth chart.
            </p>
          </header>

          <div className="compare-charts-card">
            <fieldset className="field-group" aria-describedby={selectedIds.length < 2 ? 'fund-hint' : undefined}>
              <legend className="field-label">Select funds (2–5)</legend>
              <div className="compare-charts-checkboxes">
                {FUNDS.map((f) => (
                  <label key={f.id} className="compare-charts-checkbox">
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(f.id)}
                      onChange={() => toggleFund(f.id)}
                      disabled={!selectedIds.includes(f.id) && selectedIds.length >= 5}
                      aria-label={f.label}
                    />
                    <span className="compare-charts-checkbox-label">{f.label}</span>
                  </label>
                ))}
              </div>
              {selectedIds.length < 2 && (
                <span id="fund-hint" className="field-hint" role="status">Select at least 2 funds.</span>
              )}
            </fieldset>

            <div className="field-group">
              <label className="field-label" htmlFor="compare-amount">Initial Investment</label>
              <div className="input-wrapper">
                <span className="input-prefix" aria-hidden="true">$</span>
                <input
                  id="compare-amount"
                  className="field-input"
                  type="number"
                  min="0"
                  step="1000"
                  placeholder="10,000"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  aria-label="Initial investment amount in US dollars"
                />
              </div>
            </div>

            <div className="field-group">
              <div className="slider-header">
                <label className="field-label" htmlFor="compare-years">Time Horizon</label>
                <span className="slider-value" aria-hidden="true">
                  <strong>{years}</strong> {years === 1 ? 'year' : 'years'}
                </span>
              </div>
              <input
                id="compare-years"
                className="field-range"
                type="range"
                min="1"
                max="30"
                value={years}
                style={sliderStyle}
                onChange={(e) => setYears(parseInt(e.target.value, 10))}
                aria-valuemin={1}
                aria-valuemax={30}
                aria-valuenow={years}
                aria-valuetext={`${years} ${years === 1 ? 'year' : 'years'}`}
              />
              <div className="slider-ticks">
                {YEAR_TICKS.map((tick) => (
                  <span
                    key={tick.value}
                    className={`slider-ticks__label slider-ticks__label--${tick.align}`}
                    style={{ left: `${((tick.value - 1) / 29) * 100}%` }}
                  >
                    {tick.label}
                  </span>
                ))}
              </div>
            </div>

            {error && (
              <div className="compare-charts-error" role="alert">
                {error}
              </div>
            )}

            <button
              type="button"
              className={`calc-button ${!canGenerate ? 'calc-button--disabled' : ''}`}
              onClick={handleGenerate}
              disabled={!canGenerate || loading}
            >
              {loading ? 'Generating...' : 'Generate Chart'}
            </button>
          </div>

          {chartData && chartData.data.length > 0 && (
            <>
              <div className="compare-charts-save-row">
                <button type="button" className="compare-charts-save-btn" onClick={handleOpenSaveModal}>
                  Save This Chart
                </button>
                <button type="button" className="compare-charts-save-btn" onClick={handleShareChart}>
                  Share Chart
                </button>
                {shareStatus && (
                  <span
                    className="compare-charts-share-status"
                    role="status"
                    aria-live="polite"
                    aria-atomic="true"
                  >
                    {shareStatus}
                  </span>
                )}
              </div>
              <div className="compare-charts-chart-wrap">
                <ResponsiveContainer width="100%" height={420}>
                  <LineChart data={chartData.data} margin={{ top: 16, right: 16, left: 30, bottom: 8 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke={chartGridColor} vertical={false} />
                    <XAxis
                      dataKey="year"
                      stroke={chartAxisColor}
                      tick={{ fill: chartTickColor, fontSize: 12 }}
                      tickLine={{ stroke: chartAxisColor }}
                      axisLine={{ stroke: chartAxisColor }}
                      label={{ value: 'Years', position: 'insideBottom', offset: -4, fill: chartAxisLabelColor }}
                    />
                    <YAxis
                      width={96}
                      stroke={chartAxisColor}
                      tick={{ fill: chartTickColor, fontSize: 12 }}
                      tickLine={{ stroke: chartAxisColor }}
                      axisLine={{ stroke: chartAxisColor }}
                      tickFormatter={(v) => formatCurrency(v)}
                      label={{
                        value: 'Projected value',
                        angle: -90,
                        position: 'insideLeft',
                        offset: -10,
                        fill: chartAxisLabelColor,
                      }}
                    />
                    <Tooltip
                      contentStyle={tooltipStyle}
                      labelStyle={{ color: chartTickColor }}
                      formatter={(value, name) => {
                        const fund = chartData.selectedFunds.find((f) => f.id === name)
                        return [formatCurrency(value), fund ? fund.label : name]
                      }}
                      labelFormatter={(label) => `Year ${label}`}
                    />
                    <Legend
                      wrapperStyle={{ paddingTop: 16, color: isLight ? 'rgba(13,31,51,0.8)' : 'rgba(232,234,240,0.8)' }}
                      formatter={(value) => {
                        const fund = chartData.selectedFunds.find((f) => f.id === value)
                        return fund ? fund.label : value
                      }}
                      iconType="line"
                      iconSize={10}
                    />
                    {chartData.selectedFunds.map((f, i) => (
                      <Line
                        key={f.id}
                        type="monotone"
                        dataKey={f.id}
                        name={f.id}
                        stroke={LINE_COLORS[i % LINE_COLORS.length]}
                        strokeWidth={2}
                        dot={false}
                        activeDot={{ r: 4, strokeWidth: 0 }}
                      />
                    ))}
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </>
          )}

          {saveModalOpen && (
            <div
              className="compare-charts-modal-backdrop"
              onClick={() => !saving && setSaveModalOpen(false)}
              onKeyDown={(e) => { if (e.key === 'Escape' && !saving) setSaveModalOpen(false) }}
              role="presentation"
            >
              <div
                className="compare-charts-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="save-chart-heading"
                onClick={(e) => e.stopPropagation()}
              >
                <h3 id="save-chart-heading" className="compare-charts-modal__title">Save chart</h3>
                <form onSubmit={handleSaveChart} className="compare-charts-modal__form">
                  <label className="compare-charts-modal__label" htmlFor="chart-title">
                    Title
                  </label>
                  <input
                    id="chart-title"
                    type="text"
                    className="compare-charts-modal__input"
                    value={saveTitle}
                    onChange={(e) => setSaveTitle(e.target.value)}
                    placeholder="e.g. Retirement comparison"
                    autoFocus
                    required
                    aria-required="true"
                    disabled={saving}
                    aria-describedby={saveError ? 'save-chart-error' : undefined}
                  />
                  {saveError && <p id="save-chart-error" className="compare-charts-modal__error" role="alert" aria-live="assertive">{saveError}</p>}
                  <div className="compare-charts-modal__actions">
                    <button
                      type="button"
                      className="compare-charts-modal__btn compare-charts-modal__btn--cancel"
                      onClick={() => !saving && setSaveModalOpen(false)}
                      disabled={saving}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="compare-charts-modal__btn compare-charts-modal__btn--submit"
                      disabled={saving || !saveTitle.trim()}
                    >
                      {saving ? 'Saving...' : 'Save'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      </main>
    </>
  )
}
