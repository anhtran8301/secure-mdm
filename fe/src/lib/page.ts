/** Must match AppConstants.DEFAULT_PAGE_SIZE on the backend. */
export const PAGE_SIZE = 20;

export function pageQuery(params: Record<string, string | number | undefined>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== '')
  );
}
