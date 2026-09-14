import { useEffect } from 'react';
import { FocusTargetConfig } from './focusTypes';
import { FocusRegistry } from './FocusRegistry';

/**
 * Hook enabling future vehicles, terminals, portals, and interactive objects
 * to register their focal inspection profile with the FocusManager.
 */
export const useFocusTarget = (config: FocusTargetConfig): void => {
  useEffect(() => {
    const unregister = FocusRegistry.registerTarget(config);
    return () => {
      unregister();
    };
  }, [
    config.id,
    config.name,
    config.category,
    config.targetPosition,
    config.boundingRadius,
    config.preset,
  ]);
};
