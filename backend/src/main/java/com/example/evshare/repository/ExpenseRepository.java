package com.example.evshare.repository;

import com.example.evshare.entity.Expense;
import com.example.evshare.entity.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupId(Long groupId);

    List<Expense> findByGroupIdAndCategory(Long groupId, ExpenseCategory category);

    List<Expense> findByLoggedByUserId(Long loggedByUserId);
}
