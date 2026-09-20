/**
 * EVShare 3D — Vietnamese Localization & Formatting Utilities
 * Standardized locale: vi-VN
 * Enforces:
 * - Currency: VND / ₫ (Dot thousand separators: 2.450.000 ₫)
 * - Dates: DD/MM/YYYY
 * - Time: HH:mm
 * - Days of week: Thứ Hai .. Chủ Nhật / T2 .. CN
 * - Percentage: 75,01%
 * - Standardized Vietnamese status mappings
 */

/**
 * Format number with Vietnamese notation: dot '.' as thousand separator, comma ',' as decimal.
 */
export function formatNumberVN(val: number | null | undefined, decimals = 0): string {
  if (val === null || val === undefined || isNaN(val)) return '0';
  const fixed = decimals > 0 ? val.toFixed(decimals) : Math.round(val).toString();
  const [intPart, decPart] = fixed.split('.');
  const formattedInt = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return decPart !== undefined ? `${formattedInt},${decPart}` : formattedInt;
}

/**
 * Format currency in Vietnamese Dong (VND / ₫).
 * Example: 2450000 -> "2.450.000 ₫"
 */
export function formatCurrencyVND(amount: number | null | undefined, useSymbol = true): string {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return useSymbol ? '0 ₫' : '0 VND';
  }
  const formatted = formatNumberVN(Math.round(amount), 0);
  return useSymbol ? `${formatted} ₫` : `${formatted} VND`;
}

/**
 * Parses date input safely into a Date object.
 */
function toDate(input: string | Date | number | null | undefined): Date | null {
  if (!input) return null;
  const d = new Date(input);
  return isNaN(d.getTime()) ? null : d;
}

/**
 * Formats a date into Vietnamese DD/MM/YYYY format.
 * Example: "2026-09-20T10:00:00Z" -> "20/09/2026"
 */
export function formatDateVN(input: string | Date | number | null | undefined): string {
  const d = toDate(input);
  if (!d) return '--/--/----';
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

/**
 * Formats time into Vietnamese 24-hour HH:mm format.
 * Example: "2026-09-20T08:30:00Z" -> "08:30" (or local time equivalent)
 */
export function formatTimeVN(input: string | Date | number | null | undefined): string {
  const d = toDate(input);
  if (!d) return '--:--';
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
}

/**
 * Formats date and time into Vietnamese DD/MM/YYYY HH:mm format.
 * Example: "20/09/2026 08:30"
 */
export function formatDateTimeVN(input: string | Date | number | null | undefined): string {
  const d = toDate(input);
  if (!d) return '--/--/---- --:--';
  return `${formatDateVN(d)} ${formatTimeVN(d)}`;
}

/**
 * Returns the Vietnamese day of week.
 * short=false: Thứ Hai .. Chủ Nhật
 * short=true: T2 .. CN
 */
export function formatDayOfWeekVN(input: string | Date | number | null | undefined, short = false): string {
  const d = toDate(input);
  if (!d) return '';
  const dayIndex = d.getDay(); // 0 = Sunday, 1 = Monday, ...
  const fullDays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
  const shortDays = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
  return short ? shortDays[dayIndex] : fullDays[dayIndex];
}

/**
 * Formats percentage with Vietnamese decimal comma.
 * Example: 75.012 -> "75,01%"
 */
export function formatPercentageVN(val: number | null | undefined, decimals = 2): string {
  if (val === null || val === undefined || isNaN(val)) return '0%';
  const isWhole = Math.floor(val) === val;
  const decs = isWhole ? 0 : decimals;
  return `${formatNumberVN(val, decs)}%`;
}

/**
 * Standardized status translations for all EVShare 3D domain entities.
 */
const STATUS_TRANSLATIONS: Record<string, string> = {
  // Common
  ACTIVE: 'Đang hoạt động',
  INACTIVE: 'Không hoạt động',
  PENDING: 'Đang chờ xử lý',
  APPROVED: 'Đã phê duyệt',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Đã hết hạn',
  COMPLETED: 'Đã hoàn tất',
  SUCCESS: 'Thành công',
  FAILED: 'Thất bại',
  PROCESSING: 'Đang xử lý',

  // Vehicle & Fleet
  AVAILABLE: 'Sẵn sàng',
  RESERVED: 'Đã đặt trước',
  BOOKED: 'Đã được đặt',
  IN_USE: 'Đang sử dụng',
  MAINTENANCE: 'Đang bảo trì',
  CHARGING: 'Đang sạc',
  OFFLINE: 'Ngoại tuyến',
  DECOMMISSIONED: 'Ngừng hoạt động',

  // Contracts
  DRAFT: 'Bản thảo',
  PENDING_SIGNATURE: 'Chờ ký kết',
  SIGNED: 'Đã ký kết',
  TERMINATED: 'Đã chấm dứt',

  // Governance & Proposals
  VOTING_ACTIVE: 'Đang biểu quyết',
  PASSED: 'Đã thông qua',
  DEFEATED: 'Không thông qua',

  // Voting Options
  APPROVE: 'Tán thành',
  REJECT: 'Không tán thành',
  ABSTAIN: 'Không biểu quyết',

  // Disputes
  OPEN: 'Đang mở',
  UNDER_REVIEW: 'Đang xem xét',
  ESCALATED: 'Đã chuyển tiếp',
  RESOLVED: 'Đã giải quyết',

  // Fairness Tiers
  BALANCED: 'Cân bằng',
  SLIGHT_DEFICIT: 'Thiếu hụt nhẹ',
  HEAVY_DEFICIT: 'Thiếu hụt nghiêm trọng',
  OVER_UTILIZED: 'Vượt mức sử dụng',

  // User Roles
  ROLE_CO_OWNER: 'Đồng sở hữu',
  ROLE_STAFF: 'Nhân viên vận hành',
  ROLE_ADMIN: 'Quản trị viên',
};

/**
 * Translate any standard system status or enum value into idiomatic Vietnamese.
 */
export function formatStatusVN(status: string | null | undefined): string {
  if (!status) return 'Không xác định';
  const key = status.toUpperCase().trim();
  return STATUS_TRANSLATIONS[key] || status;
}
