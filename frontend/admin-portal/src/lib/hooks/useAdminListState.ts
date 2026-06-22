import { useSearchParams } from "react-router-dom";

type ListFilters = Record<string, string>;

export function useAdminListState<T extends ListFilters>(defaults: T, defaultSize = 20) {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(0, Number(searchParams.get("page") ?? 0) || 0);
  const size = Math.max(1, Number(searchParams.get("size") ?? defaultSize) || defaultSize);
  const filters = Object.fromEntries(
    Object.keys(defaults).map((key) => [key, searchParams.get(key) ?? defaults[key]]),
  ) as T;

  function update(next: Record<string, string | number | undefined>) {
    setSearchParams((current) => {
      const params = new URLSearchParams(current);
      Object.entries(next).forEach(([key, value]) => {
        if (value === undefined || value === "" || (key === "page" && value === 0)) {
          params.delete(key);
        } else {
          params.set(key, String(value));
        }
      });
      return params;
    }, { replace: true });
  }

  function setFilter<K extends keyof T>(key: K, value: T[K]) {
    update({ [String(key)]: value, page: 0 });
  }

  return {
    page,
    size,
    filters,
    setFilter,
    setPage: (nextPage: number) => update({ page: nextPage }),
    setSize: (nextSize: number) => update({ size: nextSize, page: 0 }),
    reset: () => setSearchParams({}, { replace: true }),
  };
}
