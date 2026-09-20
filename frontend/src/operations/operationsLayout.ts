import type { OperationsCameraPreset, OperationsStation } from './operationsTypes';

export const OPERATIONS_SECTOR_CENTER: [number, number, number] = [-40, 0, 0];

export const OPERATIONS_STATIONS: Record<
  OperationsStation,
  {
    name: string;
    description: string;
    relativePosition: [number, number, number];
    worldPosition: [number, number, number];
  }
> = {
  QR_DESK: {
    name: 'Trạm Quét Mã QR',
    description: 'Kiosk quét mã QR quang học để xác thực nhận xe và cấp token',
    relativePosition: [-3.4, 0, 1.2],
    worldPosition: [-43.4, 0, 1.2],
  },
  DISPATCH_CONSOLE: {
    name: 'Bàn Điều Phối & Vận Hành',
    description: 'Đo xa xe, nhật ký quãng đường/pin và động cơ nhận/trả xe',
    relativePosition: [0, 0, 2.5],
    worldPosition: [-40, 0, 2.5],
  },
  FLEET_STELA: {
    name: 'Bia Trạng Thái Đội Xe',
    description: 'Bia đo xa bản sao số cong giám sát đội xe đa khoang',
    relativePosition: [0, 0, -4.8],
    worldPosition: [-40, 0, -4.8],
  },
  NOTIFICATION_BOARD: {
    name: 'Toàn Ảnh Thông Báo Vận Hành',
    description: 'Điều phối sự cố theo thời gian thực, cảnh báo đo xa và thông báo khoang đỗ',
    relativePosition: [3.6, 0, 1.2],
    worldPosition: [-36.4, 0, 1.2],
  },
  INSPECTION_BAY: {
    name: 'Khoang Kiểm Tra Khung Gầm',
    description: 'Khu vực vật lý kiểm tra xe với quét laser LiDAR và xác thực tình trạng',
    relativePosition: [0, 0, -1.2],
    worldPosition: [-40, 0, -1.2],
  },
};

export const OPERATIONS_CAMERA_PRESETS: Record<string, OperationsCameraPreset> = {
  HANGAR_OVERVIEW: {
    name: 'Toàn Cảnh Nhà Hangar',
    position: [-40, 8.5, 12],
    target: [-40, 1.2, 0],
  },
  QR_STATION_FOCUS: {
    name: 'Trạm Quét QR',
    position: [-43.4, 2.4, 4.4],
    target: [-43.4, 1.3, 1.2],
  },
  DISPATCH_CONSOLE_FOCUS: {
    name: 'Bàn Điều Phối',
    position: [-40, 2.8, 5.8],
    target: [-40, 1.2, 2.5],
  },
  FLEET_STATUS_FOCUS: {
    name: 'Ma Trận Đội Xe',
    position: [-40, 3.2, -1.2],
    target: [-40, 1.8, -4.8],
  },
  NOTIFICATION_BOARD_FOCUS: {
    name: 'Bảng Thông Báo',
    position: [-36.4, 2.4, 4.4],
    target: [-36.4, 1.4, 1.2],
  },
  INSPECTION_BAY_FOCUS: {
    name: 'Khoang Kiểm Tra',
    position: [-40, 4.2, 2.2],
    target: [-40, 0.5, -1.2],
  },
};

export const OPERATIONS_THEME = {
  primary: '#f97316', // Operations Orange
  secondary: '#fbbf24', // Amber Caution
  darkBase: '#080b12', // Deep Industrial Slate
  panelBg: '#0f172a',
  hazardYellow: '#eab308',
  hazardBlack: '#111827',
  cyberCyan: '#06b6d4',
  alertRed: '#ef4444',
  successGreen: '#10b981',
  textLight: '#f8fafc',
  textMuted: '#94a3b8',
};

export const SERVICE_BAYS = [
  { id: 'BAY_01', name: 'KHOANG 01 — SẠC NHANH', offset: [-4.2, 0, -1.2] as [number, number, number], defaultVehicleId: 1 },
  { id: 'BAY_02', name: 'KHOANG 02 — CHỜ KIỂM TRA', offset: [0, 0, -1.2] as [number, number, number], defaultVehicleId: 2 },
  { id: 'BAY_03', name: 'KHOANG 03 — SẴN SÀNG ĐIỀU PHỐI', offset: [4.2, 0, -1.2] as [number, number, number], defaultVehicleId: 3 },
];
