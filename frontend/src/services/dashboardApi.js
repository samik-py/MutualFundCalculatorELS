const getAuthHeaders = () => {
  const token = localStorage.getItem('jwt')
  const headers = { 'Content-Type': 'application/json' }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

/**
 * @returns {Promise<{ totalCostBasis: number, totalCurrentValue: number, totalGainLoss: number, totalReturnPct: number }>}
 */
export async function getDashboard() {
  const response = await fetch('/api/user/dashboard', {
    method: 'GET',
    headers: getAuthHeaders(),
  })
  if (!response.ok) throw new Error(`Failed to load dashboard: ${response.status}`)
  return response.json()
}

/**
 * @param {number} portfolioId
 * @returns {Promise<{ holdings: Array<{ fundId: string, fundName: string, ticker: string, shares: number, costBasis: number, currentValue: number, gainLoss: number, returnPct: number }>, totalCostBasis: number, totalCurrentValue: number, totalGainLoss: number, totalReturnPct: number }>}
 */
export async function getPortfolioPerformance(portfolioId) {
  const response = await fetch(`/api/user/portfolios/${portfolioId}/performance`, {
    method: 'GET',
    headers: getAuthHeaders(),
  })
  if (!response.ok) throw new Error(`Failed to load performance: ${response.status}`)
  return response.json()
}
