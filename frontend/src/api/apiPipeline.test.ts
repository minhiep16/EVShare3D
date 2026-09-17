import { describe, it, expect, beforeEach, vi } from 'vitest';
import { apiClient } from './apiClient';
import { bookingsApi, BookingResponseDTO } from './bookingsApi';
import { InteractionPipeline } from '@/engine/interaction/InteractionPipeline';
import { InteractionContext } from '@/engine/interaction/interactionTypes';
import { useBookingStore } from '@/booking/useBookingStore';
import { useGarageStore } from '@/garage/useGarageStore';
import { useAppStore } from '@/stores/useAppStore';
import { TokenManager } from './tokenManager';

describe('3D to Backend Interaction Pipeline (09-P)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    InteractionPipeline.clear();
    TokenManager.clearTokens();
    useAppStore.getState().clearAuthSession();
  });

  it('executes the full 8-stage pipeline: 3D Object -> InteractionManager -> API Client -> REST -> Response -> Zustand -> 3D update', async () => {
    // 0. Setup authentication & token
    TokenManager.setTokens('session_jwt_auth_token');
    useAppStore.getState().setAuthSession({
      token: 'session_jwt_auth_token',
      userId: 42,
      username: 'Alex Driver',
      roles: ['ROLE_CO_OWNER'],
    });

    // 1. Setup simulated Spring Boot REST response
    const mockCreatedBooking: BookingResponseDTO = {
      id: 777,
      vehicleId: 1,
      vehicleModel: 'Tesla Model S Plaid',
      vehicleLicensePlate: '29A-888.88',
      userId: 42,
      userName: 'Alex Driver',
      userEmail: 'alex@evshare.io',
      startTime: '2026-09-20T08:00:00Z',
      endTime: '2026-09-20T12:00:00Z',
      bufferedEndTime: '2026-09-20T12:30:00Z',
      status: 'CONFIRMED',
      estimatedCost: 600000,
      createdAt: '2026-09-16T12:00:00Z',
    };

    const postSpy = vi.spyOn(apiClient.raw, 'post').mockResolvedValueOnce({
      status: 200,
      statusText: 'OK',
      data: {
        success: true,
        message: 'Booking confirmed',
        data: mockCreatedBooking,
        timestamp: new Date().toISOString(),
      },
      headers: {},
      config: {} as any,
    });

    // 2. 3D Object registers with Interaction Pipeline (Mesh does NOT contain raw fetch logic)
    let actionInvoked = false;
    InteractionPipeline.register({
      id: '3d_booking_terminal_bay1',
      name: 'Chrono-Spatial Booking Terminal Bay 1',
      actionType: 'CONFIRM_RESERVATION',
      targetPosition: [0, 1.2, 3],
      requirements: {
        maxInteractionDistance: 5.0,
        requiredRoles: ['ROLE_CO_OWNER'],
      },
      onActivate: async () => {
        actionInvoked = true;
        // Interaction triggers domain API client via store or directly
        const booking = await bookingsApi.createBooking({
          vehicleId: 1,
          startTime: '2026-09-20T08:00:00Z',
          endTime: '2026-09-20T12:00:00Z',
          userId: 42,
        });

        // Store updates Zustand state
        useBookingStore.setState({
          bookingResult: {
            bookingId: booking.id,
            status: booking.status,
            vehicleModel: booking.vehicleModel,
            startTime: booking.startTime,
            endTime: booking.endTime,
            totalCostVnd: booking.estimatedCost,
            confirmedAt: booking.createdAt,
          },
          isSubmitting: false,
        });
      },
    });

    // 3. User approaches 3D object and clicks (Interaction Context)
    const context: InteractionContext = {
      targetId: '3d_booking_terminal_bay1',
      playerPosition: [0, 0, 2], // 2m away, within 5.0m
      targetPosition: [0, 1.2, 3],
      distanceToPlayer: 1.5,
      userRoles: ['ROLE_CO_OWNER'],
      isAuthenticated: true,
      userId: 42,
    };

    // 4. InteractionManager processes pipeline execution
    const outcome = await InteractionPipeline.execute('3d_booking_terminal_bay1', context);

    // Verify pipeline steps
    expect(outcome.success).toBe(true);
    expect(actionInvoked).toBe(true);

    // Verify API Client was called with Spring Boot REST contract
    expect(postSpy).toHaveBeenCalledTimes(1);
    const callArgs = postSpy.mock.calls[0];
    expect(callArgs[0]).toBe('/bookings');
    expect(callArgs[1]).toEqual({
      vehicleId: 1,
      startTime: '2026-09-20T08:00:00Z',
      endTime: '2026-09-20T12:00:00Z',
      userId: 42,
    });

    // Verify Zustand state was updated from backend response
    const bookingResult = useBookingStore.getState().bookingResult;
    expect(bookingResult).toBeDefined();
    expect(bookingResult?.bookingId).toBe(777);
    expect(bookingResult?.status).toBe('CONFIRMED');

    // Verify 3D visual interaction state updated to SELECTED
    expect(InteractionPipeline.getVisualState('3d_booking_terminal_bay1')).toBe('SELECTED');
  });

  it('rejects pipeline execution when proximity or RBAC requirements fail before API call', async () => {
    const postSpy = vi.spyOn(apiClient.raw, 'post');

    InteractionPipeline.register({
      id: 'restricted_vault_terminal',
      name: 'Vault Sovereign Terminal',
      actionType: 'DISBURSE_FUNDS',
      targetPosition: [10, 0, 10],
      requirements: {
        maxInteractionDistance: 3.0,
        requiredRoles: ['ROLE_ADMIN'],
      },
      onActivate: async () => {
        await apiClient.apiPost('/vault/disburse', { amount: 1000 });
      },
    });

    // Far distance (15m > 3m) & non-admin role
    const invalidContext: InteractionContext = {
      targetId: 'restricted_vault_terminal',
      playerPosition: [0, 0, 0],
      targetPosition: [10, 0, 10],
      distanceToPlayer: 14.14,
      userRoles: ['ROLE_CO_OWNER'],
      isAuthenticated: true,
      userId: 99,
    };

    const outcome = await InteractionPipeline.execute('restricted_vault_terminal', invalidContext);

    expect(outcome.success).toBe(false);
    expect(postSpy).not.toHaveBeenCalled();
    expect(InteractionPipeline.getVisualState('restricted_vault_terminal')).toBe('DISABLED');
  });
});
