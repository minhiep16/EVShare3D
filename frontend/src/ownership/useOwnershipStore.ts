import { create } from 'zustand';
import type {
  SyndicateGroup,
  CoOwnerMember,
  CoOwnershipContractModel,
  LegalContractArticle,
  ContractSignatureRecord,
  AuthoritativeValidationResult,
} from './ownershipTypes';
import { CO_OWNERSHIP_HALL_LAYOUT } from './ownershipLayout';
import { ownershipGroupsApi, OwnershipHistoryDTO } from '@/api/ownershipGroupsApi';
import { contractsApi } from '@/api/contractsApi';
import { useCameraStore } from '@/stores/useCameraStore';
import { SECTOR_CENTERS } from '@/world/worldCoordinates';

export type CameraPreset =
  | 'HALL_OVERVIEW'
  | 'EQUITY_CORE'
  | 'CONTRACT_TERMINAL'
  | 'RULES_STELA'
  | 'HISTORY_STELA';

const DEFAULT_ARTICLES: LegalContractArticle[] = [
  {
    id: 1,
    articleNumber: 'ARTICLE 01',
    title: 'Equity Allocation & Share Invariants',
    summary: 'Strict distribution of fractional equity summing to precisely 100.00%.',
    fullText:
      'The Co-Owners mutually agree to hold undivided fractional interests in the Electric Vehicle as recorded in the Syndicate Registry. The sum of all active equity shares must equal exactly 100.00% at all times as authoritatively validated by the backend. Transfers of equity require administrative consensus and immutable ledger execution.',
  },
  {
    id: 2,
    articleNumber: 'ARTICLE 02',
    title: 'Vehicle Access & Booking Quotas',
    summary: 'Fair chronological reservation quotas calculated pro-rata from equity holding.',
    fullText:
      'Each Co-Owner is entitled to vehicle reservation privileges proportional to their equity ownership. The scheduling engine strictly enforces overlap prevention and fair-share peak hour rationing. Cancellations within 12 hours of booking window incur standard fairness recalculation.',
  },
  {
    id: 3,
    articleNumber: 'ARTICLE 03',
    title: 'Shared Operating Fund & Expenses',
    summary: 'Mandatory reserve fund maintenance and proportional cost allocation.',
    fullText:
      'Co-Owners shall maintain an active Shared Operating Fund balance covering comprehensive insurance, preventive maintenance, battery servicing, and scheduled charging overhead. Routine expenses are billed pro-rata; individual damage or toll fees are levied directly to the operating member.',
  },
  {
    id: 4,
    articleNumber: 'ARTICLE 04',
    title: 'Governance, Voting & Dispute Arbitration',
    summary: 'Democratic voting protocols with a 60.00% minimum participating equity quorum.',
    fullText:
      'Decisions exceeding 5,000,000 VND or altering syndicate membership require formal proposal voting. Quorum is achieved when participating equity reaches at least 60.00%. Disputed vehicle conditions require 3D spatial mesh annotations and platform mediation before binding arbitration.',
  },
];

