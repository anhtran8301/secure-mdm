import type { User } from 'src/types/api';

import { create } from 'zustand';

type AuthState = {
  user: User | null;
  /** Access token lives in memory only; it is never persisted. */
  accessToken: string | null;
  /** False until the initial silent refresh settles. */
  ready: boolean;
  setSession: (user: User, accessToken: string) => void;
  clearSession: () => void;
};

export const useAuthStore = create<AuthState>()((set) => ({
  user: null,
  accessToken: null,
  ready: false,
  setSession: (user, accessToken) => set({ user, accessToken, ready: true }),
  clearSession: () => set({ user: null, accessToken: null, ready: true }),
}));
