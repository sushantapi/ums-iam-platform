import type { ReactNode } from "react";

export function PageActionBar({ children }: { children: ReactNode }) {
  return <div className="page-action-bar">{children}</div>;
}
