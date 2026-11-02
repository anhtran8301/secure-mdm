import { create } from 'zustand';

import { PAGE_SIZE } from 'src/lib/page';

type DeviceFilterState = {
  search: string;
  page: number;
  size: number;
  setSearch: (search: string) => void;
  setPage: (page: number) => void;
  setSize: (size: number) => void;
};

/** Keeps the device list filter while navigating to a device and back. */
export const useDeviceFilterStore = create<DeviceFilterState>()((set) => ({
  search: '',
  page: 0,
  size: PAGE_SIZE,
  setSearch: (search) => set({ search, page: 0 }),
  setPage: (page) => set({ page }),
  setSize: (size) => set({ size, page: 0 }),
}));
