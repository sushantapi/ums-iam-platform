export function Pagination({
  page,
  size,
  totalElements,
  onPageChange,
}: {
  page: number;
  size: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}) {
  const totalPages = Math.max(1, Math.ceil(totalElements / size));

  return (
    <div className="pagination">
      <span>
        Page {page + 1} of {totalPages} · {totalElements} records
      </span>
      <div className="action-row">
        <button
          className="button-secondary"
          type="button"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        <button
          className="button-secondary"
          type="button"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
