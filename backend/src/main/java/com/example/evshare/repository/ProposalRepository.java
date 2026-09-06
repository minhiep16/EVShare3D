package com.example.evshare.repository;

import com.example.evshare.entity.Proposal;
import com.example.evshare.entity.enums.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findByGroupId(Long groupId);

    List<Proposal> findByGroupIdAndStatus(Long groupId, ProposalStatus status);

    List<Proposal> findByProposerUserId(Long proposerUserId);
}
