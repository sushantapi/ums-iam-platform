import { FormEvent, useEffect, useState } from "react";

import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import {
  companyProfileApi,
  type OrganizationProfileResponse,
  type UpdateOrganizationProfileRequest,
} from "./companyProfileApi";
import "./CompanySettingsPage.css";
import { HrmsOrganizationScope } from "./HrmsOrganizationScope";
import { getStoredHrmsOrganizationId } from "./hrmsOrganizationScopeStorage";

type ProfileDraft = {
  legalName: string;
  displayName: string;
  registeredAddress: string;
  businessEmail: string;
  businessPhone: string;
  website: string;
  defaultCurrency: string;
  payrollCountry: string;
  payslipFooterText: string;
  authorizedSignatoryLabel: string;
};

const EMPTY_DRAFT: ProfileDraft = {
  legalName: "",
  displayName: "",
  registeredAddress: "",
  businessEmail: "",
  businessPhone: "",
  website: "",
  defaultCurrency: "INR",
  payrollCountry: "IN",
  payslipFooterText: "",
  authorizedSignatoryLabel: "",
};

function toDraft(profile: OrganizationProfileResponse): ProfileDraft {
  return {
    legalName: profile.legalName ?? "",
    displayName: profile.displayName ?? "",
    registeredAddress: profile.registeredAddress ?? "",
    businessEmail: profile.businessEmail ?? "",
    businessPhone: profile.businessPhone ?? "",
    website: profile.website ?? "",
    defaultCurrency: profile.defaultCurrency ?? "INR",
    payrollCountry: profile.payrollCountry ?? "IN",
    payslipFooterText: profile.payslipFooterText ?? "",
    authorizedSignatoryLabel: profile.authorizedSignatoryLabel ?? "",
  };
}

function nullable(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}

