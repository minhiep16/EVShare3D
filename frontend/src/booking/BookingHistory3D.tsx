import React, { useEffect, useState } from 'react';
import { Text } from '@react-three/drei';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_THEME } from './bookingLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import type { BookingResponseDTO } from '@/api/bookingsApi';
import { formatCurrencyVND, formatStatusVN } from '@/i18n';

export const BookingHistory3D: React.FC = () => {
  const userBookings = useBookingStore((s) => s.userBookings);
  const selectedBooking = useBookingStore((s) => s.selectedBooking);
  const activeBookingHistory = useBookingStore((s) => s.activeBookingHistory);
  const isLoadingHistory = useBookingStore((s) => s.isLoadingHistory);
  const isCancelling = useBookingStore((s) => s.isCancelling);
  const cancelResult = useBookingStore((s) => s.cancelResult);
  const submissionError = useBookingStore((s) => s.submissionError);

  const fetchMyBookings = useBookingStore((s) => s.fetchMyBookings);
  const fetchBookingHistory = useBookingStore((s) => s.fetchBookingHistory);
  const selectBookingForInspection = useBookingStore((s) => s.selectBookingForInspection);
  const cancelBooking = useBookingStore((s) => s.cancelBooking);
  const setActiveStep = useBookingStore((s) => s.setActiveStep);

  const [hoveredBookingId, setHoveredBookingId] = useState<number | null>(null);
  const [viewMode, setViewMode] = useState<'LIST' | 'AUDIT'>('LIST');

  const { CHRONO_CYAN, AVAILABLE_GREEN, CONFLICT_RED, OCCUPIED_AMBER, BUFFER_PURPLE } =
    BOOKING_CHAMBER_THEME;

  useEffect(() => {
    fetchMyBookings();
  }, [fetchMyBookings]);

  const handleSelectBooking = (booking: BookingResponseDTO) => {
    AudioEngine.play('UI_CLICK');
    selectBookingForInspection(booking);
    fetchBookingHistory(booking.id);
  };

  const handleCancel = async (e: any, bookingId: number) => {
    e.stopPropagation();
    AudioEngine.play('UI_CLICK');
    await cancelBooking(bookingId, 'Co-owner cancelled reservation via 3D chamber');
  };

  return (
    <group name="BookingHistory3D" position={[0, 0, 0]}>
      {/* 1. Header Bar */}
      <group position={[0, 1.25, 0]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.125}
          color={CHRONO_CYAN}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {viewMode === 'LIST' ? 'LỊCH SỬ ĐẶT XE CỦA TÔI' : 'SỔ CÁI KIỂM TOÁN LỊCH ĐẶT XE'}
        </Text>
        <Text
          position={[0, -0.16, 0]}
          fontSize={0.075}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          {viewMode === 'LIST'
            ? 'Danh sách lịch đặt đang hoạt động, sắp tới và lịch sử theo điều khoản BR-BKG-03'
            : `Nhật ký kiểm toán mật mã cho lịch đặt #BKG-${selectedBooking?.id || ''}`}
        </Text>

        {/* View Mode Toggle Button */}
        {selectedBooking && (
          <group
            position={[1.65, 0, 0.02]}
            onClick={(e) => {
              e.stopPropagation();
              AudioEngine.play('UI_CLICK');
              setViewMode(viewMode === 'LIST' ? 'AUDIT' : 'LIST');
            }}
          >
            <mesh>
              <boxGeometry args={[1.05, 0.2, 0.02]} />
              <meshStandardMaterial color="#1e293b" emissive={CHRONO_CYAN} emissiveIntensity={0.3} />
            </mesh>
            <Text position={[0, 0, 0.02]} fontSize={0.055} color="#ffffff" anchorX="center" anchorY="middle">
              {viewMode === 'LIST' ? 'XEM KIỂM TOÁN' : 'QUAY LẠI'}
            </Text>
          </group>
        )}
      </group>

      {/* 2. Cancellation / Error Feedback Banner */}
      {cancelResult && (
        <group position={[0, 0.95, 0.02]}>
          <Text
            fontSize={0.068}
            color={cancelResult.penaltyFeeApplied ? OCCUPIED_AMBER : AVAILABLE_GREEN}
            anchorX="center"
            anchorY="middle"
            maxWidth={3.8}
          >
            {`✓ ${cancelResult.message}`}
          </Text>
        </group>
      )}

      {submissionError && (
        <group position={[0, 0.95, 0.02]}>
          <Text fontSize={0.068} color={CONFLICT_RED} anchorX="center" anchorY="middle">
            {`⚠ ${submissionError}`}
          </Text>
        </group>
      )}

      {/* 3. VIEW MODE: LIST OF USER BOOKINGS */}
      {viewMode === 'LIST' && (
        <group position={[0, 0.45, 0.02]}>
          {userBookings.length === 0 ? (
            <group position={[0, -0.4, 0]}>
              <Text fontSize={0.09} color="#64748b" anchorX="center" anchorY="middle">
                Chưa có lịch đặt xe nào
              </Text>
            </group>
          ) : (
            userBookings.slice(0, 3).map((bkg, idx) => {
              const isSelected = selectedBooking?.id === bkg.id;
              const isHovered = hoveredBookingId === bkg.id;
              const yOffset = -idx * 0.48;

              const statusColor =
                bkg.status === 'CONFIRMED' || bkg.status === 'ACTIVE'
                  ? AVAILABLE_GREEN
                  : bkg.status === 'COMPLETED'
                  ? '#38bdf8'
                  : bkg.status === 'CANCELLED'
                  ? '#94a3b8'
                  : OCCUPIED_AMBER;

              const canCancel = bkg.status === 'CONFIRMED' || bkg.status === 'PENDING';

              return (
                <group
                  key={bkg.id}
                  position={[0, yOffset, 0]}
                  onClick={() => handleSelectBooking(bkg)}
                  onPointerOver={() => setHoveredBookingId(bkg.id)}
                  onPointerOut={() => setHoveredBookingId(null)}
                >
                  {/* Card Slab */}
                  <mesh position={[0, 0, -0.01]}>
                    <planeGeometry args={[4.0, 0.42]} />
                    <meshStandardMaterial
                      color={isSelected ? '#0c192e' : isHovered ? '#0f172a' : '#070b14'}
                      roughness={0.2}
                      metalness={0.8}
                      transparent
                      opacity={0.88}
                    />
                  </mesh>

                  {/* Left Status Bar */}
                  <mesh position={[-1.95, 0, 0]}>
                    <boxGeometry args={[0.04, 0.38, 0.02]} />
                    <meshStandardMaterial color={statusColor} emissive={statusColor} emissiveIntensity={0.6} />
                  </mesh>

                  {/* Booking ID & Vehicle Model */}
                  <Text
                    position={[-1.85, 0.1, 0]}
                    fontSize={0.075}
                    color="#ffffff"
                    anchorX="left"
                    anchorY="middle"
                    font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                  >
                    {`#BKG-${bkg.id} • ${bkg.vehicleModel || 'Xe Điện'}`}
                  </Text>

                  {/* Plate and Status Tag */}
                  <Text position={[0.7, 0.1, 0]} fontSize={0.065} color={statusColor} anchorX="right" anchorY="middle">
                    {`[ ${formatStatusVN(bkg.status)} ]`}
                  </Text>

                  {/* Time Window */}
                  <Text position={[-1.85, -0.08, 0]} fontSize={0.06} color="#cbd5e1" anchorX="left" anchorY="middle">
                    {`${bkg.startTime?.replace('T', ' ').replace('Z', '') || ''} ➔ ${bkg.endTime?.substring(11, 16) || ''} (${bkg.vehicleLicensePlate || ''})`}
                  </Text>

                  {/* Cost */}
                  <Text position={[0.7, -0.08, 0]} fontSize={0.065} color={AVAILABLE_GREEN} anchorX="right" anchorY="middle">
                    {formatCurrencyVND(Number(bkg.estimatedCost) || 0)}
                  </Text>

                  {/* Cancel Button (if active) */}
                  {canCancel && (
                    <group
                      position={[1.4, 0, 0.02]}
                      onClick={(e) => handleCancel(e, bkg.id)}
                    >
                      <mesh>
                        <boxGeometry args={[0.95, 0.24, 0.02]} />
                        <meshStandardMaterial
                          color="#7f1d1d"
                          emissive={CONFLICT_RED}
                          emissiveIntensity={0.5}
                        />
                      </mesh>
                      <Text
                        position={[0, 0, 0.02]}
                        fontSize={0.052}
                        color="#ffffff"
                        anchorX="center"
                        anchorY="middle"
                      >
                        {isCancelling && selectedBooking?.id === bkg.id ? 'ĐANG HỦY...' : 'HỦY LỊCH (BR-03)'}
                      </Text>
                    </group>
                  )}
                </group>
              );
            })
          )}
        </group>
      )}

      {/* 4. VIEW MODE: AUDIT LOG ENTRIES */}
      {viewMode === 'AUDIT' && (
        <group position={[0, 0.45, 0.02]}>
          {isLoadingHistory ? (
            <Text position={[0, -0.4, 0]} fontSize={0.08} color={CHRONO_CYAN} anchorX="center" anchorY="middle">
              ĐANG TRUY VẤN DỊCH VỤ KIỂM TOÁN HỆ THỐNG...
            </Text>
          ) : activeBookingHistory.length === 0 ? (
            <Text position={[0, -0.4, 0]} fontSize={0.08} color="#94a3b8" anchorX="center" anchorY="middle">
              Không có bản ghi kiểm toán nào cho lịch đặt này
            </Text>
          ) : (
            activeBookingHistory.slice(0, 4).map((hist, hIdx) => {
              const yOffset = -hIdx * 0.32;
              return (
                <group key={hist.id || hIdx} position={[0, yOffset, 0]}>
                  <mesh position={[0, 0, -0.01]}>
                    <planeGeometry args={[3.8, 0.28]} />
                    <meshStandardMaterial
                      color="#070b14"
                      roughness={0.2}
                      metalness={0.8}
                      transparent
                      opacity={0.88}
                    />
                  </mesh>

                  <mesh position={[-1.85, 0, 0]}>
                    <boxGeometry args={[0.04, 0.24, 0.02]} />
                    <meshStandardMaterial color={BUFFER_PURPLE} emissive={BUFFER_PURPLE} emissiveIntensity={0.5} />
                  </mesh>

                  <Text
                    position={[-1.75, 0.06, 0]}
                    fontSize={0.065}
                    color={CHRONO_CYAN}
                    anchorX="left"
                    anchorY="middle"
                  >
                    {hist.action?.replace(/_/g, ' ') || 'ACTION'}
                  </Text>

                  <Text
                    position={[1.75, 0.06, 0]}
                    fontSize={0.055}
                    color="#94a3b8"
                    anchorX="right"
                    anchorY="middle"
                  >
                    {hist.createdAt ? hist.createdAt.replace('T', ' ').substring(0, 19) : ''}
                  </Text>

                  <Text
                    position={[-1.75, -0.06, 0]}
                    fontSize={0.05}
                    color="#cbd5e1"
                    anchorX="left"
                    anchorY="middle"
                  >
                    {`NGƯỜI THỰC HIỆN: #${hist.actingUserId || 1} (${hist.actingUserName || 'Nguyễn Văn A'}) • ĐÃ LƯU ẢNH CHỤP TRẠNG THÁI`}
                  </Text>
                </group>
              );
            })
          )}
        </group>
      )}

      {/* 5. Footer Action: Back to Reservation Flow */}
      <group position={[0, -0.85, 0.02]}>
        <group
          position={[0, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            AudioEngine.play('UI_CLICK');
            setActiveStep('CALENDAR');
          }}
        >
          <mesh>
            <boxGeometry args={[1.8, 0.24, 0.03]} />
            <meshStandardMaterial color="#1e293b" emissive={CHRONO_CYAN} emissiveIntensity={0.2} />
          </mesh>
          <Text position={[0, 0, 0.02]} fontSize={0.075} color="#ffffff" anchorX="center" anchorY="middle">
            {'◄ TẠO LỊCH ĐẶT MỚI'}
          </Text>
        </group>
      </group>
    </group>
  );
};
