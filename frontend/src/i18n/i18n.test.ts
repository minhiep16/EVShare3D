import { describe, it, expect } from 'vitest';
import {
  formatCurrencyVND,
  formatDateVN,
  formatTimeVN,
  formatDateTimeVN,
  formatDayOfWeekVN,
  formatNumberVN,
  formatPercentageVN,
  formatStatusVN,
  mapApiErrorToVN,
  VI,
} from './index';
import { ApiError } from '@/api/apiError';

describe('Vietnamese i18n & Formatting Foundation', () => {
  describe('formatCurrencyVND', () => {
    it('formats numbers with Vietnamese dot thousand separators and ₫ symbol', () => {
      expect(formatCurrencyVND(2450000)).toBe('2.450.000 ₫');
      expect(formatCurrencyVND(1000000)).toBe('1.000.000 ₫');
      expect(formatCurrencyVND(0)).toBe('0 ₫');
      expect(formatCurrencyVND(null)).toBe('0 ₫');
      expect(formatCurrencyVND(2450000, false)).toBe('2.450.000 VND');
    });
  });

  describe('formatDateVN & formatTimeVN', () => {
    it('formats date to DD/MM/YYYY', () => {
      const d = new Date(2026, 8, 20, 8, 30); // Month is 0-indexed (8 = Sep)
      expect(formatDateVN(d)).toBe('20/09/2026');
      expect(formatTimeVN(d)).toBe('08:30');
      expect(formatDateTimeVN(d)).toBe('20/09/2026 08:30');
    });

    it('handles null or invalid dates gracefully', () => {
      expect(formatDateVN(null)).toBe('--/--/----');
      expect(formatTimeVN(null)).toBe('--:--');
    });
  });

  describe('formatDayOfWeekVN', () => {
    it('formats days of the week in Vietnamese', () => {
      const sunday = new Date(2026, 8, 20); // 2026-09-20 is Sunday
      expect(formatDayOfWeekVN(sunday, false)).toBe('Chủ Nhật');
      expect(formatDayOfWeekVN(sunday, true)).toBe('CN');

      const monday = new Date(2026, 8, 21); // 2026-09-21 is Monday
      expect(formatDayOfWeekVN(monday, false)).toBe('Thứ Hai');
      expect(formatDayOfWeekVN(monday, true)).toBe('T2');
    });
  });

  describe('formatPercentageVN & formatNumberVN', () => {
    it('formats percentage with Vietnamese decimal comma', () => {
      expect(formatPercentageVN(100)).toBe('100%');
      expect(formatPercentageVN(75.01)).toBe('75,01%');
      expect(formatPercentageVN(33.333, 2)).toBe('33,33%');
      expect(formatNumberVN(1000000)).toBe('1.000.000');
    });
  });

  describe('formatStatusVN', () => {
    it('translates standard English statuses to idiomatic Vietnamese', () => {
      expect(formatStatusVN('AVAILABLE')).toBe('Sẵn sàng');
      expect(formatStatusVN('IN_USE')).toBe('Đang sử dụng');
      expect(formatStatusVN('MAINTENANCE')).toBe('Đang bảo trì');
      expect(formatStatusVN('ACTIVE')).toBe('Đang hoạt động');
      expect(formatStatusVN('APPROVE')).toBe('Tán thành');
      expect(formatStatusVN('REJECT')).toBe('Không tán thành');
      expect(formatStatusVN('ABSTAIN')).toBe('Không biểu quyết');
      expect(formatStatusVN('RESOLVED')).toBe('Đã giải quyết');
    });
  });

  describe('mapApiErrorToVN', () => {
    it('translates HTTP 409 conflict to Vietnamese booking conflict', () => {
      const err = new ApiError({
        message: 'Booking conflict detected',
        status: 409,
      });
      expect(mapApiErrorToVN(err)).toBe(VI.errors.conflict);
    });

    it('translates HTTP 403 forbidden to Vietnamese', () => {
      const err = new ApiError({
        message: 'Access denied',
        status: 403,
      });
      expect(mapApiErrorToVN(err)).toBe(VI.errors.forbidden);
    });
  });
});
