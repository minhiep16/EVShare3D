import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useDisputeStore } from './useDisputeStore';
import { disputesApi } from '../api/disputesApi';
import { ApiError } from '../api/apiError';
import type { DisputeDTO, DisputeEvidenceDTO } from './disputeTypes';

// Mock the disputesApi module
vi.mock('../api/disputesApi', () => ({
  disputesApi: {
    getDisputesByGroup: vi.fn(),
    getDisputeById: vi.fn(),
    createDispute: vi.fn(),
    getEvidences: vi.fn(),
    addEvidence: vi.fn(),
    getDisputesForStaffReview: vi.fn(),
    addMediationNotes: vi.fn(),
    proposeResolution: vi.fn(),
    transitionDisputeStatus: vi.fn(),
    arbitrateDispute: vi.fn(),
    arbitrateDisputeWithFundAdjustment: vi.fn(),
    getArbitrationDossier: vi.fn(),
  },
}));

describe('09-X Dispute Integration & RBAC Arbitration Test Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useDisputeStore.getState().resetToDefaults();
  });

  describe('1. Dispute Creation & 3D Spatial Defect Pinning', () => {
    it('creates a new dispute with 3D defect coordinate annotations and updates store', async () => {
      const mockCreatedDispute: DisputeDTO = {
        id: 42,
        groupId: 1,
        groupName: 'Tesla Syndicate',
        usageSessionId: 202,
        complainantUserId: 5,
        complainantUserName: 'Alice Owner',
        respondentUserId: 9,
        respondentUserName: 'Charlie Renter',
        title: 'Left Mirror Housing Crushed',
        description: 'Side mirror cracked upon return from highway session.',
        status: 'OPEN',
        evidences: [
          {
            id: 101,
            disputeId: 42,
            uploadedByUserId: 5,
            uploadedByUserName: 'Alice Owner',
            fileUrl: 'https://storage.evshare.io/evidences/mirror_crack.jpg',
            mesh3dDefectCoordinates: JSON.stringify({ x: -0.95, y: 0.85, z: -0.4 }),
            description: 'Severe impact fracture on driver side mirror housing.',
            createdAt: '2026-09-16T14:00:00Z',
          },
        ],
        createdAt: '2026-09-16T14:00:00Z',
      };

      vi.mocked(disputesApi.createDispute).mockResolvedValue(mockCreatedDispute);

      const result = await useDisputeStore.getState().createDispute({
        groupId: 1,
        title: 'Left Mirror Housing Crushed',
        description: 'Side mirror cracked upon return from highway session.',
        relatedUsageSessionId: 202,
        respondentUserId: 9,
        initialEvidence: {
          fileUrl: 'https://storage.evshare.io/evidences/mirror_crack.jpg',
          description: 'Severe impact fracture on driver side mirror housing.',
          mesh3dDefectCoordinates: JSON.stringify({ x: -0.95, y: 0.85, z: -0.4 }),
        },
      });

      expect(result).toBeDefined();
      expect(result?.id).toBe(42);

      const state = useDisputeStore.getState();
      expect(state.activeDispute.id).toBe(42);
      expect(state.activeDispute.title).toBe('Left Mirror Housing Crushed');
      expect(state.evidenceList).toHaveLength(1);
      expect(state.defects).toHaveLength(1);
      expect(state.defects[0].position).toEqual([-0.95, 0.85, -0.4]);
      expect(state.defects[0].severity).toBe('CRITICAL');
      expect(state.feedbackMessage).toContain('Dispute #42 registered successfully');
    });

    it('attaches new evidence via API and updates the Defect Holotank in real-time', async () => {
      const mockEvidence: DisputeEvidenceDTO = {
        id: 77,
        disputeId: 10,
        uploadedByUserId: 5,
        uploadedByUserName: 'Alice Owner',
        fileUrl: 'https://storage.evshare.io/evidences/underbody_scratch.jpg',
        mesh3dDefectCoordinates: JSON.stringify({ x: 0.1, y: -0.2, z: 0.5 }),
        description: 'Battery underbody skid plate scratch.',
        createdAt: '2026-09-16T14:30:00Z',
      };

      vi.mocked(disputesApi.addEvidence).mockResolvedValue(mockEvidence);

      await useDisputeStore.getState().attachEvidence(
        'Battery underbody skid plate scratch.',
        [0.1, -0.2, 0.5],
        'https://storage.evshare.io/evidences/underbody_scratch.jpg'
      );

      const state = useDisputeStore.getState();
      expect(disputesApi.addEvidence).toHaveBeenCalledWith(
        10,
        expect.objectContaining({
          fileUrl: 'https://storage.evshare.io/evidences/underbody_scratch.jpg',
          description: 'Battery underbody skid plate scratch.',
        })
      );

      expect(state.evidenceList.some((e) => e.id === 77)).toBe(true);
      const addedDefect = state.defects.find((d) => d.evidenceId === 77);
      expect(addedDefect).toBeDefined();
      expect(addedDefect?.position).toEqual([0.1, -0.2, 0.5]);
      expect(state.selectedEvidenceId).toBe(77);
      expect(state.feedbackMessage).toContain('Evidence #77 attached with 3D defect coordinates');
    });
  });

  describe('2. Staff Mediation & Review Docket (ROLE_STAFF)', () => {
    it('blocks Co-Owners from fetching staff review docket and surfaces RBAC violation notice', async () => {
      useDisputeStore.getState().setUserRole('ROLE_CO_OWNER');

      await useDisputeStore.getState().fetchDisputesForStaffReview();

      expect(disputesApi.getDisputesForStaffReview).not.toHaveBeenCalled();
      const state = useDisputeStore.getState();
      expect(state.rbacViolationNotice).toContain('Staff mediation docket requires ROLE_STAFF or ROLE_ADMIN');
    });

    it('allows Staff to fetch disputes for review and synchronizes defects', async () => {
      useDisputeStore.getState().setUserRole('ROLE_STAFF');

      const mockDocket: DisputeDTO[] = [
        {
          id: 55,
          groupId: 2,
          groupName: 'VinFast VF9 Syndicate',
          complainantUserId: 3,
          complainantUserName: 'Dung Co-Owner',
          respondentUserId: 7,
          respondentUserName: 'Son Renter',
          title: 'Charging Port Door Latch Broken',
          description: 'Spring mechanism damaged post-session.',
          status: 'UNDER_REVIEW',
          evidences: [
            {
              id: 88,
              disputeId: 55,
              uploadedByUserId: 3,
              uploadedByUserName: 'Dung Co-Owner',
              fileUrl: 'https://storage.evshare.io/evidences/port_door.jpg',
              mesh3dDefectCoordinates: JSON.stringify({ x: 0.88, y: 0.6, z: 1.4 }),
              description: 'Latch snapped at hinge.',
              createdAt: '2026-09-16T12:00:00Z',
            },
          ],
          createdAt: '2026-09-16T11:00:00Z',
        },
      ];

      vi.mocked(disputesApi.getDisputesForStaffReview).mockResolvedValue(mockDocket);

      await useDisputeStore.getState().fetchDisputesForStaffReview();

      expect(disputesApi.getDisputesForStaffReview).toHaveBeenCalled();
      const state = useDisputeStore.getState();
      expect(state.activeDispute.id).toBe(55);
      expect(state.defects[0].position).toEqual([0.88, 0.6, 1.4]);
      expect(state.feedbackMessage).toContain('Staff Mediation Docket: Loaded 1 disputes');
    });

    it('allows Staff to record mediation notes and transition dispute status to UNDER_REVIEW', async () => {
      useDisputeStore.getState().setUserRole('ROLE_STAFF');
      useDisputeStore.getState().setStaffNotesInput('Verified telematics log matching complainant incident time.');

      const updatedDispute: DisputeDTO = {
        ...useDisputeStore.getState().activeDispute,
        status: 'UNDER_REVIEW',
        mediationNotes: 'Verified telematics log matching complainant incident time.',
        mediatorUserName: 'Staff Officer Linh',
      };

      vi.mocked(disputesApi.addMediationNotes).mockResolvedValue(updatedDispute);

      const success = await useDisputeStore.getState().recordStaffMediationNotes();
      expect(success).toBe(true);
      expect(disputesApi.addMediationNotes).toHaveBeenCalledWith(10, {
        mediationNotes: 'Verified telematics log matching complainant incident time.',
      });

      const state = useDisputeStore.getState();
      expect(state.activeDispute.status).toBe('UNDER_REVIEW');
      expect(state.activeDispute.mediationNotes).toContain('telematics log');
    });

    it('allows Staff to propose settlement terms', async () => {
      useDisputeStore.getState().setUserRole('ROLE_STAFF');
      useDisputeStore.getState().setProposedResolutionInput('Recommend respondent covers 400,000 VND parts cost.');

      const updatedDispute: DisputeDTO = {
        ...useDisputeStore.getState().activeDispute,
        status: 'UNDER_REVIEW',
        proposedResolution: 'Recommend respondent covers 400,000 VND parts cost.',
      };

      vi.mocked(disputesApi.proposeResolution).mockResolvedValue(updatedDispute);

      const success = await useDisputeStore.getState().proposeStaffResolution();
      expect(success).toBe(true);
      expect(disputesApi.proposeResolution).toHaveBeenCalledWith(10, {
        proposedResolution: 'Recommend respondent covers 400,000 VND parts cost.',
      });

      const state = useDisputeStore.getState();
      expect(state.activeDispute.proposedResolution).toBe('Recommend respondent covers 400,000 VND parts cost.');
    });

    it('escalates dispute to ESCALATED status when mediation deadlocks', async () => {
      useDisputeStore.getState().setUserRole('ROLE_STAFF');

      const escalatedDispute: DisputeDTO = {
        ...useDisputeStore.getState().activeDispute,
        status: 'ESCALATED',
      };

      vi.mocked(disputesApi.transitionDisputeStatus).mockResolvedValue(escalatedDispute);

      const success = await useDisputeStore.getState().escalateToAdminArbitration();
      expect(success).toBe(true);
      expect(disputesApi.transitionDisputeStatus).toHaveBeenCalledWith(10, {
        targetStatus: 'ESCALATED',
        reason: expect.stringContaining('Escalated to Administrator'),
      });

      const state = useDisputeStore.getState();
      expect(state.activeDispute.status).toBe('ESCALATED');
      expect(state.feedbackMessage).toContain('DISPUTE ESCALATED');
    });
  });

  describe('3. Sovereign Admin Arbitration & Fund Adjustment (ROLE_ADMIN)', () => {
    it('blocks Co-Owners and Staff from executing binding arbitration', async () => {
      // 1. Co-Owner blocked
      useDisputeStore.getState().setUserRole('ROLE_CO_OWNER');
      let success = await useDisputeStore.getState().executeAdminArbitration();
      expect(success).toBe(false);
      expect(disputesApi.arbitrateDisputeWithFundAdjustment).not.toHaveBeenCalled();
      expect(useDisputeStore.getState().rbacViolationNotice).toContain('SOVEREIGN ARBITRATION LOCKOUT (403)');

      // 2. Staff blocked
      useDisputeStore.getState().setUserRole('ROLE_STAFF');
      success = await useDisputeStore.getState().executeAdminArbitration();
      expect(success).toBe(false);
      expect(disputesApi.arbitrateDisputeWithFundAdjustment).not.toHaveBeenCalled();
      expect(useDisputeStore.getState().rbacViolationNotice).toContain('SOVEREIGN ARBITRATION LOCKOUT (403)');
    });

    it('allows Admin to execute binding arbitration with atomic SharedFund adjustment', async () => {
      useDisputeStore.getState().setUserRole('ROLE_ADMIN');
      useDisputeStore.getState().setAdminVerdictChoice('RESOLVED_COMPLAINANT_FAVORED');
      useDisputeStore.getState().setAdminDeductibleInput(750000);

      const resolvedDispute: DisputeDTO = {
        ...useDisputeStore.getState().activeDispute,
        status: 'RESOLVED',
        resolutionSummary: 'BINDING VERDICT: In favor of Complainant Alice Owner. Deductible of 750,000 VND assessed.',
        fundAdjustmentAmount: 750000,
        fundTransactionReference: 'TX-DISP-10-ADJUST',
        resolvedAt: '2026-09-16T15:00:00Z',
      };

      vi.mocked(disputesApi.arbitrateDisputeWithFundAdjustment).mockResolvedValue(resolvedDispute);

      const success = await useDisputeStore.getState().executeAdminArbitration();
      expect(success).toBe(true);

      expect(disputesApi.arbitrateDisputeWithFundAdjustment).toHaveBeenCalledWith(10, {
        adjustmentAmount: 750000,
        adjustmentDirection: 'CREDIT_FUND',
        responsibleUserId: 3,
        arbitrationVerdict: expect.stringContaining('BINDING VERDICT'),
      });

      const state = useDisputeStore.getState();
      expect(state.activeDispute.status).toBe('RESOLVED');
      expect(state.activeDispute.fundAdjustmentAmount).toBe(750000);
      expect(state.activeDispute.fundTransactionReference).toBe('TX-DISP-10-ADJUST');
      expect(state.feedbackMessage).toContain('ARBITRATION COMPLETE: Final verdict rendered');
    });

    it('properly handles and surfaces HTTP 403 Forbidden ApiError from backend', async () => {
      useDisputeStore.getState().setUserRole('ROLE_ADMIN');

      // Simulate backend rejecting with HTTP 403 ApiError (e.g. invalid JWT token or expired session)
      const forbiddenError = new ApiError({
        message: 'Forbidden: Access denied to administrative arbitration docket',
        status: 403,
        code: 'FORBIDDEN',
      });
      vi.mocked(disputesApi.arbitrateDisputeWithFundAdjustment).mockRejectedValue(forbiddenError);

      const success = await useDisputeStore.getState().executeAdminArbitration();
      expect(success).toBe(false);

      const state = useDisputeStore.getState();
      expect(state.rbacViolationNotice).toContain('FORBIDDEN (403): Forbidden: Access denied');
      expect(state.errorMessage).toContain('Forbidden: Access denied');
    });
  });
});
