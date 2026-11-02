import type { TextFieldProps } from '@mui/material/TextField';

import { Controller, useFormContext } from 'react-hook-form';

import TextField from '@mui/material/TextField';

export type RHFTextFieldProps = TextFieldProps & { name: string };

export function RHFTextField({ name, helperText, type, ...other }: RHFTextFieldProps) {
  const { control } = useFormContext();
  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState: { error } }) => (
        <TextField
          {...field}
          fullWidth
          type={type}
          value={field.value ?? ''}
          onChange={(event) =>
            field.onChange(type === 'number' ? Number(event.target.value) : event.target.value)
          }
          error={!!error}
          helperText={error?.message ?? helperText}
          {...other}
        />
      )}
    />
  );
}
