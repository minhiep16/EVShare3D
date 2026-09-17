import { apiClient } from './apiClient';

export interface AdminUserRecord {
  id: number;
  fullName: string;
  email: string;
  role: 'ROLE_CO_OWNER' | 'ROLE_STAFF' | 'ROLE_ADMIN';
  kycStatus: 'VERIFIED' | 'PENDING' | 'REJECTED';
  accountStatus: 'ACTIVE' | 'SUSPENDED';
  joinedAt: string;
}

export interface AdminFleetRecord {
  id: number;
  model: string;
  licensePlate: string;
  batterySoc: number;
  telematicsStatus: 'ONLINE' | 'STANDBY' | 'FAULT';
  lockdownState: 'UNLOCKED' | 'LOCKED_SECURE';
  diagnosticHealth: number; // 0-100%
  assignedSector: string;
}

export interface AdminSyndicateRecord {
  id: number;
  name: string;
  totalShares: number;
  allocatedShares: number;
  transferFrozen: boolean;
  complianceScore: number;
  capTableHash: string;
}

export interface AdminBookingConflictRecord {
  id: number;
  vehicleId: number;
  vehicleName: string;
  conflictingUsers: string[];
  slotTime: string;
  status: 'PENDING_RESOLUTION' | 'RESOLVED_BY_ADMIN' | 'PURGED';
}

export interface AdminTreasuryAuditRecord {
  vaultBalance: number;
  reserveLiquidity: number;
  disbursementsFrozen: boolean;
  pendingExpenseClaims: number;
  lastAuditedAt: string;
  ledgerHash: string;
}

export interface AdminDisputeDocketRecord {
  id: number;
  title: string;
  complainant: string;
  respondent: string;
  status: 'OPEN' | 'UNDER_REVIEW' | 'ESCALATED' | 'RESOLVED';
  claimAmount: number;
  escalatedAt: string;
}

export interface AdminSystemHealthRecord {
  nodeHealth: 'NOMINAL' | 'DEGRADED' | 'CRITICAL';
  metaverseFps: number;
  shaderThroughput: string;
  activeSessions: number;
  globalLockdownActive: boolean;
  lastHeartbeat: string;
}

export const adminApi = {
  // 1. User Core APIs
  getUsers: async (): Promise<AdminUserRecord[]> => {
    return apiClient.apiGet<AdminUserRecord[]>('/users');
  },
  toggleUserAccountStatus: async (
    userId: number,
    accountStatus: 'ACTIVE' | 'SUSPENDED'
  ): Promise<{ success: boolean; userId: number; status: string }> => {
    return apiClient.apiPut(`/users/${userId}/status`, { accountStatus });
  },
  verifyUserKyc: async (
    userId: number
  ): Promise<{ success: boolean; userId: number }> => {
    return apiClient.apiPost(`/users/${userId}/kyc/verify`, {});
  },
  updateUserRole: async (
    userId: number,
    role: string
  ): Promise<{ success: boolean; userId: number; role: string }> => {
    return apiClient.apiPut(`/users/${userId}/role`, { role });
  },

  // 2. Vehicle Core APIs
  getFleetTelemetry: async (): Promise<AdminFleetRecord[]> => {
    return apiClient.apiGet<AdminFleetRecord[]>('/vehicles');
  },
  setVehicleLockdown: async (
    vehicleId: number,
    lockdown: boolean
  ): Promise<{ vehicleId: number; locked: boolean }> => {
    return apiClient.apiPost(`/vehicles/${vehicleId}/lockdown`, { lockdown });
  },
  dispatchVehicleToWorkshop: async (
    vehicleId: number
  ): Promise<{ vehicleId: number; status: string }> => {
    return apiClient.apiPut(`/vehicles/${vehicleId}/status`, {
      status: 'IN_SERVICE',
    });
  },

  // 3. Ownership Core APIs
  getOwnershipSyndicates: async (): Promise<AdminSyndicateRecord[]> => {
    return apiClient.apiGet<AdminSyndicateRecord[]>('/ownership-groups');
  },
  setSyndicateTransferFreeze: async (
    groupId: number,
    freeze: boolean
  ): Promise<{ groupId: number; frozen: boolean }> => {
    return apiClient.apiPost(`/ownership-groups/${groupId}/freeze-transfers`, {
      freeze,
    });
  },
  auditCapTable: async (
    groupId: number
  ): Promise<{ groupId: number; hash: string; compliant: boolean }> => {
    return apiClient.apiGet(`/ownership-groups/${groupId}/audit-cap-table`);
  },

  // 4. Booking Core APIs
  getBookingConflicts: async (): Promise<AdminBookingConflictRecord[]> => {
    return apiClient.apiGet<AdminBookingConflictRecord[]>('/bookings/conflicts');
  },
  preemptReservation: async (
    bookingId: number,
    reason: string
  ): Promise<{ bookingId: number; status: string }> => {
    return apiClient.apiPost(`/bookings/${bookingId}/preempt`, { reason });
  },
  purgeExpiredHolds: async (): Promise<{ purgedCount: number }> => {
    return apiClient.apiPost('/bookings/purge-expired-holds', {});
  },

  // 5. Finance Core APIs
  getTreasuryAudit: async (): Promise<AdminTreasuryAuditRecord> => {
    return apiClient.apiGet<AdminTreasuryAuditRecord>('/shared-funds/1/audit');
  },
  injectReserveLiquidity: async (
    amount: number
  ): Promise<{ newReserve: number; transactionRef: string }> => {
    return apiClient.apiPost('/shared-funds/1/inject-reserve', { amount });
  },
  toggleDisbursementFreeze: async (
    freeze: boolean
  ): Promise<{ frozen: boolean }> => {
    return apiClient.apiPost('/shared-funds/1/freeze-disbursements', { freeze });
  },

  // 6. Dispute Core APIs
  getDisputeDocket: async (): Promise<AdminDisputeDocketRecord[]> => {
    return apiClient.apiGet<AdminDisputeDocketRecord[]>('/disputes/docket');
  },
  enforceSummaryArbitration: async (
    disputeId: number,
    verdict: string,
    deductibleAmount: number
  ): Promise<{ disputeId: number; status: string; ref: string }> => {
    return apiClient.apiPost(`/disputes/${disputeId}/summary-arbitration`, {
      verdict,
      deductibleAmount,
    });
  },

  // 7. System Core APIs
  getSystemHealth: async (): Promise<AdminSystemHealthRecord> => {
    return apiClient.apiGet<AdminSystemHealthRecord>('/health/telemetry');
  },
  toggleGlobalPlatformLockdown: async (
    lockdown: boolean
  ): Promise<{ lockdownActive: boolean; timestamp: string }> => {
    return apiClient.apiPost('/system/global-lockdown', { lockdown });
  },
  flushSystemCaches: async (): Promise<{ flushed: boolean; timestamp: string }> => {
    return apiClient.apiPost('/system/flush-cache', {});
  },
};
