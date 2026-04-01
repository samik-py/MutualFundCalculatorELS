import { useEffect, useMemo, useState } from 'react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  ComposedChart,
  Legend,
  Line,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import Navbar from '../components/Navbar'
import { getCryptoSuite, uploadCryptoWallet } from '../services/cryptoApi'
import './CryptoSuitePage.css'

const PIE_COLORS = ['#54779F', '#7399C6', '#8CAED5', '#A8BFDC', '#C3D4E8']

function AnimatedInsightText({ as: Tag = 'p', text, className = '', baseDelay = 0, step = 0.018 }) {
  const words = String(text ?? '').split(' ').filter(Boolean)

  return (
    <Tag className={className}>
      {words.map((word, index) => (
        <span
          key={`${word}-${index}`}
          className="crypto-suite__insight-word"
          style={{ animationDelay: `${baseDelay + index * step}s` }}
        >
          {word}&nbsp;
        </span>
      ))}
    </Tag>
  )
}

function StressTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null

  const point = payload[0]?.payload
  if (!point) return null

  const rows = [
    ['95th pct.', point.p95],
    ['75th pct.', point.p75],
    ['Median', point.median],
    ['25th pct.', point.p25],
    ['5th pct.', point.p5],
  ]

  return (
    <div className="crypto-suite__tooltip">
      <div className="crypto-suite__tooltip-title">Year {label}</div>
      {rows.map(([name, value]) => (
        <div key={name} className="crypto-suite__tooltip-row">
          <span>{name}</span>
          <strong>{formatCurrency(value)}</strong>
        </div>
      ))}
    </div>
  )
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(value)
}

function formatPct(value) {
  return `${value >= 0 ? '+' : ''}${Number(value).toFixed(2)}%`
}

