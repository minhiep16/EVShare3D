import type { AdminCoreId, AdminCameraPreset } from './adminTypes';

export const ADMIN_SECTOR_CENTER: [number, number, number] = [0, 25, 0];

export const ADMIN_CORES_CONFIG: Record<
  AdminCoreId,
  {
    name: string;
    description: string;
    relativePosition: [number, number, number];
    worldPosition: [number, number, number];
    primaryColor: string;
    accentColor: string;
  }
> = {
  USER_CORE: {
    name: 'Lõi Định Danh & Sinh Trắc Người Dùng',
    description: 'Tự động xác thực KYC, quản trị quyền tài khoản và khóa an ninh',
    relativePosition: [0, 0, -5.4],
    worldPosition: [0, 25, -5.4],
    primaryColor: '#38bdf8',
    accentColor: '#0284c7',
  },
  VEHICLE_CORE: {
    name: 'Lõi Đo Xa Đội Xe & Khóa An Ninh',
    description: 'Ma trận dữ liệu đo xa xe thời gian thực, trạng thái chẩn đoán và khóa từ xa',
    relativePosition: [4.8, 0, -2.6],
    worldPosition: [4.8, 25, -2.6],
    primaryColor: '#06b6d4',
    accentColor: '#0891b2',
  },
  OWNERSHIP_CORE: {
    name: 'Lõi Cổ Phần & Quản Trị Nhóm',
    description: 'Xác thực mật mã sổ bộ cổ phần, số dư quỹ vốn và đóng băng chuyển nhượng',
    relativePosition: [4.8, 0, 2.6],
    worldPosition: [4.8, 25, 2.6],
    primaryColor: '#a855f7',
    accentColor: '#9333ea',
  },
  BOOKING_CORE: {
    name: 'Lõi Đặt Lịch Không-Thời Gian',
    description: 'Tự động giải quyết xung đột lịch, quyền ưu tiên và xóa lịch giữ chỗ quá hạn',
    relativePosition: [0, 0, 5.4],
    worldPosition: [0, 25, 5.4],
    primaryColor: '#f59e0b',
    accentColor: '#d97706',
  },
  FINANCE_CORE: {
    name: 'Lõi Kho Bạc & Thanh Khoản Quỹ Chung',
    description: 'Số dư quỹ dự phòng, bơm vốn thanh khoản, mã băm sổ cái kiểm toán và đóng băng giải ngân',
    relativePosition: [-4.8, 0, 2.6],
    worldPosition: [-4.8, 25, 2.6],
    primaryColor: '#10b981',
    accentColor: '#059669',
  },
  DISPUTE_CORE: {
    name: 'Lõi Trọng Tài & Phán Quyết Tranh Chấp',
    description: 'Sổ thụ lý trọng tài ràng buộc, thực thi phán quyết tóm lược và cấp tín dụng bồi thường',
    relativePosition: [-4.8, 0, -2.6],
    worldPosition: [-4.8, 25, -2.6],
    primaryColor: '#ff1744',
    accentColor: '#dc2626',
  },
  SYSTEM_CORE: {
    name: 'Lõi Hệ Thống Điều Hành Tối Cao Zenith',
    description: 'Tình trạng nút Metaverse máy chủ, thông lượng đổ bóng và khóa toàn bộ nền tảng',
    relativePosition: [0, 1.8, 0],
    worldPosition: [0, 26.8, 0],
    primaryColor: '#e0f2fe',
    accentColor: '#38bdf8',
  },
};

export const ADMIN_CAMERA_PRESETS: Record<string, AdminCameraPreset> = {
  ORBITAL_OVERVIEW: {
    name: 'Toàn Cảnh Quỹ Đạo',
    position: [0, 36, 18],
    target: [0, 26.5, 0],
  },
  USER_CORE_FOCUS: {
    name: 'Lõi Người Dùng',
    position: [0, 27, -2.2],
    target: [0, 26.2, -5.4],
  },
  VEHICLE_CORE_FOCUS: {
    name: 'Lõi Đội Xe',
    position: [2.2, 27, -1.2],
    target: [4.8, 26.2, -2.6],
  },
  OWNERSHIP_CORE_FOCUS: {
    name: 'Lõi Cổ Phần',
    position: [2.2, 27, 1.2],
    target: [4.8, 26.2, 2.6],
  },
  BOOKING_CORE_FOCUS: {
    name: 'Lõi Đặt Lịch',
    position: [0, 27, 2.2],
    target: [0, 26.2, 5.4],
  },
  FINANCE_CORE_FOCUS: {
    name: 'Lõi Tài Chính',
    position: [-2.2, 27, 1.2],
    target: [-4.8, 26.2, 2.6],
  },
  DISPUTE_CORE_FOCUS: {
    name: 'Lõi Tranh Chấp',
    position: [-2.2, 27, -1.2],
    target: [-4.8, 26.2, -2.6],
  },
  SYSTEM_CORE_FOCUS: {
    name: 'Lõi Hệ Thống',
    position: [0, 27.2, 4.2],
    target: [0, 26.8, 0],
  },
};

export const COMMAND_THEME = {
  primary: '#38bdf8',
  accentGold: '#fbbf24',
  alertRed: '#ff1744',
  successGreen: '#10b981',
  royalPurple: '#c084fc',
  darkObsidian: '#020617',
  deckGlass: '#030712',
  laserGrid: '#1e3a8a',
  textLight: '#f8fafc',
  textMuted: '#94a3b8',
};
