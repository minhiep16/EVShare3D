import {
  InteractionContext,
  InteractionDefinition,
  ValidationResult,
  VisualInteractionState,
} from './interactionTypes';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { useUI3DStore } from '@/stores/useUI3DStore';

class InteractionPipelineClass {
  private definitions = new Map<string, InteractionDefinition>();
  private visualStates = new Map<string, VisualInteractionState>();
  private stateSubscribers = new Map<string, Set<(state: VisualInteractionState) => void>>();

  public register(definition: InteractionDefinition): () => void {
    this.definitions.set(definition.id, definition);
    if (!this.visualStates.has(definition.id)) {
      const initial: VisualInteractionState = definition.requirements?.disabled
        ? 'DISABLED'
        : 'IDLE';
      this.setVisualState(definition.id, initial);
    }

    return () => {
      this.definitions.delete(definition.id);
      this.visualStates.delete(definition.id);
      this.stateSubscribers.delete(definition.id);
    };
  }

  public getDefinition(id: string): InteractionDefinition | undefined {
    return this.definitions.get(id);
  }

  public getVisualState(id: string): VisualInteractionState {
    return this.visualStates.get(id) || 'IDLE';
  }

  public setVisualState(id: string, state: VisualInteractionState): void {
    this.visualStates.set(id, state);
    this.stateSubscribers.get(id)?.forEach((cb) => cb(state));
  }

  public onVisualStateChange(
    id: string,
    callback: (state: VisualInteractionState) => void
  ): () => void {
    if (!this.stateSubscribers.has(id)) {
      this.stateSubscribers.set(id, new Set());
    }
    this.stateSubscribers.get(id)?.add(callback);
    return () => {
      this.stateSubscribers.get(id)?.delete(callback);
    };
  }

  /**
   * Stage 4: Permission & Interaction Validation
   */
  public validate(
    definition: InteractionDefinition,
    context: InteractionContext
  ): ValidationResult {
    const req = definition.requirements;
    if (!req) return { allowed: true };

    // 1. Explicitly disabled
    if (req.disabled) {
      return {
        allowed: false,
        reason: req.disabledReason || `${definition.name} is currently disabled.`,
      };
    }

    // 2. Proximity Range Check (3D Distance)
    if (req.maxInteractionDistance !== undefined) {
      if (context.distanceToPlayer > req.maxInteractionDistance) {
        return {
          allowed: false,
          reason: `Too far away. Approach within ${req.maxInteractionDistance.toFixed(1)}m to interact.`,
        };
      }
    }

    // 3. RBAC Role Permission Check
    if (req.requiredRoles && req.requiredRoles.length > 0) {
      const hasRole = req.requiredRoles.some((role) => context.userRoles.includes(role));
      if (!hasRole) {
        return {
          allowed: false,
          reason: `Access Denied. Required role: ${req.requiredRoles.join(' or ')}.`,
        };
      }
    }

    // 4. Custom Contextual Validator
    if (req.customValidator) {
      const customRes = req.customValidator(context);
      if (typeof customRes === 'boolean') {
        if (!customRes) {
          return { allowed: false, reason: 'Interaction conditions not met.' };
        }
      } else if (!customRes.allowed) {
        return customRes;
      }
    }

    return { allowed: true };
  }

  /**
   * Complete 6-Stage Execution Pipeline:
   * user input -> raycast -> target detection -> validation -> action -> state update
   */
  public async execute(
    targetId: string,
    context: InteractionContext
  ): Promise<{ success: boolean; reason?: string }> {
    // Stage 3: Target Detection
    const definition = this.definitions.get(targetId);
    if (!definition) {
      return { success: false, reason: `Target "${targetId}" not registered in interaction pipeline.` };
    }

    // Stage 4: Permission / Interaction Validation
    const validation = this.validate(definition, context);
    if (!validation.allowed) {
      this.setVisualState(targetId, 'DISABLED');
      useUI3DStore.getState().addNotification({
        title: 'Interaction Blocked',
        message: validation.reason || 'Action denied.',
        type: 'WARNING',
        durationMs: 3500,
        worldPosition: definition.targetPosition,
      });
      return { success: false, reason: validation.reason };
    }

    // Stage 5: Action Execution (Decoupled from 3D Mesh Component)
    this.setVisualState(targetId, 'ACTIVE');

    try {
      if (definition.onActivate) {
        await definition.onActivate(context);
      }

      // Stage 6: State Update
      useInteractionStore.getState().setSelectedObjectId(targetId);
      this.setVisualState(targetId, 'SELECTED');
      return { success: true };
    } catch (error) {
      console.error(`[InteractionPipeline] Error executing action for "${targetId}":`, error);
      this.setVisualState(targetId, 'ERROR');
      useUI3DStore.getState().addNotification({
        title: 'Execution Error',
        message: (error as Error).message || 'Failed to complete action.',
        type: 'ERROR',
        durationMs: 4000,
      });
      return { success: false, reason: (error as Error).message };
    }
  }

  public clear(): void {
    this.definitions.clear();
    this.visualStates.clear();
    this.stateSubscribers.clear();
  }
}

export const InteractionPipeline = new InteractionPipelineClass();
