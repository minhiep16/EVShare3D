import type { DisputeCameraPreset, DisputeStation } from './disputeTypes';

export const DISPUTE_SECTOR_CENTER: [number, number, number] = [-40, 0, 80];

export const DISPUTE_STATIONS: Record<
  DisputeStation,
  {
    name: string;
    description: string;
    relativePosition: [number, number, number];
    worldPosition: [number, number, number];
  }
> = {
  DISPUTE_CRYSTAL: {
    name: 'Tinh Thể Tranh Chấp Toàn Cảnh',
    description: 'Trục năng lượng trung tâm thể hiện mức độ nghiêm trọng và trạng thái tranh chấp',
    relativePosition: [0, 2.2, 0],
    worldPosition: [-40, 2.2, 80],
  },
  DEFECT_HOLOTANK: {
    name: 'Bể Toàn Ảnh Tọa Độ Khuyết Tật 3D',
    description: 'Hình chiếu bản sao số hiển thị các điểm ghim hư hại 3D và tọa độ khiếm khuyết',
    relativePosition: [0, 0, -1.8],
    worldPosition: [-40, 0, 78.2],
  },
  EVIDENCE_CAROUSEL: {
    name: 'Băng Chuyền Trưng Bày Bằng Chứng Bất Biến',
    description: 'Bảng kiểm tra bằng chứng hiển thị ảnh xác thực, chữ ký người tải lên và biên lai kiểm toán mốc thời gian',
    relativePosition: [-3.6, 0, 1.8],
    worldPosition: [-43.6, 0, 81.8],
  },
  STAFF_CONSOLE: {
    name: 'Bàn Hòa Giải & Xem Xét Của Nhân Viên',
    description: 'Bàn hòa giải cho nhân viên nền tảng xem xét tranh chấp, ghi chú sự việc và đề xuất phương án giải quyết',
    relativePosition: [3.6, 0, 1.8],
    worldPosition: [-36.4, 0, 81.8],
  },
  ADMIN_DAIS: {
    name: 'Bục Phán Quyết Trọng Tài Quản Trị Tối Cao',
    description: 'Bục phán quyết tối cao cho quản trị viên xem xét hồ sơ và thực thi điều chỉnh số dư ràng buộc',
    relativePosition: [0, 0, 4.2],
    worldPosition: [-40, 0, 84.2],
  },
  STATUS_STELA: {
    name: 'Bia Trạng Thái & Kiểm Toán Theo Dòng Thời Gian',
    description: 'Bia đá nguyên khối hiển thị sự kiện kiểm toán bất biến và lịch sử giải quyết chính thức',
    relativePosition: [0, 0, -5.2],
    worldPosition: [-40, 0, 74.8],
  },
};

export const DISPUTE_CAMERA_PRESETS: Record<string, DisputeCameraPreset> = {
  CHAMBER_OVERVIEW: {
    name: 'Toàn Cảnh Phòng Tranh Chấp',
    position: [-40, 8.5, 92],
    target: [-40, 1.4, 80],
  },
  CRYSTAL_FOCUS: {
    name: 'Tinh Thể Tranh Chấp',
    position: [-40, 2.8, 83.8],
    target: [-40, 2.2, 80],
  },
  HOLOTANK_FOCUS: {
    name: 'Bể Toàn Ảnh Khuyết Tật',
    position: [-40, 2.4, 76.5],
    target: [-40, 1.1, 78.2],
  },
  EVIDENCE_FOCUS: {
    name: 'Băng Chuyền Bằng Chứng',
    position: [-43.6, 2.4, 84.8],
    target: [-43.6, 1.4, 81.8],
  },
  STAFF_CONSOLE_FOCUS: {
    name: 'Bàn Hòa Giải Nhân Viên',
    position: [-36.4, 2.4, 84.8],
    target: [-36.4, 1.4, 81.8],
  },
  ADMIN_DAIS_FOCUS: {
    name: 'Bục Trọng Tài Quản Trị',
    position: [-40, 2.8, 87.8],
    target: [-40, 1.8, 84.2],
  },
};

export const DISPUTE_THEME = {
  primary: '#ff1744', // Vibrant arbitration crimson
  secondary: '#fbbf24', // Gold justice accent
  darkBase: '#1a0303', // Deep obsidian slate
  panelBg: '#2b0707',
  cyberCyan: '#38bdf8',
  alertRed: '#ef4444',
  verdictGreen: '#10b981',
  textLight: '#f8fafc',
  textMuted: '#cbd5e1',
};
