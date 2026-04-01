const JWT_KEY = 'jwt'
const USER_KEY = 'user'
const AUTH_EXPIRED_EVENT = 'auth:expired'

export function buildAuthHeaders(extraHeaders = {}) {
  const token = localStorage.getItem(JWT_KEY)
  const headers = { ...extraHeaders }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return headers
}

export function clearStoredAuth() {
  localStorage.removeItem(JWT_KEY)
  localStorage.removeItem(USER_KEY)
  window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
}

export async function authFetch(url, options = {}) {
  const response = await fetch(url, options)
  if (response.status === 401 || response.status === 403) {
    clearStoredAuth()
  }
  return response
}

export { AUTH_EXPIRED_EVENT }
