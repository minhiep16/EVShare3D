/**
 * EVShare 3D - Phase 01 Specification & Architecture Verification Script
 * Validates documentation completeness, cross-referencing integrity, and quality gates.
 */

const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '..');

let totalChecks = 0;
let passedChecks = 0;
let failures = [];

function check(description, assertionFn) {
  totalChecks++;
  try {
    const result = assertionFn();
    if (result) {
      passedChecks++;
      console.log(`  [PASS] ${description}`);
    } else {
      failures.push(`[FAIL] ${description}`);
      console.error(`  [FAIL] ${description}`);
    }
  } catch (err) {
    failures.push(`[ERROR] ${description}: ${err.message}`);
    console.error(`  [ERROR] ${description}: ${err.message}`);
  }
}

function fileExistsAndNotEmpty(relPath, minBytes = 500) {
  const fullPath = path.join(ROOT_DIR, relPath);
  if (!fs.existsSync(fullPath)) return false;
  const stats = fs.statSync(fullPath);
  return stats.size >= minBytes;
}

function fileContains(relPath, substrings) {
  const fullPath = path.join(ROOT_DIR, relPath);
  if (!fs.existsSync(fullPath)) return false;
  const content = fs.readFileSync(fullPath, 'utf8').toLowerCase();
  return substrings.every(sub => content.includes(sub.toLowerCase()));
}

console.log('===============================================================');
console.log('       EVSHARE 3D - PHASE 01 QUALITY GATE AUDIT               ');
console.log('===============================================================\n');

console.log('1. Verifying Required Specification Files in /docs:');
const requiredDocs = [
  'docs/REQUIREMENTS.md',
  'docs/BUSINESS_RULES.md',
  'docs/ARCHITECTURE.md',
  'docs/RBAC.md',
  'docs/API.md',
  'docs/DATABASE.md',
  'docs/WORLD_ARCHITECTURE.md',
  'docs/3D_DESIGN_SYSTEM.md',
  'docs/AI_SPECIFICATION.md',
];
requiredDocs.forEach(doc => {
  check(`File exists and substantive: ${doc}`, () => fileExistsAndNotEmpty(doc, 1000));
});

console.log('\n2. Verifying Agent Governance Files in /agent:');
const requiredAgentFiles = [
  'agent/IMPLEMENTATION_PLAN.md',
  'agent/CURRENT_STATUS.md',
  'agent/DECISIONS.md',
  'agent/KNOWN_ISSUES.md',
  'agent/AGENTS.md',
];
requiredAgentFiles.forEach(file => {
  check(`File exists and substantive: ${file}`, () => fileExistsAndNotEmpty(file, 500));
});

console.log('\n3. Verifying Sequential Phase Roadmap (PHASE 01 to 10):');
for (let i = 1; i <= 10; i++) {
  const phaseFile = `PHASE ${String(i).padStart(2, '0')}.md`;
  check(`Phase file exists: ${phaseFile}`, () => fileExistsAndNotEmpty(phaseFile, 500));
}
check('Root README.md exists and substantive', () => fileExistsAndNotEmpty('README.md', 1000));

console.log('\n4. Verifying Cross-Specification Consistency & Requirements:');

// Check Database entities
const expectedTables = [
  'users', 'roles', 'user_roles', 'identity_verifications', 'driver_licenses',
  'vehicles', 'ownership_groups', 'ownership_shares', 'co_ownership_contracts',
  'contract_signatures', 'bookings', 'usage_sessions', 'vehicle_inspections',
  'vehicle_services', 'shared_funds', 'fund_transactions', 'expenses',
  'expense_allocations', 'payments', 'proposals', 'vote_options', 'votes',
  'disputes', 'dispute_evidences', 'notifications', 'ai_recommendations', 'audit_logs'
];
check('DATABASE.md defines all 27 critical relational tables', () => {
  return fileContains('docs/DATABASE.md', expectedTables.map(t => `Table: \`${t}\``));
});

// Check 12 3D World Rooms
const expectedRooms = [
  'EV Central Garage',
  'Co-Ownership Hall',
  'Booking Chamber',
  'Energy & Finance Center',
  'Shared Fund Vault',
  'Digital Contract Room',
  'Decision Chamber',
  'AI Mobility Intelligence Center',
  'Operations Center',
  'Service Workshop',
  'Dispute Room',
  'Admin Command Center'
];
check('WORLD_ARCHITECTURE.md defines all 12 pure 3D environments', () => {
  return fileContains('docs/WORLD_ARCHITECTURE.md', expectedRooms);
});

// Check 3D UI Components
const expectedComponents = [
  'ThreeDButton',
  'ThreeDInput',
  'ThreeDKeyboard',
  'ThreeDPanel',
  'ThreeDWindow',
  'ThreeDCard',
  'ThreeDTerminal',
  'ThreeDChart',
  'ThreeDDropdown',
  'ThreeDSlider',
  'ThreeDProgress',
  'ThreeDNotification',
  'ThreeDPortal'
];
check('3D_DESIGN_SYSTEM.md specifies full 3D spatial UI component library', () => {
  return fileContains('docs/3D_DESIGN_SYSTEM.md', expectedComponents);
});

// Check 8-State UI Model
const expectedStates = ['IDLE', 'HOVER', 'ACTIVE', 'SELECTED', 'LOADING', 'SUCCESS', 'ERROR', 'DISABLED'];
check('3D_DESIGN_SYSTEM.md specifies 8-state interactive lifecycle', () => {
  return fileContains('docs/3D_DESIGN_SYSTEM.md', expectedStates);
});

// Check RBAC Roles
const expectedRoles = ['ROLE_CO_OWNER', 'ROLE_STAFF', 'ROLE_ADMIN'];
check('RBAC.md and API.md align on core roles', () => {
  return fileContains('docs/RBAC.md', expectedRoles) && fileContains('docs/API.md', ['Co-Owner', 'Staff', 'Admin']);
});

// Check Business Rule Invariants
check('BUSINESS_RULES.md defines 100% Equity invariant mathematically', () => {
  return fileContains('docs/BUSINESS_RULES.md', ['100.00%', 'sum']);
});

check('BUSINESS_RULES.md defines Fair Usage formula and imbalance tiers', () => {
  return fileContains('docs/BUSINESS_RULES.md', ['FAIR', 'SLIGHTLY_IMBALANCED', 'IMBALANCED', 'SEVERELY_IMBALANCED']);
});

check('AI_SPECIFICATION.md enforces strict advisory-only constraint', () => {
  return fileContains('docs/AI_SPECIFICATION.md', ['advisory', 'Zero Silent Mutations']);
});

check('ARCHITECTURE.md strictly establishes Pure 3D Canvas constraint', () => {
  return fileContains('docs/ARCHITECTURE.md', ['PURE 3D', 'WebGL', 'Zero HTML Overlays']);
});

console.log('\n===============================================================');
console.log(` AUDIT SUMMARY: ${passedChecks}/${totalChecks} Checks Passed (${Math.round(passedChecks/totalChecks*100)}%)`);
if (failures.length > 0) {
  console.log('\nFAILURES:');
  failures.forEach(f => console.log(`  ${f}`));
  console.log('===============================================================');
  process.exit(1);
} else {
  console.log(' ALL SPECIFICATIONS MUTUALLY CONSISTENT & COMPLETE!');
  console.log(' PHASE 01 QUALITY GATE: 100% SATISFIED');
  console.log('===============================================================');
  process.exit(0);
}
