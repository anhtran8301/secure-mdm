import type { ReactNode } from 'react';

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

export function Heading({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      alignItems={{ xs: 'stretch', sm: 'flex-start' }}
      justifyContent="space-between"
      gap={2}
      mb={3}
    >
      <Box>
        <Typography variant="h4" component="h1" sx={{ fontSize: 26 }}>
          {title}
        </Typography>
        {description && (
          <Typography color="text.secondary" mt={1}>
            {description}
          </Typography>
        )}
      </Box>
      {action}
    </Stack>
  );
}
