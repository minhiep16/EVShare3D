import React, { Component, ErrorInfo, ReactNode } from 'react';
import { RecoveryScreen } from './recovery/RecoveryScreen';
import { useWebGLRecoveryStore } from './recovery/useWebGLRecoveryStore';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

/**
 * ErrorBoundary3D
 * Traps unhandled WebGL / Three.js render faults, prevents blank screen scenarios,
 * and launches the dedicated RecoveryScreen rather than a 2D dashboard fallback.
 */
export class ErrorBoundary3D extends Component<Props, State> {
  public override state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public override componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('[ErrorBoundary3D] Unhandled 3D render exception caught:', error, errorInfo);
    useWebGLRecoveryStore.getState().triggerRenderError(error);
    this.props.onError?.(error, errorInfo);
  }

  private handleReset = (): void => {
    useWebGLRecoveryStore.getState().retry();
    this.setState({ hasError: false, error: null });
  };

  public override render(): ReactNode {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }
      return <RecoveryScreen error={this.state.error} onRetry={this.handleReset} />;
    }

    return this.props.children;
  }
}
