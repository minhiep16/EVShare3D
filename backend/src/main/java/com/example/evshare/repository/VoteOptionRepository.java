package com.example.evshare.repository;

import com.example.evshare.entity.VoteOption;
import com.example.evshare.entity.enums.VoteOptionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    List<VoteOption> findByProposalId(Long proposalId);

    Optional<VoteOption> findByProposalIdAndOptionKey(Long proposalId, VoteOptionKey optionKey);
}
