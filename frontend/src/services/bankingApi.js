import { getSession, clearSession } from './session.js'
const baseUrl = (import.meta.env?.VITE_API_BASE_URL || '').replace(/\/$/, '')

async function request(path, options = {}, publicRequest = false) {
  const token = publicRequest ? null : getSession()?.token
  let response
  try {
    response = await fetch(baseUrl + path, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    })
  } catch {
    throw new Error('Cannot reach the banking server. Check that Spring is running.')
  }
  if (!response.ok) {
    const messages = {
      401: publicRequest ? 'Invalid username or password.' : 'Your session has expired. Please sign in again.',
      403: 'You do not have permission to perform this operation.',
      400: 'The request was rejected. Check the fields, available balance, and transfer limit.',
      404: 'The requested customer or account no longer exists. Refresh and try again.',
      409: publicRequest ? 'That username is already taken.' : 'This customer still has accounts. Delete those accounts first.',
    }
    if (response.status === 401 && !publicRequest && token && getSession()?.token === token) clearSession()
    throw new Error(messages[response.status] || 'The banking server could not complete the request.')
  }
  return response.status === 204 ? null : response.json()
}

const id = encodeURIComponent
const write = (method, body) => ({ method, body: JSON.stringify(body) })
export const login = body => request('/api/auth/login', write('POST', body), true)
export const register = body => request('/api/auth/register', write('POST', body), true)
export const getCustomers = () => request('/api/customers')
export const getAccounts = () => request('/api/accounts')
export const getAudits = () => request('/api/audits')
export const getTransactions = accountId => request('/api/accounts/' + id(accountId) + '/transactions')
export const getPremiumAccounts = threshold => request('/api/accounts/premium?threshold=' + id(threshold))
export const createCustomer = body => request('/api/customers', write('POST', body))
export const updateCustomer = (customerId, body) => request('/api/customers/' + id(customerId), write('PUT', body))
export const deleteCustomer = customerId => request('/api/customers/' + id(customerId), { method: 'DELETE' })
export const createAccount = body => request('/api/accounts', write('POST', body))
export const updateAccount = (accountId, body) => request('/api/accounts/' + id(accountId), write('PUT', body))
export const deleteAccount = accountId => request('/api/accounts/' + id(accountId), { method: 'DELETE' })
export const deposit = (accountId, amount) => request('/api/accounts/' + id(accountId) + '/deposit', write('POST', { amount }))
export const withdraw = (accountId, amount) => request('/api/accounts/' + id(accountId) + '/withdraw', write('POST', { amount }))
export const transfer = body => request('/api/accounts/transfer', write('POST', body))
