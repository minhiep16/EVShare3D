import React, { useState } from 'react';
import { useWebGLRecoveryStore } from './useWebGLRecoveryStore';

export interface RecoveryScreenProps {
  error?: Error | null;
  onRetry?: () => void;
}

export const RecoveryScreen: React.FC<RecoveryScreenProps> = ({ error: propError, onRetry }) => {
  const status = useWebGLRecoveryStore((state) => state.status);
  const storeError = useWebGLRecoveryStore((state) => state.lastError);
  const retryCount = useWebGLRecoveryStore((state) => state.retryCount);
  const diagnosticReport = useWebGLRecoveryStore((state) => state.diagnosticReport);
  const safeModeActive = useWebGLRecoveryStore((state) => state.safeModeActive);

  const retry = useWebGLRecoveryStore((state) => state.retry);
  const launchSafeMode = useWebGLRecoveryStore((state) => state.launchSafeMode);
  const runDiagnostics = useWebGLRecoveryStore((state) => state.runDiagnostics);

  const [copied, setCopied] = useState(false);

  const activeError = propError || storeError;
  const report = diagnosticReport || runDiagnostics();

  const handleRetry = () => {
    retry();
    onRetry?.();
  };

  const handleSafeMode = () => {
    launchSafeMode();
    onRetry?.();
  };

  const handleCopyDiagnostics = () => {
    const data = {
      timestamp: new Date().toISOString(),
      status,
      retryCount,
      safeModeActive,
      error: activeError ? { name: activeError.name, message: activeError.message, stack: activeError.stack } : null,
      diagnostics: report,
    };
    navigator.clipboard?.writeText(JSON.stringify(data, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  // Status banner copy
  const getBannerText = () => {
    switch (status) {
      case 'CONTEXT_LOST':
        return 'WEBGL GRAPHICS CONTEXT INTERRUPTED';
      case 'INIT_FAILED':
        return '3D HARDWARE INITIALIZATION FAULT';
      case 'UNSUPPORTED':
        return 'HARDWARE ACCELERATION REQUIRED';
      case 'RENDER_ERROR':
      default:
        return 'SPATIAL PIPELINE RENDER EXCEPTION';
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(ellipse at center, #101422 0%, #05070a 100%)',
        color: '#f0f4fc',
        padding: '1.5rem',
        textAlign: 'center',
        zIndex: 99999,
        fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, sans-serif",
      }}
    >
      <div
        style={{
          border: '1px solid rgba(255, 23, 68, 0.45)',
          background: 'rgba(10, 13, 24, 0.92)',
          backdropFilter: 'blur(20px)',
          borderRadius: '16px',
          padding: '2.5rem',
          maxWidth: '680px',
          width: '100%',
          boxShadow: '0 16px 48px rgba(0, 0, 0, 0.8), 0 0 30px rgba(255, 23, 68, 0.25)',
        }}
      >
        {/* Status Badge */}
        <div
          style={{
            display: 'inline-block',
            padding: '4px 14px',
            borderRadius: '999px',
            background: 'rgba(255, 23, 68, 0.15)',
            border: '1px solid rgba(255, 23, 68, 0.6)',
            color: '#ff1744',
            fontSize: '0.78rem',
            fontFamily: "'Orbitron', sans-serif",
            letterSpacing: '1.5px',
            marginBottom: '1rem',
          }}
        >
          {getBannerText()}
        </div>

        <h2
          style={{
            fontFamily: "'Orbitron', sans-serif",
            fontSize: '1.6rem',
            fontWeight: 700,
            letterSpacing: '1px',
            marginBottom: '0.65rem',
            color: '#ffffff',
          }}
        >
          Virtual Metaverse Engine Recovery
        </h2>

        <p
          style={{
            color: '#8a94a6',
            fontSize: '0.88rem',
            lineHeight: 1.5,
            marginBottom: '1.25rem',
          }}
        >
          {status === 'CONTEXT_LOST'
            ? 'The WebGL graphics context was interrupted by the GPU or browser power management. Attempting automatic restoration.'
            : activeError?.message ||
              'A critical rendering exception occurred inside the 3D graphics pipeline. EVShare 3D requires active hardware acceleration.'}
        </p>

        {/* Hardware & Diagnostic Readout */}
        <div
          style={{
            background: 'rgba(4, 6, 10, 0.9)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '8px',
            padding: '1rem',
            textAlign: 'left',
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '0.75rem',
            marginBottom: '1.5rem',
            color: '#8a94a6',
            lineHeight: 1.6,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>WebGL 2.0 Acceleration:</span>
            <span style={{ color: report.isWebGL2Available ? '#00e676' : '#ff1744' }}>
              {report.isWebGL2Available ? 'SUPPORTED' : 'UNAVAILABLE'}
            </span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>GPU Hardware Renderer:</span>
            <span style={{ color: '#00e5ff', maxWidth: '320px', textAlign: 'right', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {report.rendererString}
            </span>
          </div>
          {report.isSoftwareRasterizer && (
            <div style={{ color: '#ffab00', marginTop: '0.25rem' }}>
              <span>Warning: Software CPU rasterizer detected (low performance).</span>
            </div>
          )}
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>Recovery Attempts:</span>
            <span style={{ color: retryCount > 0 ? '#ffab00' : '#8a94a6' }}>
              {retryCount}
            </span>
          </div>
          {activeError && (
            <div style={{ marginTop: '0.5rem', color: '#ff5252', borderTop: '1px dashed rgba(255, 255, 255, 0.1)', paddingTop: '0.4rem' }}>
              <span>Exception: </span>
              <span>{activeError.name}: {activeError.message}</span>
            </div>
          )}
        </div>

        {/* Action Controls */}
        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <button
            onClick={handleRetry}
            style={{
              background: 'linear-gradient(135deg, #00e5ff 0%, #0091ea 100%)',
              color: '#06070a',
              border: 'none',
              borderRadius: '8px',
              padding: '0.7rem 1.4rem',
              fontFamily: "'Orbitron', sans-serif",
              fontWeight: 700,
              fontSize: '0.8rem',
              letterSpacing: '1px',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
            }}
          >
            RETRY INITIALIZATION
          </button>

          <button
            onClick={handleSafeMode}
            style={{
              background: 'rgba(255, 171, 0, 0.15)',
              color: '#ffab00',
              border: '1px solid rgba(255, 171, 0, 0.5)',
              borderRadius: '8px',
              padding: '0.7rem 1.25rem',
              fontFamily: "'Orbitron', sans-serif",
              fontWeight: 600,
              fontSize: '0.8rem',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
            }}
          >
            LAUNCH SAFE MODE (LOW)
          </button>

          <button
            onClick={handleCopyDiagnostics}
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              color: copied ? '#00e676' : '#8a94a6',
              border: copied ? '1px solid #00e676' : '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: '8px',
              padding: '0.7rem 1.1rem',
              fontFamily: "'Orbitron', sans-serif",
              fontSize: '0.75rem',
              cursor: 'pointer',
            }}
          >
            {copied ? 'REPORT COPIED!' : 'COPY REPORT'}
          </button>

          <button
            onClick={() => window.location.reload()}
            style={{
              background: 'transparent',
              color: '#555e6d',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              padding: '0.7rem 1rem',
              fontFamily: "'Orbitron', sans-serif",
              fontSize: '0.75rem',
              cursor: 'pointer',
            }}
          >
            RELOAD
          </button>
        </div>
      </div>
    </div>
  );
};
