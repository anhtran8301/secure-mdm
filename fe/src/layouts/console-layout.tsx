import { useState } from 'react';
import { Outlet, NavLink, Link as RouterLink } from 'react-router-dom';

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Drawer from '@mui/material/Drawer';
import Typography from '@mui/material/Typography';

import { paths } from 'src/routes/paths';

import { useAuthStore } from 'src/stores/auth-store';

import { Iconify } from 'src/components/iconify';
import { statusText } from 'src/components/console';

import { navItems } from './nav-config';
import { AccountMenu } from './account-menu';

export function ConsoleLayout() {
  const user = useAuthStore((state) => state.user);
  const [open, setOpen] = useState(false);

  const nav = (
    <Stack component="nav" aria-label="Điều hướng chính" gap={1} p={2} sx={{ width: 224 }}>
      <Typography
        component={RouterLink}
        to={paths.root}
        color="text.primary"
        variant="h6"
        sx={{ textDecoration: 'none', py: 2 }}
      >
        SecureMDM
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        {user?.organizationSlug} · {user && statusText(user.role)}
      </Typography>
      {navItems.map((item) => (
        <Button
          key={item.path}
          component={NavLink}
          end
          to={item.path}
          startIcon={<Iconify icon={item.icon} aria-hidden />}
          onClick={() => setOpen(false)}
          sx={{
            justifyContent: 'flex-start',
            minHeight: 44,
            color: 'text.secondary',
            '&.active': { bgcolor: 'action.selected', color: 'text.primary' },
          }}
        >
          {item.title}
        </Button>
      ))}
    </Stack>
  );

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: 'background.neutral',
        display: 'flex',
        '& .MuiButton-textPrimary, & .MuiButton-outlinedPrimary, & .MuiLink-root': {
          color: 'primary.dark',
        },
      }}
    >
      <Box
        component="a"
        href="#main-content"
        sx={{
          position: 'fixed',
          left: 16,
          top: -80,
          p: 2,
          bgcolor: 'background.paper',
          zIndex: 1500,
          '&:focus': { top: 8 },
        }}
      >
        Bỏ qua điều hướng
      </Box>
      <Box sx={{ display: { xs: 'none', lg: 'block' }, flexShrink: 0 }}>{nav}</Box>
      <Drawer open={open} onClose={() => setOpen(false)}>
        {nav}
      </Drawer>
      <Box sx={{ minWidth: 0, flex: 1, p: 1 }}>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
          sx={{ minHeight: 48, px: 2 }}
        >
          <Button
            onClick={() => setOpen(true)}
            startIcon={<Iconify icon="custom:menu-duotone" aria-hidden />}
            sx={{ display: { lg: 'none' } }}
          >
            Menu
          </Button>
          <Stack direction="row" alignItems="center" gap={2} sx={{ ml: 'auto' }}>
            <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' } }}>
              Admin Console · Thiết bị mô phỏng
            </Typography>
            <AccountMenu />
          </Stack>
        </Stack>
        <Box
          component="main"
          id="main-content"
          tabIndex={-1}
          sx={{
            bgcolor: 'background.paper',
            border: 1,
            borderColor: 'divider',
            borderRadius: 2,
            p: { xs: 2, md: 3 },
            minHeight: 'calc(100vh - 64px)',
          }}
        >
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
