import type { ScreenStatus } from "../../features/iam/screenBlueprints";

const statusClass: Record<ScreenStatus | string, string> = {
  "Live starter": "badge badge-live",
  Blueprint: "badge badge-blueprint",
  Roadmap: "badge badge-roadmap",
};

export function StatusBadge({ status }: { status: ScreenStatus | string }) {
  return <span className={statusClass[status] ?? "badge"}>{status}</span>;
}
