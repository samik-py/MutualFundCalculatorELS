import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import TickerTape from '../components/TickerTape'
import { getPortfolio, updatePortfolio, addHolding, removeHolding } from '../services/portfolioApi'
import './PortfolioDetailPage.css'

// Same 5-fund map as PredictorForm.jsx
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
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString(undefined, { dateStyle: 'medium' }) : '—'
}

export default function PortfolioDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const portfolioId = Number(id)
  const [portfolio, setPortfolio] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [editingName, setEditingName] = useState(false)
  const [nameValue, setNameValue] = useState('')
  const nameInputRef = useRef(null)

  // Add holding form
  const [formFundId, setFormFundId] = useState(FUNDS[0]?.id ?? '')
  const [formShares, setFormShares] = useState('')
  const [formPurchasePrice, setFormPurchasePrice] = useState('')
  const [formPurchaseDate, setFormPurchaseDate] = useState('')
  const [adding, setAdding] = useState(false)
  const [removingId, setRemovingId] = useState(null)

  const loadPortfolio = async () => {
    try {
      setError(null)
      const data = await getPortfolio(portfolioId)
      setPortfolio(data)
      setNameValue(data.name)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadPortfolio()
  }, [portfolioId])

  useEffect(() => {
    if (editingName && nameInputRef.current) {
      nameInputRef.current.focus()
      nameInputRef.current.select()
    }
  }, [editingName])

  const handleSaveName = async () => {
    if (!portfolio) return
    const trimmed = nameValue.trim()
    if (trimmed === portfolio.name || !trimmed) {
      setEditingName(false)
      setNameValue(portfolio.name)
      return
    }
    setEditingName(false)
    try {
      const updated = await updatePortfolio(portfolioId, trimmed)
      setPortfolio(updated)
      setNameValue(updated.name)
    } catch (e) {
      setError(e.message)
      setNameValue(portfolio.name)
    }
  }

  const handleRemoveHolding = async (holdingId) => {
    setRemovingId(holdingId)
    try {
      await removeHolding(portfolioId, holdingId)
      await loadPortfolio()
    } catch (e) {
      setError(e.message)
    } finally {
      setRemovingId(null)
    }
  }

  const handleAddHolding = async (e) => {
    e.preventDefault()
    const shares = Number(formShares)
    const purchasePrice = Number(formPurchasePrice)
    if (!formFundId || !formPurchaseDate || Number.isNaN(shares) || shares <= 0 || Number.isNaN(purchasePrice) || purchasePrice <= 0) {
      return
    }
    setAdding(true)
    try {
      await addHolding(portfolioId, {
        fundId: formFundId,
        shares,
        purchasePrice,
        purchaseDate: formPurchaseDate,
      })
      setFormShares('')
      setFormPurchasePrice('')
      setFormPurchaseDate('')
      await loadPortfolio()
    } catch (e) {
      setError(e.message)
    } finally {
      setAdding(false)
    }
  }

  if (loading) {
    return (
      <>
        <Navbar />
        <TickerTape />
        <main className="portfolio-detail-page">
          <p className="portfolio-detail-loading">Loading…</p>
        </main>
      </>
    )
  }

  if (error || !portfolio) {
    return (
      <>
        <Navbar />
        <TickerTape />
        <main className="portfolio-detail-page">
          <div className="portfolio-detail-section">
            <p className="portfolio-detail-error">{error || 'Portfolio not found'}</p>
            <button type="button" className="portfolio-detail-back" onClick={() => navigate('/portfolios')}>
              ← Back to portfolios
            </button>
          </div>
        </main>
      </>
    )
  }

  const holdings = portfolio.holdings ?? []
  const hasHoldings = holdings.length > 0

  return (
    <>
      <Navbar />
      <TickerTape />
      <main className="portfolio-detail-page">
        <div className="portfolio-detail-section">
          <button type="button" className="portfolio-detail-back" onClick={() => navigate('/portfolios')}>
            ← Back to portfolios
          </button>

          {editingName ? (
            <input
              ref={nameInputRef}
              type="text"
              className="portfolio-detail-title-input"
              value={nameValue}
              onChange={(e) => setNameValue(e.target.value)}
              onBlur={handleSaveName}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleSaveName()
                if (e.key === 'Escape') {
                  setNameValue(portfolio.name)
                  setEditingName(false)
                }
              }}
              aria-label="Portfolio name"
            />
          ) : (
            <h1
              className="portfolio-detail-title portfolio-detail-title--editable"
              onClick={() => setEditingName(true)}
              onFocus={() => setEditingName(true)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  setEditingName(true)
                }
              }}
            >
              {portfolio.name}
            </h1>
          )}
          <p className="portfolio-detail-meta">Created {formatDate(portfolio.createdAt)}</p>

          <div className="portfolio-detail-card">
            <h2 className="portfolio-detail-card__head">Holdings</h2>
            {hasHoldings ? (
              <div className="portfolio-detail-table-wrap">
                <table className="portfolio-detail-table">
                  <thead>
                    <tr>
                      <th>Fund Name</th>
                      <th>Shares</th>
                      <th>Purchase Price</th>
                      <th>Purchase Date</th>
                      <th aria-label="Actions" />
                    </tr>
                  </thead>
                  <tbody>
                    {holdings.map((h) => (
                      <tr key={h.id}>
                        <td className="portfolio-detail-table__fund">
                          {FUND_LABEL_BY_ID[h.fundId] ?? h.fundId}
                        </td>
                        <td>{Number(h.shares).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 })}</td>
                        <td>{formatCurrency(h.purchasePrice)}</td>
                        <td>{formatDate(h.purchaseDate)}</td>
                        <td>
                          <button
                            type="button"
                            className="portfolio-detail-remove-btn"
                            onClick={() => handleRemoveHolding(h.id)}
                            disabled={removingId === h.id}
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="portfolio-detail-empty">No holdings yet. Add one below.</p>
            )}

            <form onSubmit={handleAddHolding} className="portfolio-detail-add-form">
              <h3 className="portfolio-detail-add-form__title">Add Holding</h3>
              <div className="portfolio-detail-add-form__row">
                <label className="portfolio-detail-add-form__label" htmlFor="add-fund">
                  Fund
                </label>
                <select
                  id="add-fund"
                  className="portfolio-detail-add-form__select"
                  value={formFundId}
                  onChange={(e) => setFormFundId(e.target.value)}
                  required
                >
                  {FUNDS.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="portfolio-detail-add-form__row">
                <label className="portfolio-detail-add-form__label" htmlFor="add-shares">
                  Shares
                </label>
                <input
                  id="add-shares"
                  type="number"
                  min="0.0001"
                  step="any"
                  className="portfolio-detail-add-form__input"
                  value={formShares}
                  onChange={(e) => setFormShares(e.target.value)}
                  placeholder="0"
                  required
                  disabled={adding}
                />
              </div>
              <div className="portfolio-detail-add-form__row">
                <label className="portfolio-detail-add-form__label" htmlFor="add-price">
                  Purchase Price
                </label>
                <input
                  id="add-price"
                  type="number"
                  min="0"
                  step="0.01"
                  className="portfolio-detail-add-form__input"
                  value={formPurchasePrice}
                  onChange={(e) => setFormPurchasePrice(e.target.value)}
                  placeholder="0.00"
                  required
                  disabled={adding}
                />
              </div>
              <div className="portfolio-detail-add-form__row">
                <label className="portfolio-detail-add-form__label" htmlFor="add-date">
                  Purchase Date
                </label>
                <input
                  id="add-date"
                  type="date"
                  className="portfolio-detail-add-form__input"
                  value={formPurchaseDate}
                  onChange={(e) => setFormPurchaseDate(e.target.value)}
                  required
                  disabled={adding}
                />
              </div>
              <button
                type="submit"
                className="portfolio-detail-add-form__submit"
                disabled={adding}
              >
                {adding ? 'Adding…' : 'Add'}
              </button>
            </form>
          </div>
        </div>
      </main>
    </>
  )
}
