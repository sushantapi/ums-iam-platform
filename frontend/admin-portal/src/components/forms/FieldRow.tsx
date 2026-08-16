import type { ReactNode } from "react";

export function FieldRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="field-row">
      <span>{label}</span>
      {children}
    </label>
  );
}
