import type { ReactNode } from 'react';
import type { BoxProps } from '@mui/material/Box';
import type { FieldValues, UseFormReturn } from 'react-hook-form';

import { FormProvider } from 'react-hook-form';

import Box from '@mui/material/Box';

type FormProps<T extends FieldValues> = Omit<BoxProps<'form'>, 'onSubmit'> & {
  methods: UseFormReturn<T>;
  onSubmit: (values: T) => void | Promise<void>;
  children: ReactNode;
};

/** <form> bound to react-hook-form; children use the RHF* fields via context. */
export function Form<T extends FieldValues>({
  methods,
  onSubmit,
  children,
  ...other
}: FormProps<T>) {
  return (
    <FormProvider {...methods}>
      <Box component="form" noValidate onSubmit={methods.handleSubmit(onSubmit)} {...other}>
        {children}
      </Box>
    </FormProvider>
  );
}
