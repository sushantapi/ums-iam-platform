import {
  hrmsGatewayBlob,
  hrmsGatewayMultipart,
  hrmsGatewayRequest,
} from "./hrmsGatewayClient";

export type OrganizationProfileResponse = {
  organizationId: string;
  legalName: string | null;
  displayName: string | null;
  registeredAddress: string | null;
  businessEmail: string | null;
  businessPhone: string | null;
  website: string | null;
  defaultCurrency: string | null;
  payrollCountry: string | null;
  payslipFooterText: string | null;
  authorizedSignatoryLabel: string | null;
  logoAssetId: string | null;
  logoAssetVersion: number | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type UpdateOrganizationProfileRequest = {
  legalName: string | null;
  displayName: string | null;
  registeredAddress: string | null;
  businessEmail: string | null;
  businessPhone: string | null;
  website: string | null;
  defaultCurrency: string | null;
  payrollCountry: string | null;
  payslipFooterText: string | null;
  authorizedSignatoryLabel: string | null;
};

export type OrganizationLogoAssetResponse = {
  id: string;
  organizationId: string;
  version: number;
  contentType: string;
  byteSize: number;
  sha256: string;
  createdAt: string | null;
};

export const companyProfileApi = {
  get: (organizationId: string) =>
    hrmsGatewayRequest<OrganizationProfileResponse>(
      `/api/v1/organizations/${organizationId}/profile`,
    ),

  update: (organizationId: string, body: UpdateOrganizationProfileRequest) =>
    hrmsGatewayRequest<OrganizationProfileResponse>(
      `/api/v1/organizations/${organizationId}/profile`,
      {
        method: "PUT",
        body: JSON.stringify(body),
      },
    ),

  uploadLogo: (organizationId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return hrmsGatewayMultipart<OrganizationLogoAssetResponse>(
      `/api/v1/organizations/${organizationId}/profile/logo`,
      formData,
    );
  },

  getLogo: (organizationId: string, assetId: string) =>
    hrmsGatewayBlob(
      `/api/v1/organizations/${organizationId}/profile/logo/${assetId}`,
      "image/*",
    ),
};
