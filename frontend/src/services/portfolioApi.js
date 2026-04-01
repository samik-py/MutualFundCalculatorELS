import { authFetch, buildAuthHeaders } from './authFetch'

const BASE = '/api/user/portfolios'

/**
 * @returns {Promise<Array<{ id: number, name: string, holdingCount: number, createdAt: string }>>}
 */
export async function getPortfolios() {
  const response = await authFetch(BASE, {
    method: 'GET',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to load portfolios: ${response.status}`)
  return response.json()
}

/**
 * @param {string} name
 * @returns {Promise<{ id: number, name: string, holdings: Array, createdAt: string }>}
 */
export async function createPortfolio(name) {
  const response = await authFetch(BASE, {
    method: 'POST',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ name }),
  })
  if (!response.ok) throw new Error(`Failed to create portfolio: ${response.status}`)
  return response.json()
}

/**
 * @param {number} id
 * @returns {Promise<{ id: number, name: string, holdings: Array, createdAt: string }>}
 */
export async function getPortfolio(id) {
  const response = await authFetch(`${BASE}/${id}`, {
    method: 'GET',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to load portfolio: ${response.status}`)
  return response.json()
}

/**
 * @param {number} id
 * @param {string} name
 * @returns {Promise<{ id: number, name: string, holdings: Array, createdAt: string }>}
 */
export async function updatePortfolio(id, name) {
  const response = await authFetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ name }),
  })
  if (!response.ok) throw new Error(`Failed to update portfolio: ${response.status}`)
  return response.json()
}

/**
 * @param {number} id
 * @returns {Promise<void>}
 */
export async function deletePortfolio(id) {
  const response = await authFetch(`${BASE}/${id}`, {
    method: 'DELETE',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to delete portfolio: ${response.status}`)
}

/**
 * @param {number} portfolioId
 * @param {{ fundId: string, shares: number, purchasePrice: number, purchaseDate: string }} holding
 * @returns {Promise<{ id: number, name: string, holdings: Array, createdAt: string }>}
 */
export async function addHolding(portfolioId, holding) {
  const response = await authFetch(`${BASE}/${portfolioId}/holdings`, {
    method: 'POST',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(holding),
  })
  if (!response.ok) throw new Error(`Failed to add holding: ${response.status}`)
  return response.json()
}

/**
 * @param {number} portfolioId
 * @param {number} holdingId
 * @returns {Promise<void>}
 */
export async function removeHolding(portfolioId, holdingId) {
  const response = await authFetch(`${BASE}/${portfolioId}/holdings/${holdingId}`, {
    method: 'DELETE',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to remove holding: ${response.status}`)
}
