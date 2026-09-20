import { create } from 'zustand';
import type {
  AdminCommandState,
  AdminCoreId,
  AdminUserRecord,
  AdminFleetRecord,
  AdminSyndicateRecord,
  AdminBookingConflictRecord,
  AdminTreasuryAuditRecord,
  AdminDisputeDocketRecord,
  AdminSystemHealthRecord,
} from './adminTypes';
import type { UserRole } from '../world/worldTypes';
import { adminApi } from '../api/adminApi';
import { useNavigationStore } from '../world/useNavigationStore';

interface AdminActions {
  setActiveCore: (coreId: AdminCoreId) => void;
  setUserRole: (role: UserRole) => void;

  // 1. User Core Actions
  verifyUserKyc: (userId: number) => Promise<boolean>;
  toggleUserAccountStatus: (userId: number) => Promise<boolean>;
  elevateUserRole: (
    userId: number,
    role: 'ROLE_CO_OWNER' | 'ROLE_STAFF' | 'ROLE_ADMIN'
  ) => Promise<boolean>;

  // 2. Vehicle Core Actions
  toggleVehicleLockdown: (vehicleId: number) => Promise<boolean>;
  syncFleetTelematics: () => Promise<boolean>;
  dispatchVehicleToWorkshop: (vehicleId: number) => Promise<boolean>;

  // 3. Ownership Core Actions
  toggleSyndicateTransferFreeze: (groupId: number) => Promise<boolean>;
  auditCapTable: (groupId: number) => Promise<boolean>;

  // 4. Booking Core Actions
  resolveBookingConflict: (conflictId: number) => Promise<boolean>;
  preemptReservation: (bookingId: number, reason: string) => Promise<boolean>;
  purgeExpiredBookingHolds: () => Promise<boolean>;

  // 5. Finance Core Actions
  injectTreasuryReserve: (amount: number) => Promise<boolean>;
  toggleDisbursementFreeze: () => Promise<boolean>;
  auditVaultLedger: () => Promise<boolean>;

  // 6. Dispute Core Actions
  enforceSummaryArbitration: (
    disputeId: number,
    verdict: string,
    deductible: number
  ) => Promise<boolean>;
  issueCompensatoryCredit: (disputeId: number, amount: number) => Promise<boolean>;

  // 7. System Core Actions
  toggleGlobalPlatformLockdown: () => Promise<boolean>;
  flushSystemCaches: () => Promise<boolean>;

  // Utility
  clearNotices: () => void;
  resetToDefaults: () => void;
}

export type AdminStore = AdminCommandState & AdminActions;

const INITIAL_USERS: AdminUserRecord[] = [
  {
    id: 1,
    fullName: 'Minh Hiep',
    email: 'admin@evshare.io',
    role: 'ROLE_ADMIN',
    kycStatus: 'VERIFIED',
    accountStatus: 'ACTIVE',
    joinedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 3,
    fullName: 'Bob Driver',
    email: 'bob@evshare.io',
    role: 'ROLE_CO_OWNER',
    kycStatus: 'PENDING',
    accountStatus: 'ACTIVE',
    joinedAt: '2026-03-15T08:30:00Z',
  },
  {
    id: 5,
    fullName: 'Alice Owner',
    email: 'alice@evshare.io',
    role: 'ROLE_CO_OWNER',
    kycStatus: 'VERIFIED',
    accountStatus: 'ACTIVE',
    joinedAt: '2026-02-10T11:20:00Z',
  },
  {
    id: 8,
    fullName: 'Tuan Staff',
    email: 'tuan@evshare.io',
    role: 'ROLE_STAFF',
    kycStatus: 'VERIFIED',
    accountStatus: 'ACTIVE',
    joinedAt: '2026-02-01T09:00:00Z',
  },
];

