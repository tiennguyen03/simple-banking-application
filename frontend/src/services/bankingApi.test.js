import test from 'node:test'
import assert from 'node:assert/strict'
import * as api from './bankingApi.js'

test('banking requests match Spring endpoints and JSON bodies', async () => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return new Response(JSON.stringify({ accountId: 'a1', balance: 25 }), { status: 200 })
  }
  try {
    await api.createCustomer({ name: 'Test' })
    await api.updateAccount('a1', { accountType: 'SAVINGS' })
    const result = await api.deposit('a1', 25)
    await api.withdraw('a1', 10)
    await api.transfer({ fromAccountId: 'a1', toAccountId: 'a2', amount: 5 })
    await api.getTransactions('a1')
    await api.getPremiumAccounts(1000)
    await api.getAudits()
    assert.equal(calls[0].url, '/api/customers')
    assert.equal(calls[0].options.method, 'POST')
    assert.equal(calls[1].options.method, 'PUT')
    assert.deepEqual(JSON.parse(calls[2].options.body), { amount: 25 })
    assert.equal(result.balance, 25)
    assert.equal(calls[3].url, '/api/accounts/a1/withdraw')
    assert.deepEqual(JSON.parse(calls[4].options.body), { fromAccountId: 'a1', toAccountId: 'a2', amount: 5 })
    assert.equal(calls[5].url, '/api/accounts/a1/transactions')
    assert.equal(calls[6].url, '/api/accounts/premium?threshold=1000')
    assert.equal(calls[7].url, '/api/audits')
  } finally { globalThis.fetch = originalFetch }
})

test('empty delete responses and rejected operations are handled', async () => {
  const originalFetch = globalThis.fetch
  try {
    globalThis.fetch = async () => new Response(null, { status: 204 })
    assert.equal(await api.deleteAccount('a1'), null)
    globalThis.fetch = async () => new Response(null, { status: 409 })
    await assert.rejects(api.deleteCustomer('c1'), /still has accounts/)
    globalThis.fetch = async () => new Response(null, { status: 400 })
    await assert.rejects(api.withdraw('a1', 500), /rejected/)
    globalThis.fetch = async () => { throw new TypeError('Failed to fetch') }
    await assert.rejects(api.getCustomers(), /Cannot reach/)
  } finally { globalThis.fetch = originalFetch }
})
