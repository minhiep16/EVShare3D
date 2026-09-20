import { create } from 'zustand';
import { authApi, UserResponse } from '@/api/authApi';
import { TokenManager } from '@/api/tokenManager';
import { useAppStore } from '@/stores/useAppStore';
import { useNavigationStore } from '@/world/useNavigationStore';
import { UserRole } from '@/world/worldTypes';
import { getErrorMessage } from '@/api/apiError';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export type AuthMode = 'LOGIN' | 'REGISTER' | 'AUTHENTICATED';

export interface AuthState {
  authMode: AuthMode;
  isBootstrapping: boolean;
  isLoading: boolean;
  authError: string | null;
  successNotice: string | null;

  // Form Fields
  emailInput: string;
  passwordInput: string;
  fullNameInput: string;
  phoneNumberInput: string;

  // Authenticated Profile
  currentUser: UserResponse | null;
  isGateUnlocked: boolean;

  // Actions
  setEmailInput: (val: string) => void;
  setPasswordInput: (val: string) => void;
  setFullNameInput: (val: string) => void;
  setPhoneNumberInput: (val: string) => void;
  setAuthMode: (mode: AuthMode) => void;
  clearError: () => void;

  bootstrapAuth: () => Promise<boolean>;
  login: (email?: string, password?: string) => Promise<boolean>;
  register: (fullName?: string, email?: string, password?: string) => Promise<boolean>;
  logout: () => Promise<void>;
  fillDemoCredentials: (role: 'CO_OWNER' | 'STAFF' | 'ADMIN') => void;
  enterWorld: () => Promise<boolean>;
}

function determinePrimaryRole(roles: string[] = []): UserRole {
  if (roles.includes('ROLE_ADMIN')) return 'ROLE_ADMIN';
  if (roles.includes('ROLE_STAFF')) return 'ROLE_STAFF';
  if (roles.includes('ROLE_CO_OWNER')) return 'ROLE_CO_OWNER';
  return 'GUEST';
}

