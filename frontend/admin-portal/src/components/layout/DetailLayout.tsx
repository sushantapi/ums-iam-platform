import type { ReactNode } from "react";
import { PageHeader } from "../ui/PageHeader";

export function DetailLayout({
  eyebrow,
  title,
  description,
  actions,
  summary,
  children,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  actions?: ReactNode;
  summary?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="page">
      <PageHeader eyebrow={eyebrow} title={title} description={description ?? ""} actions={actions} />
      {summary}
      {children}
    </section>
  );
}
