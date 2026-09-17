import { create } from 'zustand';
import { useCameraStore } from '@/stores/useCameraStore';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import {
  getSharedFund,
  getFundTransactions,
  getFundAuditHistory,
  reconcileFund,
  contributeToFund,
  withdrawFromFund,
  type SharedFundDTO,
  type FundTransactionDTO,
  type FundAuditLogDTO,
  type FundReconciliationDTO,
  type PaymentMethod,
} from '@/api/financeApi';
import { useDigitalTwinStore } from '@/digitalTwin/useDigitalTwinStore';
import type {
  VaultTab,
  VaultTransactionNode,
  VaultAuditEntry,
  VaultReconciliationModel,
  VaultOperationStatus,
} from './vaultTypes';
import { VAULT_LAYOUT } from './vaultLayout';

// Initial fallback transactions if offline
const INITIAL_TRANSACTIONS: VaultTransactionNode[] = [
  {
    id: 1,
    reference: 'TX-VLT-9001',
    type: 'DEPOSIT',
    amountVnd: 15000000,
    balanceAfterVnd: 45000000,
    actorName: 'Minh Hiep (You)',
    description: 'Quarterly Syndicate Reserve Top-up',
    timestamp: '2026-09-14T08:30:00Z',
    receiptCode: 'RCPT-VLT-9001-A',
  },
  {
    id: 2,
    reference: 'TX-VLT-8994',
    type: 'WITHDRAWAL',
    amountVnd: 4200000,
    balanceAfterVnd: 30000000,
    actorName: 'Automated Settlement',
    description: 'Direct Debit: Fast Charging Cluster kWh Bill',
    timestamp: '2026-09-13T14:15:00Z',
    receiptCode: 'RCPT-VLT-8994-B',
  },
  {
    id: 3,
    reference: 'TX-VLT-8980',
    type: 'DEPOSIT',
    amountVnd: 8500000,
    balanceAfterVnd: 34200000,
    actorName: 'Tran Duc',
    description: 'Scheduled Maintenance Contribution',
    timestamp: '2026-09-10T11:00:00Z',
    receiptCode: 'RCPT-VLT-8980-C',
  },
  {
    id: 4,
    reference: 'TX-VLT-8955',
    type: 'WITHDRAWAL',
    amountVnd: 2400000,
    balanceAfterVnd: 25700000,
    actorName: 'EVShare Admin',
    description: 'Ceramic Coating Detailing Workshop Invoice',
    timestamp: '2026-09-08T09:45:00Z',
    receiptCode: 'RCPT-VLT-8955-D',
  },
  {
    id: 5,
    reference: 'TX-VLT-8902',
    type: 'INTEREST',
    amountVnd: 320000,
    balanceAfterVnd: 28100000,
    actorName: 'Yield Pool Treasury',
    description: 'Liquid Staking Syndicate Yield Payout',
    timestamp: '2026-09-01T00:00:00Z',
    receiptCode: 'RCPT-VLT-8902-E',
  },
];

const INITIAL_AUDIT_LOGS: VaultAuditEntry[] = [
  {
    id: 101,
    action: 'FUND_RECONCILE',
    actorName: 'System Audit Daemon',
    details: 'Cryptographic ledger balance matches on-chain and physical bank reserves.',
    ipAddress: '10.0.4.12',
    timestamp: '2026-09-15T04:00:00Z',
  },
  {
    id: 102,
    action: 'THRESHOLD_CHECK',
    actorName: 'BR-FIN-03 Watchdog',
    details: 'Minimum liquid safety reserve (15,000,000 VND) verified intact.',
    ipAddress: '10.0.4.12',
    timestamp: '2026-09-14T23:59:00Z',
  },
  {
    id: 103,
    action: 'DEPOSIT_CONFIRMED',
    actorName: 'Minh Hiep (You)',
    details: 'Contributed 15,000,000 VND via VNPAY. Reference TX-VLT-9001.',
    ipAddress: '118.69.182.50',
    timestamp: '2026-09-14T08:30:15Z',
  },
];

export interface VaultState {
  groupId: number;
  activeTab: VaultTab;
  sharedFund: SharedFundDTO | null;
  transactions: VaultTransactionNode[];
  auditHistory: VaultAuditEntry[];
  reconciliationReport: VaultReconciliationModel | null;
  selectedTransaction: VaultTransactionNode | null;
  operationStatus: VaultOperationStatus;

  // Deposit Form
  depositAmountVnd: number;
  depositPaymentMethod: PaymentMethod;
  depositDescription: string;

