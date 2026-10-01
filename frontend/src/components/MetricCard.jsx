import { Box, Paper, Typography } from '@mui/material'

export default function MetricCard({ label, value, detail, icon, tone = 'blue' }) {
  const colors = {
    blue: ['#e9f0ff', '#2557c7'],
    green: ['#e8f6f0', '#137555'],
    amber: ['#fff4df', '#a45e08'],
    slate: ['#edf0f5', '#49576b'],
  }
  const [background, foreground] = colors[tone]

  return (
    <Paper variant="outlined" sx={{ p: 2.4, minHeight: 148, borderColor: 'divider' }}>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <Box>
          <Typography color="text.secondary" sx={{ fontSize: 13, fontWeight: 600 }}>{label}</Typography>
          <Typography sx={{ fontSize: 28, fontWeight: 750, letterSpacing: '-0.035em', mt: 1 }}>{value}</Typography>
        </Box>
        <Box sx={{ width: 40, height: 40, borderRadius: 2.5, display: 'grid', placeItems: 'center', bgcolor: background, color: foreground }}>
          {icon}
        </Box>
      </Box>
      <Typography color="text.secondary" sx={{ fontSize: 12.5, mt: 2 }}>{detail}</Typography>
    </Paper>
  )
}
