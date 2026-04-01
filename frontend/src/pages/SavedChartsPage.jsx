import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { getSavedCharts, deleteChart } from '../services/chartApi'
import './SavedChartsPage.css'

const FUNDS = [
  { id: 'vanguard-500', label: 'Vanguard 500 Index (VFIAX)' },
  { id: 'fidelity-growth', label: 'Fidelity Growth Company (FDGRX)' },
  { id: 'trowe-bluechip', label: 'T. Rowe Price Blue Chip (TRBCX)' },
  { id: 'schwab-total', label: 'Schwab Total Market (SWTSX)' },
  { id: 'pimco-total', label: 'PIMCO Total Return (PTTRX)' },
]

const FUND_LABEL_BY_ID = Object.fromEntries(FUNDS.map((f) => [f.id, f.label]))

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
    minimumFractionDigits: 0,
  }).format(value)
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(undefined, { dateStyle: 'medium' })
}

function fundIdsToNames(fundIdsStr) {
  if (!fundIdsStr || !fundIdsStr.trim()) return []
  return fundIdsStr
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean)
    .map((id) => FUND_LABEL_BY_ID[id] ?? id)
}

export default function SavedChartsPage() {
  const navigate = useNavigate()
  const [charts, setCharts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deleteConfirm, setDeleteConfirm] = useState(null)

  const loadCharts = async () => {
    try {
      setError(null)
      const data = await getSavedCharts()
      setCharts(data)
    } catch (e) {
      setError(e.message)
      setCharts([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCharts()
  }, [])

  const handleDelete = async (id) => {
    if (deleteConfirm !== id) return
    try {
      await deleteChart(id)
      setDeleteConfirm(null)
      await loadCharts()
    } catch (e) {
      setError(e.message)
    }
  }

  const handleCardClick = (id) => {
    if (deleteConfirm === id) return
    navigate(`/compare?chartId=${id}`)
  }

  if (loading) {
    return (
      <>
        <Navbar />
        <main id="main-content" className="saved-charts-page">
          <div className="saved-charts-section">
            <p className="saved-charts-loading" role="status" aria-live="polite">Loading saved charts…</p>
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <Navbar />
      <main id="main-content" className="saved-charts-page">
        <div className="saved-charts-section">
          <header className="saved-charts-header" data-tour="saved-charts">
            <h1 className="saved-charts-title">Saved Charts</h1>
            <p className="saved-charts-subtitle">Open a saved chart to view or edit the comparison.</p>
          </header>

          {error && (
            <div className="saved-charts-error" role="alert">
              {error}
            </div>
          )}

          <div className="saved-charts-grid" role="list">
            {charts.map((chart) => {
              const fundNames = fundIdsToNames(chart.fundIds)
              const isConfirming = deleteConfirm === chart.id
              return (
                <article
                  key={chart.id}
                  className="saved-charts-card"
                  role="listitem"
                  tabIndex={isConfirming ? -1 : 0}
                  onClick={() => handleCardClick(chart.id)}
                  onKeyDown={(e) => {
                    if ((e.key === 'Enter' || e.key === ' ') && !isConfirming) {
                      e.preventDefault()
                      handleCardClick(chart.id)
                    }
                  }}
                  aria-label={`${chart.title}, saved ${formatDate(chart.createdAt)}, ${chart.timeHorizon} ${chart.timeHorizon === 1 ? 'year' : 'years'}`}
                >
                  <div className="saved-charts-card__content">
                    <h2 className="saved-charts-card__title">{chart.title}</h2>
                    <p className="saved-charts-card__date">Saved {formatDate(chart.createdAt)}</p>
                    <ul className="saved-charts-card__funds">
                      {fundNames.map((name) => (
                        <li key={name}>{name}</li>
                      ))}
                    </ul>
                    <p className="saved-charts-card__meta">
                      {chart.timeHorizon} {chart.timeHorizon === 1 ? 'year' : 'years'} · {formatCurrency(chart.amount)}
                    </p>
                  </div>
                  <div className="saved-charts-card__actions">
                    {isConfirming ? (
                      <>
                        <span className="saved-charts-card__confirm-text" role="status">Delete?</span>
                        <button
                          type="button"
                          className="saved-charts-card__btn saved-charts-card__btn--confirm"
                          aria-label={`Confirm delete ${chart.title}`}
                          onClick={(e) => {
                            e.stopPropagation()
                            handleDelete(chart.id)
                          }}
                        >
                          Yes
                        </button>
                        <button
                          type="button"
                          className="saved-charts-card__btn saved-charts-card__btn--cancel"
                          aria-label={`Cancel delete ${chart.title}`}
                          onClick={(e) => {
                            e.stopPropagation()
                            setDeleteConfirm(null)
                          }}
                        >
                          No
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="saved-charts-card__btn saved-charts-card__btn--delete"
                        onClick={(e) => {
                          e.stopPropagation()
                          setDeleteConfirm(chart.id)
                        }}
                        aria-label={`Delete ${chart.title}`}
                      >
                        Delete
                      </button>
                    )}
                  </div>
                </article>
              )
            })}
          </div>

          {!loading && charts.length === 0 && !error && (
            <p className="saved-charts-empty">No saved charts yet. Save a chart from Compare Funds.</p>
          )}
        </div>
      </main>
    </>
  )
}