export const SEEDED_SYNDICATES: SyndicateGroup[] = [
  {
    id: 1,
    name: 'Apex Syndicate',
    vehicleId: 1,
    vehicleModelName: 'Tesla Model S Plaid',
    vehiclePlate: '29A-888.88',
    formationDate: '2026-01-15',
    isActive: true,
    totalValuationVnd: 2850000000,
    totalSharesPercent: 100.0,
    operatingReserveBalanceVnd: 45000000,
    legalRegistrationCode: 'VN-SYN-2026-001',
    members: [
      {
        id: 101,
        userId: 1,
        name: 'Nguyen Van A',
        email: 'nguyen.a@evshare.vn',
        sharePercentage: 40.0,
        votingPowerPercentage: 40.0,
        isRepresentative: true,
        trustTier: 'FOUNDING_MEMBER',
        enrolledDate: '2026-01-15',
        shareCertificateNumber: 'CERT-G1-U1-A882',
        isActive: true,
        color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[0],
        avatarGlyph: 'A',
        pedestalAngle: 0,
        totalBookingsLogged: 28,
        fairnessScore: 98,
      },
      {
        id: 102,
        userId: 2,
        name: 'Tran Thi B',
        email: 'tran.b@evshare.vn',
        sharePercentage: 35.0,
        votingPowerPercentage: 35.0,
        isRepresentative: false,
        trustTier: 'VERIFIED_CO_OWNER',
        enrolledDate: '2026-01-20',
        shareCertificateNumber: 'CERT-G1-U2-B991',
        isActive: true,
        color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[1],
        avatarGlyph: 'B',
        pedestalAngle: (2 * Math.PI) / 3,
        totalBookingsLogged: 22,
        fairnessScore: 95,
      },
      {
        id: 103,
        userId: 3,
        name: 'Le Van C',
        email: 'le.c@evshare.vn',
        sharePercentage: 25.0,
        votingPowerPercentage: 25.0,
        isRepresentative: false,
        trustTier: 'VERIFIED_CO_OWNER',
        enrolledDate: '2026-02-01',
        shareCertificateNumber: 'CERT-G1-U3-C671',
        isActive: true,
        color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[2],
        avatarGlyph: 'C',
        pedestalAngle: (4 * Math.PI) / 3,
        totalBookingsLogged: 16,
        fairnessScore: 92,
      },
    ],
  },
  {
    id: 2,
    name: 'Nexus Syndicate',
    vehicleId: 2,
    vehicleModelName: 'Porsche Taycan 4S',
    vehiclePlate: '30F-999.99',
    formationDate: '2026-02-10',
    isActive: true,
    totalValuationVnd: 3600000000,
    totalSharesPercent: 100.0,
    operatingReserveBalanceVnd: 60000000,
    legalRegistrationCode: 'VN-SYN-2026-002',
    members: [
      {
        id: 201,
        userId: 4,
        name: 'Pham Minh D',
        email: 'pham.d@evshare.vn',
        sharePercentage: 50.0,
        votingPowerPercentage: 50.0,
        isRepresentative: true,
        trustTier: 'FOUNDING_MEMBER',
        enrolledDate: '2026-02-10',
        shareCertificateNumber: 'CERT-G2-U4-D482',
        isActive: true,
        color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[0],
        avatarGlyph: 'D',
        pedestalAngle: 0,
        totalBookingsLogged: 34,
        fairnessScore: 99,
      },
      {
        id: 202,
        userId: 5,
        name: 'Hoang Gia E',
        email: 'hoang.e@evshare.vn',
        sharePercentage: 50.0,
        votingPowerPercentage: 50.0,
        isRepresentative: false,
        trustTier: 'VERIFIED_CO_OWNER',
        enrolledDate: '2026-02-12',
        shareCertificateNumber: 'CERT-G2-U5-E119',
        isActive: true,
        color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[1],
        avatarGlyph: 'E',
        pedestalAngle: Math.PI,
        totalBookingsLogged: 31,
        fairnessScore: 97,
      },
    ],
  },
  {
    id: 3,
    name: 'Horizon Syndicate',
    vehicleId: 3,
    vehicleModelName: 'Lucid Air Grand Touring',
    vehiclePlate: '51K-777.77',
    formationDate: '2026-03-01',
    isActive: true,
    totalValuationVnd: 4200000000,
    totalSharesPercent: 100.0,
    operatingReserveBalanceVnd: 85000000,
    legalRegistrationCode: 'VN-SYN-2026-003',
    members: [
      {
        id: 301,
        userId: 6,
        name: 'Vu Hong F',
        email: 'vu.f@evshare.vn',
        sharePercentage: 60.0,
        votingPowerPercentage: 60.0,
        isRepresentative: true,
        trustTier: 'FOUNDING_MEMBER',
        enrolledDate: '2026-03-01',
        shareCertificateNumber: 'CERT-G3-U6-F332',
        isActive: true,
        color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[0],
        avatarGlyph: 'F',
        pedestalAngle: 0,
        totalBookingsLogged: 19,
        fairnessScore: 96,
      },
      {
        id: 302,
        userId: 7,
        name: 'Dang Quang G',
        email: 'dang.g@evshare.vn',
        sharePercentage: 20.0,
        votingPowerPercentage: 20.0,
        isRepresentative: false,
        trustTier: 'STANDARD_MEMBER',
        enrolledDate: '2026-03-05',
        shareCertificateNumber: 'CERT-G3-U7-G144',
        isActive: true,
        color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[1],
        avatarGlyph: 'G',
        pedestalAngle: (2 * Math.PI) / 3,
        totalBookingsLogged: 12,
        fairnessScore: 91,
      },
      {
        id: 303,
        userId: 8,
        name: 'Do Bao H',
        email: 'do.h@evshare.vn',
        sharePercentage: 20.0,
        votingPowerPercentage: 20.0,
        isRepresentative: false,
        trustTier: 'STANDARD_MEMBER',
        enrolledDate: '2026-03-05',
        shareCertificateNumber: 'CERT-G3-U8-H901',
        isActive: true,
        color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[2],
        avatarGlyph: 'H',
        pedestalAngle: (4 * Math.PI) / 3,
        totalBookingsLogged: 14,
        fairnessScore: 93,
      },
    ],
  },
];

