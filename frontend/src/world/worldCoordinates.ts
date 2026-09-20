import type { Vector3Tuple } from 'three';
import type { SectorId, SectorBounds, SectorMetadata, WorldPortalDefinition } from './worldTypes';

export const SECTOR_CENTERS: Record<SectorId, Vector3Tuple> = {
  SECURITY_CHECKPOINT: [0, 0, 80],
  CENTRAL_GARAGE: [0, 0, 0],
  CO_OWNERSHIP_HALL: [40, 0, 40],
  BOOKING_CHAMBER: [0, 0, -40],
  ENERGY_FINANCE_CENTER: [0, 0, -80],
  SHARED_FUND_VAULT: [-40, 0, -80],
  DIGITAL_CONTRACT_ROOM: [40, 0, -80],
  DECISION_CHAMBER: [40, 0, -40],
  AI_INTELLIGENCE_CENTER: [40, 0, 0],
  OPERATIONS_CENTER: [-40, 0, 0],
  SERVICE_WORKSHOP: [-40, 0, 40],
  DISPUTE_ROOM: [-40, 0, 80],
  ADMIN_COMMAND_CENTER: [0, 25, 0],
};

function createSectorBounds(center: Vector3Tuple, halfWidth: number, halfDepth: number, height = 12): SectorBounds {
  return {
    minX: center[0] - halfWidth,
    maxX: center[0] + halfWidth,
    minY: center[1],
    maxY: center[1] + height,
    minZ: center[2] - halfDepth,
    maxZ: center[2] + halfDepth,
    centerX: center[0],
    centerY: center[1],
    centerZ: center[2],
    radius: Math.sqrt(halfWidth * halfWidth + halfDepth * halfDepth),
  };
}

