import { create } from 'zustand';
import { useCameraStore } from '@/stores/useCameraStore';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import {
  contractsApi,
  type ContractDTO,
  type ContractStatus,
  type ContractSignaturesOverviewDTO,
  type ContractSignatureDTO,
  type CreateContractPayload,
} from '@/api/contractsApi';
import type {
  ContractRoomTab,
  ContractSigningState,
  ContractDocumentSection,
  ContractOperationResult,
} from './contractTypes';
import { CONTRACT_LAYOUT } from './contractLayout';

export const CONTRACT_SECTIONS: ContractDocumentSection[] = [
  {
    id: 1,
    sectionNumber: 'SECTION I',
    title: 'PREAMBLE & SYNDICATE COMPOSITION',
    subtitle: 'Parties, Digital Twin Asset, and Fractional Legal Framework',
    content:
      'This Digital Co-Ownership Master Agreement governs the shared equity, telemetry surveillance, and equitable utilization of the VinFast VF8 City Edition electric vehicle (VIN: VF8-VN2026-99412) between registered syndicate participants.',
    keyClauses: [
      'Clause 1.1: Multi-party syndicate bound under Law on Electronic Transactions No. 20/2023/QH15.',
      'Clause 1.2: Participant Minh Hiep (Lead, 35%), Tran Duc (25%), Le Hoang (20%), Pham Mai (20%).',
      'Clause 1.3: Vehicle title held in collective digital trust managed by EVShare Platform.',
    ],
  },
  {
    id: 2,
    sectionNumber: 'SECTION II',
    title: 'CAPITAL ALLOCATIONS & TREASURY COVENANTS',
    subtitle: 'Reserve Ratios, BR-FIN-03 Safety Floor, and Cost Sharing',
    content:
      'All syndicate capital contributions, fast charging electricity bills, preventative maintenance, and insurance premiums shall be cleared through the Shared Fund Vault.',
    keyClauses: [
      'Clause 2.1: Non-negotiable liquid safety reserve floor of 15,000,000 VND enforced by BR-FIN-03.',
      'Clause 2.2: Routine charging and detailing expenses allocated pro-rata based on recorded usage km.',
      'Clause 2.3: Capital deficits trigger automated liquidity calls with 48-hour cure windows.',
    ],
  },
  {
    id: 3,
    sectionNumber: 'SECTION III',
    title: 'CHRONO-SPATIAL BOOKING & FAIR USAGE',
    subtitle: 'Quota Allowances, Priority Slots, and Telemetry Telematics',
    content:
      'Reservations are coordinated exclusively via the Chrono-Spatial 3D Booking Chamber. Dynamic fair-usage algorithms prevent monopolization and optimize fleet uptime.',
    keyClauses: [
      'Clause 3.1: Monthly guaranteed driving quota of 168 hours proportional to co-owner equity share.',
      'Clause 3.2: 15-minute mandatory buffer window between reservations for ultrasonic sensor calibration.',
      'Clause 3.3: Geo-fenced telemetry tracking active; reckless driving triggers automated quota reduction.',
    ],
  },
  {
    id: 4,
    sectionNumber: 'SECTION IV',
    title: 'GOVERNANCE QUORUM & SMART ARBITRATION',
    subtitle: 'Voting Thresholds, Amendments, and Conflict Resolution',
    content:
      'Syndicate decisions are ratified through the Parliamentary Decision Chamber. Governance rules ensure minority protections and strict democratic transparency.',
    keyClauses: [
      'Clause 4.1: Operational resolutions require simple majority (>50% equity weight).',
      'Clause 4.2: Extraordinary amendments, asset liquidation, or overdraft require supermajority (>=75%).',
      'Clause 4.3: Unresolved disputes escalate to the 3D Dispute Resolution Chamber with cryptographic evidence logs.',
    ],
  },
  {
    id: 5,
    sectionNumber: 'SECTION V',
    title: 'RATIFICATION & MULTI-SIG DIGITAL SIGNATURES',
    subtitle: 'Cryptographic SHA-256 Digital Signatures & Binding Execution',
    content:
      'Execution of this agreement requires multi-signature cryptographic ratification from all syndicate members. Once signed, the legal status permanently transitions to ACTIVE.',
    keyClauses: [
      'Clause 5.1: Signatures recorded with SHA-256 digests, UTC timestamps, and IP provenance.',
      'Clause 5.2: Any unilateral document alteration invalidates the contract hash, requiring re-ratification.',
      'Clause 5.3: Ratified document is permanently archived on immutable syndicate ledger.',
    ],
  },
];

