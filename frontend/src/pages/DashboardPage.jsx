import { useEffect, useState } from 'react'
import { AccountBalanceWalletOutlined, GroupsOutlined, ShieldOutlined, SwapHorizRounded } from '@mui/icons-material'
import {
  Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  LinearProgress, MenuItem, Paper, Snackbar, Stack, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, TextField, Typography,
} from '@mui/material'
import MetricCard from '../components/MetricCard'
import Sidebar from '../components/Sidebar'
import * as api from '../services/bankingApi'

const pages = ['Overview', 'Customers', 'Accounts', 'Transactions', 'Audit trail']
const money = value => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value || 0))
const date = value => value ? new Date(value).toLocaleString() : '—'
const shortId = value => value ? '…' + value.slice(-8) : '—'

function DataTable({ headings, rows }) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small">
        <TableHead><TableRow sx={{ bgcolor: '#f8f9fb' }}>
          {headings.map(h => <TableCell key={h} sx={{ fontWeight: 700, py: 2 }}>{h}</TableCell>)}
        </TableRow></TableHead>
        <TableBody>
          {rows.length ? rows : <TableRow><TableCell colSpan={headings.length} sx={{ py: 5, textAlign: 'center', color: 'text.secondary' }}>No records to display.</TableCell></TableRow>}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