export default function CryptoSuitePage() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [uploading, setUploading] = useState(false)
  const [uploadMessage, setUploadMessage] = useState(null)

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const response = await getCryptoSuite()
        if (active) setData(response)
      } catch (e) {
        if (active) setError(e.message)
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => {
      active = false
    }
  }, [])

  const reloadSuite = async () => {
    const response = await getCryptoSuite()
    setData(response)
  }

  const handleWalletUpload = async (event) => {
    const file = event.target.files?.[0]
    if (!file) return
    setUploading(true)
    setUploadMessage(null)
    setError(null)
    try {
      const result = await uploadCryptoWallet(file)
      if (result.suite) {
        setData(result.suite)
      } else {
        await reloadSuite()
      }
      setUploadMessage(`Imported ${result.holdingsImported} holdings from ${result.fileName}.`)
    } catch (e) {
      setError(e.message)
    } finally {
      setUploading(false)
      event.target.value = ''
    }
  }

  const totals = useMemo(() => {
    if (!data) return null
    const currentValue = data.holdings.reduce((sum, item) => sum + item.currentValue, 0)
    const costBasis = data.holdings.reduce((sum, item) => sum + item.costBasis, 0)
    const pnl = currentValue - costBasis
    return { currentValue, costBasis, pnl }
  }, [data])

  const hasPortfolioData = Boolean(data?.holdings?.length)
  const insightAnimationKey = data?.aiInsights
    ? [data.aiInsights.headline, data.aiInsights.summary, ...data.aiInsights.bullets, data.aiInsights.outlook].join('|')
    : 'empty'

  return (
    <>
      <Navbar />
      <main id="main-content" className="crypto-suite">
        <section className="crypto-suite__hero" data-tour="crypto-suite">
          <div className="crypto-suite__hero-copy">
            <span className="crypto-suite__eyebrow">Coinbase Integrated Crypto Desk</span>
            <h1 className="crypto-suite__title">Cryptocurrency Portfolio Suite</h1>
            <p className="crypto-suite__subtitle">
              Audit tax lots, analyze trading decisions, quantify Coinbase fee drag, and stress test your
              crypto book with institutional-style scenario analysis.
            </p>
          </div>

          <div className="crypto-suite__status-card">
            <span className={`crypto-suite__status-pill ${data?.demoMode ? 'crypto-suite__status-pill--demo' : ''}`}>
              {data?.connectionLabel || 'Loading connection status'}
            </span>
            <p className="crypto-suite__status-hint">
              {data?.connectionHint || 'Connecting to Coinbase account intelligence...'}
            </p>
            {data?.sourceLabel && (
              <p className="crypto-suite__source">
                Source: <strong>{data.sourceLabel}</strong>
              </p>
            )}
            {data?.connectUrl && (
              <a className="crypto-suite__connect" href={data.connectUrl} target="_blank" rel="noreferrer">
                Connect with Coinbase
              </a>
            )}
            <label className="crypto-suite__upload">
              <input
                type="file"
                accept=".csv,.json"
                onChange={handleWalletUpload}
                disabled={uploading}
                aria-label="Upload wallet file (CSV or JSON)"
              />
              <span aria-hidden="true">{uploading ? 'Uploading wallet…' : 'Upload Wallet File'}</span>
            </label>
            <p className="crypto-suite__upload-hint">
              Supports CSV or JSON wallet exports with holdings and optional transactions.
            </p>
            {uploadMessage && <p className="crypto-suite__upload-success">{uploadMessage}</p>}
          </div>
        </section>

        {loading && (
          <section className="crypto-suite__loading-card" role="status" aria-live="polite">
            <p>Loading crypto suite…</p>
          </section>
        )}

        {error && (
          <section className="crypto-suite__error-card" role="alert">
            <p>{error}</p>
          </section>
        )}

        {data && totals && (
          <>
            {!hasPortfolioData && (
              <section className="crypto-suite__empty-card">
                <span className="crypto-suite__panel-kicker">Upload Required</span>
                <h2 className="crypto-suite__panel-title">Upload your crypto wallet to unlock the suite</h2>
                <p className="crypto-suite__panel-copy">
                  The crypto desk now waits for your own wallet export before showing analytics. Upload a JSON or CSV
                  wallet file above to populate holdings, tax-lot analysis, fee audits, and Monte Carlo stress tests.
                </p>
              </section>
            )}

            {hasPortfolioData && data.aiInsights && (
              <section className="crypto-suite__insights">
                <article key={insightAnimationKey} className="crypto-suite__insights-card">
                  <div className="crypto-suite__panel-head">
                    <div>
                      <span className="crypto-suite__panel-kicker">AI Insights</span>
                      <h2 className="crypto-suite__panel-title crypto-suite__insight-heading">
                        {data.aiInsights.headline}
                      </h2>
                    </div>
                    <div className="crypto-suite__callout">Gemini Desk Read</div>
                  </div>
                  <AnimatedInsightText
                    text={data.aiInsights.summary}
                    className="crypto-suite__panel-copy crypto-suite__insights-summary"
                    baseDelay={0.08}
                  />
                  <div className="crypto-suite__insights-grid">
                    <ul className="crypto-suite__highlights">
                      {data.aiInsights.bullets.map((item, index) => (
                        <li
                          key={item}
                          className="crypto-suite__insight-bullet"
                          style={{ animationDelay: `${0.22 + index * 0.14}s` }}
                        >
                          {item}
                        </li>
                      ))}
                    </ul>
                    <div className="crypto-suite__insights-outlook">
                      <span className="crypto-suite__metric-label">Forward Outlook</span>
                      <AnimatedInsightText
                        text={data.aiInsights.outlook}
                        baseDelay={0.5}
                        className="crypto-suite__insight-outlook-copy"
                      />
                    </div>
                  </div>
                </article>
              </section>
            )}

            {hasPortfolioData && <section className="crypto-suite__metrics">
              <article className="crypto-suite__metric-card">
                <span className="crypto-suite__metric-label">Portfolio Value</span>
                <strong className="crypto-suite__metric-value">{formatCurrency(totals.currentValue)}</strong>
              </article>
              <article className="crypto-suite__metric-card">
                <span className="crypto-suite__metric-label">Net Unrealized P&amp;L</span>
                <strong className={`crypto-suite__metric-value ${totals.pnl >= 0 ? 'is-positive' : 'is-negative'}`}>
                  {formatCurrency(totals.pnl)}
                </strong>
              </article>
              <article className="crypto-suite__metric-card">
                <span className="crypto-suite__metric-label">Tax Savings Opportunity</span>
                <strong className="crypto-suite__metric-value">{formatCurrency(data.taxOptimizer.estimatedTaxSavings)}</strong>
              </article>
              <article className="crypto-suite__metric-card">
                <span className="crypto-suite__metric-label">Potential Fee Savings</span>
                <strong className="crypto-suite__metric-value">{formatCurrency(data.feeAudit.potentialSavings)}</strong>
              </article>
            </section>}

            {hasPortfolioData && <section className="crypto-suite__grid crypto-suite__grid--top">
              <article className="crypto-suite__panel">
                <div className="crypto-suite__panel-head">
                  <div>
                    <span className="crypto-suite__panel-kicker">Holdings</span>
                    <h2 className="crypto-suite__panel-title">Coinbase Portfolio Snapshot</h2>
                  </div>
                </div>
                <div className="crypto-suite__holdings-table">
                  {data.holdings.map((holding) => (
                    <div key={holding.asset} className="crypto-suite__holding-row">
                      <div>
                        <div className="crypto-suite__holding-asset">{holding.asset}</div>
                        <div className="crypto-suite__holding-name">{holding.name}</div>
                      </div>
                      <div className="crypto-suite__holding-meta">
                        <span>{holding.quantity.toLocaleString()}</span>
                        <span>{formatCurrency(holding.currentValue)}</span>
                        <span className={holding.unrealizedPnL >= 0 ? 'is-positive' : 'is-negative'}>
                          {formatPct(holding.unrealizedPnLPct)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </article>

              <article className="crypto-suite__panel">
                <div className="crypto-suite__panel-head">
                  <div>
                    <span className="crypto-suite__panel-kicker">Allocation</span>
                    <h2 className="crypto-suite__panel-title">Asset Mix by Current Value</h2>
                  </div>
                </div>
                <div className="crypto-suite__chart-wrap">
                  <ResponsiveContainer width="100%" height={320}>
                    <PieChart>
                      <Pie data={data.holdings} dataKey="currentValue" nameKey="asset" outerRadius={105} innerRadius={54}>
                        {data.holdings.map((_, index) => (
                          <Cell key={index} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value) => [formatCurrency(value), 'Value']} />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </article>
            </section>}

            {hasPortfolioData && <section className="crypto-suite__grid">
              <article className="crypto-suite__panel">
                <div className="crypto-suite__panel-head">
                  <div>
                    <span className="crypto-suite__panel-kicker">Advanced Trade Market Pulse</span>
                    <h2 className="crypto-suite__panel-title">{data.marketPulse.primaryPair} microstructure</h2>
                  </div>
                  <div className="crypto-suite__callout">{data.marketPulse.spreadBps.toFixed(1)} bps spread</div>
                </div>
                <div className="crypto-suite__audit-grid">
                  <div>
                    <span>Mid price</span>
                    <strong>{formatCurrency(data.marketPulse.midPrice)}</strong>
                  </div>
                  <div>
                    <span>Bid depth</span>
                    <strong>{formatCurrency(data.marketPulse.bidDepthUsd)}</strong>
                  </div>
                  <div>
                    <span>Ask depth</span>
                    <strong>{formatCurrency(data.marketPulse.askDepthUsd)}</strong>
                  </div>
                  <div>
                    <span>Book imbalance</span>
                    <strong className={data.marketPulse.imbalancePct >= 0 ? 'is-positive' : 'is-negative'}>
                      {formatPct(data.marketPulse.imbalancePct)}
                    </strong>
                  </div>
                </div>
                <ul className="crypto-suite__highlights">
                  {data.marketPulse.highlights.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </article>

              <article className="crypto-suite__panel">
                <div className="crypto-suite__panel-head">
                  <div>
                    <span className="crypto-suite__panel-kicker">Base / On-Chain Summary</span>
                    <h2 className="crypto-suite__panel-title">
                      {data.onchainSummary.baseActivityDetected ? 'Base activity detected' : 'Awaiting on-chain activity'}
                    </h2>
                  </div>
                  <div className="crypto-suite__callout">{data.onchainSummary.walletCount} wallets</div>
                </div>
                <div className="crypto-suite__audit-grid">
                  <div>
                    <span>30D transactions</span>
                    <strong>{data.onchainSummary.transactionCount30d}</strong>
                  </div>
                  <div>
                    <span>30D gas spend</span>
                    <strong>{formatCurrency(data.onchainSummary.gasSpentUsd30d)}</strong>
                  </div>
                  <div>
                    <span>Bridge volume</span>
                    <strong>{formatCurrency(data.onchainSummary.bridgeVolumeUsd30d)}</strong>
                  </div>
                  <div>
                    <span>Base linked</span>
                    <strong>{data.onchainSummary.baseActivityDetected ? 'Yes' : 'No'}</strong>
                  </div>
                </div>
                <ul className="crypto-suite__highlights">
                  {data.onchainSummary.highlights.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </article>
            </section>}

            {hasPortfolioData && <section className="crypto-suite__grid">
              <article className="crypto-suite__panel">
                <div className="crypto-suite__panel-head">
                  <div>
                    <span className="crypto-suite__panel-kicker">1. Tax Efficiency Optimizer</span>
                    <h2 className="crypto-suite__panel-title">{data.taxOptimizer.recommendedMethod} beats FIFO right now</h2>
                  </div>
                  <div className="crypto-suite__callout">{formatCurrency(data.taxOptimizer.estimatedTaxSavings)} tax alpha</div>
                </div>
                <p className="crypto-suite__panel-copy">{data.taxOptimizer.summary}</p>
                <div className="crypto-suite__mini-metrics">
                  <div>
                    <span>FIFO gain</span>
                    <strong>{formatCurrency(data.taxOptimizer.fifoTaxableGain)}</strong>
                  </div>
                  <div>
                    <span>HIFO gain</span>
                    <strong>{formatCurrency(data.taxOptimizer.hifoTaxableGain)}</strong>
                  </div>
                </div>
                <div className="crypto-suite__lots">
                  {data.taxOptimizer.recommendations.map((item) => (
                    <div key={`${item.asset}-${item.acquiredAt}`} className="crypto-suite__lot-card">
                      <div className="crypto-suite__lot-top">
                        <strong>{item.asset}</strong>
                        <span>{item.acquiredAt}</span>
                      </div>
                      <div className="crypto-suite__lot-grid">
                        <span>{item.quantity.toLocaleString()} units</span>
                        <span>Cost {formatCurrency(item.unitCost)}</span>
                        <span>Now {formatCurrency(item.currentPrice)}</span>
                        <span className={item.estimatedGain >= 0 ? 'is-positive' : 'is-negative'}>
                          Tax impact {formatCurrency(item.estimatedTaxImpact)}
                        </span>
                      </div>
                      <p>{item.rationale}</p>
                    </div>
                  ))}
                </div>
              </article>

              <article className="crypto-suite__panel">
                <div className="crypto-suite__panel-head">
                  <div>
                    <span className="crypto-suite__panel-kicker">2. Realized vs Potential</span>
                    <h2 className="crypto-suite__panel-title">{data.performanceAudit.verdict}</h2>
                  </div>
                  <div className="crypto-suite__callout">{formatCurrency(data.performanceAudit.auditScore)} score</div>
                </div>
                <div className="crypto-suite__audit-grid">
                  <div>
                    <span>Realized P&amp;L</span>
                    <strong>{formatCurrency(data.performanceAudit.realizedPnL)}</strong>
                  </div>
                  <div>
                    <span>Hold vs sell delta</span>
                    <strong>{formatCurrency(data.performanceAudit.holdVsSellDelta)}</strong>
                  </div>
                  <div>
                    <span>Missed peak upside</span>
                    <strong>{formatCurrency(data.performanceAudit.missedPeakUpside)}</strong>
                  </div>
                </div>
                <ul className="crypto-suite__highlights">
                  {data.performanceAudit.highlights.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </article>
            </section>}

            {hasPortfolioData && <section className="crypto-suite__grid">
              <article className="crypto-suite__panel">
                <div className="crypto-suite__panel-head">
                  <div>
                    <span className="crypto-suite__panel-kicker">3. Fee &amp; Spread Audit</span>
                    <h2 className="crypto-suite__panel-title">Coinbase execution drag, quantified</h2>
                  </div>
                  <div className="crypto-suite__callout">{formatCurrency(data.feeAudit.potentialSavings)} recoverable</div>
                </div>
                <div className="crypto-suite__audit-grid">
                  <div>
                    <span>Coinbase fees</span>
                    <strong>{formatCurrency(data.feeAudit.totalCoinbaseFees)}</strong>
                  </div>
                  <div>
                    <span>Estimated spread</span>
                    <strong>{formatCurrency(data.feeAudit.estimatedSpreadCost)}</strong>
                  </div>
                  <div>
                    <span>Advanced Trade equivalent</span>
                    <strong>{formatCurrency(data.feeAudit.advancedTradeEquivalentCost)}</strong>
                  </div>
                  <div>
                    <span>Effective cost rate</span>
                    <strong>{data.feeAudit.effectiveCostRatePct.toFixed(2)}%</strong>
                  </div>
                </div>
                <ul className="crypto-suite__highlights">
                  {data.feeAudit.highlights.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </article>

              <article className="crypto-suite__panel">
                <div className="crypto-suite__panel-head">
                  <div>
                    <span className="crypto-suite__panel-kicker">4. Portfolio Stress Tester</span>
                    <h2 className="crypto-suite__panel-title">Monte Carlo fan chart</h2>
                  </div>
                </div>
                <div className="crypto-suite__chart-wrap">
                  <ResponsiveContainer width="100%" height={320}>
                    <ComposedChart data={data.stressTest.fanChart}>
                      <CartesianGrid stroke="rgba(255,255,255,0.06)" vertical={false} />
                      <XAxis dataKey="year" stroke="rgba(237,242,248,0.55)" />
                      <YAxis stroke="rgba(237,242,248,0.55)" tickFormatter={(value) => `$${Math.round(value / 1000)}k`} />
                      <Tooltip content={<StressTooltip />} />
                      <Legend />
                      <Area type="monotone" dataKey="p95" stroke="transparent" fill="rgba(115, 153, 198, 0.12)" name="95th pct." />
                      <Area type="monotone" dataKey="p75" stroke="transparent" fill="rgba(115, 153, 198, 0.18)" name="75th pct." />
                      <Area type="monotone" dataKey="p25" stroke="transparent" fill="rgba(115, 153, 198, 0.22)" name="25th pct." />
                      <Area type="monotone" dataKey="p5" stroke="transparent" fill="rgba(6, 20, 38, 0.95)" name="5th pct." />
                      <Line type="monotone" dataKey="median" stroke="#A8BFDC" strokeWidth={2.5} dot={false} name="Median" />
                    </ComposedChart>
                  </ResponsiveContainer>
                </div>
              </article>
            </section>}

            {hasPortfolioData && <section className="crypto-suite__scenario-strip">
              {data.stressTest.scenarios.map((scenario) => (
                <article key={scenario.name} className="crypto-suite__scenario-card">
                  <span className="crypto-suite__panel-kicker">{scenario.name}</span>
                  <h3>{formatCurrency(scenario.fiveYearMedian)}</h3>
                  <p>5Y median outcome</p>
                  <div className="crypto-suite__scenario-grid">
                    <span>1Y {formatCurrency(scenario.oneYearMedian)}</span>
                    <span>3Y {formatCurrency(scenario.threeYearMedian)}</span>
                    <span>95% floor {formatCurrency(scenario.downside95)}</span>
                    <span>95% upside {formatCurrency(scenario.upside95)}</span>
                  </div>
                </article>
              ))}
            </section>}
          </>
        )}
      </main>
    </>
  )
}
