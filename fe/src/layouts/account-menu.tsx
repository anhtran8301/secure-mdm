import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';

import Box from '@mui/material/Box';
import Menu from '@mui/material/Menu';
import Avatar from '@mui/material/Avatar';
import Divider from '@mui/material/Divider';
import MenuItem from '@mui/material/MenuItem';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';

import { logout } from 'src/lib/session';
import { useAuthStore } from 'src/stores/auth-store';

import { Label } from 'src/components/label';
import { toast } from 'src/components/snackbar';
import { statusText } from 'src/components/console';

/** Avatar in the top bar; opens who is signed in and the sign-out action. */
export function AccountMenu() {
  const user = useAuthStore((state) => state.user);
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const signOut = useMutation({
    mutationFn: logout,
    onError: (error) => toast.error(error.message),
  });

  if (!user) return null;

  return (
    <>
      <IconButton
        aria-label="Tài khoản"
        aria-haspopup="menu"
        aria-expanded={!!anchor}
        aria-controls={anchor ? 'account-menu' : undefined}
        onClick={(event) => setAnchor(event.currentTarget)}
        sx={{ p: 0.5, border: 2, borderColor: 'divider' }}
      >
        <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: 16 }}>
          {user.organizationSlug.charAt(0).toUpperCase()}
        </Avatar>
      </IconButton>

      <Menu
        id="account-menu"
        anchorEl={anchor}
        open={!!anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { width: 240, mt: 1 } } }}
      >
        <Box sx={{ px: 2, py: 1.5 }}>
          <Typography variant="subtitle2" noWrap>
            {user.organizationSlug}
          </Typography>
          <Label color="info" sx={{ mt: 0.5 }}>
            {statusText(user.role)}
          </Label>
        </Box>
        <Divider sx={{ borderStyle: 'dashed' }} />
        <MenuItem
          disabled={signOut.isPending}
          onClick={() => signOut.mutate()}
          sx={{
            mt: 0.5,
            justifyContent: 'center',
            color: 'error.main',
            fontWeight: 'fontWeightBold',
          }}
        >
          Đăng xuất
        </MenuItem>
      </Menu>
    </>
  );
}
