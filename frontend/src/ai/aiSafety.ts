import type { AISafetyActionType, AISafetyAttempt } from './aiTypes';

/**
 * AI Safety Boundary Protocol (BR-AI-SAFE-01)
 *
 * MANDATORY ARCHITECTURAL RULES:
 * The AI Intelligence Core is strictly an advisory, analytical, and predictive engine.
 * It is hardcoded to NEVER possess autonomous authority to:
 * 1. Authorize financial payments or debit syndicate reserves
 * 2. Alter fractional equity shares or member cap tables
 * 3. Bypass Role-Based Access Control (RBAC) security boundaries
 * 4. Sign, approve, or ratify legal contracts
 * 5. Execute irreversible financial or governance decisions
 */

export const AI_SAFETY_RULES: Record<
  AISafetyActionType,
  { label: string; blockedReason: string; requiredAuthority: string }
> = {
  BYPASS_AUTHENTICATION: {
    label: 'Bypass User Authentication / Identity Verification',
    blockedReason:
      'VIOLATION: AI algorithms cannot forge, bypass, or substitute cryptographic user credentials or JWT identity tokens. Authentication is strictly enforced by the platform authentication layer.',
    requiredAuthority: 'CRYPTOGRAPHIC_USER_CREDENTIALS_AND_JWT',
  },
  BYPASS_RBAC: {
    label: 'Bypass Role-Based Access Control (RBAC)',
    blockedReason:
      'VIOLATION: Cryptographic security policies cannot be overridden by predictive algorithms. Security tokens must originate from Keycloak/JWT authentication.',
    requiredAuthority: 'PLATFORM_SECURITY_ADMINISTRATOR',
  },
  ALTER_OWNERSHIP: {
    label: 'Alter Member Ownership / Equity Shares',
    blockedReason:
      'VIOLATION: AI cannot modify cap tables or syndicate equity allocations. Ownership transfers require formal amendment voting in Decision Chamber (BR-VOT-01).',
    requiredAuthority: 'SYNDICATE_PARLIAMENTARY_CONSENSUS (75% SUPERMAJORITY)',
  },
  AUTHORIZE_PAYMENT: {
    label: 'Authorize Direct Payment / Fund Withdrawal',
    blockedReason:
      'VIOLATION: AI is strictly advisory. Financial payments require explicit 2FA biometric authorization by co-owners in Energy & Finance Center (BR-FIN-03).',
    requiredAuthority: 'HUMAN_CO_OWNER_BIOMETRIC_SIGNATURE',
  },
  APPROVE_CONTRACT: {
    label: 'Execute / Sign Digital Contract',
    blockedReason:
      'VIOLATION: Legal agreements require verifiable personal digital signatures and individual cryptographic hash provenance in Digital Contract Room (BR-CON-02).',
    requiredAuthority: 'AUTHENTICATED_CO_OWNER_LEGAL_SIGNATURE',
  },
  OVERRIDE_VOTING_RULES: {
    label: 'Override Syndicate Voting Rules / Quorum',
    blockedReason:
      'VIOLATION: AI cannot cast ballots, bypass the 60.00% quorum requirement, or override democratic consensus thresholds. Output is purely advisory recommendation.',
    requiredAuthority: 'DEMOCRATIC_CO_OWNER_BALLOT_QUORUM',
  },
  IRREVERSIBLE_FINANCIAL_ACTION: {
    label: 'Irreversible Financial Commitment',
    blockedReason:
      'VIOLATION: Fund reserve rebalancing or asset divestment cannot be executed autonomously by AI. Requires co-owner quorum verification.',
    requiredAuthority: 'SYNDICATE_CO_OWNERS_QUORUM_APPROVAL',
  },
};

export const AISafetyEnforcer = {
  /**
   * Evaluates whether an action is permitted to be autonomously executed by AI.
   * By safety invariant, all prohibited action types return false.
   */
  canAIExecuteAutonomously: (actionType: AISafetyActionType): boolean => {
    return false;
  },

  /**
   * Enforces safety boundary, generating an immutable audit record of the blocked attempt.
   */
  interceptAttempt: (actionType: AISafetyActionType): AISafetyAttempt => {
    const rule = AI_SAFETY_RULES[actionType];
    return {
      id: `SAFETY_BLOCK_${Date.now()}_${Math.floor(Math.random() * 1000)}`,
      attemptedAction: actionType,
      actionLabel: rule.label,
      blockedReason: rule.blockedReason,
      timestamp: new Date().toISOString(),
      requiredAuthority: rule.requiredAuthority,
    };
  },
};
