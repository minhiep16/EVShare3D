import type { WorkshopCameraPreset, WorkshopStation } from './workshopTypes';

export const WORKSHOP_SECTOR_CENTER: [number, number, number] = [-40, 0, 40];

export const WORKSHOP_STATIONS: Record<
  WorkshopStation,
  {
    name: string;
    description: string;
    relativePosition: [number, number, number];
    worldPosition: [number, number, number];
  }
> = {
  HYDRAULIC_LIFT: {
    name: 'Cầu Nâng Thủy Lực Hai Cột',
    description: 'Cầu nâng xe 4 tấn phục vụ bảo dưỡng gầm và hệ thống truyền động',
    relativePosition: [0, 0, 0],
    worldPosition: [-40, 0, 40],
  },
  DIAGNOSTIC_CART: {
    name: 'Xe Đẩy Chẩn Đoán OBD-II',
    description: 'Xe đẩy quét dữ liệu đo xa di động để trích xuất mã lỗi & hiệu chuẩn',
    relativePosition: [-3.6, 0, 2.4],
    worldPosition: [-43.6, 0, 42.4],
  },
  PARTS_RACK: {
    name: 'Giá Phụ Tùng Thay Thế Mô-đun',
    description: 'Kệ lưu trữ đĩa phanh gốm, mô-đun LiDAR và cell pin',
    relativePosition: [3.8, 0, 2.4],
    worldPosition: [-36.2, 0, 42.4],
  },
  WORK_ORDER_STELA: {
    name: 'Bia Lệnh Sửa Chữa & Trạng Thái',
    description: 'Bia điều phối điện tử kết nối với sổ cái backend và quy trình bàn giao xe',
    relativePosition: [0, 0, -4.8],
    worldPosition: [-40, 0, 35.2],
  },
};

export const WORKSHOP_CAMERA_PRESETS: Record<string, WorkshopCameraPreset> = {
  WORKSHOP_OVERVIEW: {
    name: 'Toàn Cảnh Xưởng Dịch Vụ',
    position: [-40, 8.5, 52],
    target: [-40, 1.2, 40],
  },
  HYDRAULIC_LIFT_FOCUS: {
    name: 'Cầu Nâng Thủy Lực',
    position: [-40, 2.8, 45.2],
    target: [-40, 1.4, 40],
  },
  DIAGNOSTIC_CART_FOCUS: {
    name: 'Xe Chẩn Đoán',
    position: [-43.6, 2.2, 43.8],
    target: [-43.6, 1.3, 42.4],
  },
  PARTS_RACK_FOCUS: {
    name: 'Giá Phụ Tùng',
    position: [-36.2, 2.2, 43.8],
    target: [-36.2, 1.4, 42.4],
  },
  WORK_ORDER_STELA_FOCUS: {
    name: 'Bia Lệnh Công Việc',
    position: [-40, 3.2, 38.8],
    target: [-40, 1.8, 35.2],
  },
  UNDERCARRIAGE_INSPECTION: {
    name: 'Kiểm Tra Gầm Xe',
    position: [-40, 0.9, 41.8],
    target: [-40, 1.2, 40],
  },
};

export const WORKSHOP_THEME = {
  primary: '#38bdf8', // Electric cyan / steel blue
  secondary: '#f59e0b', // Industrial amber
  darkBase: '#09090b', // Deep metallic slate
  panelBg: '#18181b',
  steelMetal: '#27272a',
  hazardYellow: '#eab308',
  hazardBlack: '#18181b',
  alertRed: '#ef4444',
  successGreen: '#10b981',
  textLight: '#f8fafc',
  textMuted: '#94a3b8',
};
