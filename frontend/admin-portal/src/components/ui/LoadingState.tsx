export function LoadingState({ label = "Loading data" }: { label?: string }) {
  return <div className="loading-state">{label}...</div>;
}
