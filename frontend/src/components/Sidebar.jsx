import {
  AccountBalanceOutlined,
  DashboardRounded,
  GroupsOutlined,
  HistoryOutlined,
  SwapHorizRounded,
} from '@mui/icons-material'
import { Box, Divider, List, ListItemButton, ListItemIcon, ListItemText, Typography } from '@mui/material'

const items = [
  ['Overview', DashboardRounded],
  ['Customers', GroupsOutlined],
  ['Accounts', AccountBalanceOutlined],
  ['Transactions', SwapHorizRounded],
  ['Audit trail', HistoryOutlined],
]

export default function Sidebar({ page, onNavigate, username }) {
  return (
    <Box
      component="aside"
      sx={{
        width: 264,
        flexShrink: 0,
        bgcolor: '#0d1b2e',
        color: '#fff',
        minHeight: '100vh',
        display: { xs: 'none', md: 'flex' },
        flexDirection: 'column',
        px: 2,
        py: 2.5,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.4, px: 1.5, mb: 3.5 }}>
        <Box sx={{ width: 38, height: 38, borderRadius: 2, bgcolor: '#2f67db', display: 'grid', placeItems: 'center' }}>
          <AccountBalanceOutlined fontSize="small" />
        </Box>
        <Box>
          <Typography sx={{ fontSize: 16, fontWeight: 750, lineHeight: 1.2 }}>Simple Bank</Typography>
          <Typography sx={{ color: '#91a0b7', fontSize: 11.5 }}>Operations Console</Typography>
        </Box>
      </Box>

      <Typography sx={{ color: '#72839d', fontSize: 10.5, fontWeight: 700, letterSpacing: 1.3, px: 1.5, mb: 1 }}>
        WORKSPACE
      </Typography>
      <List disablePadding sx={{ display: 'grid', gap: 0.5 }}>
        {items.map(([label, Icon]) => {
          return (
            <ListItemButton
              key={label}
              selected={page === label}
              onClick={() => onNavigate(label)}
              sx={{
                borderRadius: 2,
                minHeight: 44,
                color: page === label ? '#fff' : '#aab6c8',
                '&.Mui-selected': { bgcolor: '#1b3150' },
                '&.Mui-selected:hover': { bgcolor: '#1b3150' },
              }}
            >
              <ListItemIcon sx={{ minWidth: 38, color: 'inherit' }}><Icon /></ListItemIcon>
              <ListItemText primary={label} slotProps={{ primary: { fontSize: 14, fontWeight: page === label ? 650 : 500 } }} />
            </ListItemButton>
          )
        })}
      </List>

      <Box sx={{ mt: 'auto' }}>
        <Divider sx={{ borderColor: '#23344b', mb: 1.5 }} />
        <Box sx={{ display: 'flex', gap: 1.4, alignItems: 'center', px: 1.5, pt: 2.5 }}>
          <Box sx={{ width: 34, height: 34, borderRadius: '50%', bgcolor: '#d7e3ff', color: '#183d8f', display: 'grid', placeItems: 'center', fontWeight: 750, fontSize: 13 }}>{username.slice(0, 2).toUpperCase()}</Box>
          <Box>
            <Typography sx={{ fontSize: 13.5, fontWeight: 650 }}>{username}</Typography>
            <Typography sx={{ color: '#8291a8', fontSize: 11.5 }}>Administrator</Typography>
          </Box>
        </Box>
      </Box>
    </Box>
  )
}
