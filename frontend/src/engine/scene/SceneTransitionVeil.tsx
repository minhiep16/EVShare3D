import React from 'react';
import { useLoadingStore } from '@/stores/useLoadingStore';

export const SceneTransitionVeil: React.FC = () => {
  const isTransitioning = useLoadingStore((state) => state.isLoading('scene_transition'));
  const progress = useLoadingStore((state) => state.progress);
  const message = useLoadingStore((state) => state.loadingMessage);

  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        pointerEvents: isTransitioning ? 'all' : 'none',
        opacity: isTransitioning ? 1 : 0,
        transition: 'opacity 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        background: 'radial-gradient(ellipse at center, rgba(16, 20, 34, 0.88) 0%, rgba(6, 7, 10, 0.96) 100%)',
        backdropFilter: isTransitioning ? 'blur(8px)' : 'none',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 50,
        color: '#f0f4fc',
      }}
    >
      <div
        style={{
          width: '320px',
          textAlign: 'center',
          transform: isTransitioning ? 'scale(1)' : 'scale(0.95)',
          transition: 'transform 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
      >
        <div
          style={{
            fontFamily: "'Orbitron', sans-serif",
            fontSize: '0.85rem',
            color: '#00e5ff',
            letterSpacing: '2px',
            marginBottom: '0.75rem',
            textTransform: 'uppercase',
          }}
        >
          SECTOR TRANSITION
        </div>

        <div
          style={{
            fontFamily: "'Inter', sans-serif",
            fontSize: '0.85rem',
            color: '#8a94a6',
            marginBottom: '1.25rem',
            minHeight: '1.2rem',
          }}
        >
          {message || 'Reconfiguring spatial shaders...'}
        </div>

        {/* Cyber Progress Bar */}
        <div
          style={{
            width: '100%',
            height: '4px',
            background: 'rgba(255, 255, 255, 0.08)',
            borderRadius: '2px',
            overflow: 'hidden',
            border: '1px solid rgba(0, 229, 255, 0.2)',
          }}
        >
          <div
            style={{
              width: `${progress}%`,
              height: '100%',
              background: 'linear-gradient(90deg, #00e5ff 0%, #00e676 100%)',
              boxShadow: '0 0 10px #00e5ff',
              transition: 'width 0.25s ease-out',
            }}
          />
        </div>

        <div
          style={{
            marginTop: '0.5rem',
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '0.75rem',
            color: '#00e5ff',
            textAlign: 'right',
          }}
        >
          {Math.round(progress)}%
        </div>
      </div>
    </div>
  );
};
