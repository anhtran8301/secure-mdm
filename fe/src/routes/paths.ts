export const paths = {
  root: '/',
  signIn: '/sign-in',
  devices: {
    root: '/devices',
    detail: (id: string) => `/devices/${id}`,
  },
  enrollment: '/enrollment',
  policies: '/policies',
  commands: '/commands',
  audit: '/audit',
};
