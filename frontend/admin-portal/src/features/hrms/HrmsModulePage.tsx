import { PageHeader } from "../../components/ui/PageHeader";

type HrmsModule =
  | "employees"
  | "departments"
  | "designations"
  | "attendance"
  | "leave"
  | "payroll";

const moduleCopy: Record<
  HrmsModule,
  { title: string; description: string }
> = {
  employees: {
    title: "Employees",
    description:
      "Tenant-scoped employee management using the existing IAM identity and HRMS employee foundation.",
  },
  departments: {
    title: "Departments",
    description:
      "Tenant-scoped department master data for employee assignment and HRMS organization structure.",
  },
  designations: {
    title: "Designations",
    description:
      "Tenant-scoped designation master data for employee role and job-title references.",
  },
  attendance: {
    title: "Attendance",
    description:
      "Daily attendance management backed by the existing attendance-service Gateway APIs.",
  },
  leave: {
    title: "Leave",
    description:
      "Leave requests and permitted approval-state transitions backed by the leave-service Gateway APIs.",
  },
  payroll: {
    title: "Payroll",
    description:
      "Salary structures, payroll runs, immutable entries, and payslip snapshots backed by payroll-service.",
  },
};

export function HrmsModulePage({ module }: { module: HrmsModule }) {
  const copy = moduleCopy[module];

  return (
    <section className="page">
      <PageHeader
        eyebrow="HRMS Basic v1"
        title={copy.title}
        description={copy.description}
      />

      <section className="panel">
        <h2>Integration foundation ready</h2>
        <p>
          This authenticated, permission-guarded route is wired into the existing
          UMS admin portal. The module business workflow will be connected to the
          API Gateway in the next implementation increment.
        </p>
      </section>
    </section>
  );
}