  // Withdrawal Form
  withdrawalAmountVnd: number;
  withdrawalMethod: PaymentMethod;
  withdrawalReason: string;
  withdrawalAllowOverdraft: boolean;

  // Status flags
  isLoading: boolean;
  isSyncing: boolean;
  lastSyncedAt: string | null;
  error: string | null;

  // Actions
  setActiveTab: (tab: VaultTab) => void;
  focusCamera: (presetKey: keyof typeof VAULT_LAYOUT.cameras) => void;
  selectTransaction: (tx: VaultTransactionNode | null) => void;
  setDepositAmount: (amount: number) => void;
  setDepositPaymentMethod: (method: PaymentMethod) => void;
  setDepositDescription: (desc: string) => void;
  setWithdrawalAmount: (amount: number) => void;
  setWithdrawalMethod: (method: PaymentMethod) => void;
  setWithdrawalReason: (reason: string) => void;
  setWithdrawalAllowOverdraft: (allowed: boolean) => void;
  resetOperationStatus: () => void;

  // Backend Async Actions
  fetchVaultData: (groupId?: number) => Promise<void>;
  contribute: (groupId?: number) => Promise<void>;
  withdraw: (groupId?: number) => Promise<void>;
  reconcile: (groupId?: number) => Promise<void>;
}

