/**
 * Interaction Pipeline Types
 * Strict separation: Mesh components only render visuals;
 * Pipeline handles permissions, validations, actions, and state updates.
 */

export type VisualInteractionState =
  | 'IDLE'
  | 'HOVER'
  | 'ACTIVE'
  | 'SELECTED'
  | 'DISABLED'
  | 'LOADING'
  | 'SUCCESS'
  | 'ERROR';

export interface InteractionContext {
  targetId: string;
  playerPosition: [number, number, number];
  targetPosition: [number, number, number];
  distanceToPlayer: number;
  userRoles: string[];
  isAuthenticated: boolean;
  userId: number | null;
}

export interface ValidationResult {
  allowed: boolean;
  reason?: string;
}

export interface InteractionRequirement {
  requiredRoles?: string[];
  maxInteractionDistance?: number; // Distance in 3D units player must be within
  disabled?: boolean;
  disabledReason?: string;
  customValidator?: (context: InteractionContext) => ValidationResult | boolean;
}

export interface InteractionDefinition {
  id: string;
  name: string;
  actionType: string;
  targetPosition: [number, number, number];
  requirements?: InteractionRequirement;
  onActivate?: (context: InteractionContext) => Promise<void> | void;
  onSelect?: (context: InteractionContext) => void;
  onHoverChange?: (isHovered: boolean, context: InteractionContext) => void;
}