export const SECTOR_METADATA_REGISTRY: Record<SectorId, SectorMetadata> = {
  SECURITY_CHECKPOINT: {
    id: 'SECURITY_CHECKPOINT',
    name: 'Cổng An Ninh',
    subtitle: 'Cổng Vào & Xác Thực Danh Tính Sinh Trắc Học',
    description: 'Cổng an ninh công nghệ cao với sương mù thể tích và rào chắn laser bảo vệ.',
    centerCoordinates: SECTOR_CENTERS.SECURITY_CHECKPOINT,
    bounds: createSectorBounds(SECTOR_CENTERS.SECURITY_CHECKPOINT, 14, 14),
    primaryEnvironmentPreset: 'SECURITY_GATE',
    spawnPointId: 'SPAWN_SECURITY_ENTRY',
  },
  CENTRAL_GARAGE: {
    id: 'CENTRAL_GARAGE',
    name: 'Garage Trung Tâm EV',
    subtitle: 'Phòng Trưng Bày Metaverse & Trạm Đội Xe Digital Twin',
    description: 'Phòng trưng bày hiện đại với sàn phản chiếu bóng loáng, ánh sáng tự nhiên và các trụ sạc đang hoạt động.',
    centerCoordinates: SECTOR_CENTERS.CENTRAL_GARAGE,
    bounds: createSectorBounds(SECTOR_CENTERS.CENTRAL_GARAGE, 16, 16),
    primaryEnvironmentPreset: 'GARAGE_DAYLIGHT',
    spawnPointId: 'SPAWN_GARAGE_CENTER',
  },
  CO_OWNERSHIP_HALL: {
    id: 'CO_OWNERSHIP_HALL',
    name: 'Sảnh Đồng Sở Hữu',
    subtitle: 'Định Danh Hợp Tác Xã & Sổ Bộ Tin Cậy',
    description: 'Khán đài hình trụ bằng đá obsidian với đèn neon vàng hiển thị các bục cổ phần thành viên.',
    centerCoordinates: SECTOR_CENTERS.CO_OWNERSHIP_HALL,
    bounds: createSectorBounds(SECTOR_CENTERS.CO_OWNERSHIP_HALL, 14, 14),
    primaryEnvironmentPreset: 'GOLDEN_OBSIDIAN',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_CO_OWNERSHIP_ENTRY',
  },
  BOOKING_CHAMBER: {
    id: 'BOOKING_CHAMBER',
    name: 'Phòng Đặt Lịch',
    subtitle: 'Dòng Thời Gian Không-Thời Gian & Vòng Cung Đặt Chỗ',
    description: 'Được bao quanh bởi quầng sáng lịch toàn cảnh với các điểm kéo thả thời gian tương tác.',
    centerCoordinates: SECTOR_CENTERS.BOOKING_CHAMBER,
    bounds: createSectorBounds(SECTOR_CENTERS.BOOKING_CHAMBER, 14, 14),
    primaryEnvironmentPreset: 'CHRONO_CYAN',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_BOOKING_ENTRY',
  },
  ENERGY_FINANCE_CENTER: {
    id: 'ENERGY_FINANCE_CENTER',
    name: 'Trung Tâm Tài Chính & Năng Lượng',
    subtitle: 'Phòng Phân Bổ Chi Phí & Kiosk Thanh Toán',
    description: 'Phòng thí nghiệm tài chính sạch với đường ống dữ liệu ngọc lục bảo, tinh thể chi phí trôi nổi và thanh toán một chạm.',
    centerCoordinates: SECTOR_CENTERS.ENERGY_FINANCE_CENTER,
    bounds: createSectorBounds(SECTOR_CENTERS.ENERGY_FINANCE_CENTER, 14, 14),
    primaryEnvironmentPreset: 'FINANCE_EMERALD',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_FINANCE_ENTRY',
  },
  SHARED_FUND_VAULT: {
    id: 'SHARED_FUND_VAULT',
    name: 'Kho Quỹ Chung',
    subtitle: 'Kho Bạc Hợp Tác Xã & Cột Dự Trữ Thanh Khoản',
    description: 'Kho tiền mạng điều khiển học với cơ chế khóa titan kiên cố, cột số dư thanh khoản và dải ruy băng giao dịch cuộn.',
    centerCoordinates: SECTOR_CENTERS.SHARED_FUND_VAULT,
    bounds: createSectorBounds(SECTOR_CENTERS.SHARED_FUND_VAULT, 14, 14),
    primaryEnvironmentPreset: 'VAULT_AMBER',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_VAULT_ENTRY',
  },
  DIGITAL_CONTRACT_ROOM: {
    id: 'DIGITAL_CONTRACT_ROOM',
    name: 'Phòng Hợp Đồng Số',
    subtitle: 'Khu Pháp Lý Cấp Cao & Chữ Ký Toàn Ảnh',
    description: 'Phòng pháp lý điều hành với bản thảo 3D trôi nổi và các tấm cảm ứng chữ ký sinh trắc học.',
    centerCoordinates: SECTOR_CENTERS.DIGITAL_CONTRACT_ROOM,
    bounds: createSectorBounds(SECTOR_CENTERS.DIGITAL_CONTRACT_ROOM, 14, 14),
    primaryEnvironmentPreset: 'EXECUTIVE_SLATE',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_CONTRACT_ENTRY',
  },
  DECISION_CHAMBER: {
    id: 'DECISION_CHAMBER',
    name: 'Phòng Biểu Quyết',
    subtitle: 'Quản Trị Dân Chủ & Bục Túc Số Nghị Viện',
    description: 'Đấu trường nghị viện tương lai với bục toàn ảnh đề xuất, bục bỏ phiếu và cột chất lỏng túc số trực tiếp.',
    centerCoordinates: SECTOR_CENTERS.DECISION_CHAMBER,
    bounds: createSectorBounds(SECTOR_CENTERS.DECISION_CHAMBER, 14, 14),
    primaryEnvironmentPreset: 'PARLIAMENT_INDIGO',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_DECISION_ENTRY',
  },
  AI_INTELLIGENCE_CENTER: {
    id: 'AI_INTELLIGENCE_CENTER',
    name: 'Trung Tâm Trí Tuệ Nhân Tạo EV',
    subtitle: 'Lõi Nơ-ron & Trục Phân Tích Dự Đoán',
    description: 'Trục nơ-ron với các vòng con quay hồi chuyển đồng tâm quay tròn và các nút khuyến nghị tư vấn trôi nổi.',
    centerCoordinates: SECTOR_CENTERS.AI_INTELLIGENCE_CENTER,
    bounds: createSectorBounds(SECTOR_CENTERS.AI_INTELLIGENCE_CENTER, 14, 14),
    primaryEnvironmentPreset: 'NEURAL_PURPLE',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_AI_ENTRY',
  },
  OPERATIONS_CENTER: {
    id: 'OPERATIONS_CENTER',
    name: 'Trung Tâm Vận Hành',
    subtitle: 'Bàn Chẩn Đoán Nhân Viên & Trạm Quét QR',
    description: 'Nhà chứa điều khiển công nghiệp với trạm quét mã QR 3D và bàn chẩn đoán dữ liệu đo xa OBD-II thời gian thực.',
    centerCoordinates: SECTOR_CENTERS.OPERATIONS_CENTER,
    bounds: createSectorBounds(SECTOR_CENTERS.OPERATIONS_CENTER, 14, 14),
    primaryEnvironmentPreset: 'OPERATIONS_ORANGE',
    requiredRole: 'ROLE_STAFF',
    spawnPointId: 'SPAWN_OPERATIONS_ENTRY',
  },
  SERVICE_WORKSHOP: {
    id: 'SERVICE_WORKSHOP',
    name: 'Khu Bảo Dưỡng Kỹ Thuật',
    subtitle: 'Khoang Bảo Dưỡng & Cầu Nâng Thủy Lực',
    description: 'Khoang dịch vụ ô tô với cầu nâng xe thủy lực, cánh tay robot và các trạm thay thế phụ tùng.',
    centerCoordinates: SECTOR_CENTERS.SERVICE_WORKSHOP,
    bounds: createSectorBounds(SECTOR_CENTERS.SERVICE_WORKSHOP, 14, 14),
    primaryEnvironmentPreset: 'WORKSHOP_STEEL',
    requiredRole: 'ROLE_STAFF',
    spawnPointId: 'SPAWN_WORKSHOP_ENTRY',
  },
  DISPUTE_ROOM: {
    id: 'DISPUTE_ROOM',
    name: 'Phòng Giải Quyết Tranh Chấp',
    subtitle: 'Phòng Hòa Giải Trung Lập & Bể Toàn Ảnh Bằng Chứng',
    description: 'Phòng trọng tài trung lập với tinh thể tranh chấp màu đỏ thẫm trôi nổi và bể tọa độ khiếm khuyết 3D.',
    centerCoordinates: SECTOR_CENTERS.DISPUTE_ROOM,
    bounds: createSectorBounds(SECTOR_CENTERS.DISPUTE_ROOM, 14, 14),
    primaryEnvironmentPreset: 'DISPUTE_CRIMSON',
    spawnPointId: 'SPAWN_DISPUTE_ENTRY',
  },
  ADMIN_COMMAND_CENTER: {
    id: 'ADMIN_COMMAND_CENTER',
    name: 'Trung Tâm Quản Trị Tối Cao',
    subtitle: 'Đài Quan Sát Quỹ Đạo & 7 Lõi Lệnh',
    description: 'Đài quan sát trên cao nhìn bao quát toàn bộ metaverse, nơi chứa 7 lõi điều hành nguyên khối tương tác.',
    centerCoordinates: SECTOR_CENTERS.ADMIN_COMMAND_CENTER,
    bounds: createSectorBounds(SECTOR_CENTERS.ADMIN_COMMAND_CENTER, 16, 16, 15),
    primaryEnvironmentPreset: 'COMMAND_HORIZON',
    requiredRole: 'ROLE_ADMIN',
    spawnPointId: 'SPAWN_ADMIN_ENTRY',
  },
};

