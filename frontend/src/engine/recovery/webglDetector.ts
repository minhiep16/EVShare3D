export interface WebGLSupportReport {
  isWebGL2Available: boolean;
  isWebGL1Available: boolean;
  isSupported: boolean;
  rendererString: string;
  vendorString: string;
  isSoftwareRasterizer: boolean;
  maxTextureSize: number;
  maxRenderBufferSize: number;
  maxVertexAttribs: number;
  supportedExtensionsCount: number;
  errorMessage: string | null;
}

const SOFTWARE_RASTERIZER_KEYWORDS = [
  'swiftshader',
  'llvmpipe',
  'softpipe',
  'software',
  'virtualbox',
  'mesa off-screen',
  'microsoft basic render',
];

/**
 * Probes browser WebGL capabilities and detects hardware acceleration issues.
 */
export function checkWebGLSupport(): WebGLSupportReport {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return {
      isWebGL2Available: false,
      isWebGL1Available: false,
      isSupported: false,
      rendererString: 'HEADLESS_ENV',
      vendorString: 'NONE',
      isSoftwareRasterizer: false,
      maxTextureSize: 0,
      maxRenderBufferSize: 0,
      maxVertexAttribs: 0,
      supportedExtensionsCount: 0,
      errorMessage: 'Window or document is undefined in current environment.',
    };
  }

  let gl: WebGLRenderingContext | WebGL2RenderingContext | null = null;
  let isWebGL2 = false;
  let isWebGL1 = false;
  let errorMessage: string | null = null;

  try {
    const canvas = document.createElement('canvas');

    // Try WebGL2 first
    gl = canvas.getContext('webgl2');
    if (gl) {
      isWebGL2 = true;
      isWebGL1 = true;
    } else {
      // Fallback to WebGL1
      gl = canvas.getContext('webgl') || (canvas.getContext('experimental-webgl') as WebGLRenderingContext);
      if (gl) {
        isWebGL1 = true;
      }
    }
  } catch (err) {
    errorMessage = err instanceof Error ? err.message : 'Context creation failed unexpectedly.';
  }

  if (!gl) {
    return {
      isWebGL2Available: false,
      isWebGL1Available: false,
      isSupported: false,
      rendererString: 'UNAVAILABLE',
      vendorString: 'UNAVAILABLE',
      isSoftwareRasterizer: false,
      maxTextureSize: 0,
      maxRenderBufferSize: 0,
      maxVertexAttribs: 0,
      supportedExtensionsCount: 0,
      errorMessage: errorMessage || 'No WebGL context could be created. Hardware acceleration may be disabled.',
    };
  }

  // Query hardware details
  let rendererString = 'UNKNOWN';
  let vendorString = 'UNKNOWN';

  const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
  if (debugInfo) {
    rendererString = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) || 'UNKNOWN';
    vendorString = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) || 'UNKNOWN';
  } else {
    rendererString = gl.getParameter(gl.RENDERER) || 'UNKNOWN';
    vendorString = gl.getParameter(gl.VENDOR) || 'UNKNOWN';
  }

  const isSoftwareRasterizer = SOFTWARE_RASTERIZER_KEYWORDS.some((kw) =>
    rendererString.toLowerCase().includes(kw)
  );

  const maxTextureSize = gl.getParameter(gl.MAX_TEXTURE_SIZE) || 0;
  const maxRenderBufferSize = gl.getParameter(gl.MAX_RENDERBUFFER_SIZE) || 0;
  const maxVertexAttribs = gl.getParameter(gl.MAX_VERTEX_ATTRIBS) || 0;
  const supportedExtensionsCount = gl.getSupportedExtensions()?.length || 0;

  return {
    isWebGL2Available: isWebGL2,
    isWebGL1Available: isWebGL1,
    isSupported: isWebGL1 || isWebGL2,
    rendererString,
    vendorString,
    isSoftwareRasterizer,
    maxTextureSize,
    maxRenderBufferSize,
    maxVertexAttribs,
    supportedExtensionsCount,
    errorMessage: null,
  };
}
