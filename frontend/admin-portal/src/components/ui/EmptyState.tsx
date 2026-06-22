import { Construction } from "lucide-react";

export function EmptyState({ title, body }: { title: string; body: string }) {
  return (
    <div className="empty-state">
      <Construction size={28} />
      <h2>{title}</h2>
      <p>{body}</p>
    </div>
  );
}
