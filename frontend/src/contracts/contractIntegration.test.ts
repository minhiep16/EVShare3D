import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useContractStore, getActiveContractSections, CONTRACT_SECTIONS } from './useContractStore';
import { contractsApi, type ContractDTO, type ContractSignaturesOverviewDTO, type ContractSignatureDTO } from '@/api/contractsApi';

describe('Phase 09-V: Contract Integration Layer (Contract Room, Lectern, Version Stela, Biometric Dais)', () => {
  const sampleActiveContract: ContractDTO = {
    id: 10,
    groupId: 1,
    groupName: 'VinFast VF8 Founders Syndicate',
    contractTitle: 'Co-Ownership Master Agreement v2.0 (Active)',
    contractTermsText: 'Authoritative Spring Boot master terms governing shared VF8 telemetry, maintenance, and voting.',
    version: 2,
    status: 'ACTIVE',
    effectiveDate: '2026-09-01',
    expiryDate: '2027-09-01',
    createdAt: '2026-08-28T10:00:00Z',
  };

  const sampleVersions: ContractDTO[] = [
    sampleActiveContract,
    {
      id: 9,
      groupId: 1,
      groupName: 'VinFast VF8 Founders Syndicate',
      contractTitle: 'Co-Ownership Master Agreement v1.0 (Superseded)',
      contractTermsText: 'Historical delivery contract.',
      version: 1,
      status: 'SIGNED',
      effectiveDate: '2026-01-15',
      createdAt: '2026-01-10T08:00:00Z',
    },
  ];

  const sampleSignaturesOverview: ContractSignaturesOverviewDTO = {
    contractId: 10,
    contractVersion: 2,
    contractStatus: 'ACTIVE',
    totalRequiredSignatures: 4,
    totalSubmittedSignatures: 4,
    allSigned: true,
    signatures: [
      {
        id: 1,
        contractId: 10,
        contractVersion: 2,
        userId: 1,
        userFullName: 'Minh Hiep (You)',
        userEmail: 'hiep.minh@evshare.vn',
        signatureHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        signedAt: '2026-08-29T09:15:00Z',
        ipAddress: '118.69.182.50',
      },
    ],
    pendingSigners: [],
  };

  beforeEach(() => {
    vi.restoreAllMocks();

    useContractStore.setState({
      groupId: 1,
      activeTab: 'DOCUMENT',
      activeContract: sampleActiveContract,
      contractVersions: sampleVersions,
      signaturesOverview: sampleSignaturesOverview,
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

  describe('1. Dynamic 3D Document Integration with Backend Response', () => {
    it('reactively binds backend title, version, and contractTermsText into 3D sections', () => {
      const state = useContractStore.getState();
      const sections = getActiveContractSections(state.activeContract);

      expect(sections).toHaveLength(5);
      expect(sections[0].title).toBe(sampleActiveContract.contractTitle.toUpperCase());
      expect(sections[0].subtitle).toContain('Version 2.0');
      expect(sections[0].subtitle).toContain('Status: ACTIVE');
      expect(sections[0].content).toBe(sampleActiveContract.contractTermsText);
      expect(sections[0].keyClauses[0]).toContain('Law No. 20/2023/QH15');
    });

    it('falls back to standard default sections if active contract is null', () => {
      const sections = getActiveContractSections(null);
      expect(sections).toEqual(CONTRACT_SECTIONS);
    });
  });

  describe('2. Contract Draft Creation Pipeline (POST /api/v1/contracts)', () => {
    it('creates a new contract draft and updates 3D Document Lectern state', async () => {
      const newlyDraftedContract: ContractDTO = {
        id: 11,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        contractTitle: 'Co-Ownership Master Agreement v3.0 (Draft Amendment)',
        contractTermsText: 'Amendment v3.0 codifying battery health warranties and fast-charging quotas.',
        version: 3,
        status: 'DRAFT',
        effectiveDate: '2026-10-01',
        createdAt: '2026-09-16T12:00:00Z',
      };

      const createSpy = vi.spyOn(contractsApi, 'createContract').mockResolvedValueOnce(newlyDraftedContract);
      const listSpy = vi.spyOn(contractsApi, 'getContractsByGroupId').mockResolvedValueOnce([
        newlyDraftedContract,
        ...sampleVersions,
      ]);
      const overviewSpy = vi.spyOn(contractsApi, 'getSignaturesOverview').mockResolvedValueOnce({
        contractId: 11,
        contractVersion: 3,
        contractStatus: 'DRAFT',
        totalRequiredSignatures: 4,
        totalSubmittedSignatures: 0,
        allSigned: false,
        signatures: [],
        pendingSigners: [
          { userId: 1, userFullName: 'Minh Hiep', userEmail: 'hiep@evshare.vn', sharePercentage: 35 },
          { userId: 2, userFullName: 'Tran Duc', userEmail: 'duc@evshare.vn', sharePercentage: 25 },
        ],
      });

      const success = await useContractStore.getState().createContractDraft({
        contractTitle: 'Co-Ownership Master Agreement v3.0 (Draft Amendment)',
        contractTermsText: 'Amendment v3.0 codifying battery health warranties and fast-charging quotas.',
      });

      expect(success).toBe(true);
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          contractTitle: 'Co-Ownership Master Agreement v3.0 (Draft Amendment)',
        })
      );

      const state = useContractStore.getState();
      expect(state.activeContract?.id).toBe(11);
      expect(state.activeContract?.version).toBe(3);
      expect(state.activeContract?.status).toBe('DRAFT');
      expect(state.contractVersions).toHaveLength(3);
      expect(state.signaturesOverview?.totalSubmittedSignatures).toBe(0);

      // Verify 3D document sections reactively update to the new draft
      const sections = getActiveContractSections(state.activeContract);
      expect(sections[0].title).toBe('CO-OWNERSHIP MASTER AGREEMENT V3.0 (DRAFT AMENDMENT)');
      expect(sections[0].content).toContain('Amendment v3.0 codifying battery health warranties');
    });
  });

  describe('3. Version History & Switching (GET /api/v1/contracts/{id})', () => {
    it('fetches full version history for the ownership group on mount', async () => {
      vi.spyOn(contractsApi, 'getContractsByGroupId').mockResolvedValueOnce(sampleVersions);
      vi.spyOn(contractsApi, 'getActiveContractByGroupId').mockResolvedValueOnce(sampleActiveContract);
      vi.spyOn(contractsApi, 'getSignaturesOverview').mockResolvedValueOnce(sampleSignaturesOverview);

      await useContractStore.getState().fetchContractData(1);

      const state = useContractStore.getState();
      expect(state.contractVersions).toHaveLength(2);
      expect(state.activeContract?.id).toBe(10);
      expect(state.signaturesOverview?.allSigned).toBe(true);
    });

    it('switches loaded version onto 3D Lectern when selected from Version Stela', async () => {
      const historicalContract: ContractDTO = {
        id: 9,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        contractTitle: 'Co-Ownership Master Agreement v1.0 (Historical)',
        contractTermsText: 'Initial delivery terms executed at showroom delivery.',
        version: 1,
        status: 'SIGNED',
        effectiveDate: '2026-01-15',
        createdAt: '2026-01-10T08:00:00Z',
      };

      vi.spyOn(contractsApi, 'getContractById').mockResolvedValueOnce(historicalContract);
      vi.spyOn(contractsApi, 'getSignaturesOverview').mockResolvedValueOnce({
        contractId: 9,
        contractVersion: 1,
        contractStatus: 'SIGNED',
        totalRequiredSignatures: 4,
        totalSubmittedSignatures: 4,
        allSigned: true,
        signatures: [],
        pendingSigners: [],
      });

      await useContractStore.getState().selectVersion(9);

      const state = useContractStore.getState();
      expect(state.activeContract?.id).toBe(9);
      expect(state.activeContract?.version).toBe(1);

      const sections = getActiveContractSections(state.activeContract);
      expect(sections[0].title).toBe('CO-OWNERSHIP MASTER AGREEMENT V1.0 (HISTORICAL)');
      expect(sections[0].content).toBe('Initial delivery terms executed at showroom delivery.');
    });
  });

  describe('4. Biometric SHA-256 Multi-Sig Signing (POST /api/v1/contracts/{id}/sign)', () => {
    it('requires terms acknowledgment before executing cryptographic signature', async () => {
      useContractStore.setState({
        acceptedTerms: false,
      });

      const signSpy = vi.spyOn(contractsApi, 'signContract');

      await useContractStore.getState().executeSignContract();

      expect(signSpy).not.toHaveBeenCalled();
      const state = useContractStore.getState();
      expect(state.operationResult.status).toBe('ERROR');
      expect(state.operationResult.message).toContain('Legal terms acknowledgment required');
    });

    it('submits signature, captures SHA-256 digest, and updates quorum overview', async () => {
      const pendingContract: ContractDTO = {
        id: 11,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        contractTitle: 'Co-Ownership Master Agreement v3.0 (Pending Signatures)',
        contractTermsText: 'Amendment v3.0.',
        version: 3,
        status: 'PENDING_SIGNATURE',
        effectiveDate: '2026-10-01',
        createdAt: '2026-09-16T12:00:00Z',
      };

      const pendingOverview: ContractSignaturesOverviewDTO = {
        contractId: 11,
        contractVersion: 3,
        contractStatus: 'PENDING_SIGNATURE',
        totalRequiredSignatures: 2,
        totalSubmittedSignatures: 1,
        allSigned: false,
        signatures: [
          {
            id: 101,
            contractId: 11,
            contractVersion: 3,
            userId: 2,
            userFullName: 'Tran Duc',
            userEmail: 'duc.tran@evshare.vn',
            signatureHash: '7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069',
            signedAt: '2026-09-16T12:10:00Z',
            ipAddress: '14.161.22.84',
          },
        ],
        pendingSigners: [
          { userId: 1, userFullName: 'Minh Hiep (You)', userEmail: 'hiep.minh@evshare.vn', sharePercentage: 35 },
        ],
      };

      useContractStore.setState({
        activeContract: pendingContract,
        signaturesOverview: pendingOverview,
        acceptedTerms: true,
      });

      const newSignature: ContractSignatureDTO = {
        id: 102,
        contractId: 11,
        contractVersion: 3,
        userId: 1,
        userFullName: 'Minh Hiep (You)',
        userEmail: 'hiep.minh@evshare.vn',
        signatureHash: 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
        signedAt: '2026-09-16T12:30:00Z',
        ipAddress: '118.69.182.50',
      };

      vi.spyOn(contractsApi, 'signContract').mockResolvedValueOnce(newSignature);
      vi.spyOn(contractsApi, 'getSignaturesOverview').mockResolvedValueOnce({
        ...pendingOverview,
        totalSubmittedSignatures: 2,
        allSigned: true,
        contractStatus: 'SIGNED',
        signatures: [newSignature, ...pendingOverview.signatures],
        pendingSigners: [],
      });
      vi.spyOn(contractsApi, 'getContractById').mockResolvedValueOnce({
        ...pendingContract,
        status: 'SIGNED',
      });

      await useContractStore.getState().executeSignContract();

      const state = useContractStore.getState();
      expect(state.signingState).toBe('SIGNED');
      expect(state.lastSignedHash).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad');
      expect(state.activeContract?.status).toBe('SIGNED');
      expect(state.signaturesOverview?.allSigned).toBe(true);
    });
  });

  describe('5. Contract Lifecycle State Machine (PATCH /api/v1/contracts/{id}/status)', () => {
    it('transitions contract from DRAFT to PENDING_SIGNATURE', async () => {
      const draftContract: ContractDTO = {
        ...sampleActiveContract,
        id: 12,
        status: 'DRAFT',
      };

      useContractStore.setState({
        activeContract: draftContract,
        contractVersions: [draftContract],
      });

      const transitionedContract: ContractDTO = {
        ...draftContract,
        status: 'PENDING_SIGNATURE',
      };

      const patchSpy = vi.spyOn(contractsApi, 'transitionStatus').mockResolvedValueOnce(transitionedContract);

      await useContractStore.getState().transitionStatus('PENDING_SIGNATURE', 'Draft submitted for signatures');

      expect(patchSpy).toHaveBeenCalledWith(12, {
        targetStatus: 'PENDING_SIGNATURE',
        reason: 'Draft submitted for signatures',
      });

      const state = useContractStore.getState();
      expect(state.activeContract?.status).toBe('PENDING_SIGNATURE');
      expect(state.contractVersions[0].status).toBe('PENDING_SIGNATURE');
    });

    it('transitions contract from SIGNED to ACTIVE and updates 3D status seal', async () => {
      const signedContract: ContractDTO = {
        ...sampleActiveContract,
        id: 12,
        status: 'SIGNED',
      };

      useContractStore.setState({
        activeContract: signedContract,
        contractVersions: [signedContract],
      });

      const activatedContract: ContractDTO = {
        ...signedContract,
        status: 'ACTIVE',
      };

      vi.spyOn(contractsApi, 'transitionStatus').mockResolvedValueOnce(activatedContract);

      await useContractStore.getState().transitionStatus('ACTIVE', 'Multi-signature quorum reached');

      const state = useContractStore.getState();
      expect(state.activeContract?.status).toBe('ACTIVE');
      expect(state.operationResult.status).toBe('SUCCESS');
    });
  });
});
