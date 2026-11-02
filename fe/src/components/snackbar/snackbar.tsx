import { Toaster } from 'sonner';

import { useTheme } from '@mui/material/styles';

/** Global toast host (Sonner) using the MUI theme's font and palette mode. */
export function Snackbar() {
  const theme = useTheme();
  return (
    <Toaster
      position="top-right"
      richColors
      closeButton
      theme={theme.palette.mode}
      toastOptions={{ style: { fontFamily: theme.typography.fontFamily } }}
    />
  );
}