export default function DashboardPage({ session, onSignOut }) {
  const [page, setPage] = useState('Overview')
  const [customers, setCustomers] = useState([])
  const [accounts, setAccounts] = useState([])
  const [audits, setAudits] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [search, setSearch] = useState('')
  const [customerFilter, setCustomerFilter] = useState('')
  const [threshold, setThreshold] = useState('1000')
  const [premiumIds, setPremiumIds] = useState(null)
  const [premiumBusy, setPremiumBusy] = useState(false)
  const [selectedAccount, setSelectedAccount] = useState('')
  const [transactions, setTransactions] = useState([])
  const [historyBusy, setHistoryBusy] = useState(false)
  const [historyError, setHistoryError] = useState('')
  const [dialog, setDialog] = useState(null)
  const [form, setForm] = useState({})
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  async function refresh() {
    setLoading(true)
    setError('')
    try {
      const [customerData, accountData, auditData] = await Promise.all([
        api.getCustomers(), api.getAccounts(), api.getAudits(),
      ])
      setCustomers(customerData)
      setAccounts(accountData)
      setAudits(auditData)
      setPremiumIds(null)
    } catch (failure) {
      setError(failure.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    Promise.all([api.getCustomers(), api.getAccounts(), api.getAudits()])
      .then(([c, a, logs]) => {
        if (active) { setCustomers(c); setAccounts(a); setAudits(logs) }
      })
      .catch(failure => { if (active) setError(failure.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    if (!selectedAccount) return () => { active = false }
    api.getTransactions(selectedAccount)
      .then(data => { if (active) setTransactions(data.sort((a, b) => b.date.localeCompare(a.date))) })
      .catch(failure => { if (active) setHistoryError(failure.message) })
      .finally(() => { if (active) setHistoryBusy(false) })
    return () => { active = false }
  }, [selectedAccount])

  function selectAccount(accountId) {
    setTransactions([])
    setHistoryError('')
    setHistoryBusy(Boolean(accountId))
    setSelectedAccount(accountId)
  }

  const customerName = userId => customers.find(c => c.id === userId)?.name || 'Deleted customer'
  const accountLabel = a => customerName(a.userId) + ' · ' + a.accountType + ' · ' + shortId(a.accountId)
  const matches = value => String(value).toLowerCase().includes(search.toLowerCase())
  const visibleCustomers = customers.filter(c => matches(c.name + ' ' + c.id))
  const visibleAccounts = accounts.filter(a =>
    (!customerFilter || a.userId === customerFilter) &&
    (!premiumIds || premiumIds.includes(a.accountId)) &&
    matches(customerName(a.userId) + ' ' + a.accountType + ' ' + a.accountId))
  const visibleAudits = audits.filter(a => matches([a.action, a.status, a.customerId, a.sourceAccountId, a.destinationAccountId, a.reason].join(' ')))
  const totalBalance = accounts.reduce((total, a) => total + Number(a.balance), 0)
  const rejected = audits.filter(a => a.status === 'REJECTED').length

  function open(kind, record = {}) {
    setDialog({ kind, record })
    setForm({
      name: record.name || '', userId: record.userId || customers[0]?.id || '',
      accountType: record.accountType || 'SAVINGS', amount: '',
      fromAccountId: record.accountId || accounts[0]?.accountId || '',
      toAccountId: accounts.find(a => a.accountId !== (record.accountId || accounts[0]?.accountId))?.accountId || '',
    })
    setFormError('')
  }
  const field = name => ({ value: form[name] || '', onChange: event => setForm(current => ({ ...current, [name]: event.target.value })) })

  async function submit(event) {
    event.preventDefault()
    const { kind, record } = dialog
    setSaving(true)
    setFormError('')
    try {
      const amount = Number(form.amount)
      if (['Deposit', 'Withdraw', 'Transfer'].includes(kind)) {
        if (!Number.isFinite(amount) || amount <= 0) throw new Error('Enter a positive amount.')
        if (kind === 'Transfer' && amount > 10000) throw new Error('The maximum transfer amount is $10,000.')
        if (kind === 'Transfer' && form.fromAccountId === form.toAccountId) throw new Error('Choose two different accounts.')
      }
      switch (kind) {
        case 'Create customer': await api.createCustomer({ name: form.name.trim() }); break
        case 'Edit customer': await api.updateCustomer(record.id, { name: form.name.trim() }); break
        case 'Delete customer': await api.deleteCustomer(record.id); break
        case 'Open account': await api.createAccount({ userId: form.userId, accountType: form.accountType }); break
        case 'Edit account': await api.updateAccount(record.accountId, { accountType: form.accountType }); break
        case 'Delete account': await api.deleteAccount(record.accountId); break
        case 'Deposit': await api.deposit(record.accountId, amount); break
        case 'Withdraw': await api.withdraw(record.accountId, amount); break
        case 'Transfer': await api.transfer({ fromAccountId: form.fromAccountId, toAccountId: form.toAccountId, amount }); break
        default: throw new Error('Unknown operation.')
      }
      setDialog(null)
      setNotice(kind + ' completed successfully.')
      selectAccount('')
      await refresh()
    } catch (failure) {
      setFormError(failure.message)
    } finally {
      setSaving(false)
    }
  }

  async function filterPremium() {
    if (!Number.isFinite(Number(threshold)) || Number(threshold) < 0) {
      setError('Enter a non-negative premium threshold.')
      return
    }
    setPremiumBusy(true)
    try {
      const data = await api.getPremiumAccounts(threshold)
      setPremiumIds(data.map(a => a.accountId))
      setError('')
    } catch (failure) { setError(failure.message) }
    finally { setPremiumBusy(false) }
  }

  const accountRows = list => list.map(a => (
    <TableRow key={a.accountId}>
      <TableCell sx={{ py: 2 }}><Typography fontWeight={600}>{customerName(a.userId)}</Typography><Typography variant="caption">{a.accountId}</Typography></TableCell>
      <TableCell>{a.accountType}</TableCell><TableCell sx={{ fontWeight: 700 }}>{money(a.balance)}</TableCell>
      <TableCell><Stack direction="row" flexWrap="wrap" gap={0.5}>
        {['Deposit', 'Withdraw', 'Edit account', 'Delete account'].map(kind => <Button key={kind} size="small" color={kind.startsWith('Delete') ? 'error' : 'primary'} disabled={loading} onClick={() => open(kind, a)}>{kind.replace(' account', '')}</Button>)}
        <Button size="small" onClick={() => { selectAccount(a.accountId); setPage('Transactions') }}>History</Button>
      </Stack></TableCell>
    </TableRow>
  ))

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar page={page} onNavigate={setPage} username={session.username} />
      <Box component="main" sx={{ flex: 1, minWidth: 0 }}>
        <Box component="header" sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap', minHeight: 72, p: 2, bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
            <TextField size="small" label="Search current records" value={search} onChange={e => setSearch(e.target.value)} />
            <Button disabled={loading} onClick={refresh}>Refresh</Button>
            <Button onClick={onSignOut}>Sign out</Button>
          </Box>
          <Chip sx={{ ml: 'auto', flexShrink: 0 }} label={loading ? 'Connecting' : error ? 'Connection needs attention' : 'Connected'} color={error ? 'warning' : 'success'} variant="outlined" />
        </Box>
        <Box sx={{ display: { xs: 'flex', md: 'none' }, p: 1, overflowX: 'auto' }}>
          {pages.map(p => <Button key={p} onClick={() => setPage(p)} variant={page === p ? 'contained' : 'text'}>{p}</Button>)}
        </Box>
        {loading && <LinearProgress />}
        <Box sx={{ p: { xs: 2, sm: 4 }, maxWidth: 1500, mx: 'auto' }}>
          <Box sx={{ display: 'flex', width: '100%', flexDirection: { xs: 'column', sm: 'row' }, alignItems: { xs: 'flex-start', sm: 'center' }, justifyContent: 'space-between', gap: 2, mb: 3 }}>
            <Box><Typography variant="h4">{page}</Typography><Typography color="text.secondary" sx={{ mt: 1 }}>Manage customers, accounts, and banking operations.</Typography></Box>
            <Box sx={{ display: 'flex', gap: 1, ml: { xs: 0, sm: 'auto' }, flexShrink: 0, '& .MuiButton-root': { height: 36, px: 1.75, borderRadius: 1.5, whiteSpace: 'nowrap' } }}>
              <Button size="small" variant="outlined" disabled={loading || accounts.length < 2} onClick={() => open('Transfer')}>New transfer</Button>
              <Button size="small" variant="contained" disabled={loading || !customers.length} onClick={() => open('Open account')}>Open account</Button>
            </Box>
          </Box>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {page === 'Overview' && <Stack spacing={3}>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,1fr)', xl: 'repeat(4,1fr)' }, gap: 2 }}>
              <MetricCard label="Total account balances" value={loading ? '…' : money(totalBalance)} detail="Combined balance across current accounts" icon={<AccountBalanceWalletOutlined />} />
              <MetricCard label="Customers" value={loading ? '…' : customers.length} detail="Registered customers" icon={<GroupsOutlined />} tone="green" />
              <MetricCard label="Accounts" value={loading ? '…' : accounts.length} detail="Customer banking accounts" icon={<SwapHorizRounded />} tone="slate" />
              <MetricCard label="Rejected operations" value={loading ? '…' : rejected} detail="Recorded in the audit trail" icon={<ShieldOutlined />} tone="amber" />
            </Box>
            <Typography variant="h6">Account portfolio</Typography>
            <DataTable headings={['Customer / account ID', 'Type', 'Balance', 'Actions']} rows={accountRows(visibleAccounts.slice(0, 5))} />
            <Button sx={{ alignSelf: 'flex-start' }} onClick={() => setPage('Accounts')}>View all accounts</Button>
            <Typography variant="h6">Recent audit activity</Typography>
            <DataTable headings={['Time', 'Action', 'Status', 'Amount']} rows={audits.slice(0, 5).map(a => <TableRow key={a.auditId}><TableCell>{date(a.timestamp)}</TableCell><TableCell>{a.action}</TableCell><TableCell><Chip size="small" label={a.status} color={a.status === 'SUCCESS' ? 'success' : 'warning'} /></TableCell><TableCell>{money(a.amount)}</TableCell></TableRow>)} />
          </Stack>}
          {page === 'Customers' && <Stack spacing={2}>
            <Button variant="contained" sx={{ alignSelf: 'flex-start' }} disabled={loading} onClick={() => open('Create customer')}>Create customer</Button>
            <DataTable headings={['Name', 'Customer ID', 'Accounts', 'Actions']} rows={visibleCustomers.map(c => <TableRow key={c.id}><TableCell sx={{ py: 2 }}>{c.name}</TableCell><TableCell>{c.id}</TableCell><TableCell>{accounts.filter(a => a.userId === c.id).length}</TableCell><TableCell><Button onClick={() => { setCustomerFilter(c.id); setPage('Accounts') }}>Accounts</Button><Button onClick={() => open('Edit customer', c)}>Edit</Button><Button color="error" onClick={() => open('Delete customer', c)}>Delete</Button></TableCell></TableRow>)} />
          </Stack>}
          {page === 'Accounts' && <Stack spacing={2}>
            <Stack direction={{ xs: 'column', sm: 'row' }} gap={1}>
              <TextField select label="Customer" size="small" value={customerFilter} onChange={e => setCustomerFilter(e.target.value)} sx={{ minWidth: 200 }}>
                <MenuItem value="">All customers</MenuItem>{customers.map(c => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
              </TextField>
              <TextField size="small" type="number" label="Premium balance threshold" value={threshold} onChange={e => setThreshold(e.target.value)} />
              <Button disabled={premiumBusy || loading} onClick={filterPremium}>Show premium accounts</Button>
              {premiumIds && <Button onClick={() => setPremiumIds(null)}>Clear premium filter</Button>}
            </Stack>
            <DataTable headings={['Customer / account ID', 'Type', 'Balance', 'Actions']} rows={accountRows(visibleAccounts)} />
          </Stack>}
          {page === 'Transactions' && <Stack spacing={2}>
            <TextField select label="Account" value={selectedAccount} onChange={e => selectAccount(e.target.value)} sx={{ maxWidth: 600 }}>
              <MenuItem value="">Choose an account</MenuItem>{accounts.map(a => <MenuItem key={a.accountId} value={a.accountId}>{accountLabel(a)}</MenuItem>)}
            </TextField>
            {historyBusy && <LinearProgress />}
            {historyError && <Alert severity="error">{historyError}</Alert>}
            {!selectedAccount ? <Alert severity="info">Choose an account to view its transaction history.</Alert> :
              <DataTable headings={['Time', 'Transaction ID', 'Type', 'Amount']} rows={transactions.filter(t => matches(t.type + ' ' + t.transactionId)).map(t => <TableRow key={t.transactionId}><TableCell sx={{ py: 2 }}>{date(t.date)}</TableCell><TableCell>{t.transactionId}</TableCell><TableCell>{t.type}</TableCell><TableCell>{money(t.amount)}</TableCell></TableRow>)} />}
          </Stack>}
          {page === 'Audit trail' && <DataTable headings={['Time', 'Action / ID', 'Customer', 'Accounts involved', 'Amount', 'Result']} rows={visibleAudits.map(a => <TableRow key={a.auditId}><TableCell>{date(a.timestamp)}</TableCell><TableCell>{a.action}<Typography variant="caption" display="block">{a.auditId}</Typography></TableCell><TableCell>{a.customerId || 'Unknown'}</TableCell><TableCell>{a.sourceAccountId || '—'}<Typography variant="caption" display="block">{a.destinationAccountId || ''}</Typography></TableCell><TableCell>{money(a.amount)}</TableCell><TableCell><Chip size="small" label={a.status} color={a.status === 'SUCCESS' ? 'success' : 'warning'} /><Typography variant="caption" display="block">{a.reason}</Typography></TableCell></TableRow>)} />}
        </Box>
      </Box>
      <Dialog open={Boolean(dialog)} onClose={() => { if (!saving) setDialog(null) }} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={submit}>
          <DialogTitle>{dialog?.kind}</DialogTitle>
          <DialogContent><Stack spacing={2} sx={{ pt: 1 }}>
            {formError && <Alert severity="error">{formError}</Alert>}
            {dialog?.kind.includes('customer') && !dialog.kind.startsWith('Delete') && <TextField required label="Customer name" {...field('name')} />}
            {dialog?.kind === 'Open account' && <TextField select required label="Customer" {...field('userId')}>{customers.map(c => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}</TextField>}
            {['Open account', 'Edit account'].includes(dialog?.kind) && <TextField required label="Account type" helperText="For example: SAVINGS or CHECKING" {...field('accountType')} />}
            {dialog?.kind === 'Transfer' && ['fromAccountId', 'toAccountId'].map(key => <TextField key={key} select required label={key === 'fromAccountId' ? 'From account' : 'To account'} {...field(key)}>{accounts.map(a => <MenuItem key={a.accountId} value={a.accountId}>{accountLabel(a)} — {money(a.balance)}</MenuItem>)}</TextField>)}
            {['Deposit', 'Withdraw', 'Transfer'].includes(dialog?.kind) && <>
              {dialog.kind !== 'Transfer' && <Typography>{accountLabel(dialog.record)} — balance {money(dialog.record.balance)}</Typography>}
              <TextField required type="number" label="Amount (USD)" {...field('amount')} slotProps={{ htmlInput: { min: '0.01', step: '0.01' } }} helperText={dialog.kind === 'Transfer' ? 'Maximum $10,000 per transfer.' : 'Enter the amount to move.'} />
            </>}
            {dialog?.kind.startsWith('Delete') && <Alert severity="warning">Delete {dialog.record.name || accountLabel(dialog.record)}? This permanently removes the record. Existing transaction and audit records are retained.</Alert>}
          </Stack></DialogContent>
          <DialogActions><Button disabled={saving} onClick={() => setDialog(null)}>Cancel</Button><Button type="submit" variant="contained" color={dialog?.kind.startsWith('Delete') ? 'error' : 'primary'} disabled={saving}>{saving ? 'Saving…' : 'Confirm'}</Button></DialogActions>
        </Box>
      </Dialog>
      <Snackbar open={Boolean(notice)} autoHideDuration={5000} onClose={() => setNotice('')}><Alert severity="success" onClose={() => setNotice('')}>{notice}</Alert></Snackbar>
    </Box>
  )
}
