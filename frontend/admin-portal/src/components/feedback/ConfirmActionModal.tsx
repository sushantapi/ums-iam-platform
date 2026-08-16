export function ConfirmActionModal({
  open,
  title,
  body,
  onCancel,
  onConfirm,
  pending = false,
  confirmLabel = "Confirm",
}: {
  open: boolean;
  title: string;
  body: string;
  onCancel: () => void;
  onConfirm: () => void;
  pending?: boolean;
  confirmLabel?: string;
}) {
  if (!open) return null;

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <h2 id="confirm-title">{title}</h2>
        <p>{body}</p>
        <div className="action-row">
          <button className="button-secondary" type="button" onClick={onCancel} disabled={pending}>
            Cancel
          </button>
          <button className="button-primary" type="button" onClick={onConfirm} disabled={pending}>
            {pending ? "Working..." : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
