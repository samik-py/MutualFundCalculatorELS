// Centralized API base URL.
//
// In development, defaults to "/api" so the Vite dev server proxy
// (vite.config.js) forwards requests to http://localhost:8080.
//
// In production (Vercel build), set VITE_API_BASE_URL to the absolute
// backend URL, e.g. "https://capmlab-backend.onrender.com/api".
const RAW_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

// Strip a trailing slash so callers can safely do `${API_BASE}/funds`.
export const API_BASE = RAW_BASE.replace(/\/+$/, '')

/**
 * Join the API base with a relative path that should start with "/".
 * Example: apiUrl('/funds') -> '/api/funds' or 'https://.../api/funds'
 */
export function apiUrl(path) {
  if (!path) return API_BASE
  return path.startsWith('/') ? `${API_BASE}${path}` : `${API_BASE}/${path}`
}
