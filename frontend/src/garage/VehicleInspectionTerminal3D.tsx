import React, { useMemo } from 'react';
import { Text } from '@react-three/drei';
import { useGarageStore } from './useGarageStore';
import { formatNumberVN, formatStatusVN } from '@/i18n';

export const VehicleInspectionTerminal3D: React.FC = () => {
  const inspectedVehicle = useGarageStore((s) => s.inspectedVehicle);
  const selectVehicle = useGarageStore((s) => s.selectVehicle);
  const toggleVehicleLock = useGarageStore((s) => s.toggleVehicleLock);
  const toggleCharging = useGarageStore((s) => s.toggleCharging);
  const transitionVehicleStatus = useGarageStore((s) => s.transitionVehicleStatus);
  const fetchVehicleTelemetry = useGarageStore((s) => s.fetchVehicleTelemetry);
  const getPermittedActions = useGarageStore((s) => s.getPermittedActions);
  const isActionExecuting = useGarageStore((s) => s.isActionExecuting);
  const actionNotice = useGarageStore((s) => s.actionNotice);
  const actionError = useGarageStore((s) => s.actionError);

  const permittedActions = useMemo(() => {
    if (!inspectedVehicle) return [];
    return getPermittedActions(inspectedVehicle.status);
  }, [inspectedVehicle, getPermittedActions]);

  if (!inspectedVehicle) return null;

  const [vx, vy, vz] = inspectedVehicle.position;
  // Position the inspection terminal offset from vehicle
  const terminalPos: [number, number, number] = [vx + 2.3, vy + 1.25, vz + 0.6];

  const estimatedRange = Math.round(inspectedVehicle.batteryLevel * 4.6);
  const isCharging = inspectedVehicle.status === 'CHARGING' || inspectedVehicle.isCharging;
  const isMaintenance = inspectedVehicle.status === 'MAINTENANCE';

  // Battery bar color logic
  const batteryBarColor = isCharging
    ? '#00e5ff'
    : inspectedVehicle.batteryLevel > 50
    ? '#10b981'
    : inspectedVehicle.batteryLevel > 20
    ? '#f59e0b'
    : '#ef4444';

  // Status color logic
  const statusColor =
    inspectedVehicle.status === 'AVAILABLE'
      ? '#10b981'
      : inspectedVehicle.status === 'CHARGING'
      ? '#00e5ff'
      : inspectedVehicle.status === 'MAINTENANCE'
      ? '#ef4444'
      : inspectedVehicle.status === 'IN_USE'
      ? '#38bdf8'
      : '#f59e0b';

  return (
    <group position={terminalPos} rotation={[0, -Math.PI / 4, 0]}>
      {/* 1. Terminal Holographic Frame */}
      <mesh position={[0, 0, 0]}>
        <boxGeometry args={[2.7, 2.5, 0.08]} />
        <meshStandardMaterial
          color="#030712"
          roughness={0.2}
          metalness={0.9}
          transparent
          opacity={0.95}
        />
      </mesh>

      {/* Frame Border Accent */}
      <mesh position={[0, 0, 0.045]}>
        <planeGeometry args={[2.66, 2.46]} />
        <meshStandardMaterial
          color="#00e5ff"
          emissive="#00e5ff"
          emissiveIntensity={0.6}
          wireframe
        />
      </mesh>

      {/* 2. Header & Live Backend Indicator */}
      <group position={[-1.2, 1.02, 0.05]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.11}
          color="#00e5ff"
          anchorX="left"
          anchorY="middle"
        >
          BÀN KIỂM TRA THÔNG SỐ VÀ TRẠNG THÁI XE
        </Text>
        <Text
          position={[0, -0.15, 0]}
          fontSize={0.075}
          color="#94a3b8"
          anchorX="left"
          anchorY="middle"
        >
          {`${inspectedVehicle.manufacturer} ${inspectedVehicle.modelName} • ${inspectedVehicle.licensePlate}`}
        </Text>
      </group>

      {/* 3. Live Metrics Grid */}
      <group position={[-1.2, 0.62, 0.05]}>
        {/* Battery Metric & Visual Bar */}
        <Text position={[0, 0, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          DUNG LƯỢNG PIN:
        </Text>
        <Text position={[1.05, 0, 0]} fontSize={0.085} color={batteryBarColor} anchorX="left" anchorY="middle">
          {`${inspectedVehicle.batteryLevel}% (Tầm hoạt động ~${estimatedRange} km)`}
        </Text>

        {/* 3D Visual Battery Bar Container */}
        <mesh position={[1.85, 0, 0]}>
          <planeGeometry args={[0.65, 0.09]} />
          <meshBasicMaterial color="#1e293b" />
        </mesh>
        {/* 3D Visual Battery Fill Bar */}
        <mesh
          position={[
            1.85 - 0.65 / 2 + (0.65 * (inspectedVehicle.batteryLevel / 100)) / 2,
            0,
            0.005,
          ]}
        >
          <planeGeometry args={[0.65 * (inspectedVehicle.batteryLevel / 100), 0.075]} />
          <meshBasicMaterial color={batteryBarColor} />
        </mesh>

        {/* Lifecycle Status */}
        <Text position={[0, -0.16, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          TRẠNG THÁI XE:
        </Text>
        <Text position={[1.05, -0.16, 0]} fontSize={0.085} color={statusColor} anchorX="left" anchorY="middle">
          {`[ ${formatStatusVN(inspectedVehicle.status)} ]`}
        </Text>

        {/* Stall Location */}
        <Text position={[0, -0.32, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          VỊ TRÍ ĐỖ XE:
        </Text>
        <Text position={[1.05, -0.32, 0]} fontSize={0.085} color="#f8fafc" anchorX="left" anchorY="middle">
          {inspectedVehicle.stallLocationCode}
        </Text>

        {/* VIN & Odometer */}
        <Text position={[0, -0.48, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          KM ĐÃ ĐI / SỐ VIN:
        </Text>
        <Text position={[1.05, -0.48, 0]} fontSize={0.075} color="#94a3b8" anchorX="left" anchorY="middle">
          {`${formatNumberVN(inspectedVehicle.odometerKm)} km • ${inspectedVehicle.vin}`}
        </Text>

        {/* Supercharger Dock Status */}
        <Text position={[0, -0.64, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          TRẠM SẠC NHANH:
        </Text>
        <Text
          position={[1.05, -0.64, 0]}
          fontSize={0.08}
          color={isCharging ? '#00e5ff' : '#64748b'}
          anchorX="left"
          anchorY="middle"
        >
          {isCharging
            ? `⚡ Đang sạc (${inspectedVehicle.chargingPowerKw || 150} kW)`
            : 'Đã ngắt kết nối (Chờ)'}
        </Text>

        {/* Permitted State Machine Transitions */}
        <Text position={[0, -0.8, 0]} fontSize={0.07} color="#64748b" anchorX="left" anchorY="middle">
          HÀNH ĐỘNG HỢP LỆ:
        </Text>
        <Text position={[1.05, -0.8, 0]} fontSize={0.07} color="#a855f7" anchorX="left" anchorY="middle">
          {permittedActions.length > 0 ? permittedActions.map(a => formatStatusVN(a)).join(' • ') : 'KHÔNG CÓ'}
        </Text>
      </group>

      {/* 4. Execution Notice / Pending / Error Bar */}
      <group position={[0, -0.38, 0.05]}>
        {isActionExecuting ? (
          <Text fontSize={0.075} color="#38bdf8" anchorX="center" anchorY="middle">
            {'⏳ ĐANG TRUYỀN DỮ LIỆU ĐẾN MÁY CHỦ...'}
          </Text>
        ) : actionError ? (
          <Text fontSize={0.065} color="#ef4444" anchorX="center" anchorY="middle" maxWidth={2.4}>
            {`⚠️ ${actionError}`}
          </Text>
        ) : actionNotice ? (
          <Text fontSize={0.065} color="#10b981" anchorX="center" anchorY="middle" maxWidth={2.4}>
            {`✓ ${actionNotice}`}
          </Text>
        ) : (
          <Text fontSize={0.06} color="#475569" anchorX="center" anchorY="middle">
            Dữ liệu đồng bộ máy chủ thời gian thực
          </Text>
        )}
      </group>

      {/* 5. Interactive Permitted Action Buttons */}
      {/* Row 1 Actions: Lock, Charging, Maintenance */}
      <group position={[0, -0.62, 0.06]}>
        {/* Button 1: Toggle Lock */}
        <group
          position={[-0.85, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            if (!isActionExecuting) toggleVehicleLock(inspectedVehicle.id);
          }}
        >
          <mesh>
            <boxGeometry args={[0.78, 0.2, 0.04]} />
            <meshStandardMaterial
              color={inspectedVehicle.isLocked ? '#1e293b' : '#047857'}
              emissive={inspectedVehicle.isLocked ? '#475569' : '#10b981'}
              emissiveIntensity={0.6}
            />
          </mesh>
          <Text position={[0, 0, 0.03]} fontSize={0.065} color="#f8fafc" anchorX="center" anchorY="middle">
            {inspectedVehicle.isLocked ? '🔒 MỞ KHÓA' : '🔓 KHÓA XE'}
          </Text>
        </group>

        {/* Button 2: Toggle Charging */}
        <group
          position={[0, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            if (!isActionExecuting) toggleCharging(inspectedVehicle.id);
          }}
        >
          <mesh>
            <boxGeometry args={[0.78, 0.2, 0.04]} />
            <meshStandardMaterial
              color={isCharging ? '#0284c7' : '#1e293b'}
              emissive={isCharging ? '#00e5ff' : '#475569'}
              emissiveIntensity={0.6}
            />
          </mesh>
          <Text position={[0, 0, 0.03]} fontSize={0.065} color="#f8fafc" anchorX="center" anchorY="middle">
            {isCharging ? '⚡ NGẮT SẠC' : '⚡ BẬT SẠC'}
          </Text>
        </group>

        {/* Button 3: Toggle Maintenance */}
        <group
          position={[0.85, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            if (!isActionExecuting) {
              const targetStatus = isMaintenance ? 'AVAILABLE' : 'MAINTENANCE';
              transitionVehicleStatus(
                inspectedVehicle.id,
                targetStatus,
                isMaintenance
                  ? 'Bảo trì hoàn tất, xe sẵn sàng'
                  : 'Chuyển sang xưởng bảo trì chẩn đoán'
              );
            }
          }}
        >
          <mesh>
            <boxGeometry args={[0.78, 0.2, 0.04]} />
            <meshStandardMaterial
              color={isMaintenance ? '#991b1b' : '#1e293b'}
              emissive={isMaintenance ? '#ef4444' : '#475569'}
              emissiveIntensity={0.6}
            />
          </mesh>
          <Text position={[0, 0, 0.03]} fontSize={0.062} color="#f8fafc" anchorX="center" anchorY="middle">
            {isMaintenance ? '🛠 SẴN SÀNG' : '🛠 BẢO TRÌ'}
          </Text>
        </group>
      </group>

      {/* Row 2 Actions: Re-Sync Telemetry, Book Trip, Close */}
      <group position={[0, -0.88, 0.06]}>
        {/* Button 4: Live Telemetry Refresh */}
        <group
          position={[-0.85, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            if (!isActionExecuting) fetchVehicleTelemetry(inspectedVehicle.id);
          }}
        >
          <mesh>
            <boxGeometry args={[0.78, 0.2, 0.04]} />
            <meshStandardMaterial
              color="#0f172a"
              emissive="#38bdf8"
              emissiveIntensity={0.4}
            />
          </mesh>
          <Text position={[0, 0, 0.03]} fontSize={0.062} color="#38bdf8" anchorX="center" anchorY="middle">
            {'🔄 ĐỒNG BỘ'}
          </Text>
        </group>

        {/* Button 5: 3D Booking Chamber Entry */}
        <group
          position={[0, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            import('@/booking/useBookingStore').then(({ useBookingStore }) => {
              useBookingStore.getState().selectVehicle(inspectedVehicle.id);
              useBookingStore.getState().setActiveStep('CALENDAR');
              useBookingStore.getState().setCameraPreset('TERMINAL_FOCUS');
            });
            import('@/world/useNavigationStore').then(({ useNavigationStore }) => {
              useNavigationStore.getState().teleportToSector('BOOKING_CHAMBER');
            });
            selectVehicle(null);
          }}
        >
          <mesh>
            <boxGeometry args={[0.78, 0.2, 0.04]} />
            <meshStandardMaterial
              color="#0284c7"
              emissive="#00e5ff"
              emissiveIntensity={0.7}
            />
          </mesh>
          <Text position={[0, 0, 0.03]} fontSize={0.065} color="#ffffff" anchorX="center" anchorY="middle">
            {'📅 ĐẶT LỊCH XE'}
          </Text>
        </group>

        {/* Button 6: Close / Dismiss Terminal */}
        <group
          position={[0.85, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            selectVehicle(null);
          }}
        >
          <mesh>
            <boxGeometry args={[0.78, 0.2, 0.04]} />
            <meshStandardMaterial
              color="#334155"
              roughness={0.5}
              metalness={0.6}
            />
          </mesh>
          <Text position={[0, 0, 0.03]} fontSize={0.065} color="#94a3b8" anchorX="center" anchorY="middle">
            {'✕ ĐÓNG'}
          </Text>
        </group>
      </group>
    </group>
  );
};