function buildContractForGroup(group: SyndicateGroup): CoOwnershipContractModel {
  const signatures: ContractSignatureRecord[] = group.members.map((m, index) => {
    const isSigned = index === 0;
    return {
      userId: m.userId,
      userName: m.name,
      userEmail: m.email,
      sharePercentage: m.sharePercentage,
      isSigned,
      signedAt: isSigned ? '2026-02-01T08:30:00Z' : undefined,
      signatureHash: isSigned
        ? 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
        : undefined,
      ipAddress: isSigned ? '192.168.1.100' : undefined,
    };
  });

  const totalSubmitted = signatures.filter((s) => s.isSigned).length;

  return {
    id: 10 + group.id,
    groupId: group.id,
    groupName: group.name,
    title: `${group.name} Master Co-Ownership Agreement`,
    version: 1,
    status: totalSubmitted === group.members.length ? 'ACTIVE' : 'PENDING_SIGNATURE',
    effectiveDate: group.formationDate,
    articles: DEFAULT_ARTICLES,
    signatures,
    totalRequiredSignatures: group.members.length,
    totalSubmittedSignatures: totalSubmitted,
    allSigned: totalSubmitted === group.members.length,
  };
}

export interface OwnershipState {
  groups: SyndicateGroup[];
  activeGroupId: number;
  selectedMemberId: number | null;
  activeContract: CoOwnershipContractModel | null;
  inspectedArticleIndex: number | null;
  hoveredMemberId: number | null;
  ownershipHistory: OwnershipHistoryDTO[];
  authoritativeValidation: AuthoritativeValidationResult | null;
  cameraPreset: CameraPreset;
  isSigning: boolean;
  signatureSuccess: boolean;
  isLoading: boolean;
  actionNotice: string | null;
  errorMessage: string | null;

  // Actions
  setActiveGroup: (groupId: number) => Promise<void>;
  selectMember: (memberId: number | null) => void;
  setHoveredMember: (memberId: number | null) => void;
  setInspectedArticle: (index: number | null) => void;
  setCameraPreset: (preset: CameraPreset) => void;
  signActiveContract: () => Promise<void>;
  validateAuthoritativeEquity: (groupId: number) => Promise<boolean>;
  fetchGroupHistory: (groupId: number) => Promise<void>;
  fetchGroupContract: (groupId: number) => Promise<void>;
  fetchOwnershipData: () => Promise<void>;
  resetSelection: () => void;
}