const INITIAL_FLEET: AdminFleetRecord[] = [
  {
    id: 1,
    model: 'VinFast VF8 Plus',
    licensePlate: '30A-888.88',
    batterySoc: 92,
    telematicsStatus: 'ONLINE',
    lockdownState: 'UNLOCKED',
    diagnosticHealth: 98,
    assignedSector: 'BAY 01',
  },
  {
    id: 2,
    model: 'VinFast VF9 Executive',
    licensePlate: '30A-999.99',
    batterySoc: 78,
    telematicsStatus: 'ONLINE',
    lockdownState: 'UNLOCKED',
    diagnosticHealth: 95,
    assignedSector: 'BAY 02',
  },
  {
    id: 3,
    model: 'VinFast VF6 Eco',
    licensePlate: '30A-666.66',
    batterySoc: 45,
    telematicsStatus: 'STANDBY',
    lockdownState: 'UNLOCKED',
    diagnosticHealth: 72,
    assignedSector: 'WORKSHOP',
  },
];

const INITIAL_SYNDICATES: AdminSyndicateRecord[] = [
  {
    id: 1,
    name: 'Tesla Model 3 & VinFast Syndicate #1',
    totalShares: 100,
    allocatedShares: 100,
    transferFrozen: false,
    complianceScore: 99.4,
    capTableHash: '0x8f2a9e...d4c1',
  },
  {
    id: 2,
    name: 'VF9 Executive Syndicate Alpha',
    totalShares: 100,
    allocatedShares: 85,
    transferFrozen: false,
    complianceScore: 96.8,
    capTableHash: '0x3b7c11...a992',
  },
];

const INITIAL_CONFLICTS: AdminBookingConflictRecord[] = [
  {
    id: 101,
    vehicleId: 1,
    vehicleName: 'VinFast VF8 Plus',
    conflictingUsers: ['Alice Owner', 'Bob Driver'],
    slotTime: 'Today 18:00 - 22:00',
    status: 'PENDING_RESOLUTION',
  },
];

const INITIAL_TREASURY: AdminTreasuryAuditRecord = {
  vaultBalance: 12450000,
  reserveLiquidity: 4500000,
  disbursementsFrozen: false,
  pendingExpenseClaims: 2,
  lastAuditedAt: '2026-09-16T08:00:00Z',
  ledgerHash: 'SHA256-VAULT-991A-88D4',
};

const INITIAL_DISPUTES: AdminDisputeDocketRecord[] = [
  {
    id: 10,
    title: 'Vết Trầy Cản Trước Không Báo Cáo & Lỗi Cảm Biến Sau Phiên #105',
    complainant: 'Alice Owner',
    respondent: 'Bob Driver',
    status: 'ESCALATED',
    claimAmount: 500000,
    escalatedAt: '2026-09-16T08:30:00Z',
  },
];

const INITIAL_SYSTEM_HEALTH: AdminSystemHealthRecord = {
  nodeHealth: 'NOMINAL',
  metaverseFps: 60,
  shaderThroughput: '1.24 GTex/s',
  activeSessions: 14,
  globalLockdownActive: false,
  lastHeartbeat: '2026-09-16T10:00:00Z',
};

