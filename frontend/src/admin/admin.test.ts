import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useAdminStore } from './useAdminStore';
import {
  ADMIN_SECTOR_CENTER,
  ADMIN_CORES_CONFIG,
  ADMIN_CAMERA_PRESETS,
  COMMAND_THEME,
} from './adminLayout';

describe('Admin Command Center & 7 Conceptual Cores (09-N)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAdminStore.getState().resetToDefaults();
  });

  describe('1. Spatial Layout & Panopticon Deck Architecture', () => {
    it('verifies elevated sector center at [0, 25, 0] (Y=25m)', () => {
      expect(ADMIN_SECTOR_CENTER).toEqual([0, 25, 0]);
    });

    it('verifies all 7 conceptual cores are registered with geometric positions', () => {
      const coreIds = Object.keys(ADMIN_CORES_CONFIG);
      expect(coreIds).toHaveLength(7);
      expect(coreIds).toContain('USER_CORE');
      expect(coreIds).toContain('VEHICLE_CORE');
      expect(coreIds).toContain('OWNERSHIP_CORE');
      expect(coreIds).toContain('BOOKING_CORE');
      expect(coreIds).toContain('FINANCE_CORE');
      expect(coreIds).toContain('DISPUTE_CORE');
      expect(coreIds).toContain('SYSTEM_CORE');

      // Relative coordinates
      expect(ADMIN_CORES_CONFIG.SYSTEM_CORE.relativePosition).toEqual([0, 1.8, 0]);
      expect(ADMIN_CORES_CONFIG.USER_CORE.relativePosition).toEqual([0, 0, -5.4]);
      expect(ADMIN_CORES_CONFIG.VEHICLE_CORE.relativePosition).toEqual([4.8, 0, -2.6]);
      expect(ADMIN_CORES_CONFIG.OWNERSHIP_CORE.relativePosition).toEqual([4.8, 0, 2.6]);
      expect(ADMIN_CORES_CONFIG.BOOKING_CORE.relativePosition).toEqual([0, 0, 5.4]);
      expect(ADMIN_CORES_CONFIG.FINANCE_CORE.relativePosition).toEqual([-4.8, 0, 2.6]);
      expect(ADMIN_CORES_CONFIG.DISPUTE_CORE.relativePosition).toEqual([-4.8, 0, -2.6]);
    });

    it('verifies camera presets for all 7 cores and orbital overview', () => {
      const presets = Object.keys(ADMIN_CAMERA_PRESETS);
      expect(presets).toHaveLength(8);
      expect(ADMIN_CAMERA_PRESETS.ORBITAL_OVERVIEW).toBeDefined();
      expect(ADMIN_CAMERA_PRESETS.USER_CORE_FOCUS).toBeDefined();
      expect(ADMIN_CAMERA_PRESETS.VEHICLE_CORE_FOCUS).toBeDefined();
      expect(ADMIN_CAMERA_PRESETS.OWNERSHIP_CORE_FOCUS).toBeDefined();
      expect(ADMIN_CAMERA_PRESETS.BOOKING_CORE_FOCUS).toBeDefined();
      expect(ADMIN_CAMERA_PRESETS.FINANCE_CORE_FOCUS).toBeDefined();
      expect(ADMIN_CAMERA_PRESETS.DISPUTE_CORE_FOCUS).toBeDefined();
      expect(ADMIN_CAMERA_PRESETS.SYSTEM_CORE_FOCUS).toBeDefined();
    });

    it('verifies command theme palette', () => {
      expect(COMMAND_THEME.primary).toBe('#38bdf8');
      expect(COMMAND_THEME.accentGold).toBe('#fbbf24');
      expect(COMMAND_THEME.alertRed).toBe('#ff1744');
    });
  });

  describe('2. Strict RBAC Exclusivity & Security Lockouts', () => {
    it('blocks ROLE_CO_OWNER from executing any core actions', async () => {
      useAdminStore.getState().setUserRole('ROLE_CO_OWNER');
      const store = useAdminStore.getState();

      const kycResult = await store.verifyUserKyc(3);
      expect(kycResult).toBe(false);
      expect(useAdminStore.getState().rbacViolationNotice).toContain('SOVEREIGN LOCKOUT');

      const lockResult = await store.toggleVehicleLockdown(1);
      expect(lockResult).toBe(false);

      const freezeResult = await store.toggleSyndicateTransferFreeze(1);
      expect(freezeResult).toBe(false);

      const injectResult = await store.injectTreasuryReserve(1000000);
      expect(injectResult).toBe(false);

      const arbitrateResult = await store.enforceSummaryArbitration(10, 'FAVOR_ALICE', 500000);
      expect(arbitrateResult).toBe(false);

      const lockdownResult = await store.toggleGlobalPlatformLockdown();
      expect(lockdownResult).toBe(false);
    });

    it('blocks ROLE_STAFF from administrative operations', async () => {
      useAdminStore.getState().setUserRole('ROLE_STAFF');
      const store = useAdminStore.getState();

      const kycResult = await store.verifyUserKyc(3);
      expect(kycResult).toBe(false);
      expect(useAdminStore.getState().rbacViolationNotice).toContain('SOVEREIGN LOCKOUT');

      const lockdownResult = await store.toggleGlobalPlatformLockdown();
      expect(lockdownResult).toBe(false);
    });
  });

  describe('3. Core 1: User Identity & Biometric Core', () => {
    it('allows ROLE_ADMIN to verify user KYC biometric credential', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().verifyUserKyc(3); // Bob Driver

      expect(success).toBe(true);
      const bob = useAdminStore.getState().users.find((u) => u.id === 3);
      expect(bob?.kycStatus).toBe('VERIFIED');
      expect(useAdminStore.getState().feedbackNotice).toContain('KYC verified');
    });

    it('allows ROLE_ADMIN to toggle account suspension status', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().toggleUserAccountStatus(3);

      expect(success).toBe(true);
      const bob = useAdminStore.getState().users.find((u) => u.id === 3);
      expect(bob?.accountStatus).toBe('SUSPENDED');
      expect(useAdminStore.getState().feedbackNotice).toContain('SUSPENDED');
    });

    it('allows ROLE_ADMIN to elevate user role', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().elevateUserRole(3, 'ROLE_STAFF');

      expect(success).toBe(true);
      const bob = useAdminStore.getState().users.find((u) => u.id === 3);
      expect(bob?.role).toBe('ROLE_STAFF');
    });
  });

  describe('4. Core 2: Fleet Telematics & Lockdown Core', () => {
    it('engages emergency anti-theft lockdown on vehicle', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().toggleVehicleLockdown(1);

      expect(success).toBe(true);
      const vf8 = useAdminStore.getState().fleet.find((v) => v.id === 1);
      expect(vf8?.lockdownState).toBe('LOCKED_SECURE');
      expect(useAdminStore.getState().feedbackNotice).toContain('LOCKED_SECURE');
    });

    it('synchronizes fleet CAN-bus telematics links', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().syncFleetTelematics();

      expect(success).toBe(true);
      expect(useAdminStore.getState().feedbackNotice).toContain('synchronized');
    });

    it('dispatches vehicle to service workshop', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().dispatchVehicleToWorkshop(1);

      expect(success).toBe(true);
      const vf8 = useAdminStore.getState().fleet.find((v) => v.id === 1);
      expect(vf8?.assignedSector).toBe('WORKSHOP');
    });
  });

  describe('5. Core 3: Syndicate Equity & Governance Core', () => {
    it('freezes syndicate equity share transfers', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().toggleSyndicateTransferFreeze(1);

      expect(success).toBe(true);
      const syn = useAdminStore.getState().syndicates.find((s) => s.id === 1);
      expect(syn?.transferFrozen).toBe(true);
    });

    it('audits syndicate cap table cryptography with 100% compliance', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().auditCapTable(1);

      expect(success).toBe(true);
      const syn = useAdminStore.getState().syndicates.find((s) => s.id === 1);
      expect(syn?.complianceScore).toBe(100);
    });
  });

  describe('6. Core 4: Chrono-Spatial Booking Core', () => {
    it('arbitrates schedule conflict in favor of priority co-owner', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().resolveBookingConflict(101);

      expect(success).toBe(true);
      const conflict = useAdminStore.getState().conflicts.find((c) => c.id === 101);
      expect(conflict?.status).toBe('RESOLVED_BY_ADMIN');
    });

    it('preempts reservation with platform reason', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().preemptReservation(105, 'VIP Dispatch');

      expect(success).toBe(true);
      expect(useAdminStore.getState().feedbackNotice).toContain('preempted');
    });

    it('purges expired reservations from the calendar pool', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().purgeExpiredBookingHolds();

      expect(success).toBe(true);
      expect(useAdminStore.getState().feedbackNotice).toContain('purged');
    });
  });

  describe('7. Core 5: SharedFund Treasury & Liquidity Core', () => {
    it('injects liquidity reserve into vault balance', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const initialBalance = useAdminStore.getState().treasury.vaultBalance;

      const success = await useAdminStore.getState().injectTreasuryReserve(2000000);
      expect(success).toBe(true);

      const state = useAdminStore.getState();
      expect(state.treasury.vaultBalance).toBe(initialBalance + 2000000);
      expect(state.feedbackNotice).toContain('Injected 2,000,000 VND');
    });

    it('freezes outbound SharedFund disbursements', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().toggleDisbursementFreeze();

      expect(success).toBe(true);
      expect(useAdminStore.getState().treasury.disbursementsFrozen).toBe(true);
    });

    it('audits vault ledger hash consistency', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().auditVaultLedger();

      expect(success).toBe(true);
      expect(useAdminStore.getState().feedbackNotice).toContain('verified');
    });
  });

  describe('8. Core 6: Arbitration Docket & Summary Core', () => {
    it('enforces summary arbitration verdict and assesses deductible', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore
        .getState()
        .enforceSummaryArbitration(10, 'FAVOR_COMPLAINANT', 500000);

      expect(success).toBe(true);
      const dispute = useAdminStore.getState().disputes.find((d) => d.id === 10);
      expect(dispute?.status).toBe('RESOLVED');
      expect(useAdminStore.getState().feedbackNotice).toContain('Summary verdict rendered');
    });

    it('issues compensatory credit to co-owner', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().issueCompensatoryCredit(10, 250000);

      expect(success).toBe(true);
      expect(useAdminStore.getState().feedbackNotice).toContain('Credited 250,000 VND');
    });
  });

  describe('9. Core 7: Omni-Command Zenith System Core', () => {
    it('triggers and deactivates global emergency platform lockdown', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');

      // Trigger lockdown
      const successLock = await useAdminStore.getState().toggleGlobalPlatformLockdown();
      expect(successLock).toBe(true);
      expect(useAdminStore.getState().systemHealth.globalLockdownActive).toBe(true);
      expect(useAdminStore.getState().systemHealth.nodeHealth).toBe('CRITICAL');

      // Deactivate lockdown
      const successUnlock = await useAdminStore.getState().toggleGlobalPlatformLockdown();
      expect(successUnlock).toBe(true);
      expect(useAdminStore.getState().systemHealth.globalLockdownActive).toBe(false);
      expect(useAdminStore.getState().systemHealth.nodeHealth).toBe('NOMINAL');
    });

    it('flushes node caches and WebGL shader pipelines', async () => {
      useAdminStore.getState().setUserRole('ROLE_ADMIN');
      const success = await useAdminStore.getState().flushSystemCaches();

      expect(success).toBe(true);
      expect(useAdminStore.getState().feedbackNotice).toContain('flushed successfully');
    });
  });
});
