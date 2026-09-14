import React from 'react';

interface WebGLFallbackProps {
  error?: Error | null;
  onRetry?: () => void;
}

export const WebGLFallback: React.FC<WebGLFallbackProps> = ({ error, onRetry }) => {
  const isWebgl2 = typeof window !== 'undefined' && (() => {
    try {
      const canvas = document.createElement('canvas');
      return Boolean(canvas.getContext('webgl2'));
    } catch {
      return false;
    }
  })();

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(ellipse at center, #141724 0%, #06070a 100%)',
        color: '#f0f4fc',
        padding: '2rem',
        textAlign: 'center',
        zIndex: 9999,
      }}
    >
      <div
        style={{
          border: '1px solid rgba(255, 23, 68, 0.4)',
          background: 'rgba(16, 20, 34, 0.85)',
          backdropFilter: 'blur(16px)',
          borderRadius: '16px',
          padding: '2.5rem 3rem',
          maxWidth: '640px',
          width: '100%',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.6), 0 0 20px rgba(255, 23, 68, 0.2)',
        }}
      >
        <div
          style={{
            display: 'inline-block',
            padding: '4px 12px',
            borderRadius: '999px',
            background: 'rgba(255, 23, 68, 0.15)',
            border: '1px solid rgba(255, 23, 68, 0.5)',
            color: '#ff1744',
            fontSize: '0.8rem',
            fontFamily: "'Orbitron', sans-serif",
            letterSpacing: '1.5px',
            marginBottom: '1rem',
          }}
        >
          GRAPHICS SUBSYSTEM ERROR
        </div>

        <h2
          style={{
            fontFamily: "'Orbitron', sans-serif",
            fontSize: '1.75rem',
            fontWeight: 700,
            letterSpacing: '1px',
            marginBottom: '0.75rem',
            color: '#ffffff',
          }}
        >
          3D Engine Acceleration Required
        </h2>

        <p
          style={{
            color: '#8a94a6',
            fontSize: '0.95rem',
            lineHeight: 1.6,
            marginBottom: '1.5rem',
          }}
        >
          {error?.message ||
            'EVShare 3D requires hardware-accelerated WebGL 2.0 to render the virtual metaverse complex, spatial shaders, and interactive vehicles.'}
        </p>

        <div
          style={{
            background: 'rgba(6, 7, 10, 0.8)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '8px',
            padding: '1rem',
            textAlign: 'left',
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '0.8rem',
            marginBottom: '1.5rem',
            color: '#8a94a6',
          }}
        >
          <div>
            <span>WebGL 2.0 Support: </span>
            <span style={{ color: isWebgl2 ? '#00e676' : '#ff1744' }}>
              {isWebgl2 ? 'AVAILABLE' : 'UNAVAILABLE / DISABLED'}
            </span>
          </div>
          <div>
            <span>Target Subsystem: </span>
            <span style={{ color: '#00e5ff' }}>Three.js / React Three Fiber</span>
          </div>
          {error && (
            <div style={{ marginTop: '0.5rem', color: '#ff5252' }}>
              <span>Exception: </span>
              <span>{error.name}: {error.message}</span>
            </div>
          )}
        </div>

        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
          {onRetry && (
            <button
              onClick={onRetry}
              style={{
                background: 'linear-gradient(135deg, #00e5ff 0%, #0091ea 100%)',
                color: '#06070a',
                border: 'none',
                borderRadius: '8px',
                padding: '0.75rem 1.75rem',
                fontFamily: "'Orbitron', sans-serif",
                fontWeight: 700,
                fontSize: '0.85rem',
                letterSpacing: '1px',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              RETRY INITIALIZATION
            </button>
          )}
          <button
            onClick={() => window.location.reload()}
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              color: '#f0f4fc',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: '8px',
              padding: '0.75rem 1.5rem',
              fontFamily: "'Orbitron', sans-serif",
              fontSize: '0.85rem',
              cursor: 'pointer',
            }}
          >
            RELOAD BROWSER
          </button>
        </div>
      </div>
    </div>
  );
};
