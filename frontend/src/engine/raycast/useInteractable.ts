import { useEffect, useRef } from 'react';
import * as THREE from 'three';
import { InteractableConfig } from './raycastTypes';
import { InteractableRegistry } from './interactableRegistry';

export interface UseInteractableProps extends Omit<InteractableConfig, 'object'> {
  ref: React.RefObject<THREE.Object3D>;
}

export const useInteractable = ({
  id,
  ref,
  priority = 0,
  cursor = 'POINTER',
  enabled = true,
  callbacks,
}: UseInteractableProps): void => {
  const callbacksRef = useRef(callbacks);
  callbacksRef.current = callbacks;

  useEffect(() => {
    const object = ref.current;
    if (!object || !enabled) return;

    const unregister = InteractableRegistry.register({
      id,
      object,
      priority,
      cursor,
      enabled,
      callbacks: {
        onHoverEnter: (hit) => callbacksRef.current?.onHoverEnter?.(hit),
        onHoverLeave: () => callbacksRef.current?.onHoverLeave?.(),
        onClick: (hit) => callbacksRef.current?.onClick?.(hit),
        onPointerDown: (hit) => callbacksRef.current?.onPointerDown?.(hit),
        onPointerUp: () => callbacksRef.current?.onPointerUp?.(),
      },
    });

    return () => {
      unregister();
    };
  }, [id, ref, priority, cursor, enabled]);
};