export const WORLD_PORTALS: WorldPortalDefinition[] = [
  {
    id: 'PORTAL_GATE_TO_GARAGE',
    name: 'Cổng vào Garage Trung tâm',
    label: 'GARAGE TRUNG TÂM',
    fromSector: 'SECURITY_CHECKPOINT',
    toSector: 'CENTRAL_GARAGE',
    position: [0, 0, 70],
    destinationPosition: [0, 0, 18],
    color: '#00e5ff',
  },
  {
    id: 'PORTAL_GARAGE_TO_GATE',
    name: 'Lối ra Cổng An ninh',
    label: 'CỔNG AN NINH',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'SECURITY_CHECKPOINT',
    position: [0, 0, 22],
    destinationPosition: [0, 0, 72],
    color: '#00e5ff',
  },
  {
    id: 'PORTAL_GARAGE_TO_BOOKING',
    name: 'Cổng vào Phòng Đặt lịch',
    label: 'PHÒNG ĐẶT LỊCH',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'BOOKING_CHAMBER',
    position: [0, 0, -20],
    destinationPosition: [0, 0, -32],
    color: '#00e5ff',
    requiredRole: 'ROLE_CO_OWNER',
  },
  {
    id: 'PORTAL_GARAGE_TO_CO_OWNERSHIP',
    name: 'Cổng Sảnh Đồng sở hữu',
    label: 'SẢNH ĐỒNG SỞ HỮU',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'CO_OWNERSHIP_HALL',
    position: [20, 0, 5],
    destinationPosition: [32, 0, 12],
    color: '#ffab00',
    requiredRole: 'ROLE_CO_OWNER',
  },
  {
    id: 'PORTAL_GARAGE_TO_AI',
    name: 'Cổng Trung tâm Trí tuệ AI',
    label: 'TRUNG TÂM TRÍ TUỆ AI',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'AI_INTELLIGENCE_CENTER',
    position: [20, 0, -5],
    destinationPosition: [32, 0, -2],
    color: '#b388ff',
    requiredRole: 'ROLE_CO_OWNER',
  },
  {
    id: 'PORTAL_GARAGE_TO_OPERATIONS',
    name: 'Cổng Trung tâm Vận hành',
    label: 'TRUNG TÂM VẬN HÀNH',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'OPERATIONS_CENTER',
    position: [-20, 0, 0],
    destinationPosition: [-32, 0, 0],
    color: '#ff9100',
    requiredRole: 'ROLE_STAFF',
  },
  {
    id: 'PORTAL_GARAGE_TO_ADMIN',
    name: 'Thang máy Quỹ đạo lên Trung tâm Quản trị',
    label: 'TRUNG TÂM QUẢN TRỊ',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'ADMIN_COMMAND_CENTER',
    position: [0, 0, 0],
    destinationPosition: [0, 25, 10],
    color: '#ffffff',
    requiredRole: 'ROLE_ADMIN',
  },
];

