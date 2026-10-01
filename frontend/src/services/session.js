let session = null
let expiryTimer
const listeners = new Set()
export const getSession = () => session
export const subscribe = listener => { listeners.add(listener); return () => listeners.delete(listener) }
export function clearSession() {
  clearTimeout(expiryTimer)
  session = null
  listeners.forEach(listener => listener())
}
export function setSession(response) {
  if (!response.token || !['ADMIN', 'CUSTOMER'].includes(response.role) || !(response.expiresIn > 0) ||
      (response.role === 'CUSTOMER' && !response.customerId)) throw new Error('The server returned an incomplete login response.')
  clearTimeout(expiryTimer)
  // Bearer tokens stay in memory, out of persistent browser storage.
  session = { ...response, expiresAt: Date.now() + response.expiresIn * 1000 }
  expiryTimer = setTimeout(clearSession, response.expiresIn * 1000)
  expiryTimer.unref?.()
  listeners.forEach(listener => listener())
}