export const useVaultStore = create<VaultState>((set, get) => ({
  groupId: 1,
  activeTab: 'OVERVIEW',
  sharedFund: {
    id: 1,
    groupId: 1,
    groupName: 'VinFast VF8 Founders Syndicate',
    currentBalance: 45000000,
    minimumReserve: 15000000,
    currency: 'VND',
    isLowLiquidity: false,
    safetyDeficitAmount: 0,
    updatedAt: new Date().toISOString(),
  },
  transactions: INITIAL_TRANSACTIONS,
  auditHistory: INITIAL_AUDIT_LOGS,
  reconciliationReport: {
    currentBalanceVnd: 45000000,
    calculatedLedgerBalanceVnd: 45000000,
    totalCreditsVnd: 52000000,
    totalDebitsVnd: 7000000,
    creditCount: 14,
    debitCount: 5,
    transactionCount: 19,
    isReconciled: true,
    reconciliationDeltaVnd: 0,
    reconciledAt: new Date().toISOString(),
    summary: 'Ledger state mathematically verified. All credits and debits balance to 45,000,000 VND.',
  },
  selectedTransaction: INITIAL_TRANSACTIONS[0],
  operationStatus: { status: 'IDLE' },

  depositAmountVnd: 5000000,
  depositPaymentMethod: 'VNPAY',
  depositDescription: 'Syndicate Operational Liquidity Injection',

  withdrawalAmountVnd: 3500000,
  withdrawalMethod: 'BANK_TRANSFER',
  withdrawalReason: 'Emergency Roadside Tire Replacement & Sensor Recalibration',
  withdrawalAllowOverdraft: false,

  isLoading: false,
  isSyncing: false,
  lastSyncedAt: null,
  error: null,

  setActiveTab: (tab: VaultTab) => {
    AudioEngine.play('UI_CLICK');
    set({ activeTab: tab });

    // Move camera to optimal angle based on active tab
    const { focusCamera } = get();
    if (tab === 'OVERVIEW') {
      focusCamera('VAULT_OVERVIEW');
    } else if (tab === 'CONTRIBUTE' || tab === 'WITHDRAW') {
      focusCamera('TERMINAL_FOCUS');
    } else if (tab === 'TRANSACTIONS') {
      focusCamera('RIBBON_FOCUS');
    } else if (tab === 'AUDIT_RECONCILIATION') {
      focusCamera('AUDIT_FOCUS');
    }
  },

  focusCamera: (presetKey: keyof typeof VAULT_LAYOUT.cameras) => {
    const preset = VAULT_LAYOUT.cameras[presetKey];
    if (preset) {
      useCameraStore.getState().moveTo(preset.position, preset.target, {
        duration: 1.2,
        ease: 'easeInOutCubic',
      });
      AudioEngine.play('CAMERA_WHOOSH');
    }
  },

  selectTransaction: (tx: VaultTransactionNode | null) => {
    if (tx) AudioEngine.play('UI_CLICK');
    set({ selectedTransaction: tx });
  },

  setDepositAmount: (amount: number) => {
    set({ depositAmountVnd: Math.max(100000, amount) });
  },

  setDepositPaymentMethod: (method: PaymentMethod) => {
    AudioEngine.play('UI_CLICK');
    set({ depositPaymentMethod: method });
  },

  setDepositDescription: (desc: string) => {
    set({ depositDescription: desc });
  },

  setWithdrawalAmount: (amount: number) => {
    set({ withdrawalAmountVnd: Math.max(100000, amount) });
  },

  setWithdrawalMethod: (method: PaymentMethod) => {
    AudioEngine.play('UI_CLICK');
    set({ withdrawalMethod: method });
  },

  setWithdrawalReason: (reason: string) => {
    set({ withdrawalReason: reason });
  },

  setWithdrawalAllowOverdraft: (allowed: boolean) => {
    AudioEngine.play('UI_CLICK');
    set({ withdrawalAllowOverdraft: allowed });
  },

  resetOperationStatus: () => {
    set({ operationStatus: { status: 'IDLE' } });
  },

  fetchVaultData: async (groupIdParam?: number) => {
    const targetGroupId = groupIdParam ?? get().groupId;
    set({ isLoading: true, isSyncing: true, error: null });

    try {
      const [fundData, txList, auditList, reconciliation] = await Promise.allSettled([
        getSharedFund(targetGroupId),
        getFundTransactions(targetGroupId),
        getFundAuditHistory(targetGroupId),
        reconcileFund(targetGroupId),
      ]);

      let updatedFund = get().sharedFund;
      if (fundData.status === 'fulfilled' && fundData.value) {
        updatedFund = fundData.value;
        // Synchronize Digital Twin finance facet
        useDigitalTwinStore.getState().updateFinance(1, {
          vaultBalanceVnd: fundData.value.currentBalance,
        });
      }

      let updatedTxs = get().transactions;
      if (txList.status === 'fulfilled' && Array.isArray(txList.value) && txList.value.length > 0) {
        updatedTxs = txList.value.map((dto: FundTransactionDTO) => ({
          id: dto.id,
          reference: dto.reference || `TX-VLT-${dto.id}`,
          type: dto.type,
          amountVnd: dto.amount,
          balanceAfterVnd: dto.balanceAfter,
          actorName: dto.userName || 'Syndicate Member',
          description: dto.description || 'Treasury Transaction',
          timestamp: dto.createdAt,
          receiptCode: `RCPT-${dto.id}-${(dto.reference || 'VLT').slice(-4)}`,
        }));
      }

      let updatedAudit = get().auditHistory;
      if (auditList.status === 'fulfilled' && Array.isArray(auditList.value) && auditList.value.length > 0) {
        updatedAudit = auditList.value.map((log: FundAuditLogDTO) => ({
          id: log.id,
          action: log.action,
          actorName: log.actorName || 'Audit Daemon',
          details: log.detailsJson || 'Ledger event logged.',
          ipAddress: log.ipAddress || '10.0.4.1',
          timestamp: log.createdAt,
        }));
      }

      let updatedRecon = get().reconciliationReport;
      if (reconciliation.status === 'fulfilled' && reconciliation.value) {
        const r = reconciliation.value;
        updatedRecon = {
          currentBalanceVnd: r.currentBalance,
          calculatedLedgerBalanceVnd: r.calculatedLedgerBalance,
          totalCreditsVnd: r.totalCredits,
          totalDebitsVnd: r.totalDebits,
          creditCount: r.creditCount,
          debitCount: r.debitCount,
          transactionCount: r.transactionCount,
          isReconciled: r.isReconciled,
          reconciliationDeltaVnd: r.reconciliationDelta,
          reconciledAt: r.reconciledAt,
          summary: r.summary,
        };
      }

      set({
        sharedFund: updatedFund,
        transactions: updatedTxs,
        auditHistory: updatedAudit,
        reconciliationReport: updatedRecon,
        selectedTransaction: updatedTxs[0] || null,
        isLoading: false,
        isSyncing: false,
        lastSyncedAt: new Date().toISOString(),
      });
    } catch (err: unknown) {
      console.warn('[useVaultStore] Backend sync notice (using robust cached data):', err);
      set({
        isLoading: false,
        isSyncing: false,
        error: (err as Error)?.message || 'Failed to sync with backend vault API',
      });
    }
  },

  contribute: async (groupIdParam?: number) => {
    const state = get();
    const targetGroupId = groupIdParam ?? state.groupId;
    const amount = state.depositAmountVnd;
    const method = state.depositPaymentMethod;
    const desc = state.depositDescription || 'Syndicate Capital Contribution';

    if (amount <= 0) {
      AudioEngine.play('NOTIF_ERROR');
      set({
        operationStatus: {
          status: 'ERROR',
          message: 'Contribution amount must be greater than 0 VND.',
        },
      });
      return;
    }

    set({
      operationStatus: { status: 'PROCESSING', message: 'Cryptographic deposit transmission initiated...' },
    });

    try {
      const txDto = await contributeToFund(targetGroupId, {
        amount,
        paymentMethod: method,
        description: desc,
      });

      const newNode: VaultTransactionNode = {
        id: txDto.id,
        reference: txDto.reference || `TX-VLT-${Date.now().toString().slice(-4)}`,
        type: 'DEPOSIT',
        amountVnd: txDto.amount,
        balanceAfterVnd: txDto.balanceAfter,
        actorName: txDto.userName || 'Minh Hiep (You)',
        description: txDto.description,
        timestamp: txDto.createdAt || new Date().toISOString(),
        receiptCode: `RCPT-DEP-${txDto.id}`,
      };

      const updatedBalance = txDto.balanceAfter;
      const currentFund = state.sharedFund;
      const updatedFund: SharedFundDTO | null = currentFund
        ? {
            ...currentFund,
            currentBalance: updatedBalance,
            isLowLiquidity: updatedBalance < currentFund.minimumReserve,
            safetyDeficitAmount: Math.max(0, currentFund.minimumReserve - updatedBalance),
            updatedAt: new Date().toISOString(),
          }
        : null;

      const newAudit: VaultAuditEntry = {
        id: Date.now(),
        action: 'DEPOSIT_CONFIRMED',
        actorName: 'Minh Hiep (You)',
        details: `Deposited ${amount.toLocaleString('vi-VN')} VND via ${method}. Reference: ${newNode.reference}`,
        ipAddress: '118.69.182.50',
        timestamp: new Date().toISOString(),
      };

      AudioEngine.play('NOTIF_SUCCESS');
      set({
        sharedFund: updatedFund,
        transactions: [newNode, ...state.transactions],
        auditHistory: [newAudit, ...state.auditHistory],
        selectedTransaction: newNode,
        operationStatus: {
          status: 'SUCCESS',
          message: `Successfully deposited ${amount.toLocaleString('vi-VN')} VND to vault!`,
          receiptId: newNode.receiptCode,
        },
      });

      // Synchronize Digital Twin finance facet
      useDigitalTwinStore.getState().updateFinance(1, {
        vaultBalanceVnd: updatedBalance,
        lastFundDeductionRef: newNode.reference,
      });

      // Refresh reconciliation in background
      get().fetchVaultData(targetGroupId).catch(() => {});
    } catch (err: unknown) {
      console.error('[useVaultStore] Deposit failed:', err);
      AudioEngine.play('NOTIF_ERROR');
      set({
        operationStatus: {
          status: 'ERROR',
          message: (err as Error)?.message || 'Failed to complete fund deposit.',
        },
      });
    }
  },

  withdraw: async (groupIdParam?: number) => {
    const state = get();
    const targetGroupId = groupIdParam ?? state.groupId;
    const amount = state.withdrawalAmountVnd;
    const method = state.withdrawalMethod;
    const reason = state.withdrawalReason || 'Emergency Syndicate Withdrawal';
    const allowOverdraft = state.withdrawalAllowOverdraft;
    const fund = state.sharedFund;

    if (amount <= 0) {
      AudioEngine.play('NOTIF_ERROR');
      set({
        operationStatus: {
          status: 'ERROR',
          message: 'Withdrawal amount must be greater than 0 VND.',
        },
      });
      return;
    }

    // Strict Enforcement of Business Rule BR-FIN-03: Minimum Safety Reserve
    if (fund && !allowOverdraft) {
      const remainingBalance = fund.currentBalance - amount;
      if (remainingBalance < fund.minimumReserve) {
        AudioEngine.play('NOTIF_WARNING');
        set({
          operationStatus: {
            status: 'ERROR',
            errorCode: 'BR-FIN-03',
            message: `BR-FIN-03 Violation: Withdrawal of ${amount.toLocaleString('vi-VN')} VND breaches the required safety reserve of ${fund.minimumReserve.toLocaleString('vi-VN')} VND (projected: ${remainingBalance.toLocaleString('vi-VN')} VND). Toggle 'Allow Overdraft' with syndicate quorum or lower the amount.`,
          },
        });
        return;
      }
    }

    set({
      operationStatus: { status: 'PROCESSING', message: 'Validating safety reserve threshold and signing withdrawal...' },
    });

    try {
      const txDto = await withdrawFromFund(targetGroupId, {
        amount,
        paymentMethod: method,
        description: reason,
        allowOverdraft,
      });

      const newNode: VaultTransactionNode = {
        id: txDto.id,
        reference: txDto.reference || `TX-WTH-${Date.now().toString().slice(-4)}`,
        type: 'WITHDRAWAL',
        amountVnd: txDto.amount,
        balanceAfterVnd: txDto.balanceAfter,
        actorName: txDto.userName || 'Minh Hiep (You)',
        description: txDto.description,
        timestamp: txDto.createdAt || new Date().toISOString(),
        receiptCode: `RCPT-WTH-${txDto.id}`,
      };

      const updatedBalance = txDto.balanceAfter;
      const updatedFund: SharedFundDTO | null = fund
        ? {
            ...fund,
            currentBalance: updatedBalance,
            isLowLiquidity: updatedBalance < fund.minimumReserve,
            safetyDeficitAmount: Math.max(0, fund.minimumReserve - updatedBalance),
            updatedAt: new Date().toISOString(),
          }
        : null;

      const newAudit: VaultAuditEntry = {
        id: Date.now(),
        action: 'WITHDRAWAL_CONFIRMED',
        actorName: 'Minh Hiep (You)',
        details: `Disbursed ${amount.toLocaleString('vi-VN')} VND via ${method}. Reference: ${newNode.reference}. AllowOverdraft=${allowOverdraft}`,
        ipAddress: '118.69.182.50',
        timestamp: new Date().toISOString(),
      };

      AudioEngine.play('NOTIF_SUCCESS');
      set({
        sharedFund: updatedFund,
        transactions: [newNode, ...state.transactions],
        auditHistory: [newAudit, ...state.auditHistory],
        selectedTransaction: newNode,
        operationStatus: {
          status: 'SUCCESS',
          message: `Disbursement of ${amount.toLocaleString('vi-VN')} VND completed successfully.`,
          receiptId: newNode.receiptCode,
        },
      });

      // Synchronize Digital Twin finance facet
      useDigitalTwinStore.getState().updateFinance(1, {
        vaultBalanceVnd: updatedBalance,
        lastFundDeductionRef: newNode.reference,
      });

      // Refresh reconciliation
      get().fetchVaultData(targetGroupId).catch(() => {});
    } catch (err: unknown) {
      console.error('[useVaultStore] Withdrawal failed:', err);
      AudioEngine.play('NOTIF_ERROR');
      set({
        operationStatus: {
          status: 'ERROR',
          message: (err as Error)?.message || 'Withdrawal rejected by backend server.',
        },
      });
    }
  },

  reconcile: async (groupIdParam?: number) => {
    const state = get();
    const targetGroupId = groupIdParam ?? state.groupId;

    set({
      operationStatus: { status: 'PROCESSING', message: 'Executing zero-knowledge mathematical ledger reconciliation...' },
    });

    try {
      const r: FundReconciliationDTO = await reconcileFund(targetGroupId);
      const reconModel: VaultReconciliationModel = {
        currentBalanceVnd: r.currentBalance,
        calculatedLedgerBalanceVnd: r.calculatedLedgerBalance,
        totalCreditsVnd: r.totalCredits,
        totalDebitsVnd: r.totalDebits,
        creditCount: r.creditCount,
        debitCount: r.debitCount,
        transactionCount: r.transactionCount,
        isReconciled: r.isReconciled,
        reconciliationDeltaVnd: r.reconciliationDelta,
        reconciledAt: r.reconciledAt,
        summary: r.summary,
      };

      const newAudit: VaultAuditEntry = {
        id: Date.now(),
        action: 'RECONCILIATION_RUN',
        actorName: 'Cryptographic Audit Stela',
        details: `Audited ${r.transactionCount} transactions. In balance: ${r.isReconciled}. Delta: ${r.reconciliationDelta.toLocaleString('vi-VN')} VND.`,
        ipAddress: '10.0.4.12',
        timestamp: new Date().toISOString(),
      };

      AudioEngine.play('NOTIF_SUCCESS');
      set({
        reconciliationReport: reconModel,
        auditHistory: [newAudit, ...state.auditHistory],
        operationStatus: {
          status: 'SUCCESS',
          message: r.isReconciled
            ? 'Vault ledger 100% balanced with zero discrepancy!'
            : `Warning: Reconciliation discrepancy of ${r.reconciliationDelta.toLocaleString('vi-VN')} VND detected!`,
        },
      });
    } catch (err: unknown) {
      console.error('[useVaultStore] Reconciliation failed:', err);
      AudioEngine.play('NOTIF_ERROR');
      set({
        operationStatus: {
          status: 'ERROR',
          message: (err as Error)?.message || 'Reconciliation execution failed.',
        },
      });
    }
  },
}));
