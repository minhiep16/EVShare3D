import React from 'react';
import { Text } from '@react-three/drei';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_THEME } from './bookingLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import { useWorldEnvironmentStore } from '@/world/useWorldEnvironmentStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { getSectorSpawnPoint } from '@/world/spawnPoints';
import { formatDateTimeVN, formatStatusVN } from '@/i18n';

export const BookingResult3D: React.FC = () => {
  const bookingResult = useBookingStore((s) => s.bookingResult);
  const resetBooking = useBookingStore((s) => s.resetBooking);
  const setActiveSector = useWorldEnvironmentStore((s) => s.setActiveSector);
  const teleportTo = usePlayerStore((s) => s.teleportTo);

  const { CHRONO_CYAN, AVAILABLE_GREEN, BUFFER_PURPLE } = BOOKING_CHAMBER_THEME;

  if (!bookingResult) return null;

  return (
    <group name="BookingResult3D" position={[0, 0, 0]}>
      {/* 1. Header Banner */}
      <group position={[0, 1.25, 0]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.14}
          color={AVAILABLE_GREEN}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          ĐẶT LỊCH THÀNH CÔNG
        </Text>
        <Text
          position={[0, -0.16, 0]}
          fontSize={0.075}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          {`HỢP ĐỒNG ĐÃ ĐƯỢC GHI NHẬN VÀO HỆ THỐNG • MÃ ĐẶT: #BKG-${bookingResult.bookingId}`}
        </Text>
      </group>

      {/* 2. Hologram Certificate Details */}
      <group position={[-1.7, 0.72, 0.02]}>
        <Text position={[0, 0, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          MÃ ĐẶT LỊCH:
        </Text>
        <Text
          position={[1.5, 0, 0]}
          fontSize={0.09}
          color={CHRONO_CYAN}
          anchorX="left"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {`#BKG-${bookingResult.bookingId}`}
        </Text>

        <Text position={[0, -0.18, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          XE ĐƯỢC CẤP:
        </Text>
        <Text position={[1.5, -0.18, 0]} fontSize={0.085} color="#ffffff" anchorX="left" anchorY="middle">
          {`${bookingResult.vehicleModel} (${bookingResult.vehicleLicensePlate})`}
        </Text>

        <Text position={[0, -0.36, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          THỜI GIAN BẮT ĐẦU:
        </Text>
        <Text position={[1.5, -0.36, 0]} fontSize={0.08} color="#e2e8f0" anchorX="left" anchorY="middle">
          {formatDateTimeVN(bookingResult.startTime)}
        </Text>

        <Text position={[0, -0.54, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          THỜI GIAN KẾT THÚC:
        </Text>
        <Text position={[1.5, -0.54, 0]} fontSize={0.08} color="#e2e8f0" anchorX="left" anchorY="middle">
          {formatDateTimeVN(bookingResult.endTime)}
        </Text>

        <Text position={[0, -0.72, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          KHOẢNG ĐỆM BẢO VỆ:
        </Text>
        <Text position={[1.5, -0.72, 0]} fontSize={0.075} color={BUFFER_PURPLE} anchorX="left" anchorY="middle">
          {`Khóa đến ${formatDateTimeVN(bookingResult.bufferedEndTime)}`}
        </Text>

        <Text position={[0, -0.9, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          TRẠNG THÁI:
        </Text>
        <Text position={[1.5, -0.9, 0]} fontSize={0.085} color={AVAILABLE_GREEN} anchorX="left" anchorY="middle">
          {formatStatusVN(bookingResult.status)}
        </Text>
      </group>

      {/* 3. QR Check-in Protocol Notice */}
      <group position={[0, -0.36, 0.02]}>
        <Text
          fontSize={0.07}
          color="#38bdf8"
          anchorX="center"
          anchorY="middle"
          maxWidth={3.2}
          lineHeight={1.3}
        >
          ℹ THÔNG BÁO QUÉT MÃ QR: Cổng nhận xe mở trước 15 phút tại Trạm QR Trung Tâm Vận Hành (BR-BKG-04).
        </Text>
      </group>

      {/* 4. Navigation Actions */}
      <group position={[0, -0.65, 0.02]}>
        {/* Button 1: Create Another Booking */}
        <group
          position={[-1.2, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            AudioEngine.play('UI_CLICK');
            resetBooking();
          }}
        >
          <mesh>
            <boxGeometry args={[1.6, 0.24, 0.03]} />
            <meshStandardMaterial color="#1e293b" />
          </mesh>
          <Text position={[0, 0, 0.02]} fontSize={0.075} color="#cbd5e1" anchorX="center" anchorY="middle">
            {'ĐẶT CHUYẾN KHÁC'}
          </Text>
        </group>

        {/* Button 2: Return to Central Garage */}
        <group
          position={[1.2, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            AudioEngine.play('UI_CLICK');
            resetBooking();
            setActiveSector('CENTRAL_GARAGE');
            const spawn = getSectorSpawnPoint('CENTRAL_GARAGE');
            if (spawn) {
              teleportTo(spawn.position, spawn.rotation);
            }
          }}
        >
          <mesh>
            <boxGeometry args={[1.8, 0.24, 0.04]} />
            <meshStandardMaterial
              color="#0284c7"
              emissive={CHRONO_CYAN}
              emissiveIntensity={0.5}
            />
          </mesh>
          <Text
            position={[0, 0, 0.03]}
            fontSize={0.075}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {'VỀ GARAGE TRUNG TÂM ►'}
          </Text>
        </group>
      </group>
    </group>
  );
};
