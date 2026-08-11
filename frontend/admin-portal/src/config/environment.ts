interface Config {
  apiBaseUrl: string;
  environment: 'development' | 'production' | 'staging';
  isDevelopment: boolean;
  isProduction: boolean;
}

const getConfig = (): Config => {
  const environment = (import.meta.env.VITE_ENV || 'development') as 'development' | 'production' | 'staging';

  return {
    apiBaseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
    environment,
    isDevelopment: environment === 'development',
    isProduction: environment === 'production',
  };
};

export const config = getConfig();
export default config;
