import * as THREE from 'three';
import { CursorMode } from '@/stores/types';

export interface RaycastHit {
  id: string;
  point: [number, number, number];
  normal: [number, number, number] | null;
  distance: number;
  object: THREE.Object3D;
  priority: number;
  uv?: [number, number];
}

export interface InteractableCallbacks {
  onHoverEnter?: (hit: RaycastHit) => void;
  onHoverLeave?: () => void;
  onClick?: (hit: RaycastHit) => void;
  onPointerDown?: (hit: RaycastHit) => void;
  onPointerUp?: () => void;
}

export interface InteractableConfig {
  id: string;
  object: THREE.Object3D;
  priority?: number; // Higher number = higher precedence (e.g., UI=100, Entity=50, Floor=0)
  cursor?: CursorMode;
  enabled?: boolean;
  callbacks?: InteractableCallbacks;
}
