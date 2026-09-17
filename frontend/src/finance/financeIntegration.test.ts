import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useFinanceStore } from './useFinanceStore';
import { useVaultStore } from '@/vault/useVaultStore';
import { useDigitalTwinStore } from '@/digitalTwin/useDigitalTwinStore';
import * as financeApi from '@/api/financeApi';

describe('Phase 09-U: Finance Integration Layer (Finance Center, Vault, Payment Terminal)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();

    // Reset Digital Twin state
    useDigitalTwinStore.getState().resetToDefaults();

    // Reset Finance Store
    useFinanceStore.setState({
      groupId: 1,
      activeTab: 'OVERVIEW',
      expenses: [],
      selectedExpenseId: null,
      allocationStrategy: 'HYBRID',
      memberSummaries: [],
      sharedFund: {
        id: 1,
        currentBalance: 50000000,
        minimumReserve: 30000000,
        isLowLiquidity: false,
        safetyDeficitAmount: 0,
      },
      paymentSession: {
        status: 'IDLE',
        amountVnd: 5000000,
        method: 'BANK_TRANSFER',
      },
    });

    // Reset Vault Store
    useVaultStore.setState({
      groupId: 1,
      activeTab: 'OVERVIEW',
      sharedFund: null,
      transactions: [],
      auditHistory: [],
      reconciliationReport: null,
      selectedTransaction: null,
      operationStatus: { status: 'IDLE' },
      depositAmountVnd: 5000000,
      withdrawalAmountVnd: 2000000,
      withdrawalAllowOverdraft: false,
    });
  });

  describe('1. Authoritative Backend Cost Allocation (BR-FIN-02 — No Frontend Math)', () => {
    it('fetches authoritative group allocation summaries directly from Spring Boot REST', async () => {
      const mockAllocSummary: financeApi.GroupAllocationSummaryDTO = {
        groupId: 1,
        strategy: 'OWNERSHIP_BASED',
        totalExpensesVnd: 20000000,
        memberSummaries: [
          {
            userId: 1,
            userName: 'Minh Hiep (You)',
            equityPercentage: 35.0,
            usagePercentage: 25.0,
            allocatedTotalVnd: 7000000, // 35% of 20M computed by backend Half-Even
            paidTotalVnd: 2000000,
            outstandingDueVnd: 5000000,
            settlementStatus: 'PARTIAL',
          },
          {
            userId: 2,
            userName: 'Tran Duc',
            equityPercentage: 65.0,
            usagePercentage: 75.0,
            allocatedTotalVnd: 13000000, // 65% of 20M computed by backend Half-Even
            paidTotalVnd: 13000000,
            outstandingDueVnd: 0,
            settlementStatus: 'SETTLED',
          },
        ],
      };

      const getSummarySpy = vi
        .spyOn(financeApi, 'getGroupAllocationSummary')
        .mockResolvedValueOnce(mockAllocSummary);

      await useFinanceStore.getState().setAllocationStrategy('OWNERSHIP_BASED');

      expect(getSummarySpy).toHaveBeenCalledWith(1, 'OWNERSHIP_BASED');
      const state = useFinanceStore.getState();
      expect(state.allocationStrategy).toBe('OWNERSHIP_BASED');
      expect(state.memberSummaries).toHaveLength(2);
      expect(state.memberSummaries[0].allocatedTotalVnd).toBe(7000000);
      expect(state.memberSummaries[0].outstandingDueVnd).toBe(5000000);

      // Verifies Digital Twin accruedExpenseLiabilityVnd is synchronized
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.finance.accruedExpenseLiabilityVnd).toBe(5000000);
    });

    it('creates expense and triggers backend allocateExpense endpoint', async () => {
      const mockCreatedExpense: financeApi.ExpenseDTO = {
        id: 401,
        groupId: 1,
        vehicleId: 1,
        category: 'MAINTENANCE',
        title: 'Brake Pad Service',
        description: 'OEM Brembo Pads Replacement',
        amount: 8500000,
        incurredDate: '2026-09-16T09:00:00Z',
        isSettled: false,
        allocations: [],
      };

      const createSpy = vi
        .spyOn(financeApi, 'createExpense')
        .mockResolvedValueOnce(mockCreatedExpense);
      const allocateSpy = vi
        .spyOn(financeApi, 'allocateExpense')
        .mockResolvedValueOnce([
          {
            id: 801,
            expenseId: 401,
            userId: 1,
            userName: 'Minh Hiep',
            sharePercentage: 35.0,
            allocatedAmount: 2975000,
            isPaid: false,
          },
        ]);

      vi.spyOn(financeApi, 'getSharedFund').mockResolvedValueOnce({
        id: 1,
        groupId: 1,
        currentBalance: 50000000,
        minimumReserve: 30000000,
        currency: 'VND',
        isLowLiquidity: false,
        safetyDeficitAmount: 0,
        totalContributions: 60000000,
        totalDisbursements: 10000000,
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-16T09:00:00Z',
      });
      vi.spyOn(financeApi, 'getGroupExpenses').mockResolvedValueOnce({
        content: [mockCreatedExpense],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      });
      vi.spyOn(financeApi, 'getGroupAllocationSummary').mockResolvedValueOnce({
        groupId: 1,
        strategy: 'HYBRID',
        totalExpensesVnd: 8500000,
        memberSummaries: [
          {
            userId: 1,
            userName: 'Minh Hiep',
            equityPercentage: 35.0,
            usagePercentage: 30.0,
            allocatedTotalVnd: 2975000,
            paidTotalVnd: 0,
            outstandingDueVnd: 2975000,
            settlementStatus: 'PENDING',
          },
        ],
      });

      const success = await useFinanceStore.getState().createNewExpenseAction({
        groupId: 1,
        vehicleId: 1,
        category: 'MAINTENANCE',
        title: 'Brake Pad Service',
        description: 'OEM Brembo Pads Replacement',
        amount: 8500000,
        incurredDate: '2026-09-16T09:00:00Z',
      });

      expect(success).toBe(true);
      expect(createSpy).toHaveBeenCalled();
      expect(allocateSpy).toHaveBeenCalledWith(401);
      expect(useFinanceStore.getState().expenses).toHaveLength(1);
    });
  });

  describe('2. Shared Fund Vault Synchronization & Digital Twin Updates', () => {
    it('synchronizes shared fund balance and last deduction reference into Digital Twin on fetch', async () => {
      vi.spyOn(financeApi, 'getSharedFund').mockResolvedValueOnce({
        id: 1,
        groupId: 1,
        currentBalance: 92500000,
        minimumReserve: 40000000,
        currency: 'VND',
        isLowLiquidity: false,
        safetyDeficitAmount: 0,
        totalContributions: 120000000,
        totalDisbursements: 27500000,
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-16T10:00:00Z',
      });
      vi.spyOn(financeApi, 'getFundTransactions').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'getFundAuditHistory').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'reconcileFund').mockResolvedValueOnce({
        currentBalance: 92500000,
        calculatedLedgerBalance: 92500000,
        totalCredits: 120000000,
        totalDebits: 27500000,
        creditCount: 5,
        debitCount: 2,
        transactionCount: 7,
        isReconciled: true,
        reconciliationDelta: 0,
        reconciledAt: '2026-09-16T10:00:00Z',
        summary: 'Cryptographically reconciled.',
      });

      await useVaultStore.getState().fetchVaultData(1);

      const vaultState = useVaultStore.getState();
      expect(vaultState.sharedFund?.currentBalance).toBe(92500000);

      // Verify Digital Twin finance facet is updated
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.finance.vaultBalanceVnd).toBe(92500000);
    });

    it('updates vault balance and lastFundDeductionRef on contribution', async () => {
      useVaultStore.setState({
        depositAmountVnd: 15000000,
        depositPaymentMethod: 'BANK_TRANSFER',
        depositDescription: 'Syndicate Capital Injection',
      });

      const txMock: financeApi.FundTransactionDTO = {
        id: 501,
        fundId: 1,
        userId: 1,
        userName: 'Minh Hiep (You)',
        type: 'DEPOSIT',
        amount: 15000000,
        balanceAfter: 107500000,
        reference: 'TX-VLT-DEP-501',
        description: 'Syndicate Capital Injection',
        createdAt: '2026-09-16T10:30:00Z',
      };

      vi.spyOn(financeApi, 'contributeToFund').mockResolvedValueOnce(txMock);
      vi.spyOn(financeApi, 'getSharedFund').mockResolvedValueOnce({
        id: 1,
        groupId: 1,
        currentBalance: 107500000,
        minimumReserve: 40000000,
        currency: 'VND',
        isLowLiquidity: false,
        safetyDeficitAmount: 0,
        totalContributions: 135000000,
        totalDisbursements: 27500000,
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-16T10:30:00Z',
      });
      vi.spyOn(financeApi, 'getFundTransactions').mockResolvedValueOnce([txMock]);
      vi.spyOn(financeApi, 'getFundAuditHistory').mockResolvedValueOnce([]);
      vi.spyOn(financeApi, 'reconcileFund').mockResolvedValueOnce({} as any);

      await useVaultStore.getState().contribute(1);

      expect(useVaultStore.getState().operationStatus.status).toBe('SUCCESS');

      // Verify Digital Twin finance facet reflects new balance and reference
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.finance.vaultBalanceVnd).toBe(107500000);
      expect(twin.finance.lastFundDeductionRef).toBe('TX-VLT-DEP-501');
    });

    it('strictly enforces BR-FIN-03 safety reserve floor on disbursement', async () => {
      useVaultStore.setState({
        sharedFund: {
          id: 1,
          groupId: 1,
          currentBalance: 35000000,
          minimumReserve: 30000000,
          currency: 'VND',
          isLowLiquidity: false,
          safetyDeficitAmount: 0,
          totalContributions: 50000000,
          totalDisbursements: 15000000,
          createdAt: '2026-09-01T00:00:00Z',
          updatedAt: '2026-09-16T10:00:00Z',
        },
        withdrawalAmountVnd: 10000000, // 35M - 10M = 25M < 30M reserve floor
        withdrawalAllowOverdraft: false,
      });

      const apiSpy = vi.spyOn(financeApi, 'withdrawFromFund');

      await useVaultStore.getState().withdraw(1);

      // Blocked before API call
      expect(apiSpy).not.toHaveBeenCalled();
      const status = useVaultStore.getState().operationStatus;
      expect(status.status).toBe('ERROR');
      expect(status.errorCode).toBe('BR-FIN-03');
      expect(status.message).toContain('BR-FIN-03 Violation');
    });
  });

  describe('3. Payment Terminal Settlement Pipeline & Digital Twin Sync', () => {
    it('initiates payment and transitions to AWAITING_SETTLEMENT with VietQR payload', async () => {
      const initiateSpy = vi.spyOn(financeApi, 'initiatePayment').mockResolvedValueOnce({
        id: 7701,
        transactionReference: 'TX-PAY-7701',
        fundId: 1,
        userId: 1,
        amount: 4500000,
        currency: 'VND',
        paymentMethod: 'BANK_TRANSFER',
        status: 'PENDING',
        providerType: 'BANK_TRANSFER',
        qrCodeData: 'vietqr://pay?acc=109887766554&amount=4500000&memo=EVSHARE_7701',
        providerInstructions: 'Scan with Banking App to clear transaction',
        createdAt: '2026-09-16T11:00:00Z',
      });

      await useFinanceStore.getState().initiatePaymentAction(4500000, 'BANK_TRANSFER', 101);

      expect(initiateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          amount: 4500000,
          paymentMethod: 'BANK_TRANSFER',
          expenseAllocationId: 101,
        })
      );

      const session = useFinanceStore.getState().paymentSession;
      expect(session.status).toBe('AWAITING_SETTLEMENT');
      expect(session.paymentId).toBe(7701);
      expect(session.reference).toBe('TX-PAY-7701');
      expect(session.qrCodeData).toContain('vietqr://pay');
    });

    it('settles payment via verifyPayment and updates Digital Twin finance facet', async () => {
      useFinanceStore.setState({
        paymentSession: {
          status: 'AWAITING_SETTLEMENT',
          paymentId: 7701,
          reference: 'TX-PAY-7701',
          amountVnd: 4500000,
          method: 'BANK_TRANSFER',
        },
      });

      vi.spyOn(financeApi, 'verifyPayment').mockResolvedValueOnce({
        id: 7701,
        transactionReference: 'TX-PAY-7701',
        fundId: 1,
        userId: 1,
        amount: 4500000,
        currency: 'VND',
        paymentMethod: 'BANK_TRANSFER',
        status: 'COMPLETED',
        providerType: 'BANK_TRANSFER',
        createdAt: '2026-09-16T11:00:00Z',
      });

      vi.spyOn(financeApi, 'getSharedFund').mockResolvedValueOnce({
        id: 1,
        groupId: 1,
        currentBalance: 54500000, // Balance updated with settlement
        minimumReserve: 30000000,
        currency: 'VND',
        isLowLiquidity: false,
        safetyDeficitAmount: 0,
        totalContributions: 60000000,
        totalDisbursements: 5500000,
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-16T11:05:00Z',
      });

      vi.spyOn(financeApi, 'getGroupExpenses').mockResolvedValueOnce({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 10,
        number: 0,
      });

      vi.spyOn(financeApi, 'getGroupAllocationSummary').mockResolvedValueOnce({
        groupId: 1,
        strategy: 'HYBRID',
        totalExpensesVnd: 10000000,
        memberSummaries: [
          {
            userId: 1,
            userName: 'Minh Hiep',
            equityPercentage: 35.0,
            usagePercentage: 30.0,
            allocatedTotalVnd: 3500000,
            paidTotalVnd: 3500000,
            outstandingDueVnd: 0, // Fully settled!
            settlementStatus: 'SETTLED',
          },
        ],
      });

      const verified = await useFinanceStore.getState().verifyPaymentAction();

      expect(verified).toBe(true);
      const session = useFinanceStore.getState().paymentSession;
      expect(session.status).toBe('COMPLETED');
      expect(session.receiptId).toContain('REC-TX-PAY-7701');

      // Verify Digital Twin finance facet reflects zero outstanding liability and new balance
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.finance.vaultBalanceVnd).toBe(54500000);
      expect(twin.finance.accruedExpenseLiabilityVnd).toBe(0);
    });
  });
});
