import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useVaultStore } from './useVaultStore';
import { VAULT_LAYOUT } from './vaultLayout';
import * as financeApi from '@/api/financeApi';

describe('Shared Fund Vault Domain & Operations (09-G)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    useVaultStore.setState({
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
        updatedAt: '2026-09-15T00:00:00Z',
      },
      depositAmountVnd: 5000000,
      depositPaymentMethod: 'VNPAY',
      depositDescription: 'Test Deposit',
      withdrawalAmountVnd: 3500000,
      withdrawalMethod: 'BANK_TRANSFER',
      withdrawalReason: 'Test Withdrawal',
      withdrawalAllowOverdraft: false,
      operationStatus: { status: 'IDLE' },
      transactions: [
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
      ],
      auditHistory: [
        {
          id: 101,
          action: 'FUND_RECONCILE',
          actorName: 'System Audit Daemon',
          details: 'Cryptographic ledger balance matches physical reserves.',
          ipAddress: '10.0.4.12',
          timestamp: '2026-09-15T04:00:00Z',
        },
      ],
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
        reconciledAt: '2026-09-15T04:00:00Z',
        summary: 'Ledger state mathematically verified.',
      },
      selectedTransaction: null,
      isLoading: false,
      isSyncing: false,
      lastSyncedAt: null,
      error: null,
    });
  });

  describe('1. Spatial Layout & Camera Architecture', () => {
    it('defines vault sector coordinates and floor bounds', () => {
      expect(VAULT_LAYOUT.sectorCenter).toEqual([-40, 0, -80]);
      expect(VAULT_LAYOUT.floorRadius).toBe(16.0);
    });

    it('provides distinct relative camera focus presets for all vault stations', () => {
      expect(VAULT_LAYOUT.cameras.VAULT_OVERVIEW).toBeDefined();
      expect(VAULT_LAYOUT.cameras.TERMINAL_FOCUS).toBeDefined();
      expect(VAULT_LAYOUT.cameras.LIQUID_COLUMN_FOCUS).toBeDefined();
      expect(VAULT_LAYOUT.cameras.RIBBON_FOCUS).toBeDefined();
      expect(VAULT_LAYOUT.cameras.AUDIT_FOCUS).toBeDefined();

      expect(VAULT_LAYOUT.cameras.VAULT_OVERVIEW.fov).toBe(42);
      expect(VAULT_LAYOUT.cameras.TERMINAL_FOCUS.fov).toBe(38);
    });
  });

  describe('2. Navigation Tabs & Transaction Selection', () => {
    it('switches active tabs and updates state', () => {
      const store = useVaultStore.getState();
      expect(store.activeTab).toBe('OVERVIEW');

      store.setActiveTab('CONTRIBUTE');
      expect(useVaultStore.getState().activeTab).toBe('CONTRIBUTE');

      store.setActiveTab('WITHDRAW');
      expect(useVaultStore.getState().activeTab).toBe('WITHDRAW');

      store.setActiveTab('TRANSACTIONS');
      expect(useVaultStore.getState().activeTab).toBe('TRANSACTIONS');

      store.setActiveTab('AUDIT_RECONCILIATION');
      expect(useVaultStore.getState().activeTab).toBe('AUDIT_RECONCILIATION');
    });

    it('selects transaction nodes for 3D receipt inspection', () => {
      const store = useVaultStore.getState();
      const firstTx = store.transactions[0];

      store.selectTransaction(firstTx);
      expect(useVaultStore.getState().selectedTransaction).toEqual(firstTx);

      store.selectTransaction(null);
      expect(useVaultStore.getState().selectedTransaction).toBeNull();
    });
  });

  describe('3. Capital Contribution & Real Backend Integration', () => {
    it('validates deposit amount must be greater than zero', async () => {
      useVaultStore.setState({ depositAmountVnd: 0 });
      await useVaultStore.getState().contribute();

      const status = useVaultStore.getState().operationStatus;
      expect(status.status).toBe('ERROR');
      expect(status.message).toContain('greater than 0');
    });

    it('successfully processes deposit, updates balance, and prepends transaction node', async () => {
      const mockTx: financeApi.FundTransactionDTO = {
        id: 999,
        fundId: 1,
        userId: 1,
        userName: 'Minh Hiep (You)',
        type: 'DEPOSIT',
        amount: 5000000,
        currency: 'VND',
        balanceAfter: 50000000,
        description: 'Syndicate Capital Top-up',
        reference: 'TX-DEP-999',
        createdAt: '2026-09-15T10:00:00Z',
      };

      vi.spyOn(financeApi, 'contributeToFund').mockResolvedValueOnce(mockTx);
      vi.spyOn(financeApi, 'getSharedFund').mockResolvedValueOnce({
        id: 1,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        currentBalance: 50000000,
        minimumReserve: 15000000,
        currency: 'VND',
        isLowLiquidity: false,
        safetyDeficitAmount: 0,
        updatedAt: '2026-09-15T10:00:00Z',
      });
      vi.spyOn(financeApi, 'getFundTransactions').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'getFundAuditHistory').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'reconcileFund').mockResolvedValueOnce({
        fundId: 1,
        groupId: 1,
        currentBalance: 50000000,
        calculatedLedgerBalance: 50000000,
        totalCredits: 57000000,
        totalDebits: 7000000,
        creditCount: 15,
        debitCount: 5,
        transactionCount: 20,
        isReconciled: true,
        reconciliationDelta: 0,
        reconciledAt: '2026-09-15T10:00:00Z',
        summary: 'Balanced',
      });

      useVaultStore.setState({ depositAmountVnd: 5000000 });
      await useVaultStore.getState().contribute();

      const state = useVaultStore.getState();
      expect(state.operationStatus.status).toBe('SUCCESS');
      expect(state.sharedFund?.currentBalance).toBe(50000000);
      expect(state.transactions[0].reference).toBe('TX-DEP-999');
      expect(state.transactions[0].amountVnd).toBe(5000000);
      expect(state.auditHistory[0].action).toBe('DEPOSIT_CONFIRMED');
    });
  });

  describe('4. Business Rule BR-FIN-03 (Minimum Safety Reserve Enforcement)', () => {
    it('blocks withdrawals that breach minimum safety reserve (BR-FIN-03)', async () => {
      // Current balance: 45M, Min reserve: 15M.
      // Attempting to withdraw 35M leaves 10M (< 15M). Must be rejected!
      useVaultStore.setState({
        withdrawalAmountVnd: 35000000,
        withdrawalAllowOverdraft: false,
      });

      const contributeSpy = vi.spyOn(financeApi, 'withdrawFromFund');

      await useVaultStore.getState().withdraw();

      const state = useVaultStore.getState();
      expect(state.operationStatus.status).toBe('ERROR');
      expect(state.operationStatus.errorCode).toBe('BR-FIN-03');
      expect(state.operationStatus.message).toContain('BR-FIN-03 Violation');
      expect(state.sharedFund?.currentBalance).toBe(45000000); // Untouched!
      expect(contributeSpy).not.toHaveBeenCalled(); // Network call aborted before violation
    });

    it('allows withdrawal when remaining balance satisfies minimum safety reserve', async () => {
      // Current balance: 45M, Min reserve: 15M.
      // Withdrawing 3.5M leaves 41.5M (>= 15M). Permitted!
      const mockTx: financeApi.FundTransactionDTO = {
        id: 1002,
        fundId: 1,
        userId: 1,
        userName: 'Minh Hiep (You)',
        type: 'WITHDRAWAL',
        amount: 3500000,
        currency: 'VND',
        balanceAfter: 41500000,
        description: 'Tire replacement',
        reference: 'TX-WTH-1002',
        createdAt: '2026-09-15T11:00:00Z',
      };

      vi.spyOn(financeApi, 'withdrawFromFund').mockResolvedValueOnce(mockTx);
      vi.spyOn(financeApi, 'getSharedFund').mockResolvedValueOnce({
        id: 1,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        currentBalance: 41500000,
        minimumReserve: 15000000,
        currency: 'VND',
        isLowLiquidity: false,
        safetyDeficitAmount: 0,
        updatedAt: '2026-09-15T11:00:00Z',
      });
      vi.spyOn(financeApi, 'getFundTransactions').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'getFundAuditHistory').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'reconcileFund').mockResolvedValueOnce({
        fundId: 1,
        groupId: 1,
        currentBalance: 41500000,
        calculatedLedgerBalance: 41500000,
        totalCredits: 52000000,
        totalDebits: 10500000,
        creditCount: 14,
        debitCount: 6,
        transactionCount: 20,
        isReconciled: true,
        reconciliationDelta: 0,
        reconciledAt: '2026-09-15T11:00:00Z',
        summary: 'Balanced',
      });

      useVaultStore.setState({
        withdrawalAmountVnd: 3500000,
        withdrawalAllowOverdraft: false,
      });

      await useVaultStore.getState().withdraw();

      const state = useVaultStore.getState();
      expect(state.operationStatus.status).toBe('SUCCESS');
      expect(state.sharedFund?.currentBalance).toBe(41500000);
      expect(state.transactions[0].reference).toBe('TX-WTH-1002');
      expect(state.auditHistory[0].action).toBe('WITHDRAWAL_CONFIRMED');
    });

    it('permits overdraft withdrawal when allowOverdraft is explicitly enabled', async () => {
      const mockTx: financeApi.FundTransactionDTO = {
        id: 1003,
        fundId: 1,
        userId: 1,
        userName: 'Minh Hiep (You)',
        type: 'WITHDRAWAL',
        amount: 35000000,
        currency: 'VND',
        balanceAfter: 10000000,
        description: 'Major battery module replacement waiver',
        reference: 'TX-WTH-1003',
        createdAt: '2026-09-15T12:00:00Z',
      };

      vi.spyOn(financeApi, 'withdrawFromFund').mockResolvedValueOnce(mockTx);
      vi.spyOn(financeApi, 'getSharedFund').mockResolvedValueOnce({
        id: 1,
        groupId: 1,
        groupName: 'VinFast VF8 Founders Syndicate',
        currentBalance: 10000000,
        minimumReserve: 15000000,
        currency: 'VND',
        isLowLiquidity: true,
        safetyDeficitAmount: 5000000,
        updatedAt: '2026-09-15T12:00:00Z',
      });
      vi.spyOn(financeApi, 'getFundTransactions').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'getFundAuditHistory').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'reconcileFund').mockResolvedValueOnce({
        fundId: 1,
        groupId: 1,
        currentBalance: 10000000,
        calculatedLedgerBalance: 10000000,
        totalCredits: 52000000,
        totalDebits: 42000000,
        creditCount: 14,
        debitCount: 6,
        transactionCount: 20,
        isReconciled: true,
        reconciliationDelta: 0,
        reconciledAt: '2026-09-15T12:00:00Z',
        summary: 'Balanced',
      });

      useVaultStore.setState({
        withdrawalAmountVnd: 35000000,
        withdrawalAllowOverdraft: true,
      });

      await useVaultStore.getState().withdraw();

      const state = useVaultStore.getState();
      expect(state.operationStatus.status).toBe('SUCCESS');
      expect(state.sharedFund?.currentBalance).toBe(10000000);
      expect(state.sharedFund?.isLowLiquidity).toBe(true);
    });
  });

  describe('5. Cryptographic Ledger Reconciliation', () => {
    it('executes zero-knowledge mathematical audit reconciliation', async () => {
      const mockRecon: financeApi.FundReconciliationDTO = {
        fundId: 1,
        groupId: 1,
        currentBalance: 45000000,
        calculatedLedgerBalance: 45000000,
        totalCredits: 52000000,
        totalDebits: 7000000,
        creditCount: 14,
        debitCount: 5,
        transactionCount: 19,
        isReconciled: true,
        reconciliationDelta: 0,
        reconciledAt: '2026-09-15T12:30:00Z',
        summary: 'Ledger mathematically verified.',
      };

      vi.spyOn(financeApi, 'reconcileFund').mockResolvedValueOnce(mockRecon);

      await useVaultStore.getState().reconcile();

      const state = useVaultStore.getState();
      expect(state.operationStatus.status).toBe('SUCCESS');
      expect(state.reconciliationReport?.isReconciled).toBe(true);
      expect(state.reconciliationReport?.reconciliationDeltaVnd).toBe(0);
      expect(state.auditHistory[0].action).toBe('RECONCILIATION_RUN');
    });
  });
});
