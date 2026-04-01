import { useState } from 'react'
import { getAIPortfolio } from '../services/api'
import './AISection.css'

const COLORS = ['#54779F', '#7399C6', '#A8BFDC', 'rgba(255,255,255,0.3)']

function riskColor(level) {
  const l = (level || '').toLowerCase()
  if (l.includes('aggressive')) return '#f97316'
  if (l.includes('conservative')) return '#5de89e'
  return '#7399C6'
}

export default function AISection() {
  const [prompt, setPrompt] = useState('')
  const [response, setResponse] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleGenerate = async () => {
    if (!prompt.trim()) return
    setLoading(true)
    setResponse(null)
    setError(null)
    try {
      const data = await getAIPortfolio(prompt)
      // Backend returns { allocation: [{name, pct}], summary, expectedReturn, riskLevel }
      // Attach colors for display
      const enriched = {
        ...data,
        allocation: data.allocation.map((item, i) => ({
          ...item,
          color: COLORS[i % COLORS.length],
        })),
        riskLevel: data.riskLevel || 'Moderate',
      }
      setResponse(enriched)
    } catch (e) {
      setError('Unable to reach the portfolio service. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="ai-section" data-tour="ai-section">
      <div className="section-divider" />
      <div className="section">
        <div className="ai-header reveal">
          <span className="ai-eyebrow">Goldman Sachs · AI &amp; Innovation</span>
          <h2 className="ai-title">Portfolio Optimization</h2>
          <p className="ai-subtitle">
            Describe your investment goals and risk tolerance. Our engine will
            recommend an optimal fund allocation for your profile.
          </p>
        </div>

        <div className="ai-card reveal reveal-delay-1">
          <div className="ai-input-area">
            <label className="field-label ai-label" htmlFor="ai-profile">Your Investment Profile</label>
            <textarea
              id="ai-profile"
              className="ai-textarea"
              placeholder="e.g. I'm 28 years old, looking for aggressive long-term growth over 20 years. I can tolerate high volatility and market downturns..."
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              rows={5}
            />
          </div>

          <div className="ai-chips">
            {['Aggressive Growth', 'Balanced Income', 'Conservative Safety'].map((label) => (
              <button
                key={label}
                type="button"
                className="ai-chip"
                aria-label={`Use profile: ${label}`}
                onClick={() => setPrompt(label.toLowerCase())}
              >
                {label}
              </button>
            ))}
          </div>

          <button
            type="button"
            className={`ai-button ${!prompt.trim() || loading ? 'ai-button--disabled' : ''}`}
            onClick={handleGenerate}
            disabled={!prompt.trim() || loading}
            aria-busy={loading}
          >
            {loading ? (
              <>
                <span className="ai-spinner" aria-hidden="true" />
                <span>Analyzing Profile…</span>
              </>
            ) : (
              <>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="ai-button__icon" aria-hidden="true" focusable="false">
                  <path d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                <span>Generate Portfolio</span>
              </>
            )}
          </button>

          {error && <p className="ai-error" role="alert" aria-live="assertive">{error}</p>}

          {/* ── AI Response ── */}
          {response && (
            <div className="ai-result" role="region" aria-label="Portfolio recommendation" aria-live="polite" aria-atomic="true">
              <div className="ai-result__header">
                <div className="ai-result__badge">
                  <span
                    className="ai-result__risk"
                    style={{ color: riskColor(response.riskLevel) }}
                  >
                    {response.riskLevel} Risk
                  </span>
                  <span className="ai-result__return">
                    Expected Return: <strong>{response.expectedReturn}</strong> / yr
                  </span>
                </div>
              </div>

              {/* Allocation bars */}
              <div className="ai-allocation">
                {response.allocation.map((item) => (
                  <div key={item.name} className="ai-alloc-row">
                    <div className="ai-alloc-info">
                      <span className="ai-alloc-name">{item.name}</span>
                      <span className="ai-alloc-pct">{item.pct}%</span>
                    </div>
                    <div className="ai-alloc-bar-track">
                      <div
                        className="ai-alloc-bar-fill"
                        style={{ width: `${item.pct}%`, background: item.color }}
                      />
                    </div>
                  </div>
                ))}
              </div>

              <p className="ai-result__summary">{response.summary}</p>

              <p className="result-disclaimer" style={{ marginTop: '16px' }}>
                * Methodology sourced from Goldman Sachs Investment Research. Recommendations
                are illustrative only and do not constitute financial advice.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Footer */}
      <footer className="app-footer">
        <div className="app-footer__gs">
          <span className="app-footer__monogram">GS</span>
          <span className="app-footer__brand">Goldman Sachs Asset Management</span>
        </div>
        <span className="app-footer__copy">
          © {new Date().getFullYear()} Goldman Sachs Group, Inc. · For illustrative purposes only · Not financial advice
        </span>
      </footer>
    </section>
  )
}
