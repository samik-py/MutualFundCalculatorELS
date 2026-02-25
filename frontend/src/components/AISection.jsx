import { useState } from 'react'
import './AISection.css'

// Mock AI responses based on risk profile keywords
const MOCK_RESPONSES = {
  aggressive: {
    allocation: [
      { name: 'Fidelity Growth Company (FDGRX)', pct: 40, color: '#d1a153' },
      { name: 'T. Rowe Price Blue Chip (TRBCX)', pct: 30, color: '#67e8f9' },
      { name: 'Vanguard 500 Index (VFIAX)', pct: 20, color: '#a78bfa' },
      { name: 'PIMCO Total Return (PTTRX)', pct: 10, color: 'rgba(255,255,255,0.3)' },
    ],
    summary:
      'High-growth portfolio optimized for long-term capital appreciation. Weighted heavily toward equity growth funds with minimal fixed-income exposure. Suitable for a 10+ year horizon with tolerance for 20–35% drawdowns.',
    expectedReturn: '12.4%',
    riskLevel: 'High',
  },
  moderate: {
    allocation: [
      { name: 'Vanguard 500 Index (VFIAX)', pct: 40, color: '#d1a153' },
      { name: 'Schwab Total Market (SWTSX)', pct: 25, color: '#67e8f9' },
      { name: 'T. Rowe Price Blue Chip (TRBCX)', pct: 20, color: '#a78bfa' },
      { name: 'PIMCO Total Return (PTTRX)', pct: 15, color: 'rgba(255,255,255,0.3)' },
    ],
    summary:
      'Balanced portfolio designed for steady growth with moderate volatility. Broad equity exposure balanced with fixed-income stability. Suitable for a 5–10 year horizon with tolerance for 10–20% drawdowns.',
    expectedReturn: '9.8%',
    riskLevel: 'Moderate',
  },
  conservative: {
    allocation: [
      { name: 'PIMCO Total Return (PTTRX)', pct: 45, color: '#67e8f9' },
      { name: 'Vanguard 500 Index (VFIAX)', pct: 30, color: '#d1a153' },
      { name: 'Schwab Total Market (SWTSX)', pct: 15, color: '#a78bfa' },
      { name: 'Fidelity Growth Company (FDGRX)', pct: 10, color: 'rgba(255,255,255,0.3)' },
    ],
    summary:
      'Capital-preservation portfolio prioritizing income stability. Bond-heavy allocation minimizes equity volatility while maintaining modest growth potential. Suitable for shorter horizons or near-retirement investors.',
    expectedReturn: '6.1%',
    riskLevel: 'Low',
  },
}

function classifyInput(text) {
  const lower = text.toLowerCase()
  if (lower.includes('aggressive') || lower.includes('high risk') || lower.includes('growth') || lower.includes('long term') || lower.includes('young')) {
    return 'aggressive'
  }
  if (lower.includes('conservative') || lower.includes('low risk') || lower.includes('safe') || lower.includes('retire') || lower.includes('preserv')) {
    return 'conservative'
  }
  return 'moderate'
}

export default function AISection() {
  const [prompt, setPrompt] = useState('')
  const [response, setResponse] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleGenerate = () => {
    if (!prompt.trim()) return
    setLoading(true)
    setResponse(null)

    // Simulate API latency
    setTimeout(() => {
      const profile = classifyInput(prompt)
      setResponse(MOCK_RESPONSES[profile])
      setLoading(false)
    }, 1400)
  }

  return (
    <section className="ai-section">
      <div className="section-divider" />
      <div className="section">
        <div className="ai-header reveal">
          <span className="ai-eyebrow">Goldman Sachs · AI & Innovation</span>
          <h2 className="ai-title">Portfolio Optimization</h2>
          <p className="ai-subtitle">
            Describe your investment goals and risk tolerance. Our AI engine will
            recommend an optimal fund allocation for your profile.
          </p>
        </div>

        <div className="ai-card reveal reveal-delay-1">
          <div className="ai-input-area">
            <label className="field-label ai-label">Your Investment Profile</label>
            <textarea
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
                className="ai-chip"
                onClick={() => setPrompt(label.toLowerCase())}
              >
                {label}
              </button>
            ))}
          </div>

          <button
            className={`ai-button ${!prompt.trim() || loading ? 'ai-button--disabled' : ''}`}
            onClick={handleGenerate}
            disabled={!prompt.trim() || loading}
          >
            {loading ? (
              <>
                <span className="ai-spinner" />
                <span>Analyzing Profile...</span>
              </>
            ) : (
              <>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="ai-button__icon">
                  <path d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                <span>Generate Portfolio</span>
              </>
            )}
          </button>

          {/* ── AI Response ── */}
          {response && (
            <div className="ai-result">
              <div className="ai-result__header">
                <div className="ai-result__badge">
                  <span className="ai-result__risk" data-level={response.riskLevel.toLowerCase()}>
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
                * Methodology sourced from Goldman Sachs Investment Research. AI recommendations are illustrative only and do not constitute financial advice.
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
