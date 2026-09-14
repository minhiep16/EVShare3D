import { describe, it, expect, beforeEach, vi } from 'vitest';
import { InteractionPipeline } from './InteractionPipeline';
import { InteractionContext, InteractionDefinition } from './interactionTypes';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { useUI3DStore } from '@/stores/useUI3DStore';

describe('Interaction Pipeline & Permission Validation Subsystem', () => {
  beforeEach(() => {
    InteractionPipeline.clear();
    useInteractionStore.setState({
      hoveredObjectId: null,
      selectedObjectId: null,
      focusedInputId: null,
      cursorMode: 'DEFAULT',
    });
    useUI3DStore.setState({ notifications: [] });
  });

  const baseContext: InteractionContext = {
    targetId: 'kiosk_01',
    playerPosition: [0, 0, 0],
    targetPosition: [0, 0, 2],
    distanceToPlayer: 2.0,
    userRoles: ['ROLE_CO_OWNER'],
    isAuthenticated: true,
    userId: 101,
  };

  describe('Validation Rules (Stage 4)', () => {
    it('allows interaction when all requirements are met', () => {
      const def: InteractionDefinition = {
        id: 'kiosk_01',
        name: 'Voting Terminal',
        actionType: 'OPEN_PROPOSAL',
        targetPosition: [0, 0, 2],
        requirements: {
          maxInteractionDistance: 3.5,
          requiredRoles: ['ROLE_CO_OWNER'],
        },
      };

      const result = InteractionPipeline.validate(def, baseContext);
      expect(result.allowed).toBe(true);
    });

    it('rejects interaction when player is outside proximity range', () => {
      const def: InteractionDefinition = {
        id: 'kiosk_01',
        name: 'Voting Terminal',
        actionType: 'OPEN_PROPOSAL',
        targetPosition: [0, 0, 10],
        requirements: {
          maxInteractionDistance: 3.0,
        },
      };

      const farContext: InteractionContext = {
        ...baseContext,
        distanceToPlayer: 10.0,
      };

      const result = InteractionPipeline.validate(def, farContext);
      expect(result.allowed).toBe(false);
      expect(result.reason).toContain('Too far away');
    });

    it('rejects interaction when user lacks required RBAC role', () => {
      const def: InteractionDefinition = {
        id: 'admin_vault',
        name: 'Shared Fund Vault Console',
        actionType: 'DISBURSE_FUNDS',
        targetPosition: [0, 0, 2],
        requirements: {
          requiredRoles: ['ROLE_ADMIN'],
        },
      };

      const coOwnerContext: InteractionContext = {
        ...baseContext,
        userRoles: ['ROLE_CO_OWNER'], // Lacks ROLE_ADMIN
      };

      const result = InteractionPipeline.validate(def, coOwnerContext);
      expect(result.allowed).toBe(false);
      expect(result.reason).toContain('Access Denied');
    });

    it('rejects interaction when entity is explicitly disabled', () => {
      const def: InteractionDefinition = {
        id: 'charger_station_03',
        name: 'Supercharger 03',
        actionType: 'START_CHARGE',
        targetPosition: [0, 0, 2],
        requirements: {
          disabled: true,
          disabledReason: 'Station under scheduled maintenance.',
        },
      };

      const result = InteractionPipeline.validate(def, baseContext);
      expect(result.allowed).toBe(false);
      expect(result.reason).toBe('Station under scheduled maintenance.');
    });

    it('evaluates custom contextual validators', () => {
      const def: InteractionDefinition = {
        id: 'vehicle_door',
        name: 'Tesla Model Y Door Lock',
        actionType: 'UNLOCK_VEHICLE',
        targetPosition: [0, 0, 2],
        requirements: {
          customValidator: (ctx) => {
            return ctx.userId === 101
              ? { allowed: true }
              : { allowed: false, reason: 'Keyfob token belongs to different co-owner.' };
          },
        },
      };

      expect(InteractionPipeline.validate(def, baseContext).allowed).toBe(true);

      const foreignUserContext: InteractionContext = {
        ...baseContext,
        userId: 999,
      };
      const rejected = InteractionPipeline.validate(def, foreignUserContext);
      expect(rejected.allowed).toBe(false);
      expect(rejected.reason).toContain('Keyfob token belongs to different co-owner');
    });
  });

  describe('Complete 6-Stage Pipeline Execution', () => {
    it('executes decoupled action and updates visual state on success', async () => {
      const actionSpy = vi.fn();

      InteractionPipeline.register({
        id: 'kiosk_01',
        name: 'Voting Terminal',
        actionType: 'OPEN_PROPOSAL',
        targetPosition: [0, 0, 2],
        requirements: {
          maxInteractionDistance: 4.0,
          requiredRoles: ['ROLE_CO_OWNER'],
        },
        onActivate: actionSpy,
      });

      expect(InteractionPipeline.getVisualState('kiosk_01')).toBe('IDLE');

      const outcome = await InteractionPipeline.execute('kiosk_01', baseContext);

      expect(outcome.success).toBe(true);
      expect(actionSpy).toHaveBeenCalledWith(baseContext);
      expect(InteractionPipeline.getVisualState('kiosk_01')).toBe('SELECTED');
      expect(useInteractionStore.getState().selectedObjectId).toBe('kiosk_01');
    });

    it('blocks execution, sets DISABLED visual state, and triggers notification on failure', async () => {
      const actionSpy = vi.fn();

      InteractionPipeline.register({
        id: 'admin_vault',
        name: 'Shared Fund Vault',
        actionType: 'DISBURSE_FUNDS',
        targetPosition: [0, 0, 2],
        requirements: {
          requiredRoles: ['ROLE_ADMIN'],
        },
        onActivate: actionSpy,
      });

      const outcome = await InteractionPipeline.execute('admin_vault', baseContext);

      expect(outcome.success).toBe(false);
      expect(actionSpy).not.toHaveBeenCalled();
      expect(InteractionPipeline.getVisualState('admin_vault')).toBe('DISABLED');
      expect(useUI3DStore.getState().notifications.length).toBe(1);
      expect(useUI3DStore.getState().notifications[0].type).toBe('WARNING');
    });
  });
});
