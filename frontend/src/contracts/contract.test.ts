import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useContractStore, CONTRACT_SECTIONS } from './useContractStore';
import { CONTRACT_LAYOUT } from './contractLayout';
import { contractsApi, type ContractDTO, type ContractSignatureDTO, type ContractSignaturesOverviewDTO } from '@/api/contractsApi';

describe('Digital Contract Room Subsystem (09-H)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    useContractStore.setState({
      groupId: 1,
      activeTab: 'DOCUMENT',
      activeContract: {
        id: 1,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        contractTitle: 'Co-Ownership Master Agreement v2.0 (Active)',
        contractTermsText: 'Digital co-ownership legal agreement governing VinFast VF8 City Edition.',
        version: 2,
        status: 'ACTIVE',
        effectiveDate: '2026-09-01',
        createdAt: '2026-08-28T10:00:00Z',
      },
      contractVersions: [
        {
          id: 1,
          groupId: 1,
          groupName: 'VinFast VF8 Founders Syndicate',
          contractTitle: 'Co-Ownership Master Agreement v2.0 (Active)',
          contractTermsText: 'Ratified master syndicate agreement.',
          version: 2,
          status: 'ACTIVE',
          effectiveDate: '2026-09-01',
          createdAt: '2026-08-28T10:00:00Z',
        },
        {
          id: 2,
          groupId: 1,
          groupName: 'VinFast VF8 Founders Syndicate',
          contractTitle: 'Co-Ownership Master Agreement v1.1 (Draft Amendment)',
          contractTermsText: 'Proposed amendment.',
          version: 3,
          status: 'PENDING_SIGNATURE',
          effectiveDate: '2026-10-01',
          createdAt: '2026-09-14T15:00:00Z',
        },
      ],
      signaturesOverview: {
        contractId: 1,
        contractVersion: 2,
        contractStatus: 'ACTIVE',
        totalRequiredSignatures: 4,
        totalSubmittedSignatures: 4,
        allSigned: true,
        signatures: [
          {
            id: 1,
            contractId: 1,
            contractVersion: 2,
            userId: 1,
            userFullName: 'Minh Hiep (You)',
            userEmail: 'hiep.minh@evshare.vn',
            signatureHash: 'a8f5c312d89b14c3e7f2231b4092d8e41198c642bb3901a2f4c1e0892a71f001',
            signedAt: '2026-08-29T09:15:00Z',
            ipAddress: '118.69.182.50',
          },
        ],
        pendingSigners: [],
      },
      currentSectionIndex: 0,
      acceptedTerms: false,
      signingState: 'IDLE',
      operationResult: { status: 'IDLE' },
      lastSignedHash: null,
      isLoading: false,
      isSyncing: false,
      error: null,
    });
  });

  describe('1. Spatial Layout & Camera Presets', () => {
    it('defines contract room sector coordinates and floor radius', () => {
      expect(CONTRACT_LAYOUT.sectorCenter).toEqual([40, 0, -80]);
      expect(CONTRACT_LAYOUT.floorRadius).toBe(16.0);
    });

    it('provides all 5 specialized camera focus presets for legal stations', () => {
      expect(CONTRACT_LAYOUT.cameras.ROOM_OVERVIEW).toBeDefined();
      expect(CONTRACT_LAYOUT.cameras.DOCUMENT_FOCUS).toBeDefined();
      expect(CONTRACT_LAYOUT.cameras.SIGNATURE_FOCUS).toBeDefined();
      expect(CONTRACT_LAYOUT.cameras.VERSION_FOCUS).toBeDefined();
      expect(CONTRACT_LAYOUT.cameras.STATUS_FOCUS).toBeDefined();

      expect(CONTRACT_LAYOUT.cameras.ROOM_OVERVIEW.fov).toBe(42);
      expect(CONTRACT_LAYOUT.cameras.DOCUMENT_FOCUS.fov).toBe(38);
    });
  });

  describe('2. Multi-Page Holographic Document Navigation', () => {
    it('contains all 5 comprehensive legal sections', () => {
      expect(CONTRACT_SECTIONS.length).toBe(5);
      expect(CONTRACT_SECTIONS[0].sectionNumber).toBe('SECTION I');
      expect(CONTRACT_SECTIONS[1].sectionNumber).toBe('SECTION II');
      expect(CONTRACT_SECTIONS[2].sectionNumber).toBe('SECTION III');
      expect(CONTRACT_SECTIONS[3].sectionNumber).toBe('SECTION IV');
      expect(CONTRACT_SECTIONS[4].sectionNumber).toBe('SECTION V');
    });

    it('navigates sequentially between sections and clamps boundaries', () => {
      const store = useContractStore.getState();
      expect(store.currentSectionIndex).toBe(0);

      store.nextSection();
      expect(useContractStore.getState().currentSectionIndex).toBe(1);

      store.nextSection();
      expect(useContractStore.getState().currentSectionIndex).toBe(2);

      store.prevSection();
      expect(useContractStore.getState().currentSectionIndex).toBe(1);

      // Clamp upper bound
      store.setSectionIndex(10);
      expect(useContractStore.getState().currentSectionIndex).toBe(4);

      // Clamp lower bound
      store.setSectionIndex(-5);
      expect(useContractStore.getState().currentSectionIndex).toBe(0);
    });
  });

  describe('3. Tab Switching & Version Selection', () => {
    it('switches between contract room tabs', () => {
      const store = useContractStore.getState();
      expect(store.activeTab).toBe('DOCUMENT');

      store.setActiveTab('VERSIONS');
      expect(useContractStore.getState().activeTab).toBe('VERSIONS');

      store.setActiveTab('SIGNATURES');
      expect(useContractStore.getState().activeTab).toBe('SIGNATURES');

      store.setActiveTab('STATUS_OVERVIEW');
      expect(useContractStore.getState().activeTab).toBe('STATUS_OVERVIEW');
    });

    it('loads selected version details and resets section pagination', async () => {
      const mockContract: ContractDTO = {
        id: 2,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        contractTitle: 'Co-Ownership Master Agreement v1.1 (Draft Amendment)',
        contractTermsText: 'Amendment text.',
        version: 3,
        status: 'PENDING_SIGNATURE',
        effectiveDate: '2026-10-01',
        createdAt: '2026-09-14T15:00:00Z',
      };

      const mockOverview: ContractSignaturesOverviewDTO = {
        contractId: 2,
        contractVersion: 3,
        contractStatus: 'PENDING_SIGNATURE',
        totalRequiredSignatures: 4,
        totalSubmittedSignatures: 2,
        allSigned: false,
        signatures: [],
        pendingSigners: [],
      };

      vi.spyOn(contractsApi, 'getContractById').mockResolvedValueOnce(mockContract);
      vi.spyOn(contractsApi, 'getSignaturesOverview').mockResolvedValueOnce(mockOverview);

      useContractStore.setState({ currentSectionIndex: 3, acceptedTerms: true });

      await useContractStore.getState().selectVersion(2);

      const state = useContractStore.getState();
      expect(state.activeContract?.id).toBe(2);
      expect(state.activeContract?.version).toBe(3);
      expect(state.currentSectionIndex).toBe(0);
      expect(state.acceptedTerms).toBe(false);
      expect(state.signaturesOverview?.totalSubmittedSignatures).toBe(2);
    });
  });

  describe('4. Biometric Digital Signature Execution & Real API Integration', () => {
    it('requires terms acceptance checkbox before signing', async () => {
      useContractStore.setState({ acceptedTerms: false });
      const signSpy = vi.spyOn(contractsApi, 'signContract');

      await useContractStore.getState().executeSignContract();

      const state = useContractStore.getState();
      expect(state.operationResult.status).toBe('ERROR');
      expect(state.operationResult.message).toContain('Legal terms acknowledgment required');
      expect(signSpy).not.toHaveBeenCalled();
    });

    it('submits digital signature, generates SHA-256 hash, and updates signatures overview', async () => {
      const mockSignature: ContractSignatureDTO = {
        id: 99,
        contractId: 1,
        contractVersion: 2,
        userId: 1,
        userFullName: 'Minh Hiep (You)',
        userEmail: 'hiep.minh@evshare.vn',
        signatureHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        signedAt: '2026-09-16T08:00:00Z',
        ipAddress: '118.69.182.50',
      };

      vi.spyOn(contractsApi, 'signContract').mockResolvedValueOnce(mockSignature);
      vi.spyOn(contractsApi, 'getSignaturesOverview').mockResolvedValueOnce({
        contractId: 1,
        contractVersion: 2,
        contractStatus: 'SIGNED',
        totalRequiredSignatures: 4,
        totalSubmittedSignatures: 4,
        allSigned: true,
        signatures: [mockSignature],
        pendingSigners: [],
      });

      useContractStore.setState({ acceptedTerms: true });

      await useContractStore.getState().executeSignContract();

      const state = useContractStore.getState();
      expect(state.signingState).toBe('SIGNED');
      expect(state.operationResult.status).toBe('SUCCESS');
      expect(state.lastSignedHash).toBe('e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855');
      expect(state.signaturesOverview?.signatures[0].signatureHash).toBe(
        'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
      );
    });
  });

  describe('5. Contract Status Lifecycle Transitions', () => {
    it('transitions contract status to ACTIVE via API', async () => {
      const updatedContract: ContractDTO = {
        id: 1,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        contractTitle: 'Co-Ownership Master Agreement v2.0 (Active)',
        contractTermsText: 'Ratified text.',
        version: 2,
        status: 'ACTIVE',
        effectiveDate: '2026-09-01',
        createdAt: '2026-08-28T10:00:00Z',
      };

      vi.spyOn(contractsApi, 'transitionStatus').mockResolvedValueOnce(updatedContract);

      await useContractStore.getState().transitionStatus('ACTIVE', 'Quorum achieved');

      const state = useContractStore.getState();
      expect(state.activeContract?.status).toBe('ACTIVE');
      expect(state.operationResult.status).toBe('SUCCESS');
    });
  });
});
