import { createTheme } from '@mui/material/styles'

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#1d4ed8', dark: '#173eab', light: '#e8efff' },
    success: { main: '#13855b', light: '#e7f6ef' },
    warning: { main: '#b66a0a', light: '#fff4dc' },
    background: { default: '#f4f6fa', paper: '#ffffff' },
    text: { primary: '#182230', secondary: '#647083' },
    divider: '#e5e9f0',
  },
  typography: {
    fontFamily: 'Inter, "Segoe UI", Arial, sans-serif',
    h4: { fontWeight: 700, letterSpacing: '-0.035em' },
    h6: { fontWeight: 700, letterSpacing: '-0.015em' },
    button: { textTransform: 'none', fontWeight: 650 },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiChip: { styleOverrides: { root: { fontWeight: 650 } } },
  },
})

export default theme