/**
 * Returns the sector containing the given 3D position, or null if outside all sectors.
 */
export function getSectorAtPosition(pos: Vector3Tuple): SectorId | null {
  const [x, y, z] = pos;
  for (const [sectorId, meta] of Object.entries(SECTOR_METADATA_REGISTRY)) {
    const { minX, maxX, minY, maxY, minZ, maxZ } = meta.bounds;
    if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
      return sectorId as SectorId;
    }
  }
  return null;
}

/**
 * Returns the nearest sector center to the given 3D coordinates.
 */
export function getNearestSector(pos: Vector3Tuple): SectorId {
  let nearestId: SectorId = 'CENTRAL_GARAGE';
  let minDistanceSq = Number.MAX_VALUE;

  for (const [sectorId, meta] of Object.entries(SECTOR_METADATA_REGISTRY)) {
    const [cx, cy, cz] = meta.centerCoordinates;
    const dx = pos[0] - cx;
    const dy = pos[1] - cy;
    const dz = pos[2] - cz;
    const distSq = dx * dx + dy * dy + dz * dz;

    if (distSq < minDistanceSq) {
      minDistanceSq = distSq;
      nearestId = sectorId as SectorId;
    }
  }

  return nearestId;
}

/**
 * Tests if a given 3D point is within the bounds of a specified sector.
 */
export function isPositionInSector(pos: Vector3Tuple, sectorId: SectorId): boolean {
  const meta = SECTOR_METADATA_REGISTRY[sectorId];
  if (!meta) return false;
  const { minX, maxX, minY, maxY, minZ, maxZ } = meta.bounds;
  return (
    pos[0] >= minX &&
    pos[0] <= maxX &&
    pos[1] >= minY &&
    pos[1] <= maxY &&
    pos[2] >= minZ &&
    pos[2] <= maxZ
  );
}
