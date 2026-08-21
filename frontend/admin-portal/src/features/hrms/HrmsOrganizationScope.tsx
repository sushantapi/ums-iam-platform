import { useEffect, useState } from "react";
import { FilterBar } from "../../components/ui/FilterBar";
import { setStoredHrmsOrganizationId } from "./hrmsOrganizationScopeStorage";

export function HrmsOrganizationScope({
  organizationId,
  onChange,
}: {
  organizationId: string;
  onChange: (organizationId: string) => void;
}) {
  const [value, setValue] = useState(organizationId);

  useEffect(() => {
    setValue(organizationId);
  }, [organizationId]);

  function apply() {
    const normalized = value.trim();
    setStoredHrmsOrganizationId(normalized);
    onChange(normalized);
  }

  return (
    <FilterBar>
      <label>
        Organization ID
        <input
          value={value}
          placeholder="Tenant organization UUID"
          onChange={(event) => setValue(event.target.value)}
        />
      </label>
      <button type="button" className="button-primary" onClick={apply}>
        Apply organization
      </button>
    </FilterBar>
  );
}
