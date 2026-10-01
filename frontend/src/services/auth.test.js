import test from 'node:test'
import assert from 'node:assert/strict'
import * as api from './bankingApi.js'
import { setSession, clearSession, getSession, subscribe } from './session.js'

const customer = { token: 'test-token', role: 'CUSTOMER', username: 'test-user', customerId: 'c1', expiresIn: 900 }

test('protected requests send bearer tokens; public login sends credentials only', async () => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return new Response(JSON.stringify(customer), { status: 200 })
  }
  try {
    setSession(customer)
    await api.getAccounts()
    await api.login({ username: 'test-user', password: 'test-password' })
    assert.equal(calls[0].options.headers.Authorization, 'Bearer test-token')
    assert.equal(calls[1].url, '/api/auth/login')
    assert.equal(calls[1].options.headers.Authorization, undefined)
    assert.deepEqual(JSON.parse(calls[1].options.body), { username: 'test-user', password: 'test-password' })
    await api.register({ username: 'test-user', password: 'test-password', name: 'Test User' })
    assert.equal(calls[2].url, '/api/auth/register')
    assert.equal(calls[2].options.headers.Authorization, undefined)
  } finally { clearSession(); globalThis.fetch = originalFetch }
})

test('401 signs out, 403 keeps the session, and failed login does not clear a session', async () => {
  const originalFetch = globalThis.fetch
  try {
    setSession(customer)
    globalThis.fetch = async () => new Response(null, { status: 403 })
    await assert.rejects(api.getAudits(), /permission/)
    assert.equal(getSession().token, customer.token)
    globalThis.fetch = async () => new Response(null, { status: 401 })
    await assert.rejects(api.login({ username: 'test', password: 'wrong' }), /Invalid username/)
    assert.equal(getSession().token, customer.token)
    await assert.rejects(api.getAccounts(), /expired/)
    assert.equal(getSession(), null)
  } finally { clearSession(); globalThis.fetch = originalFetch }
})

test('stale request failures cannot sign out a newer session', async () => {
  const originalFetch = globalThis.fetch
  let respond
  globalThis.fetch = () => new Promise(resolve => { respond = resolve })
  try {
    setSession(customer)
    const pending = api.getAccounts()
    setSession({ ...customer, token: 'new-token' })
    respond(new Response(null, { status: 401 }))
    await assert.rejects(pending, /expired/)
    assert.equal(getSession().token, 'new-token')
  } finally { clearSession(); globalThis.fetch = originalFetch }
})

test('incomplete sessions are rejected, and expiry notifies the UI', async () => {
  let notifications = 0
  const unsubscribe = subscribe(() => { notifications++ })
  try {
    assert.throws(() => setSession({ ...customer, customerId: null }), /incomplete/)
    setSession({ ...customer, expiresIn: 0.01 })
    await new Promise(resolve => setTimeout(resolve, 50))
    assert.equal(getSession(), null)
    assert.equal(notifications, 2)
  } finally { unsubscribe(); clearSession() }
})
