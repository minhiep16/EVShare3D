package com.example.evshare.repository;

import com.example.evshare.entity.ExpenseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseAllocationRepository extends JpaRepository<ExpenseAllocation, Long> {

    List<ExpenseAllocation> findByExpenseId(Long expenseId);

    List<ExpenseAllocation> findByUserId(Long userId);

    List<ExpenseAllocation> findByUserIdAndIsSettled(Long userId, Boolean isSettled);
}
