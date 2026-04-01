import { authFetch, buildAuthHeaders } from './authFetch'

const BASE = '/api/crypto'

export async function getCryptoSuite() {
  const response = await authFetch(`${BASE}/suite`, {
    method: 'GET',
    headers: buildAuthHeaders({ 'Content-Type': 'application/json' }),
    cache: 'no-store',
  })
  if (!response.ok) throw new Error(`Failed to load crypto suite: ${response.status}`)
  return response.json()
}

export async function uploadCryptoWallet(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await authFetch(`${BASE}/upload`, {
    method: 'POST',
    headers: buildAuthHeaders(),
    body: formData,
  })
  if (!response.ok) throw new Error(`Failed to upload wallet: ${response.status}`)
  return response.json()
}
