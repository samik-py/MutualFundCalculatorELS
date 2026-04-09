import { authFetch, buildAuthHeaders } from './authFetch'
import { API_BASE } from './apiBase'

const BASE = `${API_BASE}/user/charts`

/**
 * @returns {Promise<Array<{ id: number, title: string, fundIds: string, timeHorizon: number, amount: number, createdAt: string }>>}
 */
export async function getSavedCharts() {
  const response = await authFetch(BASE, {
    method: 'GET',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to load saved charts: ${response.status}`)
  return response.json()
}

/**
 * @param {{ title: string, fundIds: string, timeHorizon: number, amount: number }} config
 * @returns {Promise<{ id: number, title: string, fundIds: string, timeHorizon: number, amount: number, createdAt: string }>}
 */
export async function saveChart(config) {
  const response = await authFetch(BASE, {
    method: 'POST',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(config),
  })
  if (!response.ok) throw new Error(`Failed to save chart: ${response.status}`)
  return response.json()
}

/**
 * @param {number} id
 * @returns {Promise<{ id: number, title: string, fundIds: string, timeHorizon: number, amount: number, createdAt: string }>}
 */
export async function getSavedChart(id) {
  const response = await authFetch(`${BASE}/${id}`, {
    method: 'GET',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to load chart: ${response.status}`)
  return response.json()
}

/**
 * @param {number} id
 * @returns {Promise<void>}
 */
export async function deleteChart(id) {
  const response = await authFetch(`${BASE}/${id}`, {
    method: 'DELETE',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
  })
  if (!response.ok) throw new Error(`Failed to delete chart: ${response.status}`)
}
