import type {
  DisputeDTO,
  DisputeEvidenceDTO,
  DisputeStatus,
  DisputeAuditLogDTO,
  DisputeArbitrationDossierDTO,
} from '../api/disputesApi';
import type { UserRole } from '../world/worldTypes';

export type {
  DisputeDTO,
  DisputeEvidenceDTO,
  DisputeStatus,
  DisputeAuditLogDTO,
  DisputeArbitrationDossierDTO,
};

export type DisputeStation =
  | 'DISPUTE_CRYSTAL'
  | 'DEFECT_HOLOTANK'
  | 'EVIDENCE_CAROUSEL'
  | 'STAFF_CONSOLE'
  | 'ADMIN_DAIS'
  | 'STATUS_STELA';

export type DisputeTab =
  | 'OVERVIEW'
  | 'DEFECT_SCAN'
  | 'EVIDENCE'
  | 'STAFF_MEDIATION'
  | 'ADMIN_ARBITRATION';

export interface DefectMarker3D {
  id: string;
  evidenceId: number;
  position: [number, number, number]; // 3D coordinates on car model
  label: string;
  severity: 'MINOR' | 'MODERATE' | 'CRITICAL';
  photoUrl: string;
  uploaderName: string;
  timestamp: string;
  description: string;
}

export type ArbitrationVerdictChoice =
  | 'RESOLVED_COMPLAINANT_FAVORED'
  | 'RESOLVED_RESPONDENT_FAVORED'
  | 'DISMISSED';

export interface DisputeCameraPreset {
  name: string;
  position: [number, number, number];
  target: [number, number, number];
}

export interface DisputeState {
  // Navigation & tabs
  activeTab: DisputeTab;
  activeStation: DisputeStation;

  // Active Dispute & Evidence
  activeDispute: DisputeDTO;
  evidenceList: DisputeEvidenceDTO[];
  selectedEvidenceId: number | null;
  defects: DefectMarker3D[];
  selectedDefectId: string | null;

  // User simulated role for RBAC testing
  userRole: UserRole;

  // Staff Mediation Inputs
  staffNotesInput: string;
  proposedResolutionInput: string;

  // Admin Arbitration Inputs
  adminVerdictChoice: ArbitrationVerdictChoice;
  adminDeductibleInput: number;

  // State flags & notices
  isSubmitting: boolean;
  feedbackMessage: string | null;
  errorMessage: string | null;
  rbacViolationNotice: string | null;
}
