package com.example.evshare.service.impl;

import com.example.evshare.entity.Expense;
import com.example.evshare.entity.ExpenseAllocation;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.ExpenseAllocationRepository;
import com.example.evshare.repository.ExpenseRepository;
import com.example.evshare.service.CostAllocationService;
import com.example.evshare.service.allocation.AllocatedMemberShare;
import com.example.evshare.service.allocation.AllocationResult;
import com.example.evshare.service.allocation.CostAllocationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * High-level orchestration implementation of the syndicate cost allocation engine.
 * Dispatches to dedicated strategy beans and enforces exact reconciliation before persistence.
 */
@Service
@Transactional(readOnly = true)
public class CostAllocationServiceImpl implements CostAllocationService {

    private static final Logger log = LoggerFactory.getLogger(CostAllocationServiceImpl.class);

    private final ExpenseRepository expenseRepository;
    private final ExpenseAllocationRepository expenseAllocationRepository;
    private final Map<AllocationStrategy, CostAllocationStrategy> strategyMap = new EnumMap<>(AllocationStrategy.class);

    public CostAllocationServiceImpl(ExpenseRepository expenseRepository,
                                    ExpenseAllocationRepository expenseAllocationRepository,
                                    List<CostAllocationStrategy> strategies) {
        this.expenseRepository = expenseRepository;
        this.expenseAllocationRepository = expenseAllocationRepository;
        if (strategies != null) {
            for (CostAllocationStrategy strategy : strategies) {
                strategyMap.put(strategy.getStrategyMode(), strategy);
            }
        }
    }

    @Override
    public AllocationResult calculateAllocation(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null for allocation calculation");
        }

        AllocationStrategy mode = expense.getAllocationStrategy() != null
                ? expense.getAllocationStrategy()
                : AllocationStrategy.OWNERSHIP_BASED;

        CostAllocationStrategy strategy = strategyMap.get(mode);
        if (strategy == null) {
            throw new UnsupportedOperationException("Unsupported allocation strategy: " + mode);
        }

        log.debug("Executing cost allocation calculation using strategy {} for expense {}", mode, expense.getId());
        AllocationResult result = strategy.allocate(expense);

        // Assert exact mathematical reconciliation invariant
        BigDecimal totalAllocated = result.getTotalAllocated();
        BigDecimal expenseTotal = expense.getTotalAmount();
        if (totalAllocated == null || totalAllocated.compareTo(expenseTotal) != 0) {
            throw new IllegalStateException(String.format(
                    "Reconciliation violation: expense total %s does not equal total allocated %s",
                    expenseTotal, totalAllocated
            ));
        }

        return result;
    }

    @Override
    public AllocationResult calculateAllocation(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        return calculateAllocation(expense);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ExpenseAllocation> applyAndPersistAllocations(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null for persisting allocations");
        }

        AllocationResult result = calculateAllocation(expense);

        // Maintain existing persistent collection reference for Hibernate orphanRemoval
        if (expense.getAllocations() != null) {
            expense.getAllocations().clear();
        }

        List<ExpenseAllocation> allocationsToSave = new ArrayList<>();
        for (AllocatedMemberShare share : result.getShares()) {
            ExpenseAllocation allocation = new ExpenseAllocation();
            allocation.setExpense(expense);
            allocation.setUser(share.getUser());
            allocation.setAllocatedAmount(share.getAllocatedAmount());
            allocation.setIsSettled(false);
            allocation.setSettledAt(null);
            allocationsToSave.add(allocation);
        }

        List<ExpenseAllocation> saved = expenseAllocationRepository.saveAll(allocationsToSave);
        if (expense.getAllocations() != null) {
            expense.getAllocations().addAll(saved);
        }

        log.info("Persisted {} cost allocations for expense {} totaling {} {} (reconciled: 100.00%)",
                saved.size(), expense.getId(), result.getTotalAllocated(), expense.getCurrency());

        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ExpenseAllocation> applyAndPersistAllocations(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        return applyAndPersistAllocations(expense);
    }

    @Override
    public CostAllocationStrategy getStrategy(AllocationStrategy strategy) {
        CostAllocationStrategy resolved = strategyMap.get(strategy);
        if (resolved == null) {
            throw new UnsupportedOperationException("No strategy registered for mode: " + strategy);
        }
        return resolved;
    }
}
