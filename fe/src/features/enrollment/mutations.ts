import { useMutation } from '@tanstack/react-query';

import { toast } from 'src/components/snackbar';

import { enrollmentApi } from './api';

/** The plaintext token exists only in this mutation's result; the server keeps a hash. */
export function useIssueEnrollmentToken() {
  return useMutation({ mutationFn: enrollmentApi.issue });
}

export function useRevokeEnrollmentToken() {
  return useMutation({
    mutationFn: enrollmentApi.revoke,
    onSuccess: () => toast.success('Đã thu hồi enrollment token.'),
  });
}
