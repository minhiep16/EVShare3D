import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useDisputeStore } from './useDisputeStore';
import {
  DISPUTE_SECTOR_CENTER,
  DISPUTE_STATIONS,
  DISPUTE_CAMERA_PRESETS,
  DISPUTE_THEME,
} from './disputeLayout';

describe('Dispute Room & Arbitration Chamber (09-M)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useDisputeStore.getState().resetToDefaults();
  });

  describe('1. Dispute Chamber Architecture & Spatial Layout', () => {
    it('verifies dispute sector center coordinates at [-40, 0, 80]', () => {
      expect(DISPUTE_SECTOR_CENTER).toEqual([-40, 0, 80]);
    });

    it('verifies all 6 dispute stations are registered with spatial coordinates', () => {
      const stations = Object.keys(DISPUTE_STATIONS);
      expect(stations).toHaveLength(6);
      expect(stations).toContain('DISPUTE_CRYSTAL');
      expect(stations).toContain('DEFECT_HOLOTANK');
      expect(stations).toContain('EVIDENCE_CAROUSEL');
      expect(stations).toContain('STAFF_CONSOLE');
      expect(stations).toContain('ADMIN_DAIS');
      expect(stations).toContain('STATUS_STELA');

      // Verify relative positions
      expect(DISPUTE_STATIONS.DISPUTE_CRYSTAL.relativePosition).toEqual([0, 2.2, 0]);
      expect(DISPUTE_STATIONS.DEFECT_HOLOTANK.relativePosition).toEqual([0, 0, -1.8]);
      expect(DISPUTE_STATIONS.EVIDENCE_CAROUSEL.relativePosition).toEqual([-3.6, 0, 1.8]);
      expect(DISPUTE_STATIONS.STAFF_CONSOLE.relativePosition).toEqual([3.6, 0, 1.8]);
      expect(DISPUTE_STATIONS.ADMIN_DAIS.relativePosition).toEqual([0, 0, 4.2]);
      expect(DISPUTE_STATIONS.STATUS_STELA.relativePosition).toEqual([0, 0, -5.2]);
    });

    it('verifies camera presets for all station focus points', () => {
      const presets = Object.keys(DISPUTE_CAMERA_PRESETS);
      expect(presets).toHaveLength(6);
      expect(DISPUTE_CAMERA_PRESETS.CHAMBER_OVERVIEW).toBeDefined();
      expect(DISPUTE_CAMERA_PRESETS.CRYSTAL_FOCUS).toBeDefined();
      expect(DISPUTE_CAMERA_PRESETS.HOLOTANK_FOCUS).toBeDefined();
      expect(DISPUTE_CAMERA_PRESETS.EVIDENCE_FOCUS).toBeDefined();
      expect(DISPUTE_CAMERA_PRESETS.STAFF_CONSOLE_FOCUS).toBeDefined();
      expect(DISPUTE_CAMERA_PRESETS.ADMIN_DAIS_FOCUS).toBeDefined();
    });

    it('verifies dispute color theme', () => {
      expect(DISPUTE_THEME.primary).toBe('#ff1744');
      expect(DISPUTE_THEME.secondary).toBe('#fbbf24');
      expect(DISPUTE_THEME.darkBase).toBe('#1a0303');
    });
  });

  describe('2. Initial State & 3D Spatial Defect Mapping', () => {
    it('initializes with active dispute #10 and OPEN status', () => {
      const { activeDispute, userRole } = useDisputeStore.getState();
      expect(activeDispute.id).toBe(10);
      expect(activeDispute.status).toBe('OPEN');
      expect(activeDispute.complainantUserName).toBe('Alice Owner');
      expect(activeDispute.respondentUserName).toBe('Bob Driver');
      expect(userRole).toBe('ROLE_CO_OWNER');
    });

    it('loads 2 initial evidence files with 3D defect coordinates', () => {
      const { evidenceList, defects } = useDisputeStore.getState();
      expect(evidenceList).toHaveLength(2);
      expect(defects).toHaveLength(2);

      // Verify 3D defect coordinate mapping
      const defect1 = defects.find((d) => d.id === 'DEF-01');
      expect(defect1).toBeDefined();
      expect(defect1?.position).toEqual([-0.85, 0.45, -2.1]);
      expect(defect1?.severity).toBe('CRITICAL');
      expect(defect1?.evidenceId).toBe(1);

      const defect2 = defects.find((d) => d.id === 'DEF-02');
      expect(defect2).toBeDefined();
      expect(defect2?.position).toEqual([-0.45, 0.35, -2.15]);
      expect(defect2?.severity).toBe('MODERATE');
      expect(defect2?.evidenceId).toBe(2);
    });

    it('updates selection when a defect or evidence is selected', () => {
      useDisputeStore.getState().selectDefect('DEF-02');
      expect(useDisputeStore.getState().selectedDefectId).toBe('DEF-02');

      useDisputeStore.getState().selectEvidence(2);
      expect(useDisputeStore.getState().selectedEvidenceId).toBe(2);
    });
  });

  describe('3. Immutable Evidence Attachment (BR-DIS-01)', () => {
    it('attaches new evidence and registers matching 3D defect pin on holotank', async () => {
      const initialEvidenceCount = useDisputeStore.getState().evidenceList.length;
      const initialDefectCount = useDisputeStore.getState().defects.length;

      await useDisputeStore
        .getState()
        .attachEvidence('High-resolution photo of left fender dent.', [-0.88, 0.48, -2.08]);

      const state = useDisputeStore.getState();
      expect(state.evidenceList).toHaveLength(initialEvidenceCount + 1);
      expect(state.defects).toHaveLength(initialDefectCount + 1);

      const newDefect = state.defects[state.defects.length - 1];
      expect(newDefect.position).toEqual([-0.88, 0.48, -2.08]);
      expect(newDefect.label).toContain('High-resolution photo');
      expect(state.feedbackMessage).toContain('Evidence #');
    });
  });

  describe('4. Staff Mediation & RBAC Gating (BR-DIS-03)', () => {
    it('blocks ROLE_CO_OWNER from recording mediation notes', async () => {
      useDisputeStore.getState().setUserRole('ROLE_CO_OWNER');
      const success = await useDisputeStore.getState().recordStaffMediationNotes();

      expect(success).toBe(false);
      const state = useDisputeStore.getState();
      expect(state.rbacViolationNotice).toContain('ROLE_STAFF or ROLE_ADMIN');
    });

    it('blocks ROLE_CO_OWNER from proposing settlement resolutions', async () => {
      useDisputeStore.getState().setUserRole('ROLE_CO_OWNER');
      const success = await useDisputeStore.getState().proposeStaffResolution();

      expect(success).toBe(false);
      expect(useDisputeStore.getState().rbacViolationNotice).toContain(
        'ROLE_STAFF or ROLE_ADMIN'
      );
    });

    it('blocks ROLE_CO_OWNER from escalating dispute', async () => {
      useDisputeStore.getState().setUserRole('ROLE_CO_OWNER');
      const success = await useDisputeStore.getState().escalateToAdminArbitration();

      expect(success).toBe(false);
      expect(useDisputeStore.getState().rbacViolationNotice).toContain(
        'ROLE_STAFF or ROLE_ADMIN'
      );
    });

    it('allows ROLE_STAFF to record mediation notes and updates dispute status to UNDER_REVIEW', async () => {
      useDisputeStore.getState().setUserRole('ROLE_STAFF');
      useDisputeStore
        .getState()
        .setStaffNotesInput('Telematics telemetry confirms sensor impact timestamp.');

      const success = await useDisputeStore.getState().recordStaffMediationNotes();
      expect(success).toBe(true);

      const state = useDisputeStore.getState();
      expect(state.activeDispute.status).toBe('UNDER_REVIEW');
      expect(state.activeDispute.mediationNotes).toBe(
        'Telematics telemetry confirms sensor impact timestamp.'
      );
      expect(state.feedbackMessage).toContain('Mediation review notes recorded');
    });

    it('allows ROLE_STAFF to propose settlement resolution', async () => {
      useDisputeStore.getState().setUserRole('ROLE_STAFF');
      useDisputeStore
        .getState()
        .setProposedResolutionInput('50/50 shared deductible split between Alice and Bob.');

      const success = await useDisputeStore.getState().proposeStaffResolution();
      expect(success).toBe(true);

      const state = useDisputeStore.getState();
      expect(state.activeDispute.proposedResolution).toBe(
        '50/50 shared deductible split between Alice and Bob.'
      );
      expect(state.activeDispute.status).toBe('UNDER_REVIEW');
    });

    it('allows ROLE_STAFF to escalate dispute to ESCALATED status', async () => {
      useDisputeStore.getState().setUserRole('ROLE_STAFF');
      const success = await useDisputeStore.getState().escalateToAdminArbitration();

      expect(success).toBe(true);
      expect(useDisputeStore.getState().activeDispute.status).toBe('ESCALATED');
      expect(useDisputeStore.getState().feedbackMessage).toContain('DISPUTE ESCALATED');
    });
  });

  describe('5. Admin Binding Arbitration & Fund Adjustment (BR-DIS-05 / BR-DIS-06)', () => {
    it('blocks ROLE_STAFF from executing binding arbitration', async () => {
      useDisputeStore.getState().setUserRole('ROLE_STAFF');
      const success = await useDisputeStore.getState().executeAdminArbitration();

      expect(success).toBe(false);
      expect(useDisputeStore.getState().rbacViolationNotice).toContain('ROLE_ADMIN');
    });

    it('blocks ROLE_CO_OWNER from executing binding arbitration', async () => {
      useDisputeStore.getState().setUserRole('ROLE_CO_OWNER');
      const success = await useDisputeStore.getState().executeAdminArbitration();

      expect(success).toBe(false);
      expect(useDisputeStore.getState().rbacViolationNotice).toContain('ROLE_ADMIN');
    });

    it('allows ROLE_ADMIN to execute binding arbitration with atomic SharedFund balance adjustment', async () => {
      useDisputeStore.getState().setUserRole('ROLE_ADMIN');
      useDisputeStore.getState().setAdminVerdictChoice('RESOLVED_COMPLAINANT_FAVORED');
      useDisputeStore.getState().setAdminDeductibleInput(500000);

      const success = await useDisputeStore.getState().executeAdminArbitration();
      expect(success).toBe(true);

      const state = useDisputeStore.getState();
      expect(state.activeDispute.status).toBe('RESOLVED');
      expect(state.activeDispute.fundAdjustmentAmount).toBe(500000);
      expect(state.activeDispute.fundTransactionReference).toMatch(/^DISP-10-REF-/);
      expect(state.activeDispute.resolutionSummary).toContain('Alice Owner');
      expect(state.activeDispute.resolvedAt).toBeDefined();
      expect(state.feedbackMessage).toContain('ARBITRATION COMPLETE');
    });
  });
});
