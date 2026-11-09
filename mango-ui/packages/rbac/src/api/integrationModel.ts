import { get, post } from '@mango/common/utils/request';

export interface IntegrationConnector {
  connectorCode: string;
  connectorName: string;
  category?: string;
  provider?: string;
  status?: number;
  source?: string;
  configSchema?: string;
  authSchema?: string;
  capabilitiesJson?: string;
  tags?: string[];
  docsUrl?: string;
  instanceCount?: number;
  updatedAt?: string;
}

export interface IntegrationCredential {
  credentialCode: string;
  connectorCode: string;
  maskedFieldsJson?: string;
  fieldVersionsJson?: string;
  rotatedAt?: string;
  expiresAt?: string;
  status?: string;
  remark?: string;
}

export interface IntegrationInstance {
  instanceCode: string;
  instanceName: string;
  connectorCode: string;
  environment?: string;
  configValuesJson?: string;
  credentialCode?: string;
  capabilitiesJson?: string;
  callbackPath?: string;
  status?: string;
  connectorEnabled?: boolean;
  owner?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface IntegrationConnectorPage {
  list: IntegrationConnector[];
  total: number;
  page: number;
  size: number;
  pages: number;
}

export interface IntegrationInstancePage {
  list: IntegrationInstance[];
  total: number;
  page: number;
  size: number;
  pages: number;
}

export interface IntegrationInstancePayload {
  instanceCode: string;
  instanceName: string;
  connectorCode: string;
  environment?: string;
  configValuesJson?: string;
  credentialCode?: string;
  capabilitiesJson?: string;
  status: number;
  owner?: string;
  remark?: string;
}

export interface IntegrationConnectorPayload {
  connectorCode: string;
  connectorName: string;
  category?: string;
  provider?: string;
  status?: number;
  source?: string;
  configSchema?: string;
  authSchema?: string;
  capabilitiesJson?: string;
  tags?: string[];
  docsUrl?: string;
}

export const integrationConnectorApi = {
  page: (params: { page?: number; size?: number; keyword?: string } = {}) =>
    get<IntegrationConnectorPage>('/integration/connectors/page', { params }),
  detail: (connectorCode: string) =>
    get<IntegrationConnector>('/integration/connectors/detail', { params: { connectorCode } }),
  save: (data: IntegrationConnectorPayload) => post<boolean>('/integration/connectors', data),
};

export const integrationCredentialApi = {
  list: (connectorCode?: string) =>
    get<IntegrationCredential[]>('/integration/credentials', { params: { connectorCode } }),
  save: (data: {
    credentialCode: string;
    connectorCode: string;
    fieldsJson?: string;
    expiresAt?: string;
    remark?: string;
  }) => post<boolean>('/integration/credentials', data),
  rotate: (credentialCode: string, fieldsJson?: string) =>
    post<boolean>('/integration/credentials/rotate', { credentialCode, fieldsJson }),
  rotations: (credentialCode: string) =>
    get<
      Array<{ credentialCode: string; version: number; fieldCodes?: string; operatorId?: number; rotatedAt?: string }>
    >('/integration/credentials/rotations', { params: { credentialCode } }),
};

export const integrationMaintenanceApi = {
  cleanup: (resource: 'CALL_LOG' | 'ACCESS_LOG' | 'ROTATION', retentionDays: number) =>
    post<number>('/integration/maintenance/cleanup', { resource, retentionDays }),
};

export const integrationInstanceApi = {
  page: (params: { page?: number; size?: number; keyword?: string; connectorCode?: string; status?: number } = {}) =>
    get<IntegrationInstancePage>('/integration/instances/page', { params }),
  detail: (instanceCode: string) =>
    get<IntegrationInstance>('/integration/instances/detail', { params: { instanceCode } }),
  save: (data: IntegrationInstancePayload) => post<boolean>('/integration/instances', data),
  changeStatus: (instanceCode: string, status: number) =>
    post<boolean>('/integration/instances/change-status', { instanceCode, status }),
  testConnection: (instanceCode: string, testPath = '/') =>
    post<{ success: boolean; statusCode?: number; elapsedMs?: number; message?: string }>(
      '/integration/instances/test-connection',
      { instanceCode, testPath },
    ),
  callLogs: (instanceCode: string, limit = 50) =>
    get<Array<Record<string, unknown>>>('/integration/instances/call-logs', { params: { instanceCode, limit } }),
};
