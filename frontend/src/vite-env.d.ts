/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_APP_TITLE: string;
  readonly VITE_ENABLE_3D_AUDIO?: string;
  readonly VITE_DEFAULT_QUALITY?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
