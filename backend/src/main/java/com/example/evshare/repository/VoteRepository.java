package com.example.evshare.repository;

import com.example.evshare.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByProposalId(Long proposalId);

    Optional<Vote> findByProposalIdAndUserId(Long proposalId, Long userId);

    boolean existsByProposalIdAndUserId(Long proposalId, Long userId);

    List<Vote> findByVoteOptionId(Long voteOptionId);
}
