import { useState, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { getPortfolios } from '../services/portfolioApi'
import { getDashboard, getPortfolioPerformance } from '../services/dashboardApi'
import './DashboardPage.css'

const PIE_COLORS = ['#54779F', '#7399C6', '#8CAED5', '#A8BFDC', '#C3D4E8']

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

function formatPct(value) {
  return `${value >= 0 ? '+' : ''}${Number(value).toFixed(2)}%`
}

const TABLE_COLUMNS = [
  { key: 'fundName', label: 'Fund Name' },
  { key: 'ticker', label: 'Ticker' },
  { key: 'shares', label: 'Shares' },
  { key: 'costBasis', label: 'Cost Basis' },
  { key: 'currentValue', label: 'Current Value' },
  { key: 'gainLoss', label: 'Gain/Loss ($)' },
  { key: 'returnPct', label: 'Return (%)' },
]

export default function DashboardPage() {
  const [portfolios, setPortfolios] = useState([])
  const [selectedPortfolioId, setSelectedPortfolioId] = useState(null) // null = All
  const [dashboardTotals, setDashboardTotals] = useState(null)
  const [holdings, setHoldings] = useState([])
  const [totals, setTotals] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [sortKey, setSortKey] = useState('currentValue')
  const [sortAsc, setSortAsc] = useState(false)

  const loadData = async () => {
    setLoading(true)
    setError(null)
    try {
      const [portfoliosList, dashboard] = await Promise.all([
        getPortfolios(),
        getDashboard(),
      ])
      setPortfolios(portfoliosList)
      setDashboardTotals(dashboard)

      if (selectedPortfolioId === null) {
        if (portfoliosList.length === 0) {
          setHoldings([])
          setTotals(dashboard)
        } else {
          const perfs = await Promise.all(
            portfoliosList.map((p) => getPortfolioPerformance(p.id))
          )
          const merged = perfs.flatMap((p) => p.holdings)
          setHoldings(merged)
          setTotals(dashboard)
        }
      } else {
        const perf = await getPortfolioPerformance(selectedPortfolioId)
        setHoldings(perf.holdings)
        setTotals({
          totalCostBasis: perf.totalCostBasis,
          totalCurrentValue: perf.totalCurrentValue,
          totalGainLoss: perf.totalGainLoss,
          totalReturnPct: perf.totalReturnPct,
        })
      }
    } catch (e) {
      setError(e.message)
      setHoldings([])
      setTotals(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [selectedPortfolioId])

  const pieData = useMemo(() => {
    const byFund = new Map()
    holdings.forEach((h) => {
      const existing = byFund.get(h.fundId)
      if (existing) {
        existing.value += h.currentValue
      } else {
        byFund.set(h.fundId, { name: h.fundName, value: h.currentValue, fundId: h.fundId })
      }
    })
    return Array.from(byFund.values()).filter((d) => d.value > 0)
  }, [holdings])

  const sortedHoldings = useMemo(() => {
    const arr = [...holdings]
    arr.sort((a, b) => {
      const aVal = a[sortKey]
      const bVal = b[sortKey]
      if (typeof aVal === 'number' && typeof bVal === 'number') return sortAsc ? aVal - bVal : bVal - aVal
      const aStr = String(aVal ?? '')
      const bStr = String(bVal ?? '')
      return sortAsc ? aStr.localeCompare(bStr) : bStr.localeCompare(aStr)
    })
    return arr
  }, [holdings, sortKey, sortAsc])

  const handleSort = (key) => {
    setSortKey(key)
    setSortAsc((prev) => (sortKey === key ? !prev : true))
  }

  const hasHoldings = holdings.length > 0
  const displayTotals = totals ?? dashboardTotals

  if (loading && !displayTotals) {
    return (
      <>
        <Navbar />
        <main className="dashboard-page">
          <div className="dashboard-section">
            <p className="dashboard-loading">Loading dashboard…</p>
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <Navbar />
      <main className="dashboard-page">
        <div className="dashboard-section">
          <header className="dashboard-header">
            <h1 className="dashboard-title">Dashboard</h1>
            <p className="dashboard-subtitle">Your portfolio performance at a glance</p>
            {portfolios.length > 1 && (
              <div className="dashboard-filter">
                <label className="dashboard-filter__label" htmlFor="portfolio-filter">
                  Portfolio
                </label>
                <select
                  id="portfolio-filter"
                  className="dashboard-filter__select"
                  value={selectedPortfolioId ?? 'all'}
                  onChange={(e) => setSelectedPortfolioId(e.target.value === 'all' ? null : Number(e.target.value))}
                >
                  <option value="all">All Portfolios</option>
                  {portfolios.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </header>

          {error && (
            <div className="dashboard-error" role="alert">
              {error}
            </div>
          )}

          {!hasHoldings && !loading && (
            <div className="dashboard-empty">
              <p className="dashboard-empty__text">You don&apos;t have any holdings yet.</p>
              <p className="dashboard-empty__hint">Create a portfolio and add funds to see performance here.</p>
              <Link to="/portfolios" className="dashboard-empty__cta">
                Go to Portfolios
              </Link>
            </div>
          )}

          {hasHoldings && displayTotals && (
            <>
              <div className="dashboard-cards">
                <div className="dashboard-card">
                  <span className="dashboard-card__label">Total Value</span>
                  <span className="dashboard-card__value dashboard-card__value--primary">
                    {formatCurrency(displayTotals.totalCurrentValue)}
                  </span>
                </div>
                <div className="dashboard-card">
                  <span className="dashboard-card__label">Total Gain/Loss</span>
                  <span
                    className={`dashboard-card__value ${
                      displayTotals.totalGainLoss >= 0 ? 'dashboard-card__value--positive' : 'dashboard-card__value--negative'
                    }`}
                  >
                    {formatCurrency(displayTotals.totalGainLoss)}
                  </span>
                </div>
                <div className="dashboard-card">
                  <span className="dashboard-card__label">Overall Return</span>
                  <span
                    className={`dashboard-card__value ${
                      displayTotals.totalReturnPct >= 0 ? 'dashboard-card__value--positive' : 'dashboard-card__value--negative'
                    }`}
                  >
                    {formatPct(displayTotals.totalReturnPct)}
                  </span>
                </div>
              </div>

              {pieData.length > 0 && (
                <div className="dashboard-chart-card">
                  <h2 className="dashboard-chart-title">Allocation by fund</h2>
                  <ResponsiveContainer width="100%" height={320}>
                    <PieChart>
                      <Pie
                        data={pieData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        outerRadius={100}
                        label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                      >
                        {pieData.map((_, i) => (
                          <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip
                        formatter={(value) => [formatCurrency(value), 'Value']}
                        contentStyle={{
                          background: 'rgba(10, 30, 60, 0.95)',
                          border: '1px solid rgba(255,255,255,0.15)',
                          borderRadius: '8px',
                          color: '#e8eaf0',
                        }}
                      />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}

              <div className="dashboard-table-card">
                <h2 className="dashboard-chart-title">Holdings</h2>
                <div className="dashboard-table-wrap">
                  <table className="dashboard-table">
                    <thead>
                      <tr>
                        {TABLE_COLUMNS.map(({ key, label }) => (
                          <th
                            key={key}
                            className="dashboard-table__th"
                            onClick={() => handleSort(key)}
                          >
                            {label}
                            <span className="dashboard-table__sort">
                              {sortKey === key ? (sortAsc ? ' ↑' : ' ↓') : ''}
                            </span>
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {sortedHoldings.map((h, i) => (
                        <tr key={`${h.fundId}-${i}`}>
                          <td className="dashboard-table__fund">{h.fundName}</td>
                          <td>{h.ticker}</td>
                          <td>{Number(h.shares).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 })}</td>
                          <td>{formatCurrency(h.costBasis)}</td>
                          <td>{formatCurrency(h.currentValue)}</td>
                          <td className={h.gainLoss >= 0 ? 'dashboard-table--positive' : 'dashboard-table--negative'}>
                            {formatCurrency(h.gainLoss)}
                          </td>
                          <td className={h.returnPct >= 0 ? 'dashboard-table--positive' : 'dashboard-table--negative'}>
                            {formatPct(h.returnPct)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      </main>
    </>
  )
}
