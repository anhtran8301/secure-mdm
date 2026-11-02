import type { TextFieldProps } from '@mui/material/TextField';

import { Controller, useFormContext } from 'react-hook-form';

import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';

export type RHFSelectOption = { value: string; label: string };

export type RHFSelectProps = TextFieldProps & { name: string; options: RHFSelectOption[] };

export function RHFSelect({ name, options, helperText, ...other }: RHFSelectProps) {
  const { control } = useFormContext();
  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState: { error } }) => (
        <TextField
          {...field}
          select
          fullWidth
          value={field.value ?? ''}
          error={!!error}
          helperText={error?.message ?? helperText}
          {...other}
        >
          {options.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </TextField>
      )}
    />
  );
}
