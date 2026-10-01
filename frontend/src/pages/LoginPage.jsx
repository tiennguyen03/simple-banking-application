import { useState } from 'react'
import * as api from '../services/bankingApi'
import { setSession } from '../services/session'
import { AccountBalanceOutlined, ArrowForwardRounded, VisibilityOffOutlined, VisibilityOutlined } from '@mui/icons-material'
import { Alert, Box, Button, Divider, IconButton, InputAdornment, Paper, Stack, TextField, Typography } from '@mui/material'

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [registering, setRegistering] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    setBusy(true)
    setMessage('')
    try {
      const body = { username: data.get('username'), password: data.get('password') }
      if (registering) body.name = data.get('name')
      const response = await (registering ? api.register(body) : api.login(body))
      form.reset()
      setSession(response)
    } catch (error) { setMessage(error.message) }
    finally { setBusy(false) }
  }

  return (
    <Box sx={{ minHeight: '100dvh', display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
      <Box sx={{ bgcolor: '#0d1b2e', color: '#fff', px: { xs: 3, md: 7, lg: 10 }, py: { xs: 4, md: 6 }, display: 'flex', flexDirection: 'column' }}>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <Box sx={{ bgcolor: '#2f67db', borderRadius: 2, width: 42, height: 42, display: 'grid', placeItems: 'center' }}><AccountBalanceOutlined /></Box>
          <Typography sx={{ fontSize: 19, fontWeight: 700 }}>Simple Bank</Typography>
        </Stack>
        <Box sx={{ my: 'auto', py: { xs: 5, md: 10 }, maxWidth: 490 }}>
          <Typography sx={{ color: '#9bb4da', fontSize: 12, letterSpacing: 2, fontWeight: 600, mb: 2 }}>YOUR BANKING WORKSPACE</Typography>
          <Typography component="h1" sx={{ fontSize: { xs: 34, lg: 48 }, fontWeight: 700, letterSpacing: '-0.04em', lineHeight: 1.15 }}>A clear view of your banking.</Typography>
          <Typography sx={{ color: '#afbdd0', mt: 3, fontSize: 16, lineHeight: 1.8 }}>Access your accounts, review your activity, and manage your money from one place.</Typography>
          <Divider sx={{ borderColor: '#293a51', my: 4 }} />
          <Stack spacing={2.5}>
            {[
              ['For customers', 'Your accounts, balances, and transaction history.'],
              ['For administrators', 'Customer management and banking operations.'],
            ].map(([title, description]) => <Box key={title}><Typography sx={{ fontWeight: 600, mb: 0.5 }}>{title}</Typography><Typography sx={{ color: '#afbdd0', fontSize: 14 }}>{description}</Typography></Box>)}
          </Stack>
        </Box>
        <Typography sx={{ color: '#8291a8', fontSize: 12 }}>Simple Bank · Customer and administrator portal</Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', px: 3, py: 6 }}>
        <Box sx={{ width: '100%', maxWidth: 440 }}>
          <Typography component="h2" variant="h4">{registering ? 'Create your login' : 'Welcome back'}</Typography>
          <Typography color="text.secondary" sx={{ mt: 1, mb: 3 }}>{registering ? 'Register as a new Simple Bank customer.' : 'Sign in to your Simple Bank account.'}</Typography>
          <Paper variant="outlined" sx={{ p: { xs: 3, sm: 4 } }}>
            <Box component="form" onSubmit={submit}>
              <Stack spacing={2.5}>
                {registering && <TextField label="Full name" name="name" autoComplete="name" required fullWidth disabled={busy} slotProps={{ htmlInput: { maxLength: 120 } }} />}
                <TextField label="Username" name="username" autoComplete="username" required fullWidth disabled={busy} helperText={registering ? '3–80 letters, numbers, periods, underscores, or hyphens.' : undefined} slotProps={{ htmlInput: { maxLength: 80, ...(registering ? { pattern: '[A-Za-z0-9._-]{3,80}' } : {}) } }} />
                <TextField label="Password" name="password" disabled={busy} autoComplete={registering ? 'new-password' : 'current-password'} type={showPassword ? 'text' : 'password'} required fullWidth helperText={registering ? 'At least 10 characters; no more than 72 UTF-8 bytes.' : undefined} slotProps={{ htmlInput: { maxLength: 72, minLength: registering ? 10 : undefined }, input: { endAdornment: <InputAdornment position="end"><IconButton disabled={busy} aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword(!showPassword)} edge="end">{showPassword ? <VisibilityOffOutlined /> : <VisibilityOutlined />}</IconButton></InputAdornment> } }} />
                {message && <Alert severity="error">{message}</Alert>}
                <Button type="submit" disabled={busy} variant="contained" fullWidth endIcon={<ArrowForwardRounded />} sx={{ height: 44 }}>{busy ? 'Please wait…' : registering ? 'Create customer login' : 'Sign in'}</Button>
                <Button disabled={busy} onClick={() => { setRegistering(!registering); setMessage('') }}>{registering ? 'Already registered? Sign in' : 'New customer? Create a login'}</Button>
              </Stack>
            </Box>
            <Divider sx={{ my: 3 }} />
            <Typography color="text.secondary" sx={{ fontSize: 13, lineHeight: 1.7 }}>Customers and administrators use the same sign-in page. Your account permissions determine which workspace you can access.</Typography>
          </Paper>
          <Typography color="text.secondary" sx={{ mt: 2.5, fontSize: 12, textAlign: 'center' }}>Customer registration creates a new banking profile. Administrators are provisioned separately.</Typography>
        </Box>
      </Box>
    </Box>
  )
}
