import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { getPortfolios, createPortfolio, deletePortfolio } from '../services/portfolioApi'
import './PortfoliosPage.css'

export default function PortfoliosPage() {
  const navigate = useNavigate()
  const [portfolios, setPortfolios] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [newName, setNewName] = useState('')
  const [creating, setCreating] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState(null)

  const loadPortfolios = async () => {
    try {
      setError(null)
      const data = await getPortfolios()
      setPortfolios(data)
    } catch (e) {
      setError(e.message)
      setPortfolios([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadPortfolios()
  }, [])

  const handleCreate = async (e) => {
    e.preventDefault()
    const name = newName.trim()
    if (!name) return
    setCreating(true)
    try {
      await createPortfolio(name)
      setNewName('')
      setModalOpen(false)
      await loadPortfolios()
    } catch (e) {
      setError(e.message)
    } finally {
      setCreating(false)
    }
  }

  const handleDelete = async (id) => {
    if (deleteConfirm !== id) return
    try {
      await deletePortfolio(id)
      setDeleteConfirm(null)
      await loadPortfolios()
    } catch (e) {
      setError(e.message)
    }
  }

  const formatDate = (iso) => {
    if (!iso) return '—'
    const d = new Date(iso)
    return d.toLocaleDateString(undefined, { dateStyle: 'medium' })
  }

  if (loading) {
    return (
      <>
        <Navbar />
        <main id="main-content" className="portfolios-page">
          <div className="portfolios-section">
            <p className="portfolios-loading" role="status" aria-live="polite">Loading portfolios…</p>
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <Navbar />
      <main id="main-content" className="portfolios-page">
        <div className="portfolios-section">
          <header className="portfolios-header" data-tour="portfolios">
            <h1 className="portfolios-title">My Portfolios</h1>
            <p className="portfolios-subtitle">View and manage your portfolios</p>
            <button
              type="button"
              className="portfolios-create-btn"
              onClick={() => setModalOpen(true)}
            >
              Create New Portfolio
            </button>
          </header>

          {error && (
            <div className="portfolios-error" role="alert">
              {error}
            </div>
          )}

          <div className="portfolios-grid" role="list">
            {portfolios.map((p) => (
              <article
                key={p.id}
                className="portfolio-card"
                role="listitem"
                tabIndex={deleteConfirm === p.id ? -1 : 0}
                onClick={() => deleteConfirm !== p.id && navigate(`/portfolios/${p.id}`)}
                onKeyDown={(e) => {
                  if ((e.key === 'Enter' || e.key === ' ') && deleteConfirm !== p.id) {
                    e.preventDefault()
                    navigate(`/portfolios/${p.id}`)
                  }
                }}
                aria-label={`${p.name}, ${p.holdingCount} ${p.holdingCount === 1 ? 'holding' : 'holdings'}, created ${formatDate(p.createdAt)}`}
              >
                <div className="portfolio-card__content">
                  <h2 className="portfolio-card__name">{p.name}</h2>
                  <p className="portfolio-card__meta">
                    {p.holdingCount} {p.holdingCount === 1 ? 'holding' : 'holdings'}
                  </p>
                  <p className="portfolio-card__date">{formatDate(p.createdAt)}</p>
                </div>
                <div className="portfolio-card__actions">
                  {deleteConfirm === p.id ? (
                    <>
                      <span className="portfolio-card__confirm-text" role="status">Delete?</span>
                      <button
                        type="button"
                        className="portfolio-card__btn portfolio-card__btn--confirm"
                        aria-label={`Confirm delete ${p.name}`}
                        onClick={(e) => {
                          e.stopPropagation()
                          handleDelete(p.id)
                        }}
                      >
                        Yes
                      </button>
                      <button
                        type="button"
                        className="portfolio-card__btn portfolio-card__btn--cancel"
                        aria-label={`Cancel delete ${p.name}`}
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
                      className="portfolio-card__btn portfolio-card__btn--delete"
                      onClick={(e) => {
                        e.stopPropagation()
                        setDeleteConfirm(p.id)
                      }}
                      aria-label={`Delete ${p.name}`}
                    >
                      Delete
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>

          {!loading && portfolios.length === 0 && !error && (
            <p className="portfolios-empty">No portfolios yet. Create one to get started.</p>
          )}
        </div>

        {modalOpen && (
          <div
            className="portfolios-modal-backdrop"
            onClick={() => !creating && setModalOpen(false)}
            onKeyDown={(e) => { if (e.key === 'Escape' && !creating) setModalOpen(false) }}
            role="presentation"
          >
            <div
              className="portfolios-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="create-portfolio-heading"
              onClick={(e) => e.stopPropagation()}
            >
              <h3 id="create-portfolio-heading" className="portfolios-modal__title">New Portfolio</h3>
              <form onSubmit={handleCreate} className="portfolios-modal__form">
                <label className="portfolios-modal__label" htmlFor="portfolio-name">
                  Name
                </label>
                <input
                  id="portfolio-name"
                  type="text"
                  className="portfolios-modal__input"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="e.g. Retirement 2030"
                  autoFocus
                  required
                  aria-required="true"
                  disabled={creating}
                />
                <div className="portfolios-modal__actions">
                  <button
                    type="button"
                    className="portfolios-modal__btn portfolios-modal__btn--cancel"
                    onClick={() => !creating && setModalOpen(false)}
                    disabled={creating}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="portfolios-modal__btn portfolios-modal__btn--submit"
                    disabled={creating || !newName.trim()}
                  >
                    {creating ? 'Creating…' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </main>
    </>
  )
}
