import { useEffect, useState } from 'react'
import { AccountBalanceOutlined } from '@mui/icons-material'
import { Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, LinearProgress, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from '@mui/material'
import * as api from '../services/bankingApi'

const money = value => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value || 0))

export default function CustomerDashboard({ session, onSignOut }) {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)
  const [selected, setSelected] = useState('')
  const [transactions, setTransactions] = useState([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyError, setHistoryError] = useState('')
  const [transferOpen, setTransferOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [transferError, setTransferError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    api.getAccounts().then(data => {
      if (active) {
        setAccounts(data)
        setSelected(previous => data.some(a => a.accountId === previous) ? previous : '')
      }
    }).catch(failure => { if (active) setError(failure.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [reload])

  useEffect(() => {
    let active = true
    if (selected) api.getTransactions(selected).then(data => {
      if (active) setTransactions([...data].sort((a, b) => b.date.localeCompare(a.date)))
    }).catch(failure => { if (active) setHistoryError(failure.message) })
      .finally(() => { if (active) setHistoryLoading(false) })
    return () => { active = false }
  }, [selected, reload])

  function refresh() {
    setLoading(true)
    setError('')
    setTransactions([])
    setHistoryError('')
    setHistoryLoading(Boolean(selected))
    setReload(value => value + 1)
  }

  function selectAccount(id) {
    if (id === selected) return
    setTransactions([])
    setHistoryError('')
    setHistoryLoading(Boolean(id))
    setSelected(id)
  }

  async function submitTransfer(event) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    const body = { fromAccountId: fields.get('fromAccountId'), toAccountId: fields.get('toAccountId'), amount: fields.get('amount') }
    if (body.fromAccountId === body.toAccountId) { setTransferError('Choose two different accounts.'); return }
    if (!Number.isFinite(Number(body.amount)) || Number(body.amount) <= 0 || Number(body.amount) > 10000) {
      setTransferError('Enter a positive amount of no more than $10,000.'); return
    }
    setSaving(true)
    setTransferError('')
    try {
      await api.transfer(body)
      setTransferOpen(false)
      setNotice('Transfer completed.')
      refresh()
    } catch (failure) { setTransferError(failure.message) }
    finally { setSaving(false) }
  }

  return <Box sx={{ minHeight: '100vh' }}>
    <Box component="header" sx={{ bgcolor: '#0d1b2e', color: 'white', px: { xs: 3, md: 6 }, py: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
      <AccountBalanceOutlined /><Typography fontWeight={700}>Simple Bank</Typography>
      <Typography sx={{ ml: 'auto', fontSize: 14 }}>{session.username}</Typography>
      <Button onClick={onSignOut} sx={{ color: 'white' }}>Sign out</Button>
    </Box>
    {loading && <LinearProgress />}
    <Box component="main" sx={{ maxWidth: 1200, mx: 'auto', p: { xs: 3, md: 6 } }}>
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', mb: 4 }}>
        <Box sx={{ flex: 1 }}><Typography variant="h4" component="h1">My banking</Typography><Typography color="text.secondary" sx={{ mt: 1 }}>Your accounts and recent activity.</Typography></Box>
        <Button disabled={loading} onClick={refresh}>Refresh</Button>
        <Button variant="contained" disabled={loading || accounts.length < 2} onClick={() => { setTransferError(''); setTransferOpen(true) }}>Transfer between my accounts</Button>
      </Box>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {notice && <Alert severity="success" onClose={() => setNotice('')} sx={{ mb: 2 }}>{notice}</Alert>}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' }, gap: 2, mb: 4 }}>
        {accounts.map(account => <Paper key={account.accountId} variant="outlined" sx={{ p: 3 }}>
          <Typography color="text.secondary" variant="body2">{account.accountType}</Typography>
          <Typography variant="h5" sx={{ my: 1, overflowWrap: 'anywhere' }}>{money(account.balance)}</Typography>
          <Typography variant="caption" sx={{ overflowWrap: 'anywhere' }}>Account {account.accountId}</Typography>
          <Box sx={{ mt: 2 }}><Button size="small" onClick={() => selectAccount(account.accountId)}>View transactions</Button></Box>
        </Paper>)}
      </Box>
      {!loading && !error && !accounts.length && <Alert severity="info" sx={{ mb: 4 }}>You have no accounts yet. Contact an administrator to open an account.</Alert>}
      <Typography variant="h6" component="h2" sx={{ mb: 2 }}>Transaction history</Typography>
      <TextField select label="Account" value={selected} onChange={event => selectAccount(event.target.value)} size="small" fullWidth sx={{ mb: 2 }}>
        <MenuItem value="">Select an account</MenuItem>
        {accounts.map(account => <MenuItem key={account.accountId} value={account.accountId}>{account.accountType} — {account.accountId}</MenuItem>)}
      </TextField>
      {historyLoading && selected && <LinearProgress />}
      {historyError && <Alert severity="error" sx={{ mb: 2 }}>{historyError}</Alert>}
      <TableContainer component={Paper} variant="outlined"><Table size="small">
        <TableHead><TableRow><TableCell>Date</TableCell><TableCell>Type</TableCell><TableCell align="right">Amount</TableCell></TableRow></TableHead>
        <TableBody>{transactions.map(transaction => <TableRow key={transaction.transactionId}>
          <TableCell>{new Date(transaction.date).toLocaleString()}</TableCell><TableCell>{transaction.type}</TableCell><TableCell align="right">{money(transaction.amount)}</TableCell>
        </TableRow>)}
        {!transactions.length && <TableRow><TableCell colSpan={3} sx={{ py: 4, textAlign: 'center' }}>{selected ? historyLoading ? 'Loading transactions…' : 'No transactions to display.' : 'Select an account to view its history.'}</TableCell></TableRow>}
        </TableBody>
      </Table></TableContainer>
    </Box>
    <Dialog open={transferOpen} onClose={() => { if (!saving) setTransferOpen(false) }} fullWidth maxWidth="sm">
      <Box component="form" onSubmit={submitTransfer}>
        <DialogTitle>Transfer between your accounts</DialogTitle>
        <DialogContent><Stack spacing={2} sx={{ pt: 1 }}>
          {['fromAccountId', 'toAccountId'].map((name, index) => <TextField key={name} name={name} label={index ? 'To account' : 'From account'} select required defaultValue="" disabled={saving}>
            {accounts.map(account => <MenuItem key={account.accountId} value={account.accountId}>{account.accountType} — {account.accountId}</MenuItem>)}
          </TextField>)}
          <TextField name="amount" label="Amount" type="number" required disabled={saving} slotProps={{ htmlInput: { min: '0.01', max: '10000', step: '0.01' } }} />
          <Typography color="text.secondary" variant="body2">Transfers are limited to $10,000 and accounts that belong to you.</Typography>
          {transferError && <Alert severity="error">{transferError}</Alert>}
        </Stack></DialogContent>
        <DialogActions><Button disabled={saving} onClick={() => setTransferOpen(false)}>Cancel</Button><Button type="submit" variant="contained" disabled={saving}>{saving ? 'Transferring…' : 'Confirm transfer'}</Button></DialogActions>
      </Box>
    </Dialog>
  </Box>
}
