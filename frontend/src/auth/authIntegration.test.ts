import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { useAuthStore } from './useAuthStore';
import { authApi, AuthResponse, UserResponse } from '@/api/authApi';
import { TokenManager, StorageLike } from '@/api/tokenManager';
import { useAppStore } from '@/stores/useAppStore';
import { useNavigationStore } from '@/world/useNavigationStore';
import { ApiError } from '@/api/apiError';

class MockMemoryStorage implements StorageLike {
  private data: Record<string, string> = {};

  getItem(key: string): string | null {
    return this.data[key] ?? null;
  }
  setItem(key: string, value: string): void {
    this.data[key] = String(value);
  }
  removeItem(key: string): void {
    delete this.data[key];
  }
  clear(): void {
    this.data = {};
  }
}

describe('3D Authentication & Role-Aware World Access (09-Q)', () => {
  let mockStorage: MockMemoryStorage;

  const mockCoOwnerUser: UserResponse = {
    id: 101,
    email: 'alice@evshare.io',
    fullName: 'Alice Co-Owner',
    phoneNumber: '+84901234567',
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    roles: ['ROLE_CO_OWNER'],
  };

  const mockAdminUser: UserResponse = {
    id: 999,
    email: 'admin@evshare.io',
    fullName: 'Minh Hiep Sovereign Admin',
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    roles: ['ROLE_ADMIN', 'ROLE_CO_OWNER'],
  };

  const mockStaffUser: UserResponse = {
    id: 505,
    email: 'tuan@evshare.io',
    fullName: 'Tuan Operations Staff',
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    roles: ['ROLE_STAFF'],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockStorage = new MockMemoryStorage();
    TokenManager.setStorage(mockStorage);
    TokenManager.clearTokens();
    useAppStore.getState().clearAuthSession();
    useNavigationStore.setState({
      currentSector: 'SECURITY_CHECKPOINT',
      destinationSector: null,
      navigationHistory: [],
      transitionPhase: 'IDLE',
      deniedAccessNotice: null,
      userRole: 'GUEST',
    });
    useAuthStore.setState({
      authMode: 'LOGIN',
      isBootstrapping: false,
      isLoading: false,
      authError: null,
      successNotice: null,
      currentUser: null,
      isGateUnlocked: false,
    });
  });

  afterEach(() => {
    TokenManager.clearTokens();
    TokenManager.setStorage(null);
    useAppStore.getState().clearAuthSession();
  });

  describe('1. 3D Boot Flow & Session Bootstrap', () => {
    it('initializes at Security Gate with GUEST role and locked gate when no token exists', async () => {
      const success = await useAuthStore.getState().bootstrapAuth();

      expect(success).toBe(false);
      const state = useAuthStore.getState();
      expect(state.authMode).toBe('LOGIN');
      expect(state.currentUser).toBeNull();
      expect(state.isGateUnlocked).toBe(false);
      expect(useAppStore.getState().isAuthenticated).toBe(false);
      expect(useNavigationStore.getState().userRole).toBe('GUEST');
    });

    it('restores authenticated user session and unlocks gate when valid token is found on boot', async () => {
      TokenManager.setTokens('valid_boot_access_jwt', 'valid_boot_refresh_jwt');
      vi.spyOn(authApi, 'getCurrentUser').mockResolvedValueOnce(mockCoOwnerUser);

      const success = await useAuthStore.getState().bootstrapAuth();

      expect(success).toBe(true);
      const state = useAuthStore.getState();
      expect(state.authMode).toBe('AUTHENTICATED');
      expect(state.currentUser?.email).toBe('alice@evshare.io');
      expect(state.isGateUnlocked).toBe(true);
      expect(useAppStore.getState().isAuthenticated).toBe(true);
      expect(useAppStore.getState().auth.userId).toBe(101);
      expect(useNavigationStore.getState().userRole).toBe('ROLE_CO_OWNER');
    });

    it('clears session and returns to login mode if stored token is rejected on boot', async () => {
      TokenManager.setTokens('expired_boot_token');
      vi.spyOn(authApi, 'getCurrentUser').mockRejectedValueOnce(
        new ApiError({ message: 'Token expired', status: 401, code: 'UNAUTHORIZED' })
      );

      const success = await useAuthStore.getState().bootstrapAuth();

      expect(success).toBe(false);
      expect(TokenManager.hasAccessToken()).toBe(false);
      expect(useAuthStore.getState().authMode).toBe('LOGIN');
      expect(useAuthStore.getState().isGateUnlocked).toBe(false);
      expect(useNavigationStore.getState().userRole).toBe('GUEST');
    });
  });

  describe('2. 3D Login & Credential Authorization', () => {
    it('authenticates credentials via REST, updates tokens, unlocks gate, and syncs role', async () => {
      const mockAuthResponse: AuthResponse = {
        accessToken: 'jwt_alice_access_token',
        refreshToken: 'jwt_alice_refresh_token',
        tokenType: 'Bearer',
        user: mockCoOwnerUser,
      };

      vi.spyOn(authApi, 'login').mockResolvedValueOnce(mockAuthResponse);

      const success = await useAuthStore
        .getState()
        .login('alice@evshare.io', 'Password123!');

      expect(success).toBe(true);
      expect(authApi.login).toHaveBeenCalledWith({
        email: 'alice@evshare.io',
        password: 'Password123!',
      });

      const state = useAuthStore.getState();
      expect(state.authMode).toBe('AUTHENTICATED');
      expect(state.isGateUnlocked).toBe(true);
      expect(state.currentUser?.fullName).toBe('Alice Co-Owner');
      expect(state.authError).toBeNull();

      expect(TokenManager.getAccessToken()).toBe('jwt_alice_access_token');
      expect(useAppStore.getState().isAuthenticated).toBe(true);
      expect(useNavigationStore.getState().userRole).toBe('ROLE_CO_OWNER');
    });

    it('handles invalid credentials with 3D error notice and keeps gate locked', async () => {
      vi.spyOn(authApi, 'login').mockRejectedValueOnce(
        new ApiError({
          message: 'Invalid email or password',
          status: 401,
          code: 'UNAUTHORIZED',
        })
      );

      const success = await useAuthStore
        .getState()
        .login('wrong@evshare.io', 'BadPassword');

      expect(success).toBe(false);
      const state = useAuthStore.getState();
      expect(state.authError).toContain('Invalid email or password');
      expect(state.isGateUnlocked).toBe(false);
      expect(useAppStore.getState().isAuthenticated).toBe(false);
    });

    it('pre-fills testing credentials for Co-Owner, Staff, and Admin roles', () => {
      useAuthStore.getState().fillDemoCredentials('CO_OWNER');
      expect(useAuthStore.getState().emailInput).toBe('alice@evshare.io');

      useAuthStore.getState().fillDemoCredentials('STAFF');
      expect(useAuthStore.getState().emailInput).toBe('tuan@evshare.io');

      useAuthStore.getState().fillDemoCredentials('ADMIN');
      expect(useAuthStore.getState().emailInput).toBe('admin@evshare.io');
    });
  });

  describe('3. 3D Registration Flow', () => {
    it('submits registration via REST and automatically authenticates new user', async () => {
      const newRegisteredUser: UserResponse = {
        id: 202,
        email: 'bob@evshare.io',
        fullName: 'Bob New Driver',
        isActive: true,
        createdAt: '2026-09-16T00:00:00Z',
        updatedAt: '2026-09-16T00:00:00Z',
        roles: ['ROLE_CO_OWNER'],
      };

      vi.spyOn(authApi, 'register').mockResolvedValueOnce(newRegisteredUser);
      vi.spyOn(authApi, 'login').mockResolvedValueOnce({
        accessToken: 'jwt_bob_token',
        refreshToken: 'jwt_bob_refresh',
        tokenType: 'Bearer',
        user: newRegisteredUser,
      });

      const success = await useAuthStore
        .getState()
        .register('Bob New Driver', 'bob@evshare.io', 'Password123!');

      expect(success).toBe(true);
      expect(authApi.register).toHaveBeenCalledWith({
        fullName: 'Bob New Driver',
        email: 'bob@evshare.io',
        password: 'Password123!',
        phoneNumber: '+84901234567',
      });
      expect(useAuthStore.getState().isGateUnlocked).toBe(true);
    });
  });

  describe('4. 3D Logout & Session Termination', () => {
    it('revokes tokens, locks barrier, resets session to GUEST, and returns to Security Gate', async () => {
      TokenManager.setTokens('active_token');
      useAppStore.getState().setAuthSession({
        token: 'active_token',
        userId: 101,
        username: 'alice',
        roles: ['ROLE_CO_OWNER'],
      });
      useNavigationStore.setState({
        currentSector: 'CENTRAL_GARAGE',
        userRole: 'ROLE_CO_OWNER',
      });
      useAuthStore.setState({
        authMode: 'AUTHENTICATED',
        isGateUnlocked: true,
        currentUser: mockCoOwnerUser,
      });

      const logoutSpy = vi.spyOn(authApi, 'logout').mockResolvedValueOnce();

      await useAuthStore.getState().logout();

      expect(logoutSpy).toHaveBeenCalledTimes(1);
      expect(TokenManager.hasAccessToken()).toBe(false);
      expect(useAppStore.getState().isAuthenticated).toBe(false);

      const authState = useAuthStore.getState();
      expect(authState.authMode).toBe('LOGIN');
      expect(authState.isGateUnlocked).toBe(false);
      expect(authState.currentUser).toBeNull();

      expect(useNavigationStore.getState().userRole).toBe('GUEST');
      expect(useNavigationStore.getState().currentSector).toBe('SECURITY_CHECKPOINT');
    });
  });

  describe('5. Role-Aware World Access & Unauthorized Rejection', () => {
    it('strictly denies unauthenticated guest from warping to Central Garage Showroom', async () => {
      useAppStore.getState().clearAuthSession();
      useNavigationStore.setState({
        currentSector: 'SECURITY_CHECKPOINT',
        userRole: 'GUEST',
      });

      expect(useNavigationStore.getState().canAccessSector('CENTRAL_GARAGE')).toBe(false);

      const success = await useNavigationStore
        .getState()
        .teleportToSector('CENTRAL_GARAGE', 'PORTAL_GATE_TO_GARAGE');

      expect(success).toBe(false);
      const notice = useNavigationStore.getState().deniedAccessNotice;
      expect(notice).toBeDefined();
      expect(notice?.message).toContain('UNAUTHORIZED: Complete 3D Biometric Authentication');
      expect(useNavigationStore.getState().currentSector).toBe('SECURITY_CHECKPOINT');
    });

    it('permits authenticated CO_OWNER to access Showroom and Booking Chamber, but denies Admin Deck', async () => {
      useAppStore.getState().setAuthSession({
        token: 'alice_jwt',
        userId: 101,
        username: 'alice',
        roles: ['ROLE_CO_OWNER'],
      });
      useNavigationStore.setState({
        currentSector: 'CENTRAL_GARAGE',
        userRole: 'ROLE_CO_OWNER',
      });

      const nav = useNavigationStore.getState();
      expect(nav.canAccessSector('CENTRAL_GARAGE')).toBe(true);
      expect(nav.canAccessSector('BOOKING_CHAMBER')).toBe(true);
      expect(nav.canAccessSector('SHARED_FUND_VAULT')).toBe(true);

      // Denied access to sovereign admin deck
      expect(nav.canAccessSector('ADMIN_COMMAND_CENTER')).toBe(false);

      const attemptedAdminWarp = await useNavigationStore
        .getState()
        .teleportToSector('ADMIN_COMMAND_CENTER', 'PORTAL_TO_ADMIN');

      expect(attemptedAdminWarp).toBe(false);
      const notice = useNavigationStore.getState().deniedAccessNotice;
      expect(notice?.message).toContain('ACCESS RESTRICTED: Requires ROLE_ADMIN');
    });

    it('permits authenticated STAFF to access Operations Center and Service Workshop', async () => {
      useAppStore.getState().setAuthSession({
        token: 'staff_jwt',
        userId: 505,
        username: 'tuan',
        roles: ['ROLE_STAFF'],
      });
      useNavigationStore.setState({
        currentSector: 'CENTRAL_GARAGE',
        userRole: 'ROLE_STAFF',
      });

      const nav = useNavigationStore.getState();
      expect(nav.canAccessSector('OPERATIONS_CENTER')).toBe(true);
      expect(nav.canAccessSector('SERVICE_WORKSHOP')).toBe(true);
      expect(nav.canAccessSector('ADMIN_COMMAND_CENTER')).toBe(false);
    });

    it('grants sovereign unrestricted access to all sectors for ROLE_ADMIN', async () => {
      useAppStore.getState().setAuthSession({
        token: 'admin_jwt',
        userId: 999,
        username: 'admin',
        roles: ['ROLE_ADMIN'],
      });
      useNavigationStore.setState({
        currentSector: 'CENTRAL_GARAGE',
        userRole: 'ROLE_ADMIN',
      });

      const nav = useNavigationStore.getState();
      expect(nav.canAccessSector('ADMIN_COMMAND_CENTER')).toBe(true);
      expect(nav.canAccessSector('OPERATIONS_CENTER')).toBe(true);
      expect(nav.canAccessSector('SERVICE_WORKSHOP')).toBe(true);
      expect(nav.canAccessSector('CENTRAL_GARAGE')).toBe(true);
      expect(nav.canAccessSector('BOOKING_CHAMBER')).toBe(true);
    });
  });
});
