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
    title: 'LỜI NÓI ĐẦU & CƠ CẤU NHÓM ĐỒNG SỞ HỮU',
    subtitle: 'Các Bên, Tài Sản Bản Sao Số, và Khung Pháp Lý Đồng Sở Hữu Phân Số',
    content:
      'Hợp đồng Khung Đồng Sở Hữu Kỹ Thuật Số này điều chỉnh tỷ lệ sở hữu chung, giám sát dữ liệu viễn thông từ xa, và việc sử dụng công bằng xe điện VinFast VF8 City Edition (Số khung VIN: VF8-VN2026-99412) giữa các thành viên nhóm đã xác thực.',
    keyClauses: [
      'Điều khoản 1.1: Nhóm đa phương được ràng buộc pháp lý theo Luật Giao dịch điện tử số 20/2023/QH15.',
      'Điều khoản 1.2: Thành viên gồm Minh Hiệp (Trưởng nhóm, 35%), Trần Đức (25%), Lê Hoàng (20%), Phạm Mai (20%).',
      'Điều khoản 1.3: Quyền sở hữu xe được ủy thác kỹ thuật số tập thể quản lý thông qua Nền tảng EVShare.',
    ],
  },
  {
    id: 2,
    sectionNumber: 'SECTION II',
    title: 'PHÂN BỔ NGUỒN VỐN & ĐIỀU KHOẢN KHO QUỸ',
    subtitle: 'Tỷ Lệ Dự Phòng, Hạn Mức An Toàn BR-FIN-03, và Chia Sẻ Chi Phí',
    content:
      'Toàn bộ vốn góp của nhóm, hóa đơn sạc nhanh, bảo dưỡng định kỳ và phí bảo hiểm bắt buộc đều phải được quyết toán minh bạch qua Kho Quỹ Chung.',
    keyClauses: [
      'Điều khoản 2.1: Hạn mức dự phòng an toàn không thương lượng là 15.000.000 ₫ được kiểm soát chặt chẽ bởi BR-FIN-03.',
      'Điều khoản 2.2: Chi phí sạc định kỳ và vệ sinh được phân bổ theo tỷ lệ quãng đường km đã sử dụng thực tế.',
      'Điều khoản 2.3: Thâm hụt vốn sẽ tự động kích hoạt yêu cầu bổ sung thanh khoản với thời hạn xử lý 48 giờ.',
    ],
  },
  {
    id: 3,
    sectionNumber: 'SECTION III',
    title: 'ĐẶT LỊCH KHÔNG GIAN - THỜI GIAN & SỬ DỤNG CÔNG BẰNG',
    subtitle: 'Hạn Mức Sử Dụng, Khung Giờ Ưu Tiên, và Dữ Liệu Viễn Thông Từ Xa',
    content:
      'Việc đặt lịch xe được điều phối độc quyền thông qua Buồng Đặt Lịch 3D Không Gian - Thời Gian. Thuật toán sử dụng công bằng ngăn chặn hành vi độc chiếm và tối ưu hóa thời gian vận hành của xe.',
    keyClauses: [
      'Điều khoản 3.1: Hạn mức lái xe bảo đảm hàng tháng là 168 giờ, tương ứng với tỷ lệ cổ phần sở hữu.',
      'Điều khoản 3.2: Khoảng đệm bắt buộc 15 phút giữa các lượt đặt xe để hiệu chuẩn cảm biến và làm mát pin.',
      'Điều khoản 3.3: Định vị viễn thông hàng rào địa lý được bật; vi phạm tốc độ sẽ tự động bị giảm hạn mức ưu tiên.',
    ],
  },
  {
    id: 4,
    sectionNumber: 'SECTION IV',
    title: 'ĐIỀU KIỆN BIỂU QUYẾT & TRỌNG TÀI TRANH CHẤP',
    subtitle: 'Ngưỡng Tỷ Lệ Biểu Quyết, Tu Chính Án, và Giải Quyết Bất Đồng',
    content:
      'Các quyết định chung của nhóm được biểu quyết qua Phòng Hội Nghị Biểu Quyết Nghị Viện. Quy tắc quản trị bảo đảm quyền lợi của cổ đông thiểu số và tính minh bạch dân chủ tuyệt đối.',
    keyClauses: [
      'Điều khoản 4.1: Các nghị quyết vận hành thông thường yêu cầu quá bán (>50% tổng trọng số cổ phần).',
      'Điều khoản 4.2: Tu chính án bất thường, thanh lý tài sản hoặc chi vượt dự phòng yêu cầu đa số tuyệt đối (>=75%).',
      'Điều khoản 4.3: Tranh chấp không thể hòa giải sẽ được chuyển đến Phòng Phân Xử Tranh Chấp 3D với nhật ký bằng chứng mật mã.',
    ],
  },
  {
    id: 5,
    sectionNumber: 'SECTION V',
    title: 'PHÊ CHUẨN & KÝ SỐ ĐA BÊN (MULTI-SIG)',
    subtitle: 'Chữ Ký Số Mật Mã SHA-256 & Hiệu Lực Ràng Buộc Pháp Lý',
    content:
      'Việc thực thi hợp đồng này đòi hỏi sự phê chuẩn bằng chữ ký số mật mã đa bên từ tất cả các thành viên trong nhóm. Sau khi ký đủ, trạng thái pháp lý sẽ vĩnh viễn chuyển sang ĐANG HOẠT ĐỘNG.',
    keyClauses: [
      'Điều khoản 5.1: Chữ ký được lưu trữ kèm mã băm SHA-256, mốc thời gian chuẩn UTC và xuất xứ địa chỉ IP.',
      'Điều khoản 5.2: Bất kỳ sửa đổi đơn phương nào đều làm vô hiệu hóa mã băm hợp đồng và buộc phải ký lại từ đầu.',
      'Điều khoản 5.3: Hợp đồng đã phê chuẩn được lưu trữ vĩnh viễn trên sổ cái kiểm toán bất biến của nhóm.',
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
        subtitle: `Version ${activeContract.version}.0 • Status: ${activeContract.status} • Có hiệu lực từ: ${activeContract.effectiveDate || 'Ngay lập tức'}`,
        content: activeContract.contractTermsText || section.content,
        keyClauses: [
          `Điều khoản 1.1: Ràng buộc pháp lý theo Luật Giao dịch điện tử Law No. 20/2023/QH15.`,
          `Điều khoản 1.2: Nhóm đồng sở hữu: ${activeContract.groupName || 'Founders Syndicate'} (Mã nhóm: #${activeContract.groupId}).`,
          `Điều khoản 1.3: Phiên bản hợp đồng số bất biến: v${activeContract.version}.0 [${activeContract.status}].`,
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