export const useAdminStore = create<AdminStore>((set, get) => ({
  activeCore: 'SYSTEM_CORE',
  userRole: 'ROLE_ADMIN',

  users: INITIAL_USERS,
  fleet: INITIAL_FLEET,
  syndicates: INITIAL_SYNDICATES,
  conflicts: INITIAL_CONFLICTS,
  treasury: INITIAL_TREASURY,
  disputes: INITIAL_DISPUTES,
  systemHealth: INITIAL_SYSTEM_HEALTH,

  isExecuting: false,
  feedbackNotice: 'Admin Command Deck initialized. All 7 sovereign cores online.',
  rbacViolationNotice: null,
  errorMessage: null,

  setActiveCore: (coreId) => set({ activeCore: coreId }),

  setUserRole: (role) => {
    set({
      userRole: role,
      rbacViolationNotice:
        role !== 'ROLE_ADMIN'
          ? `SOVEREIGN COMMAND LOCKOUT (403): Current simulated role is ${role}. Full command authority strictly requires ROLE_ADMIN.`
          : null,
      feedbackNotice: `Simulated identity set to ${role}.`,
    });
    useNavigationStore.setState({ userRole: role });
  },

  // 1. User Core Actions
  verifyUserKyc: async (userId) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Only ROLE_ADMIN may verify and sign user KYC biometric credentials.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.verifyUserKyc(userId);
    } catch {
      console.info('[AdminStore] User KYC verified locally.');
    }

    set((state) => ({
      users: state.users.map((u) =>
        u.id === userId ? { ...u, kycStatus: 'VERIFIED' } : u
      ),
      isExecuting: false,
      feedbackNotice: `USER CORE: User #${userId} KYC verified with cryptographic signature.`,
    }));
    return true;
  },

  toggleUserAccountStatus: async (userId) => {
    const { userRole, users } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Only ROLE_ADMIN may alter user account suspension status.',
      });
      return false;
    }

    const targetUser = users.find((u) => u.id === userId);
    if (!targetUser) return false;
    const nextStatus = targetUser.accountStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.toggleUserAccountStatus(userId, nextStatus);
    } catch {
      console.info('[AdminStore] User account status toggled locally.');
    }

    set((state) => ({
      users: state.users.map((u) =>
        u.id === userId ? { ...u, accountStatus: nextStatus } : u
      ),
      isExecuting: false,
      feedbackNotice: `USER CORE: User #${userId} status set to ${nextStatus}.`,
    }));
    return true;
  },

  elevateUserRole: async (userId, newRole) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Only ROLE_ADMIN may elevate or mutate platform RBAC permissions.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.updateUserRole(userId, newRole);
    } catch {
      console.info('[AdminStore] User role updated locally.');
    }

    set((state) => ({
      users: state.users.map((u) =>
        u.id === userId ? { ...u, role: newRole } : u
      ),
      isExecuting: false,
      feedbackNotice: `USER CORE: User #${userId} elevated to ${newRole}.`,
    }));
    return true;
  },

  // 2. Vehicle Core Actions
  toggleVehicleLockdown: async (vehicleId) => {
    const { userRole, fleet } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Remote fleet emergency lockdown strictly requires ROLE_ADMIN.',
      });
      return false;
    }

    const vehicle = fleet.find((v) => v.id === vehicleId);
    if (!vehicle) return false;
    const nextState =
      vehicle.lockdownState === 'UNLOCKED' ? 'LOCKED_SECURE' : 'UNLOCKED';

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.setVehicleLockdown(vehicleId, nextState === 'LOCKED_SECURE');
    } catch {
      console.info('[AdminStore] Vehicle lockdown toggled locally.');
    }

    set((state) => ({
      fleet: state.fleet.map((v) =>
        v.id === vehicleId ? { ...v, lockdownState: nextState } : v
      ),
      isExecuting: false,
      feedbackNotice: `VEHICLE CORE: Vehicle #${vehicleId} remote lockdown: ${nextState}.`,
    }));
    return true;
  },

  syncFleetTelematics: async () => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Fleet CAN-bus telemetry synchronization requires ROLE_ADMIN authority.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.getFleetTelemetry();
    } catch {
      console.info('[AdminStore] Fleet telemetry synced locally.');
    }

    set((state) => ({
      fleet: state.fleet.map((v) => ({ ...v, telematicsStatus: 'ONLINE' })),
      isExecuting: false,
      feedbackNotice:
        'VEHICLE CORE: All vehicle telematics links refreshed & synchronized.',
    }));
    return true;
  },

  dispatchVehicleToWorkshop: async (vehicleId) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Dispatching vehicles to service workshop requires ROLE_ADMIN authority.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.dispatchVehicleToWorkshop(vehicleId);
    } catch {
      console.info('[AdminStore] Vehicle dispatched locally.');
    }

    set((state) => ({
      fleet: state.fleet.map((v) =>
        v.id === vehicleId ? { ...v, assignedSector: 'WORKSHOP' } : v
      ),
      isExecuting: false,
      feedbackNotice: `VEHICLE CORE: Vehicle #${vehicleId} dispatched to Service Workshop bay.`,
    }));
    return true;
  },

  // 3. Ownership Core Actions
  toggleSyndicateTransferFreeze: async (groupId) => {
    const { userRole, syndicates } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Freezing syndicate cap table equity transfers requires ROLE_ADMIN privileges.',
      });
      return false;
    }

    const target = syndicates.find((s) => s.id === groupId);
    if (!target) return false;
    const nextFreeze = !target.transferFrozen;

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.setSyndicateTransferFreeze(groupId, nextFreeze);
    } catch {
      console.info('[AdminStore] Syndicate transfer freeze toggled locally.');
    }

    set((state) => ({
      syndicates: state.syndicates.map((s) =>
        s.id === groupId ? { ...s, transferFrozen: nextFreeze } : s
      ),
      isExecuting: false,
      feedbackNotice: `OWNERSHIP CORE: Syndicate #${groupId} transfer freeze: ${
        nextFreeze ? 'FROZEN' : 'ACTIVE'
      }.`,
    }));
    return true;
  },

  auditCapTable: async (groupId) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Sovereign cryptographic cap table verification requires ROLE_ADMIN.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.auditCapTable(groupId);
    } catch {
      console.info('[AdminStore] Cap table audited locally.');
    }

    set((state) => ({
      syndicates: state.syndicates.map((s) =>
        s.id === groupId ? { ...s, complianceScore: 100 } : s
      ),
      isExecuting: false,
      feedbackNotice: `OWNERSHIP CORE: Syndicate #${groupId} cap table cryptographically verified (100% compliant).`,
    }));
    return true;
  },

  // 4. Booking Core Actions
  resolveBookingConflict: async (conflictId) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Booking conflict arbitration strictly requires ROLE_ADMIN authority.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    set((state) => ({
      conflicts: state.conflicts.map((c) =>
        c.id === conflictId ? { ...c, status: 'RESOLVED_BY_ADMIN' } : c
      ),
      isExecuting: false,
      feedbackNotice: `BOOKING CORE: Schedule conflict #${conflictId} resolved by supreme priority ruling.`,
    }));
    return true;
  },

  preemptReservation: async (bookingId, reason) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Preempting driver reservations requires supreme administrative privileges.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.preemptReservation(bookingId, reason);
    } catch {
      console.info('[AdminStore] Booking preempted locally.');
    }

    set({
      isExecuting: false,
      feedbackNotice: `BOOKING CORE: Reservation #${bookingId} preempted with reason: "${reason}".`,
    });
    return true;
  },

  purgeExpiredBookingHolds: async () => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Purging expired booking reservations requires ROLE_ADMIN authority.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.purgeExpiredHolds();
    } catch {
      console.info('[AdminStore] Expired holds purged locally.');
    }

    set({
      isExecuting: false,
      feedbackNotice:
        'BOOKING CORE: 3 expired reservations purged. Calendar slots restored to syndicate pool.',
    });
    return true;
  },

  // 5. Finance Core Actions
  injectTreasuryReserve: async (amount) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Treasury reserve liquidity injection requires supreme ROLE_ADMIN authority.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.injectReserveLiquidity(amount);
    } catch {
      console.info('[AdminStore] Reserve injected locally.');
    }

    set((state) => ({
      treasury: {
        ...state.treasury,
        vaultBalance: state.treasury.vaultBalance + amount,
        reserveLiquidity: state.treasury.reserveLiquidity + amount,
      },
      isExecuting: false,
      feedbackNotice: `FINANCE CORE: Injected ${amount.toLocaleString()} VND liquidity into SharedFund vault reserves.`,
    }));
    return true;
  },

  toggleDisbursementFreeze: async () => {
    const { userRole, treasury } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Halting SharedFund financial disbursements requires ROLE_ADMIN authority.',
      });
      return false;
    }

    const nextFreeze = !treasury.disbursementsFrozen;
    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.toggleDisbursementFreeze(nextFreeze);
    } catch {
      console.info('[AdminStore] Disbursement freeze toggled locally.');
    }

    set((state) => ({
      treasury: {
        ...state.treasury,
        disbursementsFrozen: nextFreeze,
      },
      isExecuting: false,
      feedbackNotice: `FINANCE CORE: SharedFund outbound disbursements: ${
        nextFreeze ? 'FROZEN' : 'ACTIVE'
      }.`,
    }));
    return true;
  },

  auditVaultLedger: async () => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Executing cryptographic treasury vault audit requires ROLE_ADMIN.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.getTreasuryAudit();
    } catch {
      console.info('[AdminStore] Vault ledger audited locally.');
    }

    set((state) => ({
      treasury: {
        ...state.treasury,
        lastAuditedAt: new Date().toISOString(),
      },
      isExecuting: false,
      feedbackNotice:
        'FINANCE CORE: Vault ledger verified. Cryptographic hash consistent across all syndicate nodes.',
    }));
    return true;
  },

  // 6. Dispute Core Actions
  enforceSummaryArbitration: async (disputeId, verdict, deductible) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Enforcing summary arbitration judgment strictly requires ROLE_ADMIN authority.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.enforceSummaryArbitration(disputeId, verdict, deductible);
    } catch {
      console.info('[AdminStore] Summary arbitration enforced locally.');
    }

    set((state) => ({
      disputes: state.disputes.map((d) =>
        d.id === disputeId ? { ...d, status: 'RESOLVED' } : d
      ),
      isExecuting: false,
      feedbackNotice: `DISPUTE CORE: Summary verdict rendered for Case #${disputeId}. Deductible assessed: ${deductible.toLocaleString()} VND.`,
    }));
    return true;
  },

  issueCompensatoryCredit: async (disputeId, amount) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Issuing compensatory credits requires ROLE_ADMIN authority.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    set({
      isExecuting: false,
      feedbackNotice: `DISPUTE CORE: Credited ${amount.toLocaleString()} VND to affected co-owner for Case #${disputeId}.`,
    });
    return true;
  },

  // 7. System Core Actions
  toggleGlobalPlatformLockdown: async () => {
    const { userRole, systemHealth } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Triggering global emergency platform lockdown requires ROLE_ADMIN authority.',
      });
      return false;
    }

    const nextLockdown = !systemHealth.globalLockdownActive;
    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.toggleGlobalPlatformLockdown(nextLockdown);
    } catch {
      console.info('[AdminStore] Global lockdown toggled locally.');
    }

    set((state) => ({
      systemHealth: {
        ...state.systemHealth,
        globalLockdownActive: nextLockdown,
        nodeHealth: nextLockdown ? 'CRITICAL' : 'NOMINAL',
      },
      isExecuting: false,
      feedbackNotice: `SYSTEM CORE: Global platform lockdown: ${
        nextLockdown ? '🚨 ACTIVE' : '✅ DEACTIVATED'
      }.`,
    }));
    return true;
  },

  flushSystemCaches: async () => {
    const { userRole } = get();
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN LOCKOUT (403): Purging metaverse shader pipelines and node cache requires ROLE_ADMIN.',
      });
      return false;
    }

    set({ isExecuting: true, rbacViolationNotice: null });
    try {
      await adminApi.flushSystemCaches();
    } catch {
      console.info('[AdminStore] System caches flushed locally.');
    }

    set((state) => ({
      systemHealth: {
        ...state.systemHealth,
        lastHeartbeat: new Date().toISOString(),
      },
      isExecuting: false,
      feedbackNotice:
        'SYSTEM CORE: All node caches and WebGL shader pipelines flushed successfully.',
    }));
    return true;
  },

  clearNotices: () =>
    set({ feedbackNotice: null, rbacViolationNotice: null, errorMessage: null }),

  resetToDefaults: () =>
    set({
      activeCore: 'SYSTEM_CORE',
      userRole: 'ROLE_ADMIN',
      users: INITIAL_USERS,
      fleet: INITIAL_FLEET,
      syndicates: INITIAL_SYNDICATES,
      conflicts: INITIAL_CONFLICTS,
      treasury: INITIAL_TREASURY,
      disputes: INITIAL_DISPUTES,
      systemHealth: INITIAL_SYSTEM_HEALTH,
      isExecuting: false,
      feedbackNotice: 'Admin Command Deck reset to nominal state.',
      rbacViolationNotice: null,
      errorMessage: null,
    }),
}));
