import type {
  AdminUserRecord,
  AdminFleetRecord,
  AdminSyndicateRecord,
  AdminBookingConflictRecord,
  AdminTreasuryAuditRecord,
  AdminDisputeDocketRecord,
  AdminSystemHealthRecord,
} from '../api/adminApi';
import type { UserRole } from '../world/worldTypes';

export type {
  AdminUserRecord,
  AdminFleetRecord,
  AdminSyndicateRecord,
  AdminBookingConflictRecord,
  AdminTreasuryAuditRecord,
  AdminDisputeDocketRecord,
  AdminSystemHealthRecord,
};

export type AdminCoreId =
  | 'USER_CORE'
  | 'VEHICLE_CORE'
  | 'OWNERSHIP_CORE'
  | 'BOOKING_CORE'
  | 'FINANCE_CORE'
  | 'DISPUTE_CORE'
  | 'SYSTEM_CORE';

export interface AdminCameraPreset {
  name: string;
  position: [number, number, number];
  target: [number, number, number];
}

export interface AdminCommandState {
  // Navigation & Active Inspection
  activeCore: AdminCoreId;

  // RBAC Simulated Identity for Live Authorization Verification
  userRole: UserRole;

  // Conceptual Core Data Collections
  users: AdminUserRecord[];
  fleet: AdminFleetRecord[];
  syndicates: AdminSyndicateRecord[];
  conflicts: AdminBookingConflictRecord[];
  treasury: AdminTreasuryAuditRecord;
  disputes: AdminDisputeDocketRecord[];
  systemHealth: AdminSystemHealthRecord;

  // Global Flags & Feedback
  isExecuting: boolean;
  feedbackNotice: string | null;
  rbacViolationNotice: string | null;
  errorMessage: string | null;
}