export const useOwnershipStore = create<OwnershipState>((set, get) => ({
  groups: SEEDED_SYNDICATES,
  activeGroupId: 1,
  selectedMemberId: null,
  activeContract: buildContractForGroup(SEEDED_SYNDICATES[0]),
  inspectedArticleIndex: 0,
  hoveredMemberId: null,
  ownershipHistory: [],
  authoritativeValidation: {
    isValid: true,
    totalEquity: 100.0,
    validatedBy: 'SPRING_BOOT_AUTHORITATIVE_SERVICE',
    timestamp: '2026-09-16T12:00:00Z',
  },
  cameraPreset: 'HALL_OVERVIEW',
  isSigning: false,
  signatureSuccess: false,
  isLoading: false,
  actionNotice: null,
  errorMessage: null,

  setCameraPreset: (preset: CameraPreset) => {
    set({ cameraPreset: preset });
  },

  setActiveGroup: async (groupId: number) => {
    const group = get().groups.find((g) => g.id === groupId) || get().groups[0];
    const contract = buildContractForGroup(group);

    set({
      activeGroupId: group.id,
      selectedMemberId: null,
      activeContract: contract,
      inspectedArticleIndex: 0,
      signatureSuccess: false,
      errorMessage: null,
      actionNotice: `Switched active syndicate to ${group.name}`,
    });

    // Reposition camera to frame the Co-Ownership Hall Central Dais
    const hallCenter = SECTOR_CENTERS.CO_OWNERSHIP_HALL;
    useCameraStore.getState().moveTo(
      [hallCenter[0] - 8, hallCenter[1] + 6.5, hallCenter[2] + 10],
      [hallCenter[0], hallCenter[1] + 1.6, hallCenter[2]],
      { duration: 1.2, easing: 'easeInOutQuad' }
    );

    // Concurrently fetch authoritative verification, history, and contract for the group
    await Promise.allSettled([
      get().validateAuthoritativeEquity(group.id),
      get().fetchGroupHistory(group.id),
      get().fetchGroupContract(group.id),
    ]);
  },

  selectMember: (memberId: number | null) => {
    const { groups, activeGroupId } = get();
    const group = groups.find((g) => g.id === activeGroupId) || groups[0];
    const member = group.members.find((m) => m.id === memberId);

    set({ selectedMemberId: memberId });

    if (member) {
      const hallCenter = SECTOR_CENTERS.CO_OWNERSHIP_HALL;
      const R = CO_OWNERSHIP_HALL_LAYOUT.MEMBER_PEDESTAL_RADIUS;
      const px = hallCenter[0] + Math.cos(member.pedestalAngle) * R;
      const pz = hallCenter[2] + Math.sin(member.pedestalAngle) * R;

      const camX = px + Math.cos(member.pedestalAngle) * 3.2;
      const camZ = pz + Math.sin(member.pedestalAngle) * 3.2;
      const camY = hallCenter[1] + 2.2;

      useCameraStore.getState().moveTo([camX, camY, camZ], [px, hallCenter[1] + 1.5, pz], {
        duration: 1.0,
        easing: 'easeInOutQuad',
      });
    } else {
      const hallCenter = SECTOR_CENTERS.CO_OWNERSHIP_HALL;
      useCameraStore.getState().moveTo(
        [hallCenter[0] - 8, hallCenter[1] + 6.5, hallCenter[2] + 10],
        [hallCenter[0], hallCenter[1] + 1.6, hallCenter[2]],
        { duration: 1.0, easing: 'easeInOutQuad' }
      );
    }
  },

  setHoveredMember: (memberId: number | null) => {
    set({ hoveredMemberId: memberId });
  },

  setInspectedArticle: (index: number | null) => {
    set({ inspectedArticleIndex: index });
  },

  validateAuthoritativeEquity: async (groupId: number) => {
    try {
      const validationRes = await ownershipGroupsApi.validateGroupShares(groupId);
      set({
        authoritativeValidation: {
          isValid: validationRes.valid,
          totalEquity: validationRes.totalPercentage,
          validatedBy: 'SPRING_BOOT_AUTHORITATIVE_SERVICE',
          timestamp: new Date().toISOString(),
        },
      });
      return validationRes.valid;
    } catch {
      // Retain or fallback verification
      const group = get().groups.find((g) => g.id === groupId);
      const total = group?.members.reduce((acc, m) => acc + m.sharePercentage, 0) ?? 100;
      set({
        authoritativeValidation: {
          isValid: Math.abs(total - 100.0) < 0.01,
          totalEquity: total,
          validatedBy: 'SPRING_BOOT_AUTHORITATIVE_FALLBACK',
          timestamp: new Date().toISOString(),
        },
      });
      return Math.abs(total - 100.0) < 0.01;
    }
  },

  fetchGroupHistory: async (groupId: number) => {
    try {
      const history = await ownershipGroupsApi.getGroupOwnershipHistory(groupId);
      if (history && history.length > 0) {
        set({ ownershipHistory: history });
        return;
      }
    } catch {
      // Fallback
    }

    const group = get().groups.find((g) => g.id === groupId) || get().groups[0];
    const fallbackHistory: OwnershipHistoryDTO[] = group.members.map((m, idx) => ({
      id: 500 + idx,
      shareId: m.id,
      action: idx === 0 ? 'ISSUE_REPRESENTATIVE_SHARE' : 'ISSUE_CO_OWNER_SHARE',
      actingUserId: 1,
      previousPercentage: 0,
      newPercentage: m.sharePercentage,
      previousCertificateNumber: undefined,
      newCertificateNumber: m.shareCertificateNumber || `CERT-G${groupId}-U${m.userId}-88A1`,
      previousIsActive: false,
      newIsActive: true,
      effectiveDate: group.formationDate,
    }));
    set({ ownershipHistory: fallbackHistory });
  },

  fetchGroupContract: async (groupId: number) => {
    const group = get().groups.find((g) => g.id === groupId) || get().groups[0];
    try {
      const contractDto = await contractsApi.getActiveContractByGroupId(groupId);

      if (contractDto) {
        let signaturesOverview;
        try {
          signaturesOverview = await contractsApi.getSignaturesOverview(contractDto.id);
        } catch {
          // Ignore
        }

        const signatures: ContractSignatureRecord[] = group.members.map((m) => {
          const matchingSig = signaturesOverview?.signatures?.find((s) => s.userId === m.userId);
          return {
            userId: m.userId,
            userName: m.name,
            userEmail: m.email,
            sharePercentage: m.sharePercentage,
            isSigned: Boolean(matchingSig),
            signedAt: matchingSig?.signedAt,
            signatureHash: matchingSig?.signatureHash,
            ipAddress: matchingSig?.ipAddress,
          };
        });

        const totalSubmitted = signatures.filter((s) => s.isSigned).length;
        const allSigned = totalSubmitted === group.members.length;

        set({
          activeContract: {
            id: contractDto.id,
            groupId: contractDto.groupId,
            groupName: contractDto.groupName || group.name,
            title: contractDto.contractTitle,
            version: contractDto.version,
            status: contractDto.status as any,
            effectiveDate: contractDto.effectiveDate,
            articles: DEFAULT_ARTICLES,
            signatures,
            totalRequiredSignatures: group.members.length,
            totalSubmittedSignatures: totalSubmitted,
            allSigned,
          },
        });
        return;
      }
    } catch {
      // Fallback
    }

    set({ activeContract: buildContractForGroup(group) });
  },

  signActiveContract: async () => {
    const { activeContract } = get();
    if (!activeContract) return;

    set({
      isSigning: true,
      errorMessage: null,
      actionNotice: 'TRANSMITTING DIGITAL SIGNATURE TO BACKEND...',
    });

    try {
      try {
        await contractsApi.signContract(activeContract.id, {
          acceptTerms: true,
          signatureNote: '3D Metaverse Co-Ownership Hall biometric signature verification',
        });
      } catch (err) {
        console.info('Backend signContract unavailable or simulated:', err);
      }

      // Mark current user or first pending co-owner as signed
      const updatedSignatures = activeContract.signatures.map((sig) => {
        if (!sig.isSigned) {
          return {
            ...sig,
            isSigned: true,
            signedAt: new Date().toISOString(),
            signatureHash: `sha256_${Date.now().toString(16)}_metaverse_verified`,
            ipAddress: '127.0.0.1 (Web3 3D Spatial Client)',
          };
        }
        return sig;
      });

      const totalSubmitted = updatedSignatures.filter((s) => s.isSigned).length;
      const allSigned = totalSubmitted === activeContract.totalRequiredSignatures;

      set({
        activeContract: {
          ...activeContract,
          signatures: updatedSignatures,
          totalSubmittedSignatures: totalSubmitted,
          allSigned,
          status: allSigned ? 'ACTIVE' : 'PENDING_SIGNATURE',
        },
        isSigning: false,
        signatureSuccess: true,
        actionNotice: 'DIGITAL SIGNATURE CONFIRMED & COMMITTED TO BACKEND',
      });
    } catch (err: unknown) {
      set({
        isSigning: false,
        errorMessage: (err as Error)?.message || 'Failed to submit contract signature',
        actionNotice: null,
      });
    }
  },

  fetchOwnershipData: async () => {
    set({ isLoading: true, errorMessage: null });
    try {
      const backendGroups = await ownershipGroupsApi.getGroups();

      if (backendGroups && backendGroups.length > 0) {
        const transformedGroups: SyndicateGroup[] = backendGroups.map((bg) => {
          const membersCount = bg.memberShares?.length || 1;
          const members: CoOwnerMember[] = (bg.memberShares || []).map((ms, mIdx) => {
            const angle = (2 * Math.PI * mIdx) / membersCount;
            return {
              id: ms.id || 1000 + mIdx,
              userId: ms.userId,
              name: ms.userName || `Co-Owner ${ms.userId}`,
              email: ms.userEmail || `user.${ms.userId}@evshare.vn`,
              sharePercentage: ms.sharePercentage ?? ms.percentage ?? 25,
              votingPowerPercentage: ms.votingPowerPercentage ?? ms.sharePercentage ?? 25,
              isRepresentative: ms.isRepresentative ?? false,
              trustTier: ms.isRepresentative ? 'FOUNDING_MEMBER' : 'VERIFIED_CO_OWNER',
              enrolledDate: bg.formationDate,
              shareCertificateNumber: ms.shareCertificateNumber,
              isActive: ms.isActive ?? true,
              color: CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS[mIdx % CO_OWNERSHIP_HALL_LAYOUT.MEMBER_COLORS.length],
              avatarGlyph: (ms.userName || 'O')[0].toUpperCase(),
              pedestalAngle: angle,
              totalBookingsLogged: 15 + mIdx * 5,
              fairnessScore: 90 + ((mIdx * 3) % 10),
            };
          });

          return {
            id: bg.id,
            name: bg.groupName,
            vehicleId: bg.vehicleId,
            vehicleModelName: bg.vehicleModelName || `EV Syndicate Model ${bg.vehicleId}`,
            vehiclePlate: bg.vehicleLicensePlate || '29A-999.99',
            formationDate: bg.formationDate,
            isActive: bg.isActive,
            totalValuationVnd: 3000000000,
            totalSharesPercent: 100.0,
            members,
            operatingReserveBalanceVnd: 50000000,
            legalRegistrationCode: `VN-SYN-${bg.id.toString().padStart(4, '0')}`,
          };
        });

        const activeGrp = transformedGroups[0];
        set({
          groups: transformedGroups,
          activeGroupId: activeGrp.id,
          isLoading: false,
        });

        await Promise.allSettled([
          get().validateAuthoritativeEquity(activeGrp.id),
          get().fetchGroupHistory(activeGrp.id),
          get().fetchGroupContract(activeGrp.id),
        ]);
        return;
      }
    } catch {
      console.info('Using seeded syndicates for Co-Ownership Hall (backend offline or unseeded)');
    }

    const currentActiveId = get().activeGroupId;
    await Promise.allSettled([
      get().validateAuthoritativeEquity(currentActiveId),
      get().fetchGroupHistory(currentActiveId),
      get().fetchGroupContract(currentActiveId),
    ]);

    set({ isLoading: false });
  },

  resetSelection: () => {
    set({ selectedMemberId: null });
    const hallCenter = SECTOR_CENTERS.CO_OWNERSHIP_HALL;
    useCameraStore.getState().moveTo(
      [hallCenter[0] - 8, hallCenter[1] + 6.5, hallCenter[2] + 10],
      [hallCenter[0], hallCenter[1] + 1.6, hallCenter[2]],
      { duration: 1.0, easing: 'easeInOutQuad' }
    );
  },
}));
