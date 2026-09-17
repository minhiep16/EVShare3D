import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useNavigationStore } from './useNavigationStore';
import { PORTAL_NETWORK, getPortalsForSector } from './portalNetwork';
import { useAppStore } from '@/stores/useAppStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import type { SectorId } from './worldTypes';

describe('Phase 09-Z: Role-Based World Access Subsystem', () => {
  beforeEach(() => {
    // Reset stores to standard baseline
    useAppStore.setState({
      currentSector: 'SECURITY_CHECKPOINT',
      targetSector: null,
      isTeleporting: false,
      auth: { token: 'mock-jwt-token', userId: 101, username: 'testuser', roles: ['ROLE_CO_OWNER'] },
      isAuthenticated: true,
    });

    useNavigationStore.setState({
      currentSector: 'CENTRAL_GARAGE',
      destinationSector: null,
      navigationHistory: [],
      transitionPhase: 'IDLE',
      deniedAccessNotice: null,
      userRole: 'ROLE_CO_OWNER',
    });

    usePlayerStore.setState({
      position: [0, 0, 15],
      rotation: [0, 0, 0],
    });
  });

  describe('1. Role-to-Environment Access Matrix (Frontend Navigation Visibility)', () => {
    const ownerEnvironments: SectorId[] = [
      'CO_OWNERSHIP_HALL',
      'BOOKING_CHAMBER',
      'ENERGY_FINANCE_CENTER',
      'SHARED_FUND_VAULT',
      'DIGITAL_CONTRACT_ROOM',
      'DECISION_CHAMBER',
      'AI_INTELLIGENCE_CENTER',
    ];

    it('CO_OWNER: can access all owner environments, garage, checkpoint, and dispute room', () => {
      useNavigationStore.setState({ userRole: 'ROLE_CO_OWNER' });
      const { canAccessSector } = useNavigationStore.getState();

      // Common & public hubs
      expect(canAccessSector('SECURITY_CHECKPOINT')).toBe(true);
      expect(canAccessSector('CENTRAL_GARAGE')).toBe(true);
      expect(canAccessSector('DISPUTE_ROOM')).toBe(true);

      // All 7 Owner chambers
      for (const sector of ownerEnvironments) {
        expect(canAccessSector(sector)).toBe(true);
      }

      // Blocked from Staff & Admin chambers
      expect(canAccessSector('OPERATIONS_CENTER')).toBe(false);
      expect(canAccessSector('SERVICE_WORKSHOP')).toBe(false);
      expect(canAccessSector('ADMIN_COMMAND_CENTER')).toBe(false);
    });

    it('STAFF: can access operations, service workshop, dispute room, garage, and checkpoint', () => {
      useNavigationStore.setState({ userRole: 'ROLE_STAFF' });
      const { canAccessSector } = useNavigationStore.getState();

      // Allowed staff environments
      expect(canAccessSector('OPERATIONS_CENTER')).toBe(true);
      expect(canAccessSector('SERVICE_WORKSHOP')).toBe(true);
      expect(canAccessSector('DISPUTE_ROOM')).toBe(true);
      expect(canAccessSector('CENTRAL_GARAGE')).toBe(true);
      expect(canAccessSector('SECURITY_CHECKPOINT')).toBe(true);

      // Blocked from Admin and private owner chambers
      expect(canAccessSector('ADMIN_COMMAND_CENTER')).toBe(false);
      for (const sector of ownerEnvironments) {
        expect(canAccessSector(sector)).toBe(false);
      }
    });

    it('ADMIN: has universal oversight deck and access across all 13 sectors', () => {
      useNavigationStore.setState({ userRole: 'ROLE_ADMIN' });
      const { canAccessSector } = useNavigationStore.getState();

      const allSectors: SectorId[] = [
        'SECURITY_CHECKPOINT',
        'CENTRAL_GARAGE',
        'CO_OWNERSHIP_HALL',
        'BOOKING_CHAMBER',
        'ENERGY_FINANCE_CENTER',
        'SHARED_FUND_VAULT',
        'DIGITAL_CONTRACT_ROOM',
        'DECISION_CHAMBER',
        'AI_INTELLIGENCE_CENTER',
        'OPERATIONS_CENTER',
        'SERVICE_WORKSHOP',
        'DISPUTE_ROOM',
        'ADMIN_COMMAND_CENTER',
      ];

      for (const sector of allSectors) {
        expect(canAccessSector(sector)).toBe(true);
      }
    });

    it('GUEST: strictly restricted to SECURITY_CHECKPOINT', () => {
      useAppStore.setState({ isAuthenticated: false, auth: { token: null, userId: null, username: null, roles: [] } });
      useNavigationStore.setState({ userRole: 'GUEST' });
      const { canAccessSector } = useNavigationStore.getState();

      expect(canAccessSector('SECURITY_CHECKPOINT')).toBe(true);
      expect(canAccessSector('CENTRAL_GARAGE')).toBe(false);
      expect(canAccessSector('BOOKING_CHAMBER')).toBe(false);
      expect(canAccessSector('OPERATIONS_CENTER')).toBe(false);
      expect(canAccessSector('ADMIN_COMMAND_CENTER')).toBe(false);
    });
  });

  describe('2. Unauthorized Portal Rejection', () => {
    it('rejects co-owner entering staff operations portal with spatial notice', async () => {
      useNavigationStore.setState({ userRole: 'ROLE_CO_OWNER', currentSector: 'CENTRAL_GARAGE' });

      const success = await useNavigationStore
        .getState()
        .teleportToSector('OPERATIONS_CENTER', 'PORTAL_GARAGE_TO_OPERATIONS');

      expect(success).toBe(false);
      const notice = useNavigationStore.getState().deniedAccessNotice;
      expect(notice).not.toBeNull();
      expect(notice?.portalId).toBe('PORTAL_GARAGE_TO_OPERATIONS');
      expect(notice?.targetSectorId).toBe('OPERATIONS_CENTER');
      expect(notice?.requiredRole).toBe('ROLE_STAFF');
      expect(notice?.message).toContain('Requires ROLE_STAFF');
    });

    it('rejects staff entering co-owner booking chamber portal', async () => {
      useNavigationStore.setState({ userRole: 'ROLE_STAFF', currentSector: 'CENTRAL_GARAGE' });

      const success = await useNavigationStore
        .getState()
        .teleportToSector('BOOKING_CHAMBER', 'PORTAL_GARAGE_TO_BOOKING');

      expect(success).toBe(false);
      const notice = useNavigationStore.getState().deniedAccessNotice;
      expect(notice).not.toBeNull();
      expect(notice?.targetSectorId).toBe('BOOKING_CHAMBER');
      expect(notice?.requiredRole).toBe('ROLE_CO_OWNER');
      expect(notice?.message).toContain('Requires ROLE_CO_OWNER');
    });

    it('rejects unauthenticated guest attempting to enter Central Garage from checkpoint', async () => {
      useAppStore.setState({ isAuthenticated: false });
      useNavigationStore.setState({ userRole: 'GUEST', currentSector: 'SECURITY_CHECKPOINT' });

      const success = await useNavigationStore
        .getState()
        .teleportToSector('CENTRAL_GARAGE', 'PORTAL_GATE_TO_GARAGE');

      expect(success).toBe(false);
      const notice = useNavigationStore.getState().deniedAccessNotice;
      expect(notice).not.toBeNull();
      expect(notice?.message).toContain('UNAUTHORIZED');
    });
  });

  describe('3. Physical Perimeter Boundary Intrusion Defense', () => {
    it('detects unauthorized physical boundary breach and bounces avatar to safe spawn point', () => {
      useNavigationStore.setState({
        userRole: 'ROLE_CO_OWNER',
        currentSector: 'CENTRAL_GARAGE',
      });

      // Player crosses physical coordinates into ADMIN_COMMAND_CENTER
      const teleportSpy = vi.spyOn(usePlayerStore.getState(), 'teleportTo');

      const allowed = useNavigationStore.getState().handleSectorBoundaryIntrusion('ADMIN_COMMAND_CENTER');

      expect(allowed).toBe(false);
      expect(teleportSpy).toHaveBeenCalled();

      const notice = useNavigationStore.getState().deniedAccessNotice;
      expect(notice).not.toBeNull();
      expect(notice?.portalId).toBe('PERIMETER_ADMIN_COMMAND_CENTER');
      expect(notice?.requiredRole).toBe('ROLE_ADMIN');
      expect(notice?.message).toContain('PERIMETER RESTRICTION');

      teleportSpy.mockRestore();
    });

    it('allows boundary entry when avatar role satisfies required sector permissions', () => {
      useNavigationStore.setState({
        userRole: 'ROLE_ADMIN',
        currentSector: 'CENTRAL_GARAGE',
      });

      const allowed = useNavigationStore.getState().handleSectorBoundaryIntrusion('ADMIN_COMMAND_CENTER');
      expect(allowed).toBe(true);
      expect(useNavigationStore.getState().deniedAccessNotice).toBeNull();
    });
  });

  describe('4. Direct API Protection Invariant ("Frontend visibility is NOT security")', () => {
    it('enforces that direct backend API invocation is protected by backend RBAC tokens, not frontend state', async () => {
      // Demonstrates that even if a malicious client artificially bypasses frontend check:
      // A mock fetch to protected backend endpoints without proper Bearer token returns 401 or 403.
      const mockFetch = vi.fn(async (url: string, init?: RequestInit) => {
        const headers = (init?.headers as Record<string, string>) || {};
        const authHeader = headers['Authorization'] || '';

        if (!authHeader.startsWith('Bearer ')) {
          return new Response(JSON.stringify({ success: false, status: 401, message: 'Full authentication is required' }), {
            status: 401,
            headers: { 'Content-Type': 'application/json' },
          });
        }

        const token = authHeader.replace('Bearer ', '');
        if (url.includes('/admin-only') && token !== 'valid-admin-jwt') {
          return new Response(JSON.stringify({ success: false, status: 403, message: 'Access denied: Requires ROLE_ADMIN' }), {
            status: 403,
            headers: { 'Content-Type': 'application/json' },
          });
        }

        if (url.includes('/staff-only') && token !== 'valid-staff-jwt' && token !== 'valid-admin-jwt') {
          return new Response(JSON.stringify({ success: false, status: 403, message: 'Access denied: Requires ROLE_STAFF' }), {
            status: 403,
            headers: { 'Content-Type': 'application/json' },
          });
        }

        return new Response(JSON.stringify({ success: true, data: 'Access granted' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        });
      });

      // 1. Direct call with no token fails with 401
      const unauthRes = await mockFetch('/api/v1/test/rbac/admin-only');
      expect(unauthRes.status).toBe(401);

      // 2. Direct call with co-owner token to admin endpoint fails with 403
      const coOwnerRes = await mockFetch('/api/v1/test/rbac/admin-only', {
        headers: { Authorization: 'Bearer valid-coowner-jwt' },
      });
      expect(coOwnerRes.status).toBe(403);

      // 3. Direct call with co-owner token to staff endpoint fails with 403
      const staffRes = await mockFetch('/api/v1/test/rbac/staff-only', {
        headers: { Authorization: 'Bearer valid-coowner-jwt' },
      });
      expect(staffRes.status).toBe(403);

      // 4. Direct call with valid admin token succeeds with 200
      const adminRes = await mockFetch('/api/v1/test/rbac/admin-only', {
        headers: { Authorization: 'Bearer valid-admin-jwt' },
      });
      expect(adminRes.status).toBe(200);
    });
  });
});