/**
 * Returns dynamic document sections for the 3D lectern, incorporating authoritative
 * terms text, title, and metadata received from Spring Boot contract response.
 */
export function getActiveContractSections(activeContract: ContractDTO | null): ContractDocumentSection[] {
  if (!activeContract) {
    return CONTRACT_SECTIONS;
  }

  return CONTRACT_SECTIONS.map((section, idx) => {
    if (idx === 0) {
      return {
        ...section,
        title: activeContract.contractTitle.toUpperCase(),
        subtitle: `Version ${activeContract.version}.0 • Status: ${activeContract.status} • Effective: ${activeContract.effectiveDate || 'Immediate'}`,
        content: activeContract.contractTermsText || section.content,
        keyClauses: [
          `Clause 1.1: Legally binding under Electronic Transactions Law No. 20/2023/QH15.`,
          `Clause 1.2: Syndicate group: ${activeContract.groupName || 'Founders Syndicate'} (ID: #${activeContract.groupId}).`,
          `Clause 1.3: Immutable digital contract version: v${activeContract.version}.0 [${activeContract.status}].`,
        ],
      };
    }
    return section;
  });
}

// Fallback initial data if offline
const INITIAL_CONTRACT: ContractDTO = {
  id: 1,
  groupId: 1,
  groupName: 'VinFast VF8 Founders Syndicate',
  contractTitle: 'Co-Ownership Master Agreement v2.0 (Ratified)',
  contractTermsText:
    'Digital co-ownership legal agreement governing VinFast VF8 City Edition between 4 verified syndicate members.',
  version: 2,
  status: 'ACTIVE',
  effectiveDate: '2026-09-01',
  expiryDate: '2027-09-01',
  createdAt: '2026-08-28T10:00:00Z',
};

const INITIAL_VERSIONS: ContractDTO[] = [
  {
    id: 1,
    groupId: 1,
    groupName: 'VinFast VF8 Founders Syndicate',
    contractTitle: 'Co-Ownership Master Agreement v2.0 (Active)',
    contractTermsText: 'Ratified master syndicate agreement with updated BR-FIN-03 reserve clause.',
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
    contractTermsText: 'Proposed amendment regarding commercial ride-hailing quota usage.',
    version: 3,
    status: 'PENDING_SIGNATURE',
    effectiveDate: '2026-10-01',
    createdAt: '2026-09-14T15:00:00Z',
  },
  {
    id: 3,
    groupId: 1,
    groupName: 'VinFast VF8 Founders Syndicate',
    contractTitle: 'Co-Ownership Master Agreement v1.0 (Superseded)',
    contractTermsText: 'Initial syndicate agreement executed upon vehicle delivery.',
    version: 1,
    status: 'SIGNED',
    effectiveDate: '2026-01-15',
    expiryDate: '2026-08-31',
    createdAt: '2026-01-10T08:00:00Z',
  },
];

const INITIAL_SIGNATURES: ContractSignaturesOverviewDTO = {
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
    {
      id: 2,
      contractId: 1,
      contractVersion: 2,
      userId: 2,
      userFullName: 'Tran Duc',
      userEmail: 'duc.tran@evshare.vn',
      signatureHash: 'f4b1e892c0194821a7199c450192e4ba88102377c8910b23d4e892c019283f12',
      signedAt: '2026-08-29T10:45:00Z',
      ipAddress: '14.161.22.84',
    },
    {
      id: 3,
      contractId: 1,
      contractVersion: 2,
      userId: 3,
      userFullName: 'Le Hoang',
      userEmail: 'hoang.le@evshare.vn',
      signatureHash: 'b9201948ac0192e4ba88102377c8910b23d4e892c019283f12a8f5c312d89b14',
      signedAt: '2026-08-30T14:20:00Z',
      ipAddress: '113.161.78.12',
    },
    {
      id: 4,
      contractId: 1,
      contractVersion: 2,
      userId: 4,
      userFullName: 'Pham Mai',
      userEmail: 'mai.pham@evshare.vn',
      signatureHash: 'd892c019283f12a8f5c312d89b14c3e7f2231b4092d8e41198c642bb3901a2f4',
      signedAt: '2026-08-31T18:00:00Z',
      ipAddress: '42.112.98.5',
    },
  ],
  pendingSigners: [],
};

export interface ContractState {
  groupId: number;
  activeTab: ContractRoomTab;
  activeContract: ContractDTO | null;
  contractVersions: ContractDTO[];
  signaturesOverview: ContractSignaturesOverviewDTO | null;
  currentSectionIndex: number;
  acceptedTerms: boolean;
  signingState: ContractSigningState;
  operationResult: ContractOperationResult;
  lastSignedHash: string | null;

