import { Controller, useFormContext } from 'react-hook-form';

import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';

export function RHFCheckbox({ name, label }: { name: string; label: string }) {
  const { control } = useFormContext();
  return (
    <Controller
      name={name}
      control={control}
      render={({ field }) => (
        <FormControlLabel
          label={label}
          control={
            <Checkbox
              name={field.name}
              checked={!!field.value}
              onBlur={field.onBlur}
              onChange={(event) => field.onChange(event.target.checked)}
            />
          }
        />
      )}
    />
  );
}
