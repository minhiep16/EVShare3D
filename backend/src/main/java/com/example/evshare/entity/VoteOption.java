package com.example.evshare.entity;

import com.example.evshare.entity.enums.VoteOptionKey;
import jakarta.persistence.*;

@Entity
@Table(
        name = "vote_options",
        uniqueConstraints = @UniqueConstraint(name = "uk_proposal_option_key", columnNames = {"proposal_id", "option_key"})
)
public class VoteOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "option_key", nullable = false, length = 30)
    private VoteOptionKey optionKey;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    public VoteOption() {
    }

    public VoteOption(Long id, Proposal proposal, VoteOptionKey optionKey, String label) {
        this.id = id;
        this.proposal = proposal;
        this.optionKey = optionKey;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public VoteOptionKey getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(VoteOptionKey optionKey) {
        this.optionKey = optionKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
