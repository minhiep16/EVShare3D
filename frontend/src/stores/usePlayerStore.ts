import { create } from 'zustand';
import { MovementMode } from './types';

interface PlayerState {
  // Spatial Coordinates & Physics
  position: [number, number, number];
  rotation: [number, number, number]; // Euler angles [pitch, yaw, roll]
  velocity: [number, number, number];
  isGrounded: boolean;

  // Movement & Avatar State
  movementMode: MovementMode;
  walkSpeed: number;
  runSpeed: number;
  isSprinting: boolean;

  // Vehicle Occupancy
  occupiedVehicleId: number | null;
  seatIndex: number | null; // 0 for driver, 1+ for passengers

  // Actions
  setPosition: (pos: [number, number, number]) => void;
  setRotation: (rot: [number, number, number]) => void;
  setVelocity: (vel: [number, number, number]) => void;
  setIsGrounded: (grounded: boolean) => void;
  setMovementMode: (mode: MovementMode) => void;
  setIsSprinting: (sprinting: boolean) => void;
  enterVehicle: (vehicleId: number, seatIndex?: number) => void;
  exitVehicle: () => void;
  teleportTo: (position: [number, number, number], rotation?: [number, number, number]) => void;
}

export const usePlayerStore = create<PlayerState>((set) => ({
  position: [0, 0, 0],
  rotation: [0, 0, 0],
  velocity: [0, 0, 0],
  isGrounded: true,

  movementMode: 'IDLE',
  walkSpeed: 4.5,
  runSpeed: 8.5,
  isSprinting: false,

  occupiedVehicleId: null,
  seatIndex: null,

  setPosition: (position) => set({ position }),
  setRotation: (rotation) => set({ rotation }),
  setVelocity: (velocity) => set({ velocity }),
  setIsGrounded: (isGrounded) => set({ isGrounded }),
  setMovementMode: (movementMode) => set({ movementMode }),
  setIsSprinting: (isSprinting) => set({ isSprinting }),

  enterVehicle: (occupiedVehicleId, seatIndex = 0) =>
    set({
      occupiedVehicleId,
      seatIndex,
      movementMode: 'DRIVING',
    }),

  exitVehicle: () =>
    set({
      occupiedVehicleId: null,
      seatIndex: null,
      movementMode: 'IDLE',
    }),

  teleportTo: (position, rotation) =>
    set((state) => ({
      position,
      rotation: rotation ?? state.rotation,
      velocity: [0, 0, 0],
    })),
}));
