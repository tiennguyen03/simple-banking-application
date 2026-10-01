import { CssBaseline, ThemeProvider } from '@mui/material'
import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'
import theme from './theme'
import { useEffect, useSyncExternalStore } from 'react'
import { getSession, subscribe, clearSession } from './services/session'
import CustomerDashboard from './pages/CustomerDashboard'

export default function App() {
  const session = useSyncExternalStore(subscribe, getSession)
  useEffect(() => {
    const path = session ? session.role === 'ADMIN' ? '/operations' : '/customer-dashboard' : '/'
    const correctPath = () => window.history.replaceState(null, '', path)
    correctPath()
    window.addEventListener('popstate', correctPath)
    return () => window.removeEventListener('popstate', correctPath)
  }, [session])
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {!session ? <LoginPage /> : session.role === 'ADMIN'
        ? <DashboardPage session={session} onSignOut={clearSession} />
        : <CustomerDashboard session={session} onSignOut={clearSession} />}
    </ThemeProvider>
  )
}
