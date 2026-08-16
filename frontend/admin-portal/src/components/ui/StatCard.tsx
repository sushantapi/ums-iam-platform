import type { LucideIcon } from "lucide-react";

export function StatCard({
  label,
  value,
  icon: Icon,
  helper,
}: {
  label: string;
  value: number | string;
  icon: LucideIcon;
  helper?: string;
}) {
  return (
    <article className="metric-card">
      <Icon size={22} />
      <span>{label}</span>
      <strong>{value}</strong>
      {helper && <small>{helper}</small>}
    </article>
  );
}