export function CompanySettingsPage() {
  const [organizationId, setOrganizationId] = useState(
    getStoredHrmsOrganizationId(),
  );
  const [profile, setProfile] = useState<OrganizationProfileResponse>();
  const [draft, setDraft] = useState<ProfileDraft>(EMPTY_DRAFT);
  const [logoFile, setLogoFile] = useState<File>();
  const [logoUrl, setLogoUrl] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string>();
  const [logoError, setLogoError] = useState<string>();
  const [notice, setNotice] = useState<string>();

  async function loadProfile(id: string) {
    if (!id) {
      setProfile(undefined);
      setDraft(EMPTY_DRAFT);
      return;
    }

    setLoading(true);
    setError(undefined);
    try {
      const response = await companyProfileApi.get(id);
      setProfile(response);
      setDraft(toDraft(response));
    } catch (err) {
      setError(`Company profile could not be loaded: ${(err as Error).message}`);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadProfile(organizationId);
  }, [organizationId]);

  useEffect(() => {
    let objectUrl: string | undefined;
    let cancelled = false;

    async function loadLogo() {
      setLogoUrl(undefined);
      setLogoError(undefined);
      if (!organizationId || !profile?.logoAssetId) {
        return;
      }

      try {
        const blob = await companyProfileApi.getLogo(
          organizationId,
          profile.logoAssetId,
        );
        if (cancelled) {
          return;
        }
        objectUrl = URL.createObjectURL(blob);
        setLogoUrl(objectUrl);
      } catch (err) {
        if (!cancelled) {
          setLogoError(`Company logo could not be loaded: ${(err as Error).message}`);
        }
      }
    }

    void loadLogo();
    return () => {
      cancelled = true;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [organizationId, profile?.logoAssetId]);

  function setField<K extends keyof ProfileDraft>(field: K, value: ProfileDraft[K]) {
    setDraft((current) => ({ ...current, [field]: value }));
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!organizationId) {
      setError("Select an organization before managing company settings.");
      return;
    }

    const body: UpdateOrganizationProfileRequest = {
      legalName: nullable(draft.legalName),
      displayName: nullable(draft.displayName),
      registeredAddress: nullable(draft.registeredAddress),
      businessEmail: nullable(draft.businessEmail),
      businessPhone: nullable(draft.businessPhone),
      website: nullable(draft.website),
      defaultCurrency: nullable(draft.defaultCurrency)?.toUpperCase() ?? null,
      payrollCountry: nullable(draft.payrollCountry)?.toUpperCase() ?? null,
      payslipFooterText: nullable(draft.payslipFooterText),
      authorizedSignatoryLabel: nullable(draft.authorizedSignatoryLabel),
    };

    setSaving(true);
    setError(undefined);
    setNotice(undefined);
    try {
      const response = await companyProfileApi.update(organizationId, body);
      setProfile(response);
      setDraft(toDraft(response));
      setNotice("Company profile saved. New payroll runs will snapshot these details.");
    } catch (err) {
      setError(`Company profile could not be saved: ${(err as Error).message}`);
    } finally {
      setSaving(false);
    }
  }

  async function uploadLogo(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!organizationId || !logoFile) {
      setLogoError("Choose a PNG or JPEG logo before uploading.");
      return;
    }

    setUploading(true);
    setLogoError(undefined);
    setNotice(undefined);
    try {
      await companyProfileApi.uploadLogo(organizationId, logoFile);
      setLogoFile(undefined);
      await loadProfile(organizationId);
      setNotice("Company logo uploaded as a new immutable version.");
    } catch (err) {
      setLogoError(`Company logo could not be uploaded: ${(err as Error).message}`);
    } finally {
      setUploading(false);
    }
  }

  return (
    <section className="page company-settings-page">
      <PageHeader
        eyebrow="HRMS"
        title="Company Settings"
        description="Maintain reusable company identity and payslip branding for this tenant. Finalized payroll snapshots keep their historical values."
      />

      <section className="company-settings-context" aria-label="Tenant context">
        <div className="company-settings-context-copy">
          <span>Tenant context</span>
          <strong>{profile?.displayName || profile?.legalName || "Selected organization"}</strong>
          <small>
            Company identity is reused across HRMS and captured into finalized payroll snapshots.
          </small>
        </div>
        <HrmsOrganizationScope
          organizationId={organizationId}
          onChange={setOrganizationId}
        />
      </section>

      {!organizationId ? (
        <ErrorState message="Enter the tenant organization ID to manage company settings." />
      ) : null}
      {loading ? <LoadingState label="Loading company settings" /> : null}
      {error ? <ErrorState message={error} /> : null}
      {notice ? (
        <p role="status" className="success-message company-settings-notice">
          {notice}
        </p>
      ) : null}

      {organizationId && profile && !loading ? (
        <div className="company-settings-grid">
          <section className="panel company-settings-card">
            <header className="company-settings-card-header">
              <div>
                <span className="company-settings-kicker">Company identity</span>
                <h2>Profile & payslip details</h2>
                <p>
                  Keep company information accurate once and reuse it whenever payroll creates
                  a professional employee-facing payslip.
                </p>
              </div>
              <span className="badge badge-live">Payslip source</span>
            </header>

            <form onSubmit={saveProfile} className="company-settings-form">
              <section className="company-settings-section">
                <div className="company-settings-section-title">
                  <strong>Business identity</strong>
                  <span>Names, contact details and registered business address.</span>
                </div>
                <div className="company-settings-fields company-settings-fields-two">
                  <label className="company-settings-field">
                    Legal company name
                    <input
                      maxLength={255}
                      value={draft.legalName}
                      onChange={(event) => setField("legalName", event.target.value)}
                      disabled={saving}
                    />
                  </label>
                  <label className="company-settings-field">
                    Display name
                    <input
                      maxLength={255}
                      value={draft.displayName}
                      onChange={(event) => setField("displayName", event.target.value)}
                      disabled={saving}
                    />
                  </label>
                  <label className="company-settings-field company-settings-field-wide">
                    Registered address
                    <textarea
                      maxLength={1000}
                      value={draft.registeredAddress}
                      onChange={(event) =>
                        setField("registeredAddress", event.target.value)
                      }
                      disabled={saving}
                    />
                  </label>
                  <label className="company-settings-field">
                    Business email
                    <input
                      type="email"
                      maxLength={255}
                      value={draft.businessEmail}
                      onChange={(event) => setField("businessEmail", event.target.value)}
                      disabled={saving}
                    />
                  </label>
                  <label className="company-settings-field">
                    Business phone
                    <input
                      maxLength={50}
                      value={draft.businessPhone}
                      onChange={(event) => setField("businessPhone", event.target.value)}
                      disabled={saving}
                    />
                  </label>
                  <label className="company-settings-field company-settings-field-wide">
                    Website
                    <input
                      maxLength={255}
                      value={draft.website}
                      onChange={(event) => setField("website", event.target.value)}
                      disabled={saving}
                    />
                  </label>
                </div>
              </section>

              <section className="company-settings-section">
                <div className="company-settings-section-title">
                  <strong>Payroll defaults</strong>
                  <span>Defaults applied when payroll captures a new immutable payslip snapshot.</span>
                </div>
                <div className="company-settings-fields company-settings-fields-two">
                  <label className="company-settings-field">
                    Default currency
                    <input
                      maxLength={3}
                      value={draft.defaultCurrency}
                      onChange={(event) =>
                        setField("defaultCurrency", event.target.value.toUpperCase())
                      }
                      disabled={saving}
                    />
                  </label>
                  <label className="company-settings-field">
                    Payroll country
                    <input
                      maxLength={2}
                      value={draft.payrollCountry}
                      onChange={(event) =>
                        setField("payrollCountry", event.target.value.toUpperCase())
                      }
                      disabled={saving}
                    />
                  </label>
                  <label className="company-settings-field company-settings-field-wide">
                    Payslip footer text
                    <textarea
                      maxLength={500}
                      value={draft.payslipFooterText}
                      onChange={(event) =>
                        setField("payslipFooterText", event.target.value)
                      }
                      disabled={saving}
                    />
                  </label>
                  <label className="company-settings-field company-settings-field-wide">
                    Authorized signatory label
                    <input
                      maxLength={255}
                      value={draft.authorizedSignatoryLabel}
                      onChange={(event) =>
                        setField("authorizedSignatoryLabel", event.target.value)
                      }
                      disabled={saving}
                    />
                  </label>
                </div>
              </section>

              <div className="company-settings-actions">
                <button type="submit" className="button-primary" disabled={saving}>
                  {saving ? "Saving..." : "Save company profile"}
                </button>
              </div>
            </form>
          </section>

          <section className="panel company-settings-card">
            <header className="company-settings-card-header">
              <div>
                <span className="company-settings-kicker">Branding</span>
                <h2>Company logo</h2>
                <p>Use a clean PNG or JPEG logo for employee-facing payroll documents.</p>
              </div>
              {profile.logoAssetVersion ? (
                <span className="badge">Version {profile.logoAssetVersion}</span>
              ) : null}
            </header>

            <div className="company-settings-logo-body">
              <div className="company-settings-logo-preview">
                {logoUrl ? (
                  <img src={logoUrl} alt="Current company logo" />
                ) : (
                  <p className="company-settings-logo-empty">
                    No company logo uploaded yet. Upload a PNG or JPEG to establish your
                    payslip branding.
                  </p>
                )}
              </div>

              <div className="company-settings-logo-meta">
                <span>Current logo version</span>
                <strong>{profile.logoAssetVersion ?? "Not set"}</strong>
              </div>

              <div className="company-settings-info-note">
                Every upload creates a new immutable logo version. Finalized historical
                payslips continue referencing the exact version captured at payroll time.
              </div>

              {logoError ? <ErrorState message={logoError} /> : null}

              <form onSubmit={uploadLogo} className="company-settings-upload-form">
                <label className="company-settings-file-field">
                  Logo file
                  <input
                    type="file"
                    accept="image/png,image/jpeg"
                    onChange={(event) => setLogoFile(event.target.files?.[0])}
                    disabled={uploading}
                  />
                </label>
                <button type="submit" className="button-primary" disabled={uploading}>
                  {uploading ? "Uploading..." : "Upload new logo version"}
                </button>
              </form>
            </div>
          </section>
        </div>
      ) : null}
    </section>
  );
}
