import type {
  Page,
  Policy,
  Assignment,
  PageParams,
  PolicyConfig,
  PolicyVersion,
} from 'src/types/api';

import { http } from 'src/lib/axios';
import { PAGE_SIZE, pageQuery } from 'src/lib/page';

export type CreatePolicyBody = { name: string; config: PolicyConfig };

export const policyApi = {
  list: ({ page, size = PAGE_SIZE }: PageParams) =>
    http.get<Page<Policy>>('/policies', { params: pageQuery({ page, size }) }).then((r) => r.data),
  versions: (policyId: string, { page, size = PAGE_SIZE }: PageParams) =>
    http
      .get<Page<PolicyVersion>>(`/policies/${policyId}/versions`, {
        params: pageQuery({ page, size }),
      })
      .then((r) => r.data),
  create: (body: CreatePolicyBody) =>
    http.post<PolicyVersion>('/policies', body).then((r) => r.data),
  createVersion: (policyId: string, config: PolicyConfig) =>
    http.post<PolicyVersion>(`/policies/${policyId}/versions`, config).then((r) => r.data),
  assignment: (deviceId: string) =>
    http.get<Assignment>(`/devices/${deviceId}/policy-assignment`).then((r) => r.data),
  assign: (deviceId: string, versionId: string) =>
    http
      .put<Assignment>(`/devices/${deviceId}/policy-assignment`, { versionId })
      .then((r) => r.data),
  unassign: (deviceId: string) =>
    http.delete<Assignment>(`/devices/${deviceId}/policy-assignment`).then((r) => r.data),
};
