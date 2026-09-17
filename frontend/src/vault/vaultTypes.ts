import type { FundTransactionType, PaymentMethod } from '@/api/financeApi';

export type VaultTab =
  | 'OVERVIEW'
  | 'CONTRIBUTE'
  | 'WITHDRAW'
  | 'TRANSACTIONS'
  | 'AUDIT_RECONCILIATION';

export interface VaultTransactionNode {
  id: number;
  reference: string;
  type: FundTransactionType;
  amountVnd: number;
  balanceAfterVnd: number;
  actorName: string;
  description: string;
  timestamp: string;
  receiptCode: string;
}

export interface VaultAuditEntry {
  id: number;
  action: string;
  actorName: string;
  details: string;
  ipAddress: string;
  timestamp: string;
}

export interface VaultReconciliationModel {
  currentBalanceVnd: number;
  calculatedLedgerBalanceVnd: number;
  totalCreditsVnd: number;
  totalDebitsVnd: number;
  creditCount: number;
  debitCount: number;
  transactionCount: number;
  isReconciled: boolean;
  reconciliationDeltaVnd: number;
  reconciledAt: string;
  summary: string;
}

export interface VaultOperationStatus {
  status: 'IDLE' | 'PROCESSING' | 'SUCCESS' | 'ERROR';
  message?: string;
  errorCode?: string;
  receiptId?: string;
}
