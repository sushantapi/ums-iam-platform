import type { ReactNode } from "react";

export function FormSection({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <section className="form-section">
      <div>
        <h2>{title}</h2>
        {description && <p>{description}</p>}
      </div>
      <div className="form-section-body">{children}</div>
    </section>
  );
}
