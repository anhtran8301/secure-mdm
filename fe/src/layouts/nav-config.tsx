import type { IconifyName } from 'src/components/iconify';

import { paths } from 'src/routes/paths';

export type NavItem = { path: string; title: string; icon: IconifyName };

// Icons must be registered in components/iconify/icon-sets.ts to load offline.
export const navItems: NavItem[] = [
  { path: paths.root, title: 'Tổng quan', icon: 'solar:home-angle-bold-duotone' },
  { path: paths.devices.root, title: 'Thiết bị', icon: 'solar:eye-bold' },
  { path: paths.enrollment, title: 'Enrollment', icon: 'solar:shield-keyhole-bold-duotone' },
  { path: paths.policies, title: 'Policies', icon: 'solar:settings-bold-duotone' },
  { path: paths.commands, title: 'Commands', icon: 'solar:chat-round-dots-bold' },
  { path: paths.audit, title: 'Audit', icon: 'solar:clock-circle-outline' },
];
