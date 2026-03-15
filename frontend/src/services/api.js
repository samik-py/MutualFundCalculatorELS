const BASE_URL = '/api'

// ── Existing ──────────────────────────────────────────────────────────────────

export async function fetchFunds() {
  const res = await fetch(`${BASE_URL}/funds`)
  if (!res.ok) throw new Error(`Failed to fetch funds: ${res.status}`)
  return res.json()
}

/**
 * Calculate future value of an investment using CAPM on the backend.
 * Falls back to client-side calculation if backend is unavailable.
 */
export async function calculateFutureValue(fundId, amount, years) {
  try {
    const response = await fetch(`${BASE_URL}/calculate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fundId, amount, years }),
    })
    if (!response.ok) throw new Error(`Server error: ${response.status}`)
    return await response.json()
  } catch {
    // Client-side fallback using representative returns
    const FUND_RETURNS = {
      'vanguard-500': 0.107, 'fxaix': 0.107, 'ivv': 0.107, 'spy': 0.107, 'voog': 0.112,
      'fidelity-growth': 0.138, 'trowe-bluechip': 0.121, 'fcntx': 0.125, 'agthx': 0.115,
      'qqq': 0.148, 'arkk': 0.12, 'xlk': 0.145,
      'schwab-total': 0.104, 'vti': 0.105, 'vtsax': 0.105,
      'vwelx': 0.09, 'prwcx': 0.10,
      'dodfx': 0.08,
      'pimco-total': 0.042, 'agg': 0.038, 'bnd': 0.037,
    }
    const r = FUND_RETURNS[fundId] ?? 0.10
    const futureValue = amount * Math.pow(1 + r, years)
    return { futureValue, gain: futureValue - amount, annualReturn: r }
  }
}

export async function getAIPortfolio(prompt) {
  const response = await fetch(`${BASE_URL}/ai/portfolio`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt }),
  })
  if (!response.ok) throw new Error(`Server error: ${response.status}`)
  return response.json()
}

// ── New ───────────────────────────────────────────────────────────────────────

/**
 * Fetch live market parameters (Rf from FRED, Rm from Yahoo Finance).
 * These are the "ground truth" CAPM inputs.
 */
export async function getMarketIndicators() {
  const res = await fetch(`${BASE_URL}/market/indicators`)
  if (!res.ok) throw new Error(`Failed to fetch indicators: ${res.status}`)
  return res.json()
}

/**
 * Compare projected values for multiple funds side-by-side.
 * @param {string[]} fundIds - list of fund IDs to compare (max 10)
 * @param {number} amount - initial investment
 * @param {number} years - projection horizon
 */
export async function compareFunds(fundIds, amount, years) {
  const res = await fetch(`${BASE_URL}/compare`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fundIds, amount, years }),
  })
  if (!res.ok) throw new Error(`Compare failed: ${res.status}`)
  return res.json()
}

/**
 * Compare two weighted portfolios over time.
 * @param {Array<{fundId, weight}>} portfolioA
 * @param {Array<{fundId, weight}>} portfolioB
 * @param {number} amount
 * @param {number} years
 * @param {string} nameA
 * @param {string} nameB
 */
export async function comparePortfolios(portfolioA, portfolioB, amount, years, nameA, nameB) {
  const res = await fetch(`${BASE_URL}/portfolio/compare`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ portfolioA, portfolioB, amount, years, nameA, nameB }),
  })
  if (!res.ok) throw new Error(`Portfolio compare failed: ${res.status}`)
  return res.json()
}

/**
 * Run Monte Carlo GBM simulation for a fund.
 * Returns percentile confidence bands for predictive visualization.
 * @param {string} fundId
 * @param {number} amount
 * @param {number} years
 * @param {number} simulations - default 500 if omitted
 */
export async function runMonteCarlo(fundId, amount, years, simulations) {
  const res = await fetch(`${BASE_URL}/monte-carlo`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fundId, amount, years, simulations }),
  })
  if (!res.ok) throw new Error(`Monte Carlo failed: ${res.status}`)
  return res.json()
}
