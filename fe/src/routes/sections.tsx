import type { RouteObject } from 'react-router';

import { lazy, Suspense } from 'react';
import { Outlet } from 'react-router-dom';

import LinearProgress from '@mui/material/LinearProgress';

import { ConsoleLayout } from 'src/layouts/console-layout';
import { SignInView } from 'src/features/auth/views/sign-in-view';

import { AuthGuard, GuestGuard } from './guards';

// One chunk per feature view.
const OverviewView = lazy(() =>
  import('src/features/dashboard/views/overview-view').then((m) => ({ default: m.OverviewView }))
);
const DeviceListView = lazy(() =>
  import('src/features/devices/views/device-list-view').then((m) => ({
    default: m.DeviceListView,
  }))
);
const DeviceDetailView = lazy(() =>
  import('src/features/devices/views/device-detail-view').then((m) => ({
    default: m.DeviceDetailView,
  }))
);
const EnrollmentView = lazy(() =>
  import('src/features/enrollment/views/enrollment-view').then((m) => ({
    default: m.EnrollmentView,
  }))
);
const PolicyListView = lazy(() =>
  import('src/features/policies/views/policy-list-view').then((m) => ({
    default: m.PolicyListView,
  }))
);
const CommandListView = lazy(() =>
  import('src/features/commands/views/command-list-view').then((m) => ({
    default: m.CommandListView,
  }))
);
const AuditView = lazy(() =>
  import('src/features/audit/views/audit-view').then((m) => ({ default: m.AuditView }))
);
const Page404 = lazy(() => import('src/pages/page-not-found'));

export const routesSection: RouteObject[] = [
  {
    element: (
      <Suspense fallback={<LinearProgress aria-label="Đang tải trang" />}>
        <Outlet />
      </Suspense>
    ),
    children: [
      {
        element: (
          <AuthGuard>
            <ConsoleLayout />
          </AuthGuard>
        ),
        children: [
          { index: true, element: <OverviewView /> },
          { path: 'devices', element: <DeviceListView /> },
          { path: 'devices/:id', element: <DeviceDetailView /> },
          { path: 'enrollment', element: <EnrollmentView /> },
          { path: 'policies', element: <PolicyListView /> },
          { path: 'commands', element: <CommandListView /> },
          { path: 'audit', element: <AuditView /> },
        ],
      },
      {
        path: 'sign-in',
        element: (
          <GuestGuard>
            <SignInView />
          </GuestGuard>
        ),
      },
      { path: '*', element: <Page404 /> },
    ],
  },
];
