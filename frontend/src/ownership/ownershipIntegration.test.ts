import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useOwnershipStore, SEEDED_SYNDICATES } from './useOwnershipStore';
import { ownershipGroupsApi } from '@/api/ownershipGroupsApi';
import { contractsApi } from '@/api/contractsApi';
import { useCameraStore } from '@/stores/useCameraStore';

vi.mock('@/stores/useCameraStore', () => ({
  useCameraStore: {
    getState: vi.fn(() => ({
      moveTo: vi.fn(),
    })),
  },
}));

describe('09-S: Ownership Integration — Co-Ownership Hall & Backend Contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Reset store state
    useOwnershipStore.setState({
      groups: JSON.parse(JSON.stringify(SEEDED_SYNDICATES)),
      activeGroupId: 1,
      selectedMemberId: null,
      hoveredMemberId: null,
      cameraPreset: 'HALL_OVERVIEW',
      ownershipHistory: [],
      authoritativeValidation: null,
      isSigning: false,
      signatureSuccess: false,
      isLoading: false,
      errorMessage: null,
      actionNotice: null,
    });
  });

  describe('1. Authoritative Backend Ownership Invariant (BR-OWN-01)', () => {
    it('queries backend endpoint to authoritatively validate 100.00% equity distribution', async () => {
      const validateSpy = vi
        .spyOn(ownershipGroupsApi, 'validateGroupShares')
        .mockResolvedValueOnce({
          valid: true,
          totalPercentage: 100.0,
          groupId: 1,
        });

      const isValid = await useOwnershipStore.getState().validateAuthoritativeEquity(1);

      expect(validateSpy).toHaveBeenCalledWith(1);
      expect(isValid).toBe(true);

      const validation = useOwnershipStore.getState().authoritativeValidation;
      expect(validation).not.toBeNull();
      expect(validation?.isValid).toBe(true);
      expect(validation?.totalEquity).toBe(100.0);
      expect(validation?.validatedBy).toBe('SPRING_BOOT_AUTHORITATIVE_SERVICE');
      expect(validation?.timestamp).toBeDefined();
    });

    it('flags invariant failure when backend service reports non-100% distribution', async () => {
      vi.spyOn(ownershipGroupsApi, 'validateGroupShares').mockResolvedValueOnce({
        valid: false,
        totalPercentage: 92.5,
        groupId: 1,
      });

      const isValid = await useOwnershipStore.getState().validateAuthoritativeEquity(1);

      expect(isValid).toBe(false);
      const validation = useOwnershipStore.getState().authoritativeValidation;
      expect(validation?.isValid).toBe(false);
      expect(validation?.totalEquity).toBe(92.5);
    });

    it('falls back gracefully if backend validation endpoint errors', async () => {
      vi.spyOn(ownershipGroupsApi, 'validateGroupShares').mockRejectedValueOnce(
        new Error('Network error or endpoint unseeded')
      );

      const isValid = await useOwnershipStore.getState().validateAuthoritativeEquity(1);

      // Seeded group 1 has 40 + 35 + 25 = 100
      expect(isValid).toBe(true);
      const validation = useOwnershipStore.getState().authoritativeValidation;
      expect(validation?.isValid).toBe(true);
      expect(validation?.validatedBy).toBe('SPRING_BOOT_AUTHORITATIVE_FALLBACK');
    });
  });

  describe('2. Live Group & Shares Hydration', () => {
    it('hydrates syndicate groups and member share certificates from backend DTOs', async () => {
      const mockBackendGroups = [
        {
          id: 1,
          groupName: 'Quantum Mobility Syndicate',
          vehicleId: 10,
          vehicleModelName: 'Lucid Air Grand Touring',
          vehicleLicensePlate: '30H-777.77',
          formationDate: '2026-03-01',
          isActive: true,
          memberShares: [
            {
              id: 501,
              userId: 1,
              userName: 'Tran Van Alpha',
              userEmail: 'alpha@evshare.vn',
              sharePercentage: 60.0,
              votingPowerPercentage: 60.0,
              isRepresentative: true,
              shareCertificateNumber: 'CERT-G1-U1-9988',
              isActive: true,
            },
            {
              id: 502,
              userId: 2,
              userName: 'Le Thi Beta',
              userEmail: 'beta@evshare.vn',
              sharePercentage: 40.0,
              votingPowerPercentage: 40.0,
              isRepresentative: false,
              shareCertificateNumber: 'CERT-G1-U2-9989',
              isActive: true,
            },
          ],
        },
      ];

      vi.spyOn(ownershipGroupsApi, 'getGroups').mockResolvedValueOnce(mockBackendGroups as any);
      vi.spyOn(ownershipGroupsApi, 'validateGroupShares').mockResolvedValueOnce({
        valid: true,
        totalPercentage: 100.0,
        groupId: 1,
      });
      vi.spyOn(ownershipGroupsApi, 'getGroupOwnershipHistory').mockResolvedValueOnce([]);
      vi.spyOn(contractsApi, 'getActiveContractByGroupId').mockResolvedValueOnce(null);

      await useOwnershipStore.getState().fetchOwnershipData();

      const { groups, activeGroupId, authoritativeValidation } = useOwnershipStore.getState();
      expect(groups).toHaveLength(1);
      expect(groups[0].name).toBe('Quantum Mobility Syndicate');
      expect(groups[0].vehicleModelName).toBe('Lucid Air Grand Touring');
      expect(groups[0].members).toHaveLength(2);
      expect(groups[0].members[0].shareCertificateNumber).toBe('CERT-G1-U1-9988');
      expect(groups[0].members[0].sharePercentage).toBe(60.0);
      expect(activeGroupId).toBe(1);
      expect(authoritativeValidation?.isValid).toBe(true);
    });

    it('falls back to seeded syndicates when backend getGroups returns empty or fails', async () => {
      vi.spyOn(ownershipGroupsApi, 'getGroups').mockRejectedValueOnce(new Error('500 Internal Error'));

      await useOwnershipStore.getState().fetchOwnershipData();

      const { groups } = useOwnershipStore.getState();
      expect(groups.length).toBeGreaterThan(0);
      expect(groups[0].name).toBe('Apex Syndicate');
    });
  });

  describe('3. Ownership Audit History (Ledger)', () => {
    it('retrieves chronological share transfer and issuance history from backend', async () => {
      const mockHistory = [
        {
          id: 901,
          shareId: 101,
          action: 'ISSUE_SHARE',
          actingUserId: 1,
          previousPercentage: 0,
          newPercentage: 50.0,
          newCertificateNumber: 'CERT-G1-U1-ORIG',
          previousIsActive: false,
          newIsActive: true,
          effectiveDate: '2026-01-15T10:00:00Z',
        },
        {
          id: 902,
          shareId: 101,
          action: 'TRANSFER_SHARE',
          actingUserId: 1,
          previousPercentage: 50.0,
          newPercentage: 40.0,
          newCertificateNumber: 'CERT-G1-U1-A882',
          previousIsActive: true,
          newIsActive: true,
          effectiveDate: '2026-02-01T14:30:00Z',
        },
      ];

      vi.spyOn(ownershipGroupsApi, 'getGroupOwnershipHistory').mockResolvedValueOnce(mockHistory as any);

      await useOwnershipStore.getState().fetchGroupHistory(1);

      const { ownershipHistory } = useOwnershipStore.getState();
      expect(ownershipHistory).toHaveLength(2);
      expect(ownershipHistory[0].action).toBe('ISSUE_SHARE');
      expect(ownershipHistory[1].action).toBe('TRANSFER_SHARE');
      expect(ownershipHistory[1].previousPercentage).toBe(50.0);
      expect(ownershipHistory[1].newPercentage).toBe(40.0);
    });
  });

  describe('4. Master Contract & Digital Signatures (SHA-256)', () => {
    it('loads active group contract with signature matrix and confirms digital signature', async () => {
      const mockContract = {
        id: 77,
        groupId: 1,
        groupName: 'Apex Syndicate',
        contractTitle: 'Apex Master Co-Ownership Agreement',
        version: 1,
        status: 'PENDING_SIGNATURE',
        effectiveDate: '2026-01-15',
      };

      const mockSignaturesOverview = {
        contractId: 77,
        signatures: [
          {
            userId: 2,
            userName: 'Tran Thi B',
            signedAt: '2026-01-16T08:00:00Z',
            signatureHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
          },
        ],
      };

      vi.spyOn(contractsApi, 'getActiveContractByGroupId').mockResolvedValueOnce(mockContract as any);
      vi.spyOn(contractsApi, 'getSignaturesOverview').mockResolvedValueOnce(mockSignaturesOverview as any);

      await useOwnershipStore.getState().fetchGroupContract(1);

      const { activeContract } = useOwnershipStore.getState();
      expect(activeContract).not.toBeNull();
      expect(activeContract?.id).toBe(77);
      expect(activeContract?.signatures).toBeDefined();

      // Sign contract as user 1
      const signSpy = vi.spyOn(contractsApi, 'signContract').mockResolvedValueOnce({
        id: 888,
        contractId: 77,
        userId: 1,
        status: 'SIGNED',
        signedAt: new Date().toISOString(),
        signatureHash: 'abcdef1234567890',
      } as any);

      await useOwnershipStore.getState().signActiveContract();

      expect(signSpy).toHaveBeenCalledWith(77, expect.objectContaining({
        acceptTerms: true,
      }));

      const stateAfterSign = useOwnershipStore.getState();
      expect(stateAfterSign.signatureSuccess).toBe(true);
      expect(stateAfterSign.actionNotice).toContain('DIGITAL SIGNATURE CONFIRMED');
    });
  });

  describe('5. Pure 3D Interactions & Spatial Navigation', () => {
    it('sets camera presets for Amphitheater, Equity Core, Contract, and Audit Ledger', () => {
      const { setCameraPreset } = useOwnershipStore.getState();

      setCameraPreset('EQUITY_CORE');
      expect(useOwnershipStore.getState().cameraPreset).toBe('EQUITY_CORE');

      setCameraPreset('CONTRACT_TERMINAL');
      expect(useOwnershipStore.getState().cameraPreset).toBe('CONTRACT_TERMINAL');

      setCameraPreset('HISTORY_STELA');
      expect(useOwnershipStore.getState().cameraPreset).toBe('HISTORY_STELA');

      setCameraPreset('HALL_OVERVIEW');
      expect(useOwnershipStore.getState().cameraPreset).toBe('HALL_OVERVIEW');
    });

    it('animates camera toward member pedestal on selectMember', () => {
      const moveToSpy = vi.fn();
      vi.mocked(useCameraStore.getState).mockReturnValue({ moveTo: moveToSpy } as any);

      useOwnershipStore.getState().selectMember(101);

      expect(useOwnershipStore.getState().selectedMemberId).toBe(101);
      expect(moveToSpy).toHaveBeenCalled();
    });

    it('switches active syndicate and re-validates equity invariants', async () => {
      const validateSpy = vi
        .spyOn(ownershipGroupsApi, 'validateGroupShares')
        .mockResolvedValue({
          valid: true,
          totalPercentage: 100.0,
          groupId: 2,
        });

      await useOwnershipStore.getState().setActiveGroup(2);

      expect(useOwnershipStore.getState().activeGroupId).toBe(2);
      expect(validateSpy).toHaveBeenCalledWith(2);
    });
  });
});