  isLoading: boolean;
  isSyncing: boolean;
  error: string | null;

  // Actions
  setActiveTab: (tab: ContractRoomTab) => void;
  focusCamera: (presetKey: keyof typeof CONTRACT_LAYOUT.cameras) => void;
  setSectionIndex: (index: number) => void;
  nextSection: () => void;
  prevSection: () => void;
  setAcceptedTerms: (accepted: boolean) => void;
  resetOperationResult: () => void;

  // Backend Async Actions
  fetchContractData: (groupId?: number) => Promise<void>;
  selectVersion: (contractId: number) => Promise<void>;
  createContractDraft: (payload?: Partial<CreateContractPayload>) => Promise<boolean>;
  executeSignContract: () => Promise<void>;
  transitionStatus: (targetStatus: ContractStatus, reason?: string) => Promise<void>;
}

export const useContractStore = create<ContractState>((set, get) => ({
  groupId: 1,
  activeTab: 'DOCUMENT',
  activeContract: INITIAL_CONTRACT,
  contractVersions: INITIAL_VERSIONS,
  signaturesOverview: INITIAL_SIGNATURES,
  currentSectionIndex: 0,
  acceptedTerms: false,
  signingState: 'IDLE',
  operationResult: { status: 'IDLE' },
  lastSignedHash: null,

  isLoading: false,
  isSyncing: false,
  error: null,

  setActiveTab: (tab: ContractRoomTab) => {
    AudioEngine.play('UI_CLICK');
    set({ activeTab: tab });

    const { focusCamera } = get();
    if (tab === 'DOCUMENT') {
      focusCamera('DOCUMENT_FOCUS');
    } else if (tab === 'VERSIONS') {
      focusCamera('VERSION_FOCUS');
    } else if (tab === 'SIGNATURES') {
      focusCamera('SIGNATURE_FOCUS');
    } else if (tab === 'STATUS_OVERVIEW') {
      focusCamera('STATUS_FOCUS');
    }
  },

  focusCamera: (presetKey: keyof typeof CONTRACT_LAYOUT.cameras) => {
    const preset = CONTRACT_LAYOUT.cameras[presetKey];
    if (preset) {
      useCameraStore.getState().moveTo(preset.position, preset.target, {
        duration: 1.2,
        ease: 'easeInOutCubic',
      });
      AudioEngine.play('CAMERA_WHOOSH');
    }
  },

  setSectionIndex: (index: number) => {
    const validIndex = Math.max(0, Math.min(CONTRACT_SECTIONS.length - 1, index));
    AudioEngine.play('UI_CLICK');
    set({ currentSectionIndex: validIndex });
  },

  nextSection: () => {
    const { currentSectionIndex, setSectionIndex } = get();
    if (currentSectionIndex < CONTRACT_SECTIONS.length - 1) {
      setSectionIndex(currentSectionIndex + 1);
    }
  },

  prevSection: () => {
    const { currentSectionIndex, setSectionIndex } = get();
    if (currentSectionIndex > 0) {
      setSectionIndex(currentSectionIndex - 1);
    }
  },

  setAcceptedTerms: (accepted: boolean) => {
    AudioEngine.play('UI_CLICK');
    set({ acceptedTerms: accepted });
  },

  resetOperationResult: () => {
    set({ operationResult: { status: 'IDLE' }, signingState: 'IDLE' });
  },

  fetchContractData: async (groupIdParam?: number) => {
    const targetGroupId = groupIdParam ?? get().groupId;
    set({ isLoading: true, isSyncing: true, error: null });

    try {
      const [versionsList, activeDoc] = await Promise.allSettled([
        contractsApi.getContractsByGroupId(targetGroupId),
        contractsApi.getActiveContractByGroupId(targetGroupId),
      ]);

      let chosenContract: ContractDTO | null = get().activeContract;
      if (activeDoc.status === 'fulfilled' && activeDoc.value) {
        chosenContract = activeDoc.value;
      }

      let versions: ContractDTO[] = get().contractVersions;
      if (versionsList.status === 'fulfilled' && Array.isArray(versionsList.value) && versionsList.value.length > 0) {
        versions = versionsList.value;
        if (!chosenContract) {
          chosenContract = versions[0];
        }
      }

      // If we have an active contract, fetch its signatures overview
      let signatures = get().signaturesOverview;
      if (chosenContract?.id) {
        try {
          signatures = await contractsApi.getSignaturesOverview(chosenContract.id);
        } catch (sigErr) {
          console.warn('[useContractStore] Signatures fetch notice:', sigErr);
        }
      }

      set({
        activeContract: chosenContract,
        contractVersions: versions,
        signaturesOverview: signatures,
        isLoading: false,
        isSyncing: false,
      });
    } catch (err: unknown) {
      console.warn('[useContractStore] Backend sync notice (using robust cached data):', err);
      set({
        isLoading: false,
        isSyncing: false,
        error: (err as Error)?.message || 'Failed to sync contracts with backend',
      });
    }
  },

  selectVersion: async (contractId: number) => {
    AudioEngine.play('UI_CLICK');
    set({ isLoading: true, operationResult: { status: 'IDLE' } });

    try {
      const [contract, signatures] = await Promise.all([
        contractsApi.getContractById(contractId),
        contractsApi.getSignaturesOverview(contractId),
      ]);

      set({
        activeContract: contract,
        signaturesOverview: signatures,
        currentSectionIndex: 0,
        acceptedTerms: false,
        isLoading: false,
      });

      get().focusCamera('DOCUMENT_FOCUS');
    } catch (err: unknown) {
      console.warn('[useContractStore] Version fetch notice (fallback to local version list):', err);
      const local = get().contractVersions.find((v) => v.id === contractId);
      if (local) {
        set({
          activeContract: local,
          currentSectionIndex: 0,
          acceptedTerms: false,
          isLoading: false,
        });
      } else {
        set({ isLoading: false });
      }
    }
  },

  executeSignContract: async () => {
    const { activeContract, acceptedTerms } = get();

    if (!activeContract) {
      AudioEngine.play('NOTIF_ERROR');
      set({
        operationResult: { status: 'ERROR', message: 'No active contract selected for signing.' },
      });
      return;
    }

    if (!acceptedTerms) {
      AudioEngine.play('NOTIF_WARNING');
      set({
        operationResult: {
          status: 'ERROR',
          message: 'Legal terms acknowledgment required. Please check the agreement box before signing.',
        },
      });
      return;
    }

    set({
      signingState: 'SCANNING_BIOMETRIC',
      operationResult: {
        status: 'PROCESSING',
        message: 'Capturing biometric signature and generating SHA-256 digest...',
      },
    });
    AudioEngine.play('UI_CLICK');

    try {
      const signatureDTO: ContractSignatureDTO = await contractsApi.signContract(activeContract.id, {
        acceptTerms: true,
        signatureNote: '3D Metaverse Biometric Signing Dais Stamping',
      });

      const hash = signatureDTO.signatureHash;

      // Update local signatures overview with newly submitted signature
      const prevOverview = get().signaturesOverview;
      let updatedOverview: ContractSignaturesOverviewDTO | null = prevOverview;

      if (prevOverview) {
        const updatedList = [signatureDTO, ...prevOverview.signatures.filter((s) => s.userId !== signatureDTO.userId)];
        const totalSubmitted = updatedList.length;
        const allSigned = totalSubmitted >= prevOverview.totalRequiredSignatures;

        updatedOverview = {
          ...prevOverview,
          totalSubmittedSignatures: totalSubmitted,
          allSigned,
          contractStatus: allSigned ? 'SIGNED' : prevOverview.contractStatus,
          signatures: updatedList,
          pendingSigners: prevOverview.pendingSigners.filter((p) => p.userId !== signatureDTO.userId),
        };
      }

      AudioEngine.play('NOTIF_SUCCESS');
      set({
        signingState: 'SIGNED',
        lastSignedHash: hash,
        signaturesOverview: updatedOverview,
        operationResult: {
          status: 'SUCCESS',
          message: `Contract v${activeContract.version} successfully signed and cryptographically sealed!`,
          signatureHash: hash,
        },
      });

      // If all co-owners signed and status is PENDING_SIGNATURE, transition activeContract status to SIGNED
      if (updatedOverview?.allSigned && activeContract.status === 'PENDING_SIGNATURE') {
        const signedContract = { ...activeContract, status: 'SIGNED' as ContractStatus };
        set({
          activeContract: signedContract,
          contractVersions: get().contractVersions.map((v) => (v.id === signedContract.id ? signedContract : v)),
        });
      }

      // Refresh overview and contract from backend
      try {
        const [freshOverview, freshContract] = await Promise.all([
          contractsApi.getSignaturesOverview(activeContract.id),
          contractsApi.getContractById(activeContract.id),
        ]);
        if (freshOverview) set({ signaturesOverview: freshOverview });
        if (freshContract) {
          set({
            activeContract: freshContract,
            contractVersions: get().contractVersions.map((v) => (v.id === freshContract.id ? freshContract : v)),
          });
        }
      } catch {}
    } catch (err: unknown) {
      console.error('[useContractStore] Signing failed:', err);
      AudioEngine.play('NOTIF_ERROR');
      set({
        signingState: 'ERROR',
        operationResult: {
          status: 'ERROR',
          message: (err as Error)?.message || 'Contract signing rejected by backend API.',
        },
      });
    }
  },

  transitionStatus: async (targetStatus: ContractStatus, reason?: string) => {
    const { activeContract } = get();
    if (!activeContract) return;

    set({
      operationResult: { status: 'PROCESSING', message: `Transitioning contract to ${targetStatus}...` },
    });

    try {
      const updated = await contractsApi.transitionStatus(activeContract.id, {
        targetStatus,
        reason: reason || `Status transition via 3D Contract Room`,
      });

      const updatedVersions = get().contractVersions.map((v) => (v.id === updated.id ? updated : v));

      AudioEngine.play('NOTIF_SUCCESS');
      set({
        activeContract: updated,
        contractVersions: updatedVersions,
        operationResult: {
          status: 'SUCCESS',
          message: `Contract status successfully updated to ${targetStatus}!`,
        },
      });

      // Refresh signatures overview
      contractsApi.getSignaturesOverview(updated.id).then((fresh) => {
        set({ signaturesOverview: fresh });
      }).catch(() => {});
    } catch (err: unknown) {
      console.error('[useContractStore] Status transition failed:', err);
      AudioEngine.play('NOTIF_ERROR');
      set({
        operationResult: {
          status: 'ERROR',
          message: (err as Error)?.message || 'Status transition rejected.',
        },
      });
    }
  },

  createContractDraft: async (payload = {}) => {
    const groupId = payload.groupId ?? get().groupId;
    const { contractVersions } = get();
    const nextVersionNum = (contractVersions.reduce((max, v) => Math.max(max, v.version), 0) || 1) + 1;

    set({
      isLoading: true,
      operationResult: { status: 'PROCESSING', message: 'Generating new contract draft via Spring Boot...' },
    });
    AudioEngine.play('WARP_SWOOP');

    try {
      const draftPayload: CreateContractPayload = {
        groupId,
        contractTitle: payload.contractTitle || `Co-Ownership Master Agreement v${nextVersionNum}.0 (Draft Amendment)`,
        contractTermsText:
          payload.contractTermsText ||
          `Amendment v${nextVersionNum}.0 to the Syndicate Master Agreement. Codifying updated usage telemetry thresholds, reserve allocations, and maintenance schedules under Law on Electronic Transactions No. 20/2023/QH15.`,
        effectiveDate: payload.effectiveDate || new Date().toISOString().split('T')[0],
        expiryDate: payload.expiryDate,
      };

      const newContract = await contractsApi.createContract(draftPayload);

      // Refresh versions list from backend
      let versions = get().contractVersions;
      try {
        const freshVersions = await contractsApi.getContractsByGroupId(groupId);
        if (freshVersions && freshVersions.length > 0) {
          versions = freshVersions;
        } else {
          versions = [newContract, ...versions];
        }
      } catch {
        versions = [newContract, ...versions];
      }

      // Fetch signatures overview for the new draft
      let signatures: ContractSignaturesOverviewDTO | null = null;
      try {
        signatures = await contractsApi.getSignaturesOverview(newContract.id);
      } catch {
        signatures = {
          contractId: newContract.id,
          contractVersion: newContract.version,
          contractStatus: newContract.status,
          totalRequiredSignatures: 4,
          totalSubmittedSignatures: 0,
          allSigned: false,
          signatures: [],
          pendingSigners: [],
        };
      }

      AudioEngine.play('NOTIF_SUCCESS');
      set({
        activeContract: newContract,
        contractVersions: versions,
        signaturesOverview: signatures,
        currentSectionIndex: 0,
        acceptedTerms: false,
        signingState: 'IDLE',
        isLoading: false,
        operationResult: {
          status: 'SUCCESS',
          message: `Contract draft v${newContract.version}.0 successfully created! Loaded on 3D Lectern.`,
        },
      });

      get().focusCamera('DOCUMENT_FOCUS');
      return true;
    } catch (err: unknown) {
      console.error('[useContractStore] Contract creation failed:', err);
      AudioEngine.play('NOTIF_ERROR');
      set({
        isLoading: false,
        operationResult: {
          status: 'ERROR',
          message: (err as Error)?.message || 'Failed to create contract draft on backend.',
        },
      });
      return false;
    }
  },
}));
