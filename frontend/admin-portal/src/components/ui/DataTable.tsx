import type { ReactNode } from "react";

type DataTableProps<T> = {
  columns: Array<{ key: keyof T | string; label: string; render?: (row: T) => ReactNode }>;
  rows: T[];
  fallback: string;
  onRowClick?: (row: T) => void;
};

export function DataTable<T extends Record<string, unknown>>({
  columns,
  rows,
  fallback,
  onRowClick,
}: DataTableProps<T>) {
  if (rows.length === 0) {
    return <div className="table-empty">{fallback}</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={String(column.key)}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr
              key={String(row.id ?? row.userId ?? row.auditId ?? index)}
              className={onRowClick ? "clickable-row" : undefined}
              onClick={() => onRowClick?.(row)}
            >
              {columns.map((column) => (
                <td key={String(column.key)}>
                  {column.render ? column.render(row) : String(row[column.key] ?? "-")}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
