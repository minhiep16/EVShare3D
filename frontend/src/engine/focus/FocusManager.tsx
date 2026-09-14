import React, { useEffect } from 'react';
import { useFocusStore } from './useFocusStore';
import { InputDispatcher } from '../input/inputDispatcher';

export const FocusManager: React.FC = () => {
  const isFocused = useFocusStore((state) => state.isFocused);
  const returnToPreviousCamera = useFocusStore((state) => state.returnToPreviousCamera);

  // Pressing ESCAPE or Cancel action while focused smoothly returns to previous camera
  useEffect(() => {
    const unregister = InputDispatcher.onAction('CANCEL', () => {
      if (useFocusStore.getState().isFocused) {
        useFocusStore.getState().returnToPreviousCamera();
      }
    });

    return () => {
      unregister();
    };
  }, [returnToPreviousCamera]);

  return null;
};
