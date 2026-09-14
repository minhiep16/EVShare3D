import { create } from 'zustand';
import { AppError } from './types';

interface ErrorState {
  errors: AppError[];
  fatalError: AppError | null;

  // Actions
  addError: (error: Omit<AppError, 'id' | 'timestamp'>) => string;
  dismissError: (id: string) => void;
  clearErrors: () => void;
  setFatalError: (error: AppError | null) => void;
}

export const useErrorStore = create<ErrorState>((set) => ({
  errors: [],
  fatalError: null,

  addError: (error) => {
    const id = `err_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const newError: AppError = {
      ...error,
      id,
      timestamp: Date.now(),
    };

    set((state) => ({
      errors: [...state.errors, newError],
      fatalError: error.fatal ? newError : state.fatalError,
    }));

    return id;
  },

  dismissError: (id) =>
    set((state) => ({
      errors: state.errors.filter((e) => e.id !== id),
      fatalError: state.fatalError?.id === id ? null : state.fatalError,
    })),

  clearErrors: () =>
    set({
      errors: [],
      fatalError: null,
    }),

  setFatalError: (fatalError) => set({ fatalError }),
}));
