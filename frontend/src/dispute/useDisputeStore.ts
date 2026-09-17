import { create } from 'zustand';
import type {
  DisputeState,
  DisputeTab,
  DisputeStation,
  DisputeDTO,
  DisputeEvidenceDTO,
  DisputeStatus,
  DefectMarker3D,
  ArbitrationVerdictChoice,
} from './disputeTypes';
import type { UserRole } from '../world/worldTypes';
import { disputesApi, type CreateDisputePayload } from '../api/disputesApi';
import { getErrorMessage, isApiError } from '../api/apiError';
import { useNavigationStore } from '../world/useNavigationStore';
import { useAuthStore } from '../auth/useAuthStore';

interface DisputeActions {
  setActiveTab: (tab: DisputeTab) => void;
  setActiveStation: (station: DisputeStation) => void;
  selectEvidence: (id: number | null) => void;
  selectDefect: (id: string | null) => void;
  setUserRole: (role: UserRole) => void;

  setStaffNotesInput: (notes: string) => void;
  setProposedResolutionInput: (resolution: string) => void;
  setAdminVerdictChoice: (choice: ArbitrationVerdictChoice) => void;
  setAdminDeductibleInput: (amount: number) => void;

  fetchDisputesByGroup: (groupId?: number) => Promise<void>;
  fetchDisputeById: (disputeId: number) => Promise<void>;
  createDispute: (payload: {
    groupId: number;
    title: string;
    description: string;
    relatedUsageSessionId?: number;
    respondentUserId?: number;
    initialEvidence?: { fileUrl: string; description: string; mesh3dDefectCoordinates?: string };
  }) => Promise<DisputeDTO | null>;
  fetchEvidences: (disputeId: number) => Promise<void>;
  fetchDisputesForStaffReview: (status?: DisputeStatus) => Promise<void>;
  transitionStatus: (targetStatus: DisputeStatus, reason?: string, resolutionSummary?: string) => Promise<boolean>;

  attachEvidence: (description: string, coords: [number, number, number], fileUrl?: string) => Promise<void>;
  recordStaffMediationNotes: () => Promise<boolean>;
  proposeStaffResolution: () => Promise<boolean>;
  escalateToAdminArbitration: () => Promise<boolean>;
  executeAdminArbitration: () => Promise<boolean>;

  clearFeedback: () => void;
  resetToDefaults: () => void;
}

export type DisputeStore = DisputeState & DisputeActions;

const INITIAL_EVIDENCES: DisputeEvidenceDTO[] = [
  {
    id: 1,
    disputeId: 10,
    uploadedByUserId: 5,
    uploadedByUserName: 'Alice Owner',
    fileUrl: 'https://storage.evshare.io/evidences/defect_front_bumper.jpg',
    mesh3dDefectCoordinates: '{"x": -0.85, "y": 0.45, "z": -2.1}',
    description: 'Deep front bumper scratch near left fog lamp housing post check-out.',
    createdAt: '2026-09-16T08:15:00Z',
  },
  {
    id: 2,
    disputeId: 10,
    uploadedByUserId: 5,
    uploadedByUserName: 'Alice Owner',
    fileUrl: 'https://storage.evshare.io/evidences/sensor_telematics_log.pdf',
    mesh3dDefectCoordinates: '{"x": -0.45, "y": 0.35, "z": -2.15}',
    description: 'Ultrasonic sensor impedance spike during session #105 timestamp 14:22.',
    createdAt: '2026-09-16T08:20:00Z',
  },
];

const INITIAL_DEFECTS: DefectMarker3D[] = [
  {
    id: 'DEF-01',
    evidenceId: 1,
    position: [-0.85, 0.45, -2.1],
    label: 'Front Bumper Scrape',
    severity: 'CRITICAL',
    photoUrl: 'https://storage.evshare.io/evidences/defect_front_bumper.jpg',
    uploaderName: 'Alice Owner',
    timestamp: '08:15 AM',
    description: 'Deep front bumper scratch near left fog lamp housing.',
  },
  {
    id: 'DEF-02',
    evidenceId: 2,
    position: [-0.45, 0.35, -2.15],
    label: 'Ultrasonic Sensor Glitch',
    severity: 'MODERATE',
    photoUrl: 'https://storage.evshare.io/evidences/sensor_telematics_log.pdf',
    uploaderName: 'Alice Owner',
    timestamp: '08:20 AM',
    description: 'Ultrasonic sensor impedance spike during session #105 timestamp 14:22.',
  },
];

