import type { Policy } from 'src/types/api';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';

import { ErrorMessage } from 'src/components/console';
import { Form, RHFCheckbox, RHFTextField } from 'src/components/hook-form';

import { useSavePolicy } from '../mutations';
import { policyFormSchema, policyFormDefaults } from '../schemas';

import type { PolicyFormValues } from '../schemas';

type Props = { policy: Policy | null; onClose: () => void };

export function PolicyDialog({ policy, onClose }: Props) {
  const methods = useForm<PolicyFormValues>({
    resolver: zodResolver(policyFormSchema(!policy)),
    defaultValues: policyFormDefaults(policy),
  });
  const save = useSavePolicy(policy);

  return (
    <Dialog open onClose={onClose} fullWidth aria-labelledby="policy-title">
      <Form
        methods={methods}
        onSubmit={(values) => save.mutateAsync(values).then(onClose, () => {})}
      >
        <DialogTitle id="policy-title">
          {policy ? `Version mới · ${policy.name}` : 'Tạo policy'}
        </DialogTitle>
        <DialogContent>
          <Stack gap={3} mt={1}>
            {!policy && (
              <RHFTextField
                name="name"
                label="Tên policy"
                required
                slotProps={{ htmlInput: { maxLength: 120 } }}
              />
            )}
            <RHFCheckbox name="requireScreenLock" label="Bắt buộc screen lock" />
            <RHFCheckbox name="requireDiskEncryption" label="Bắt buộc mã hóa ổ đĩa" />
            <RHFTextField
              name="minAndroidApiLevel"
              label="Android API tối thiểu"
              type="number"
              required
              slotProps={{ htmlInput: { min: 1, max: 100 } }}
            />
            <RHFTextField
              name="blockedPackages"
              label="Ứng dụng bị chặn"
              multiline
              minRows={3}
              helperText="Một package name mỗi dòng, tối đa 200."
            />
            <ErrorMessage error={save.error} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Hủy</Button>
          <Button type="submit" variant="contained" color="inherit" disabled={save.isPending}>
            Lưu version
          </Button>
        </DialogActions>
      </Form>
    </Dialog>
  );
}
