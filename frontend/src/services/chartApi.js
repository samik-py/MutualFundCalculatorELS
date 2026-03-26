const BASE = '/api/user/charts'

const getAuthHeaders = () => {
  const token = localStorage.getItem('jwt')
  const headers = { 'Content-Type': 'application/json' }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

/**
 * @returns {Promise<Array<{ id: number, title: string, fundIds: string, timeHorizon: number, amount: number, createdAt: string }>>}
 */
export async function getSavedCharts() {
  const response = await fetch(BASE, {
    method: 'GET',
    headers: getAuthHeaders(),
  })
  if (!response.ok) throw new Error(`Failed to load saved charts: ${response.status}`)
  return response.json()
}

/**
 * @param {{ title: string, fundIds: string, timeHorizon: number, amount: number }} config
 * @returns {Promise<{ id: number, title: string, fundIds: string, timeHorizon: number, amount: number, createdAt: string }>}
 */
export async function saveChart(config) {
  const response = await fetch(BASE, {
    method: 'POST',
    headers: getAuthHeaders(),
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
  const response = await fetch(`${BASE}/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  })
  if (!response.ok) throw new Error(`Failed to load chart: ${response.status}`)
  return response.json()
}

/**
 * @param {number} id
 * @returns {Promise<void>}
 */
export async function deleteChart(id) {
  const response = await fetch(`${BASE}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  })
  if (!response.ok) throw new Error(`Failed to delete chart: ${response.status}`)
}