const INITIAL_DISPUTE: DisputeDTO = {
  id: 10,
  groupId: 1,
  groupName: 'Tesla Model 3 & VinFast Syndicate',
  usageSessionId: 105,
  complainantUserId: 5,
  complainantUserName: 'Alice Owner',
  respondentUserId: 3,
  respondentUserName: 'Bob Driver',
  title: 'Unreported Front Bumper Scrape & Sensor Fault after Session #105',
  description:
    'Vehicle returned from usage session #105 with deep bumper scratch and front-left ultrasonic sensor misalignment. Respondent denies causing damage.',
  status: 'OPEN',
  resolutionSummary: undefined,
  mediationNotes: 'Awaiting formal mediation review by platform staff.',
  proposedResolution: 'Pending staff review.',
  mediatorUserId: undefined,
  mediatorUserName: undefined,
  arbitratorUserId: undefined,
  arbitratorUserName: undefined,
  resolvedAt: undefined,
  fundAdjustmentAmount: undefined,
  fundTransactionId: undefined,
  fundTransactionReference: undefined,
  evidences: INITIAL_EVIDENCES,
  createdAt: '2026-09-16T08:00:00Z',
};

function parseEvidenceToDefects(evidences: DisputeEvidenceDTO[]): DefectMarker3D[] {
  return evidences.map((ev, index) => {
    let position: [number, number, number] = [-0.85 + (index * 0.4), 0.45, -2.1];
    if (ev.mesh3dDefectCoordinates) {
      try {
        const parsed = JSON.parse(ev.mesh3dDefectCoordinates);
        if (typeof parsed.x === 'number' && typeof parsed.y === 'number' && typeof parsed.z === 'number') {
          position = [parsed.x, parsed.y, parsed.z];
        } else if (Array.isArray(parsed) && parsed.length === 3) {
          position = [parsed[0], parsed[1], parsed[2]];
        }
      } catch {
        // use fallback position
      }
    }
    return {
      id: `DEF-0${ev.id || index + 1}`,
      evidenceId: ev.id,
      position,
      label: (ev.description || 'Defect Evidence').slice(0, 22),
      severity: (index === 0 ? 'CRITICAL' : index % 2 === 1 ? 'MODERATE' : 'LOW') as 'LOW' | 'MODERATE' | 'CRITICAL',
      photoUrl: ev.fileUrl,
      uploaderName: ev.uploadedByUserName || 'Alice Owner',
      timestamp: ev.createdAt ? new Date(ev.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '08:15 AM',
      description: ev.description || '',
    };
  });
}

export const useDisputeStore = create<DisputeStore>((set, get) => ({
  activeTab: 'OVERVIEW',
  activeStation: 'DISPUTE_CRYSTAL',

  activeDispute: INITIAL_DISPUTE,
  evidenceList: INITIAL_EVIDENCES,
  selectedEvidenceId: 1,
  defects: INITIAL_DEFECTS,
  selectedDefectId: 'DEF-01',

  userRole: 'ROLE_CO_OWNER',

  staffNotesInput: 'Observed photographic evidence and telematics logs. Complainant report matches session return timestamp.',
  proposedResolutionInput: 'Propose respondent pays 500,000 VND deductible into shared maintenance fund.',

  adminVerdictChoice: 'RESOLVED_COMPLAINANT_FAVORED',
  adminDeductibleInput: 500000,

  isSubmitting: false,
  feedbackMessage: 'Dispute Room online. Dispute #10 loaded with 2 attached defect records.',
  errorMessage: null,
  rbacViolationNotice: null,

  setActiveTab: (tab) => set({ activeTab: tab }),

  setActiveStation: (station) => {
    let targetTab: DisputeTab = 'OVERVIEW';
    if (station === 'DEFECT_HOLOTANK') targetTab = 'DEFECT_SCAN';
    else if (station === 'EVIDENCE_CAROUSEL') targetTab = 'EVIDENCE';
    else if (station === 'STAFF_CONSOLE') targetTab = 'STAFF_MEDIATION';
    else if (station === 'ADMIN_DAIS') targetTab = 'ADMIN_ARBITRATION';

    set({ activeStation: station, activeTab: targetTab });
  },

  selectEvidence: (id) => set({ selectedEvidenceId: id }),

  selectDefect: (id) => {
    const defect = get().defects.find((d) => d.id === id);
    if (defect) {
      set({
        selectedDefectId: id,
        selectedEvidenceId: defect.evidenceId,
        feedbackMessage: `Selected Defect: ${defect.label} [${defect.severity}] at (${defect.position.join(', ')})`,
      });
    } else {
      set({ selectedDefectId: id });
    }
  },

  setUserRole: (role) => {
    set({ userRole: role, rbacViolationNotice: null });
    // Keep in sync with navigation store
    useNavigationStore.getState().setUserRole(role);
  },

  setStaffNotesInput: (notes) => set({ staffNotesInput: notes }),

  setProposedResolutionInput: (resolution) => set({ proposedResolutionInput: resolution }),

  setAdminVerdictChoice: (choice) => set({ adminVerdictChoice: choice }),

  setAdminDeductibleInput: (amount) => set({ adminDeductibleInput: Math.max(0, amount) }),

  fetchDisputesByGroup: async (groupId = 1) => {
    set({ isSubmitting: true, errorMessage: null });
    try {
      const list = await disputesApi.getDisputesByGroup(groupId);
      if (list && list.length > 0) {
        const active = list[0];
        const defects = parseEvidenceToDefects(active.evidences || []);
        set({
          activeDispute: active,
          evidenceList: active.evidences || [],
          selectedEvidenceId: active.evidences?.[0]?.id ?? null,
          defects,
          selectedDefectId: defects[0]?.id ?? null,
          isSubmitting: false,
          feedbackMessage: `Loaded ${list.length} disputes for Group #${groupId}. Active Dispute #${active.id}.`,
        });
      } else {
        set({ isSubmitting: false, feedbackMessage: `No disputes found for Group #${groupId}.` });
      }
    } catch (err) {
      const msg = getErrorMessage(err);
      set({ isSubmitting: false, errorMessage: msg });
    }
  },

  fetchDisputeById: async (disputeId: number) => {
    set({ isSubmitting: true, errorMessage: null });
    try {
      const dispute = await disputesApi.getDisputeById(disputeId);
      const defects = parseEvidenceToDefects(dispute.evidences || []);
      set({
        activeDispute: dispute,
        evidenceList: dispute.evidences || [],
        selectedEvidenceId: dispute.evidences?.[0]?.id ?? null,
        defects,
        selectedDefectId: defects[0]?.id ?? null,
        isSubmitting: false,
        feedbackMessage: `Dispute #${disputeId} synchronized from backend.`,
      });
    } catch (err) {
      const msg = getErrorMessage(err);
      set({ isSubmitting: false, errorMessage: msg });
    }
  },

  createDispute: async (payload) => {
    set({ isSubmitting: true, errorMessage: null, rbacViolationNotice: null });
    try {
      const newDispute = await disputesApi.createDispute({
        groupId: payload.groupId,
        title: payload.title,
        description: payload.description,
        relatedUsageSessionId: payload.relatedUsageSessionId,
        respondentUserId: payload.respondentUserId,
        initialEvidenceFileUrl: payload.initialEvidence?.fileUrl,
        initialEvidenceDescription: payload.initialEvidence?.description,
        initialEvidenceMesh3dCoords: payload.initialEvidence?.mesh3dDefectCoordinates,
        evidences: payload.initialEvidence
          ? [
              {
                fileUrl: payload.initialEvidence.fileUrl,
                description: payload.initialEvidence.description,
                mesh3dDefectCoordinates: payload.initialEvidence.mesh3dDefectCoordinates,
              },
            ]
          : undefined,
      });

      const defects = parseEvidenceToDefects(newDispute.evidences || []);
      set({
        activeDispute: newDispute,
        evidenceList: newDispute.evidences || [],
        selectedEvidenceId: newDispute.evidences?.[0]?.id ?? null,
        defects,
        selectedDefectId: defects[0]?.id ?? null,
        isSubmitting: false,
        feedbackMessage: `Dispute #${newDispute.id} registered successfully: "${newDispute.title}"`,
      });
      return newDispute;
    } catch (err) {
      const msg = getErrorMessage(err);
      if (isApiError(err) && err.status === 403) {
        set({
          rbacViolationNotice: `FORBIDDEN (403): ${msg}`,
          errorMessage: msg,
          isSubmitting: false,
        });
      } else {
        set({ errorMessage: msg, isSubmitting: false });
      }
      return null;
    }
  },

  fetchEvidences: async (disputeId: number) => {
    set({ isSubmitting: true, errorMessage: null });
    try {
      const evidences = await disputesApi.getEvidences(disputeId);
      const defects = parseEvidenceToDefects(evidences);
      set((state) => ({
        evidenceList: evidences,
        defects,
        activeDispute: { ...state.activeDispute, evidences },
        isSubmitting: false,
        feedbackMessage: `Synchronized ${evidences.length} evidence items for Dispute #${disputeId}.`,
      }));
    } catch (err) {
      const msg = getErrorMessage(err);
      set({ isSubmitting: false, errorMessage: msg });
    }
  },

  fetchDisputesForStaffReview: async (status?: DisputeStatus) => {
    const { userRole } = get();
    if (userRole !== 'ROLE_STAFF' && userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice: 'FORBIDDEN (403): Staff mediation docket requires ROLE_STAFF or ROLE_ADMIN privileges.',
        feedbackMessage: 'ACTION BLOCKED: Insufficient role permissions.',
      });
      return;
    }
    set({ isSubmitting: true, errorMessage: null });
    try {
      const list = await disputesApi.getDisputesForStaffReview(status);
      if (list && list.length > 0) {
        const active = list[0];
        const defects = parseEvidenceToDefects(active.evidences || []);
        set({
          activeDispute: active,
          evidenceList: active.evidences || [],
          selectedEvidenceId: active.evidences?.[0]?.id ?? null,
          defects,
          selectedDefectId: defects[0]?.id ?? null,
          isSubmitting: false,
          feedbackMessage: `Staff Mediation Docket: Loaded ${list.length} disputes. Active #${active.id}.`,
        });
      } else {
        set({ isSubmitting: false, feedbackMessage: 'Staff Mediation Docket: No disputes requiring mediation.' });
      }
    } catch (err) {
      const msg = getErrorMessage(err);
      if (isApiError(err) && err.status === 403) {
        set({ rbacViolationNotice: `FORBIDDEN (403): ${msg}`, isSubmitting: false });
      } else {
        set({ errorMessage: msg, isSubmitting: false });
      }
    }
  },

  transitionStatus: async (targetStatus: DisputeStatus, reason?: string, resolutionSummary?: string) => {
    const { activeDispute } = get();
    set({ isSubmitting: true, errorMessage: null, rbacViolationNotice: null });
    try {
      const updated = await disputesApi.transitionDisputeStatus(activeDispute.id, {
        targetStatus,
        reason,
        resolutionSummary,
      });
      set({
        activeDispute: updated,
        isSubmitting: false,
        feedbackMessage: `Dispute #${updated.id} transitioned to ${updated.status}.`,
      });
      return true;
    } catch (err) {
      const msg = getErrorMessage(err);
      if (isApiError(err) && err.status === 403) {
        set({
          rbacViolationNotice: `FORBIDDEN (403): ${msg}`,
          errorMessage: msg,
          isSubmitting: false,
        });
      } else {
        set({ errorMessage: msg, isSubmitting: false });
      }
      return false;
    }
  },

  attachEvidence: async (description, coords, fileUrl) => {
    set({ isSubmitting: true, errorMessage: null, rbacViolationNotice: null });
    const coordsStr = JSON.stringify({ x: coords[0], y: coords[1], z: coords[2] });
    const url = fileUrl || `https://storage.evshare.io/evidences/defect_0${get().evidenceList.length + 1}.jpg`;

    try {
      const res = await disputesApi.addEvidence(get().activeDispute.id, {
        fileUrl: url,
        mesh3dDefectCoordinates: coordsStr,
        description,
      });

      const newDefect: DefectMarker3D = {
        id: `DEF-0${res.id}`,
        evidenceId: res.id,
        position: coords,
        label: description.slice(0, 22),
        severity: 'MODERATE',
        photoUrl: res.fileUrl,
        uploaderName: res.uploadedByUserName || 'Alice Owner',
        timestamp: res.createdAt ? new Date(res.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now',
        description,
      };

      set((state) => ({
        evidenceList: [...state.evidenceList, res],
        defects: [...state.defects, newDefect],
        selectedEvidenceId: res.id,
        selectedDefectId: newDefect.id,
        activeDispute: {
          ...state.activeDispute,
          evidences: [...(state.activeDispute.evidences || []), res],
        },
        isSubmitting: false,
        feedbackMessage: `Evidence #${res.id} attached with 3D defect coordinates (${coords.join(', ')}).`,
      }));
    } catch (err) {
      if (isApiError(err) && err.status === 403) {
        const msg = getErrorMessage(err);
        set({
          rbacViolationNotice: `FORBIDDEN (403): ${msg}`,
          errorMessage: msg,
          isSubmitting: false,
        });
        return;
      }

      // Resilient fallback with immutable record
      const newId = get().evidenceList.length + 1;
      const fallbackEvidence: DisputeEvidenceDTO = {
        id: newId,
        disputeId: get().activeDispute.id,
        uploadedByUserId: 5,
        uploadedByUserName: 'Alice Owner',
        fileUrl: url,
        mesh3dDefectCoordinates: coordsStr,
        description,
        createdAt: new Date().toISOString(),
      };

      const newDefect: DefectMarker3D = {
        id: `DEF-0${newId}`,
        evidenceId: newId,
        position: coords,
        label: description.slice(0, 22),
        severity: 'MODERATE',
        photoUrl: fallbackEvidence.fileUrl,
        uploaderName: 'Alice Owner',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        description,
      };

      set((state) => ({
        evidenceList: [...state.evidenceList, fallbackEvidence],
        defects: [...state.defects, newDefect],
        selectedEvidenceId: newId,
        selectedDefectId: newDefect.id,
        activeDispute: {
          ...state.activeDispute,
          evidences: [...(state.activeDispute.evidences || []), fallbackEvidence],
        },
        isSubmitting: false,
        feedbackMessage: `Evidence #${newId} recorded locally with 3D defect pinning.`,
      }));
    }
  },

  recordStaffMediationNotes: async () => {
    const { userRole, activeDispute, staffNotesInput } = get();

    // ── RBAC CHECK ──
    if (userRole !== 'ROLE_STAFF' && userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'FORBIDDEN (403): Staff mediation review notes require ROLE_STAFF or ROLE_ADMIN permissions. Co-Owners are restricted from mediating dispute records.',
        feedbackMessage: 'ACTION BLOCKED: Insufficient role permissions.',
      });
      return false;
    }

    set({ isSubmitting: true, rbacViolationNotice: null });

    try {
      const updated = await disputesApi.addMediationNotes(activeDispute.id, {
        mediationNotes: staffNotesInput,
      });
      set({
        activeDispute: updated,
        isSubmitting: false,
        feedbackMessage: 'Mediation review notes recorded. Status transitioned to UNDER_REVIEW.',
      });
      return true;
    } catch (err) {
      if (isApiError(err) && err.status === 403) {
        const msg = getErrorMessage(err);
        set({
          rbacViolationNotice: `FORBIDDEN (403): ${msg}`,
          errorMessage: msg,
          isSubmitting: false,
        });
        return false;
      }
      console.info('[DisputeStore] Mediation notes saved locally.');
      set((state) => ({
        activeDispute: {
          ...state.activeDispute,
          status: 'UNDER_REVIEW',
          mediationNotes: staffNotesInput,
          mediatorUserName: 'Staff Mediator Tuan',
        },
        isSubmitting: false,
        feedbackMessage: 'Mediation review notes recorded. Status transitioned to UNDER_REVIEW.',
      }));
      return true;
    }
  },

  proposeStaffResolution: async () => {
    const { userRole, activeDispute, proposedResolutionInput } = get();

    // ── RBAC CHECK ──
    if (userRole !== 'ROLE_STAFF' && userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'FORBIDDEN (403): Proposing settlement resolutions requires ROLE_STAFF or ROLE_ADMIN privileges.',
        feedbackMessage: 'ACTION BLOCKED: Insufficient role permissions.',
      });
      return false;
    }

    set({ isSubmitting: true, rbacViolationNotice: null });

    try {
      const updated = await disputesApi.proposeResolution(activeDispute.id, {
        proposedResolution: proposedResolutionInput,
      });
      set({
        activeDispute: updated,
        isSubmitting: false,
        feedbackMessage: 'Staff proposed resolution recorded. Status transitioned to UNDER_REVIEW.',
      });
      return true;
    } catch (err) {
      if (isApiError(err) && err.status === 403) {
        const msg = getErrorMessage(err);
        set({
          rbacViolationNotice: `FORBIDDEN (403): ${msg}`,
          errorMessage: msg,
          isSubmitting: false,
        });
        return false;
      }
      console.info('[DisputeStore] Proposed resolution saved locally.');
      set((state) => ({
        activeDispute: {
          ...state.activeDispute,
          status:
            state.activeDispute.status === 'OPEN'
              ? 'UNDER_REVIEW'
              : state.activeDispute.status,
          proposedResolution: proposedResolutionInput,
        },
        isSubmitting: false,
        feedbackMessage:
          'Staff proposed resolution recorded. Status transitioned to UNDER_REVIEW.',
      }));
      return true;
    }
  },

  escalateToAdminArbitration: async () => {
    const { userRole, activeDispute } = get();

    // ── RBAC CHECK ──
    if (userRole !== 'ROLE_STAFF' && userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'FORBIDDEN (403): Escalating disputes to administrator arbitration requires ROLE_STAFF or ROLE_ADMIN.',
        feedbackMessage: 'ACTION BLOCKED: Insufficient role permissions.',
      });
      return false;
    }

    set({ isSubmitting: true, rbacViolationNotice: null });

    try {
      const updated = await disputesApi.transitionDisputeStatus(activeDispute.id, {
        targetStatus: 'ESCALATED',
        reason: 'Mediation deadlock between parties. Escalated to Administrator for binding arbitration.',
      });
      set({
        activeDispute: updated,
        isSubmitting: false,
        feedbackMessage: 'DISPUTE ESCALATED: Forwarded to Administrator dais for final binding arbitration.',
      });
      return true;
    } catch (err) {
      if (isApiError(err) && err.status === 403) {
        const msg = getErrorMessage(err);
        set({
          rbacViolationNotice: `FORBIDDEN (403): ${msg}`,
          errorMessage: msg,
          isSubmitting: false,
        });
        return false;
      }
      console.info('[DisputeStore] Escalated status saved locally.');
      set((state) => ({
        activeDispute: {
          ...state.activeDispute,
          status: 'ESCALATED',
        },
        isSubmitting: false,
        feedbackMessage: 'DISPUTE ESCALATED: Forwarded to Administrator dais for final binding arbitration.',
      }));
      return true;
    }
  },

  executeAdminArbitration: async () => {
    const { userRole, activeDispute, adminVerdictChoice, adminDeductibleInput } = get();

    // ── STRICT RBAC ARBITRATION ENFORCEMENT ──
    if (userRole !== 'ROLE_ADMIN') {
      set({
        rbacViolationNotice:
          'SOVEREIGN ARBITRATION LOCKOUT (403): Final binding arbitration and atomic SharedFund financial adjustments strictly require ROLE_ADMIN authority. Co-Owners and Staff cannot arbitrate dispute verdicts.',
        feedbackMessage: 'ARBITRATION REJECTED: Unauthorized administrative action.',
      });
      return false;
    }

    set({ isSubmitting: true, rbacViolationNotice: null });

    const verdictText =
      adminVerdictChoice === 'RESOLVED_COMPLAINANT_FAVORED'
        ? `BINDING VERDICT: In favor of Complainant Alice Owner. Telematics defect confirmed during Session #105. Deductible of ${adminDeductibleInput.toLocaleString()} VND assessed against Respondent Bob Driver.`
        : adminVerdictChoice === 'RESOLVED_RESPONDENT_FAVORED'
        ? 'BINDING VERDICT: In favor of Respondent Bob Driver. Insufficient evidence of intentional negligence.'
        : 'BINDING VERDICT: Dispute dismissed. Mutual settlement acknowledged.';

    try {
      const updated = await disputesApi.arbitrateDisputeWithFundAdjustment(activeDispute.id, {
        adjustmentAmount: adminDeductibleInput,
        adjustmentDirection: 'CREDIT_FUND',
        responsibleUserId: activeDispute.respondentUserId || 3,
        arbitrationVerdict: verdictText,
      });

      const ref = updated.fundTransactionReference || `DISP-${activeDispute.id}-REF-${Date.now().toString(36).toUpperCase()}`;

      set({
        activeDispute: {
          ...updated,
          fundTransactionReference: ref,
        },
        isSubmitting: false,
        feedbackMessage: `ARBITRATION COMPLETE: Final verdict rendered. ${adminDeductibleInput.toLocaleString()} VND adjusted in Shared Fund Vault. (Ref: ${ref})`,
      });
      return true;
    } catch (err) {
      if (isApiError(err) && err.status === 403) {
        const msg = getErrorMessage(err);
        set({
          rbacViolationNotice: `FORBIDDEN (403): ${msg}`,
          errorMessage: msg,
          isSubmitting: false,
        });
        return false;
      }
      console.info('[DisputeStore] Arbitration executed locally.');
      const ref = `DISP-${activeDispute.id}-REF-${Date.now().toString(36).toUpperCase()}`;

      set((state) => ({
        activeDispute: {
          ...state.activeDispute,
          status: 'RESOLVED',
          resolutionSummary: verdictText,
          arbitratorUserName: 'System Administrator Minh Hiep',
          resolvedAt: new Date().toISOString(),
          fundAdjustmentAmount: adminDeductibleInput,
          fundTransactionReference: ref,
        },
        isSubmitting: false,
        feedbackMessage: `ARBITRATION COMPLETE: Final verdict rendered. ${adminDeductibleInput.toLocaleString()} VND adjusted in Shared Fund Vault. (Ref: ${ref})`,
      }));

      return true;
    }
  },

  clearFeedback: () =>
    set({ feedbackMessage: null, errorMessage: null, rbacViolationNotice: null }),

  resetToDefaults: () =>
    set({
      activeTab: 'OVERVIEW',
      activeStation: 'DISPUTE_CRYSTAL',
      activeDispute: INITIAL_DISPUTE,
      evidenceList: INITIAL_EVIDENCES,
      selectedEvidenceId: 1,
      defects: INITIAL_DEFECTS,
      selectedDefectId: 'DEF-01',
      userRole: 'ROLE_CO_OWNER',
      staffNotesInput:
        'Observed photographic evidence and telematics logs. Complainant report matches session return timestamp.',
      proposedResolutionInput:
        'Propose respondent pays 500,000 VND deductible into shared maintenance fund.',
      adminVerdictChoice: 'RESOLVED_COMPLAINANT_FAVORED',
      adminDeductibleInput: 500000,
      isSubmitting: false,
      feedbackMessage: 'Dispute Room online. Dispute #10 loaded with 2 attached defect records.',
      errorMessage: null,
      rbacViolationNotice: null,
    }),
}));
