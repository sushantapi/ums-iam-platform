import type { ReactNode } from "react";

export function EntitySummaryCard({
  label,
  value,
  meta,
}: {
  label: string;
  value: ReactNode;
  meta?: ReactNode;
}) {
  return (
    <article className="entity-summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
      {meta && <small>{meta}</small>}
    </article>
  );
}
