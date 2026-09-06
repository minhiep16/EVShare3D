# PHASE 07 – VOTING, DECISION CHAMBER & DISPUTE RESOLUTION

Implement only PHASE 07.

Read:

```text
docs/REQUIREMENTS.md
docs/BUSINESS_RULES.md
docs/ARCHITECTURE.md
docs/DATABASE.md
docs/API.md
```

## OBJECTIVE

Build the governance, voting, and dispute arbitration subsystems.

---

# VOTING & DECISION CHAMBER

Implement:

* `VotingService`
* Proposal lifecycle:
  * `ACTIVE` -> `PASSED` / `REJECTED` / `EXPIRED`
* Proposal categories:
  * `ROUTINE_EXPENSE`
  * `MAJOR_EXPENSE`
  * `OPERATIONAL_RULE_CHANGE`
  * `OWNER_ADMISSION_OR_EXIT`
* Vote casting:
  * Choices: `APPROVE`, `REJECT`, `ABSTAIN`
  * Equity weighting calculation based on active `OwnershipShare.percentage`
  * Quorum validation ($\ge 60\%$ total equity participation required)
  * Passing threshold checks ($>50\%$ for routine, $\ge 75\%$ for major)

---

# DISPUTE RESOLUTION

Implement:

* `DisputeService`
* Dispute lifecycle:
  * `OPEN` -> `UNDER_REVIEW` -> `RESOLVED` / `ESCALATED`
* Dispute evidence handling
* Staff mediation review
* Admin final arbitration execution with automated fund balance adjustments

---

# API

Implement:

```text
GET  /api/v1/proposals/group/{groupId}
POST /api/v1/proposals
POST /api/v1/proposals/{id}/vote
GET  /api/v1/proposals/{id}/results
GET  /api/v1/disputes/group/{groupId}
POST /api/v1/disputes
POST /api/v1/disputes/{id}/resolve
```

---

# TESTS

Test:

* Proposal creation by eligible co-owner ($\ge 10\%$ equity)
* Rejection of duplicate voting by same user
* Equity-weighted vote aggregation
* Quorum calculation logic
* Dispute filing, evidence linking, and resolution workflow
* Access control enforcement

Run:

```text
mvn clean test
```

Update:

```text
agent/CURRENT_STATUS.md
```

Then STOP.
