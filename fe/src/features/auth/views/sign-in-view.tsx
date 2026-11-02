import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';

import { login } from 'src/lib/session';

import { ErrorMessage } from 'src/components/console';
import { Form, RHFTextField } from 'src/components/hook-form';

import { loginSchema } from '../schemas';

import type { LoginValues } from '../schemas';

export function SignInView() {
  const methods = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { organizationSlug: '', email: '', password: '' },
  });
  const signIn = useMutation({ mutationFn: login });

  return (
    <Box component="main" sx={{ maxWidth: 440, mx: 'auto', p: 3, pt: { xs: 6, md: 12 } }}>
      <Typography variant="h6" component="p" mb={4}>
        SecureMDM
      </Typography>
      <Typography component="h1" variant="h4" mb={1}>
        Đăng nhập
      </Typography>
      <Typography color="text.secondary" mb={4}>
        Quản lý thiết bị, chính sách và lệnh trong tổ chức của bạn.
      </Typography>
      <Form methods={methods} onSubmit={(values) => signIn.mutateAsync(values).catch(() => {})}>
        <Stack gap={3}>
          <RHFTextField
            name="organizationSlug"
            label="Tổ chức"
            required
            autoComplete="organization"
            helperText="Slug tổ chức, ví dụ alpha"
          />
          <RHFTextField name="email" label="Email" type="email" required autoComplete="username" />
          <RHFTextField
            name="password"
            label="Mật khẩu"
            type="password"
            required
            autoComplete="current-password"
          />
          <ErrorMessage error={signIn.error} />
          <Button
            type="submit"
            color="inherit"
            variant="contained"
            size="large"
            disabled={signIn.isPending}
          >
            {signIn.isPending ? 'Đang đăng nhập…' : 'Đăng nhập'}
          </Button>
        </Stack>
      </Form>
    </Box>
  );
}