export const useAuthStore = create<AuthState>((set, get) => ({
  authMode: 'LOGIN',
  isBootstrapping: false,
  isLoading: false,
  authError: null,
  successNotice: null,

  emailInput: 'alice@evshare.io',
  passwordInput: 'Password123!',
  fullNameInput: 'Alice Owner',
  phoneNumberInput: '+84901234567',

  currentUser: null,
  isGateUnlocked: false,

  setEmailInput: (emailInput) => set({ emailInput, authError: null }),
  setPasswordInput: (passwordInput) => set({ passwordInput, authError: null }),
  setFullNameInput: (fullNameInput) => set({ fullNameInput, authError: null }),
  setPhoneNumberInput: (phoneNumberInput) => set({ phoneNumberInput }),
  setAuthMode: (authMode) => set({ authMode, authError: null, successNotice: null }),
  clearError: () => set({ authError: null }),

  bootstrapAuth: async () => {
    const hasToken = TokenManager.hasAccessToken();
    if (!hasToken) {
      set({
        isBootstrapping: false,
        isGateUnlocked: false,
        currentUser: null,
        authMode: 'LOGIN',
      });
      useNavigationStore.getState().setUserRole('GUEST');
      return false;
    }

    set({ isBootstrapping: true, isLoading: true, authError: null });

    try {
      const user = await authApi.getCurrentUser();
      const primaryRole = determinePrimaryRole(user.roles);

      useAppStore.getState().setAuthSession({
        token: TokenManager.getAccessToken(),
        userId: user.id,
        username: user.fullName || user.email,
        roles: user.roles,
      });

      useNavigationStore.getState().setUserRole(primaryRole);

      set({
        isBootstrapping: false,
        isLoading: false,
        currentUser: user,
        isGateUnlocked: true,
        authMode: 'AUTHENTICATED',
        successNotice: `Chào mừng trở lại, ${user.fullName || user.email}.`,
      });

      return true;
    } catch {
      // Session invalid or token expired
      TokenManager.clearTokens();
      useAppStore.getState().clearAuthSession();
      useNavigationStore.getState().setUserRole('GUEST');

      set({
        isBootstrapping: false,
        isLoading: false,
        currentUser: null,
        isGateUnlocked: false,
        authMode: 'LOGIN',
      });

      return false;
    }
  },

  login: async (emailOverride, passwordOverride) => {
    const email = (emailOverride ?? get().emailInput).trim();
    const password = passwordOverride ?? get().passwordInput;

    if (!email || !password) {
      set({ authError: 'Vui lòng nhập đầy đủ email và mật mã.' });
      return false;
    }

    set({ isLoading: true, authError: null, successNotice: null });

    try {
      const authData = await authApi.login({ email, password });
      const primaryRole = determinePrimaryRole(authData.user.roles);

      // Ensure tokens and session are recorded
      TokenManager.setTokens(authData.accessToken, authData.refreshToken);
      useAppStore.getState().setAuthSession({
        token: authData.accessToken,
        userId: authData.user.id,
        username: authData.user.fullName || authData.user.email,
        roles: authData.user.roles,
      });

      useNavigationStore.getState().setUserRole(primaryRole);

      AudioEngine.play('SUCCESS_CHIME');

      set({
        isLoading: false,
        currentUser: authData.user,
        isGateUnlocked: true,
        authMode: 'AUTHENTICATED',
        successNotice: `Đã cấp quyền truy cập. Danh tính xác thực: ${authData.user.fullName || authData.user.email}.`,
        authError: null,
      });

      return true;
    } catch (err) {
      const message = getErrorMessage(err);
      AudioEngine.play('NOTIF_ERROR');
      set({
        isLoading: false,
        authError: message || 'Xác thực thất bại. Vui lòng kiểm tra lại thông tin.',
        isGateUnlocked: false,
      });
      return false;
    }
  },

  register: async (nameOverride, emailOverride, passwordOverride) => {
    const fullName = (nameOverride ?? get().fullNameInput).trim();
    const email = (emailOverride ?? get().emailInput).trim();
    const password = passwordOverride ?? get().passwordInput;
    const phoneNumber = get().phoneNumberInput.trim();

    if (!fullName || !email || !password) {
      set({ authError: 'Vui lòng điền đầy đủ họ tên, email và mật khẩu.' });
      return false;
    }

    set({ isLoading: true, authError: null, successNotice: null });

    try {
      const newUser = await authApi.register({
        fullName,
        email,
        password,
        phoneNumber: phoneNumber || undefined,
      });

      // Automatically authenticate the newly registered account
      const loginSuccess = await get().login(email, password);
      if (loginSuccess) {
        set({
          successNotice: `Đăng ký tài khoản thành công! Chào mừng, ${newUser.fullName}.`,
        });
        return true;
      }

      set({
        isLoading: false,
        authMode: 'LOGIN',
        successNotice: 'Đăng ký hoàn tất. Vui lòng đăng nhập với thông tin của bạn.',
      });
      return true;
    } catch (err) {
      const message = getErrorMessage(err);
      AudioEngine.play('NOTIF_ERROR');
      set({
        isLoading: false,
        authError: message || 'Đăng ký thất bại. Email có thể đã được sử dụng.',
      });
      return false;
    }
  },

  logout: async () => {
    set({ isLoading: true });
    try {
      await authApi.logout();
    } catch {
      // Ignore network errors on logout
    } finally {
      TokenManager.clearTokens();
      useAppStore.getState().clearAuthSession();
      useNavigationStore.getState().setUserRole('GUEST');

      set({
        isLoading: false,
        currentUser: null,
        isGateUnlocked: false,
        authMode: 'LOGIN',
        authError: null,
        successNotice: 'Phiên làm việc kết thúc. Cổng an ninh đã kích hoạt khóa.',
      });

      // Navigate back to Security Checkpoint if player was inside
      const currentSector = useNavigationStore.getState().currentSector;
      if (currentSector !== 'SECURITY_CHECKPOINT') {
        await useNavigationStore.getState().teleportToSector('SECURITY_CHECKPOINT');
      }
    }
  },

  fillDemoCredentials: (role) => {
    if (role === 'CO_OWNER') {
      set({
        emailInput: 'alice@evshare.io',
        passwordInput: 'Password123!',
        fullNameInput: 'Alice Owner',
        authError: null,
        successNotice: 'Đã điền thông tin mẫu Đồng sở hữu (alice@evshare.io).',
      });
    } else if (role === 'STAFF') {
      set({
        emailInput: 'tuan@evshare.io',
        passwordInput: 'Password123!',
        fullNameInput: 'Tuan Staff',
        authError: null,
        successNotice: 'Đã điền thông tin mẫu Nhân viên vận hành (tuan@evshare.io).',
      });
    } else if (role === 'ADMIN') {
      set({
        emailInput: 'admin@evshare.io',
        passwordInput: 'Password123!',
        fullNameInput: 'Minh Hiep',
        authError: null,
        successNotice: 'Đã điền thông tin mẫu Quản trị viên (admin@evshare.io).',
      });
    }
  },

  enterWorld: async () => {
    if (!get().isGateUnlocked || !useAppStore.getState().isAuthenticated) {
      set({ authError: 'Cổng đang khóa. Vui lòng xác thực danh tính trước.' });
      return false;
    }

    // Teleport through gateway portal into Central Garage Showroom
    return useNavigationStore.getState().teleportToSector('CENTRAL_GARAGE');
  },
}));
