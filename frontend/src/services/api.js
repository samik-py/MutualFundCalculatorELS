const BASE_URL = '/api'

/**
 * Calculate future value of an investment.
 * Falls back to client-side calculation if backend is unavailable.
 *
 * @param {string} fundId - Fund identifier
 * @param {number} amount - Principal investment amount
 * @param {number} years  - Investment horizon in years
 * @returns {Promise<{ futureValue: number, gain: number, annualReturn: number }>}
 */
export async function calculateFutureValue(fundId, amount, years) {
  try {
    const response = await fetch(`${BASE_URL}/calculate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fundId, amount, years }),
    })

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`)
    }

    return await response.json()
  } catch {
    // Fallback: client-side calculation using hardcoded returns
    const FUND_RETURNS = {
      'vanguard-500': 0.107,
      'fidelity-growth': 0.138,
      'trowe-bluechip': 0.121,
      'schwab-total': 0.104,
      'pimco-total': 0.042,
    }
    const r = FUND_RETURNS[fundId] ?? 0.10
    const futureValue = amount * Math.pow(1 + r, years)
    return {
      futureValue,
      gain: futureValue - amount,
      annualReturn: r,
    }
  }
}

/**
 * Get AI portfolio recommendation.
 *
 * @param {string} prompt - User's risk/goal description
 * @returns {Promise<{ allocation: Array, summary: string, expectedReturn: string }>}
 */
export async function getAIPortfolio(prompt) {
  const response = await fetch(`${BASE_URL}/ai/portfolio`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt }),
  })

  if (!response.ok) {
    throw new Error(`Server error: ${response.status}`)
  }

  return await response.json()
}
