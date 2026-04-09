import { authFetch, buildAuthHeaders } from './authFetch'
import { API_BASE } from './apiBase'

/**
 * @returns {Promise<{ totalCostBasis: number, totalCurrentValue: number, totalGainLoss: number, totalReturnPct: number }>}
 */
export async function getDashboard() {
  const response = await authFetch(`${API_BASE}/user/dashboard`, {
    method: 'GET',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to load dashboard: ${response.status}`)
  return response.json()
}

/**
 * @param {number} portfolioId
 * @returns {Promise<{ holdings: Array<{ fundId: string, fundName: string, ticker: string, shares: number, costBasis: number, currentValue: number, gainLoss: number, returnPct: number }>, totalCostBasis: number, totalCurrentValue: number, totalGainLoss: number, totalReturnPct: number }>}
 */
export async function getPortfolioPerformance(portfolioId) {
  const response = await authFetch(`${API_BASE}/user/portfolios/${portfolioId}/performance`, {
    method: 'GET',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to load performance: ${response.status}`)
  return response.json()
}
