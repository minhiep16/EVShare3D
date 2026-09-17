import type { ContractStatus, ContractDTO, ContractSignatureDTO, ContractSignaturesOverviewDTO } from '@/api/contractsApi';

export type ContractRoomTab =
  | 'DOCUMENT'
  | 'VERSIONS'
  | 'SIGNATURES'
  | 'STATUS_OVERVIEW';

export type ContractSigningState =
  | 'IDLE'
  | 'SCANNING_BIOMETRIC'
  | 'SUBMITTING'
  | 'SIGNED'
  | 'ERROR';

export interface ContractDocumentSection {
  id: number;
  sectionNumber: string;
  title: string;
  subtitle: string;
  content: string;
  keyClauses: string[];
}

export interface ContractVersionCard {
  id: number;
  version: number;
  title: string;
  status: ContractStatus;
  effectiveDate: string;
  summary: string;
  isCurrent: boolean;
}

export interface ContractOperationResult {
  status: 'IDLE' | 'PROCESSING' | 'SUCCESS' | 'ERROR';
  message?: string;
  signatureHash?: string;
  errorCode?: string;
}
