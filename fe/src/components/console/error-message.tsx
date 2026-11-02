import Alert from '@mui/material/Alert';

export function ErrorMessage({ error }: { error: unknown }) {
  return error ? (
    <Alert severity="error" role="alert">
      {error instanceof Error ? error.message : 'Không thể hoàn tất yêu cầu.'}
    </Alert>
  ) : null;
}
