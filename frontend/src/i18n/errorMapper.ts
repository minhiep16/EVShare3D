/**
 * EVShare 3D — Vietnamese Error Translation & Mapping Subsystem
 * Maps HTTP status codes, validation constraints, and business exceptions
 * into natural, polite, and clear Vietnamese messages for users.
 * Keeps raw technical details intact for console logging.
 */

import { ApiError, isApiError } from '@/api/apiError';
import { VI } from './vi';

export function mapApiErrorToVN(error: unknown): string {
  // Always log the raw error for developer debugging
  console.debug('[EVShare:ErrorMapper]', error);

  if (isApiError(error)) {
    // 1. Check HTTP Status Codes
    if (error.status === 409) {
      if (error.message.toLowerCase().includes('duplicate') || error.message.toLowerCase().includes('vote')) {
        return 'Bạn đã thực hiện biểu quyết cho đề xuất này rồi.';
      }
      if (error.message.toLowerCase().includes('booking') || error.message.toLowerCase().includes('conflict')) {
        return VI.errors.conflict;
      }
      return 'Dữ liệu đang có xung đột hoặc thao tác đã được thực hiện trước đó.';
    }

    if (error.status === 403) {
      return VI.errors.forbidden;
    }

    if (error.status === 401) {
      return VI.errors.unauthorized;
    }

    if (error.status === 404) {
      return VI.errors.notFound;
    }

    if (error.status === 400) {
      if (error.validationErrors && error.validationErrors.length > 0) {
        const first = error.validationErrors[0];
        return `${first.field}: ${first.message || VI.errors.required}`;
      }
      if (error.message.toLowerCase().includes('balance') || error.message.toLowerCase().includes('fund')) {
        return VI.errors.insufficientFunds;
      }
      return error.message || 'Thông tin yêu cầu không hợp lệ. Vui lòng kiểm tra lại.';
    }

    if (error.status >= 500) {
      return VI.errors.serverError;
    }

    if (error.isNetworkError) {
      return VI.errors.networkError;
    }
  }

  if (error instanceof Error) {
    if (error.message.toLowerCase().includes('network') || error.message.toLowerCase().includes('failed to fetch')) {
      return VI.errors.networkError;
    }
    return error.message;
  }

  return VI.errors.serverError;
}
