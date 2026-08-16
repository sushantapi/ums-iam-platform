export function ErrorState({ message }: { message: string }) {
  return <div className="notice notice-error">{message}</div>;
}
