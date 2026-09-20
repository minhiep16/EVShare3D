import React, { useRef, useState, useMemo } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import type { VehicleDigitalTwin, DigitalTwinFacetTab } from './digitalTwinTypes';
import { useDigitalTwinStore } from './useDigitalTwinStore';
import { formatCurrencyVND, formatNumberVN, formatPercentageVN, formatStatusVN } from '@/i18n';

interface DigitalTwinVehicle3DProps {
  vehicle: VehicleDigitalTwin;
  onSelect?: (id: number) => void;
  onHover?: (id: number | null) => void;
}

export const DigitalTwinVehicle3D: React.FC<DigitalTwinVehicle3DProps> = ({
  vehicle,
  onSelect,
  onHover,
}) => {
  const groupRef = useRef<THREE.Group>(null);
  const holoRef = useRef<THREE.Group>(null);
  const [isHovered, setIsHovered] = useState(false);

  const selectedVehicleId = useDigitalTwinStore((s) => s.selectedVehicleId);
  const selectVehicle = useDigitalTwinStore((s) => s.selectVehicle);
  const activeFacetTab = useDigitalTwinStore((s) => s.activeFacetTab);
  const setActiveFacetTab = useDigitalTwinStore((s) => s.setActiveFacetTab);
  const syncFromBackend = useDigitalTwinStore((s) => s.syncFromBackend);
  const isSyncing = useDigitalTwinStore((s) => s.isSyncing);

  const isSelected = selectedVehicleId === vehicle.vehicleId;

  // 1. Status Colors
  const statusColor = useMemo(() => {
    switch (vehicle.status.status) {
      case 'AVAILABLE':
        return '#10b981';
      case 'CHARGING':
        return '#00e5ff';
      case 'IN_USE':
        return '#3b82f6';
      case 'RESERVED':
        return '#f59e0b';
      case 'MAINTENANCE':
        return '#ef4444';
      default:
        return '#94a3b8';
    }
  }, [vehicle.status.status]);

  // 2. Battery Bar Color
  const batteryColor = useMemo(() => {
    if (vehicle.battery.isCharging) return '#00e5ff';
    if (vehicle.battery.level > 50) return '#10b981';
    if (vehicle.battery.level > 20) return '#f59e0b';
    return '#ef4444';
  }, [vehicle.battery.level, vehicle.battery.isCharging]);

  // Subtle floating pulsation for holographic plate
  useFrame(({ clock }) => {
    if (holoRef.current) {
      const t = clock.getElapsedTime();
      holoRef.current.position.y =
        2.1 + Math.sin(t * 1.8 + vehicle.vehicleId) * 0.04;
    }
  });

  const FACET_TABS: Array<{ id: DigitalTwinFacetTab; label: string }> = [
    { id: 'BATTERY', label: '🔋 PIN' },
    { id: 'STATUS', label: '🏷 TRẠNG THÁI' },
    { id: 'OWNERSHIP', label: '👥 SỞ HỮU' },
    { id: 'BOOKING', label: '📅 ĐẶT LỊCH' },
    { id: 'USAGE', label: '🚗 VẬN HÀNH' },
    { id: 'MAINTENANCE', label: '🛠 BẢO TRÌ' },
    { id: 'FINANCE', label: '💳 TÀI CHÍNH' },
  ];

  return (
    <group
      ref={groupRef}
      name={`DigitalTwin_${vehicle.vehicleId}`}
      position={vehicle.status.position}
      rotation={vehicle.status.rotation}
      onClick={(e) => {
        e.stopPropagation();
        if (onSelect) {
          onSelect(vehicle.vehicleId);
        } else {
          selectVehicle(isSelected ? null : vehicle.vehicleId);
        }
      }}
      onPointerOver={(e) => {
        e.stopPropagation();
        setIsHovered(true);
        document.body.style.cursor = 'pointer';
        onHover?.(vehicle.vehicleId);
      }}
      onPointerOut={() => {
        setIsHovered(false);
        document.body.style.cursor = 'auto';
        onHover?.(null);
      }}
    >
      {/* ========================================================================= */}
      {/* 1. PROCEDURAL 3D VEHICLE BODY WITH DYNAMIC FACET SHADERS                 */}
      {/* ========================================================================= */}

      {/* Main Aerodynamic Lower Chassis */}
      <mesh position={[0, 0.35, 0]} castShadow receiveShadow>
        <boxGeometry args={[2.0, 0.45, 4.4]} />
        <meshStandardMaterial
          color={vehicle.bodyColor}
          metalness={0.85}
          roughness={0.2}
          emissive={isSelected ? vehicle.bodyColor : isHovered ? '#ffffff' : '#000000'}
          emissiveIntensity={isSelected ? 0.4 : isHovered ? 0.2 : 0}
        />
      </mesh>

      {/* Aerodynamic Front Hood Slope */}
      <mesh position={[0, 0.32, 1.8]} rotation={[-0.18, 0, 0]} castShadow>
        <boxGeometry args={[1.96, 0.35, 1.2]} />
        <meshStandardMaterial
          color={vehicle.bodyColor}
          metalness={0.9}
          roughness={0.18}
        />
      </mesh>

      {/* Cabin Roof / Tinted Glass Canopy */}
      <mesh position={[0, 0.78, -0.2]} castShadow>
        <boxGeometry args={[1.65, 0.48, 2.3]} />
        <meshStandardMaterial
          color="#0a1220"
          roughness={0.1}
          metalness={0.95}
          transparent
          opacity={0.85}
        />
      </mesh>

      {/* Front Windshield */}
      <mesh position={[0, 0.65, 0.98]} rotation={[-0.55, 0, 0]}>
        <planeGeometry args={[1.58, 0.65]} />
        <meshStandardMaterial
          color="#1e293b"
          roughness={0.05}
          metalness={0.95}
          transparent
          opacity={0.9}
        />
      </mesh>

      {/* 4 Performance Wheels & Brake Calipers */}
      {[
        [-1.02, 0.32, 1.3],
        [1.02, 0.32, 1.3],
        [-1.02, 0.32, -1.3],
        [1.02, 0.32, -1.3],
      ].map(([x, y, z], idx) => (
        <group key={idx} position={[x, y, z]}>
          <mesh rotation={[0, 0, Math.PI / 2]} castShadow>
            <cylinderGeometry args={[0.34, 0.34, 0.22, 24]} />
            <meshStandardMaterial color="#0f172a" roughness={0.7} metalness={0.5} />
          </mesh>
          <mesh rotation={[0, 0, Math.PI / 2]}>
            <cylinderGeometry args={[0.22, 0.22, 0.23, 16]} />
            <meshStandardMaterial
              color="#64748b"
              metalness={0.9}
              roughness={0.2}
            />
          </mesh>
        </group>
      ))}

      {/* Headlights (Cyber-Cyan / Amber if reserved) */}
      <mesh position={[-0.7, 0.38, 2.22]}>
        <boxGeometry args={[0.4, 0.08, 0.04]} />
        <meshStandardMaterial
          color={vehicle.status.status === 'RESERVED' ? '#fbbf24' : '#00e5ff'}
          emissive={
            vehicle.status.status === 'RESERVED' ? '#fbbf24' : '#00e5ff'
          }
          emissiveIntensity={2.0}
        />
      </mesh>
      <mesh position={[0.7, 0.38, 2.22]}>
        <boxGeometry args={[0.4, 0.08, 0.04]} />
        <meshStandardMaterial
          color={vehicle.status.status === 'RESERVED' ? '#fbbf24' : '#00e5ff'}
          emissive={
            vehicle.status.status === 'RESERVED' ? '#fbbf24' : '#00e5ff'
          }
          emissiveIntensity={2.0}
        />
      </mesh>

      {/* Tail Light Strip */}
      <mesh position={[0, 0.42, -2.21]}>
        <boxGeometry args={[1.8, 0.06, 0.04]} />
        <meshStandardMaterial
          color="#ff1744"
          emissive="#ff1744"
          emissiveIntensity={1.8}
        />
      </mesh>

      {/* Undercarriage Battery Glow (Battery Facet) */}
      <mesh position={[0, 0.08, 0]}>
        <boxGeometry args={[1.5, 0.05, 2.6]} />
        <meshStandardMaterial
          color={batteryColor}
          emissive={batteryColor}
          emissiveIntensity={vehicle.battery.isCharging ? 1.5 : 0.4}
        />
      </mesh>

      {/* Undercarriage Status Ground Halo Ring (Status Facet) */}
      <mesh position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[2.2, 2.6, 32]} />
        <meshBasicMaterial
          color={statusColor}
          transparent
          opacity={isSelected ? 0.75 : isHovered ? 0.45 : 0.25}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* ========================================================================= */}
      {/* 2. INTERACTIVE 3D HOLOGRAPHIC TELEMETRY DOSSIER (7-FACET INSPECTOR)      */}
      {/* ========================================================================= */}
      <group ref={holoRef} position={[0, 2.1, 0]}>
        {/* Holographic Backing Frame */}
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[3.2, 1.85]} />
          <meshStandardMaterial
            color="#040914"
            roughness={0.2}
            metalness={0.9}
            transparent
            opacity={0.94}
          />
        </mesh>
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[3.24, 1.89]} />
          <meshBasicMaterial color={statusColor} wireframe />
        </mesh>

        {/* Title & Identity Header */}
        <Text
          position={[-1.45, 0.74, 0.02]}
          fontSize={0.11}
          color="#ffffff"
          anchorX="left"
          anchorY="middle"
        >
          {`${vehicle.manufacturer.toUpperCase()} ${vehicle.modelName.toUpperCase()}`}
        </Text>
        <Text
          position={[1.45, 0.74, 0.02]}
          fontSize={0.09}
          color="#38bdf8"
          anchorX="right"
          anchorY="middle"
        >
          {`[${vehicle.status.stallLocationCode}] ${vehicle.licensePlate}`}
        </Text>

        {/* 7 FACET SELECTOR TABS IN 3D */}
        <group position={[0, 0.52, 0.02]}>
          {FACET_TABS.map((tab, idx) => {
            const isTabActive = activeFacetTab === tab.id;
            const xOffset = (idx - 3) * 0.44;
            return (
              <group
                key={tab.id}
                position={[xOffset, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveFacetTab(tab.id);
                }}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[0.42, 0.16]} />
                  <meshStandardMaterial
                    color={isTabActive ? '#0284c7' : '#0f172a'}
                    emissive={isTabActive ? '#38bdf8' : '#000000'}
                    emissiveIntensity={isTabActive ? 0.6 : 0}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.052}
                  color={isTabActive ? '#ffffff' : '#94a3b8'}
                  anchorX="center"
                  anchorY="middle"
                >
                  {tab.label}
                </Text>
              </group>
            );
          })}
        </group>

        {/* DYNAMIC CONTENT AREA BASED ON SELECTED FACET */}
        <group position={[-1.45, 0.18, 0.02]}>
          {/* FACET 1: BATTERY */}
          {activeFacetTab === 'BATTERY' && (
            <group>
              <Text fontSize={0.08} color="#00e5ff" anchorX="left" anchorY="middle">
                MỨC SẠC PIN (SoC): {vehicle.battery.level}%
              </Text>
              <Text
                position={[0, -0.15, 0]}
                fontSize={0.065}
                color="#cbd5e1"
                anchorX="left"
                anchorY="middle"
              >
                Tầm hoạt động ước tính: {vehicle.battery.estimatedRangeKm} km • Tuổi thọ pin:{' '}
                {vehicle.battery.healthPercentage}%
              </Text>
              <Text
                position={[0, -0.3, 0]}
                fontSize={0.065}
                color="#cbd5e1"
                anchorX="left"
                anchorY="middle"
              >
                Trạng thái:{' '}
                {vehicle.battery.isCharging
                  ? `⚡ ĐANG SẠC (${vehicle.battery.chargingPowerKw} kW)`
                  : 'ĐÃ NGẮT SẠC'}{' '}
                • Nhiệt độ lõi: {vehicle.battery.temperatureCelsius}°C
              </Text>
            </group>
          )}

          {/* FACET 2: STATUS */}
          {activeFacetTab === 'STATUS' && (
            <group>
              <Text fontSize={0.08} color={statusColor} anchorX="left" anchorY="middle">
                TRẠNG THÁI VÒNG ĐỜI: {formatStatusVN(vehicle.status.status)}
              </Text>
              <Text
                position={[0, -0.15, 0]}
                fontSize={0.065}
                color="#cbd5e1"
                anchorX="left"
                anchorY="middle"
              >
                Vị trí đỗ: {vehicle.status.stallLocationCode} • Khóa xe:{' '}
                {vehicle.status.isLocked ? '🔒 ĐÃ KHÓA AN TOÀN' : '🔓 ĐANG MỞ KHÓA'}
              </Text>
              <Text
                position={[0, -0.3, 0]}
                fontSize={0.06}
                color="#94a3b8"
                anchorX="left"
                anchorY="middle"
                maxWidth={2.8}
              >
                Nhật ký kiểm toán: "{vehicle.status.lastStatusChangeReason}"
              </Text>
            </group>
          )}

          {/* FACET 3: OWNERSHIP */}
          {activeFacetTab === 'OWNERSHIP' && (
            <group>
              <Text fontSize={0.08} color="#a855f7" anchorX="left" anchorY="middle">
                NHÓM ĐỒNG SỞ HỮU: {vehicle.ownership.groupName}
              </Text>
              <Text
                position={[0, -0.15, 0]}
                fontSize={0.065}
                color="#cbd5e1"
                anchorX="left"
                anchorY="middle"
              >
                Cổ phần sở hữu: {formatPercentageVN(vehicle.ownership.userSharePercentage)} (Quyền biểu quyết:{' '}
                {formatPercentageVN(vehicle.ownership.userVotingPower)})
              </Text>
              <Text
                position={[0, -0.3, 0]}
                fontSize={0.06}
                color="#94a3b8"
                anchorX="left"
                anchorY="middle"
              >
                Thành viên: {vehicle.ownership.memberCount} • Mã băm Cap Table:{' '}
                {vehicle.ownership.capTableHash}
              </Text>
            </group>
          )}

          {/* FACET 4: BOOKING */}
          {activeFacetTab === 'BOOKING' && (
            <group>
              <Text fontSize={0.08} color="#f59e0b" anchorX="left" anchorY="middle">
                LỊCH SỬ DỤNG: {vehicle.booking.activeBookingId ? `#${vehicle.booking.activeBookingId}` : 'CHƯA CÓ LỊCH TRÌNH'}
              </Text>
              <Text
                position={[0, -0.15, 0]}
                fontSize={0.065}
                color="#cbd5e1"
                anchorX="left"
                anchorY="middle"
              >
                Người đặt: {vehicle.booking.reservedByUserName || 'Không có'} (Khung giờ:{' '}
                {vehicle.booking.startTime ? vehicle.booking.startTime.slice(11, 16) : '--:--'} -{' '}
                {vehicle.booking.endTime ? vehicle.booking.endTime.slice(11, 16) : '--:--'})
              </Text>
              <Text
                position={[0, -0.3, 0]}
                fontSize={0.06}
                color="#94a3b8"
                anchorX="left"
                anchorY="middle"
              >
                Mục đích: "{vehicle.booking.purpose}" • Trùng lịch:{' '}
                {vehicle.booking.conflictStatus === 'NONE' ? 'Không' : vehicle.booking.conflictStatus}
              </Text>
            </group>
          )}

          {/* FACET 5: USAGE */}
          {activeFacetTab === 'USAGE' && (
            <group>
              <Text fontSize={0.08} color="#38bdf8" anchorX="left" anchorY="middle">
                THÔNG SỐ VẬN HÀNH: ĐÃ ĐI {formatNumberVN(vehicle.usage.odometerKm)} KM
              </Text>
              <Text
                position={[0, -0.15, 0]}
                fontSize={0.065}
                color="#cbd5e1"
                anchorX="left"
                anchorY="middle"
              >
                Phiên xe:{' '}
                {vehicle.usage.activeSessionId
                  ? `Phiên #${vehicle.usage.activeSessionId} (Tài xế: ${vehicle.usage.driverUserName})`
                  : 'Xe đang đỗ tại trạm (Nghỉ)'}
              </Text>
              <Text
                position={[0, -0.3, 0]}
                fontSize={0.06}
                color="#94a3b8"
                anchorX="left"
                anchorY="middle"
              >
                Tốc độ: {vehicle.usage.currentSpeedKmh} km/h • Báo cáo hư hại:{' '}
                {vehicle.usage.checkOutDamageReported ? 'CÓ' : 'KHÔNG'}
              </Text>
            </group>
          )}

          {/* FACET 6: MAINTENANCE */}
          {activeFacetTab === 'MAINTENANCE' && (
            <group>
              <Text fontSize={0.08} color="#10b981" anchorX="left" anchorY="middle">
                TÌNH TRẠNG BẢO TRÌ: {formatStatusVN(vehicle.maintenance.serviceStatus)} (Chỉ số: {vehicle.maintenance.overallHealthScore}%)
              </Text>
              <Text
                position={[0, -0.15, 0]}
                fontSize={0.062}
                color="#cbd5e1"
                anchorX="left"
                anchorY="middle"
              >
                Phân hệ: 5/5 Đạt chuẩn (Phanh gốm, Trợ lái ADAS, Pin cao áp, Giảm chấn, Biến tần)
              </Text>
              <Text
                position={[0, -0.3, 0]}
                fontSize={0.06}
                color="#94a3b8"
                anchorX="left"
                anchorY="middle"
              >
                Mã lỗi chẩn đoán DTC:{' '}
                {vehicle.maintenance.activeDtcCodes.length === 0
                  ? 'Không phát hiện lỗi (0 lỗi)'
                  : vehicle.maintenance.activeDtcCodes.join(', ')}
              </Text>
            </group>
          )}

          {/* FACET 7: FINANCE */}
          {activeFacetTab === 'FINANCE' && (
            <group>
              <Text fontSize={0.08} color="#fbbf24" anchorX="left" anchorY="middle">
                TÌNH TRẠNG TÀI CHÍNH: QUỸ {formatCurrencyVND(vehicle.finance.vaultBalanceVnd)}
              </Text>
              <Text
                position={[0, -0.15, 0]}
                fontSize={0.065}
                color="#cbd5e1"
                anchorX="left"
                anchorY="middle"
              >
                Nợ chi phí lũy kế:{' '}
                {formatCurrencyVND(vehicle.finance.accruedExpenseLiabilityVnd)} • Chi phí/km:{' '}
                {formatCurrencyVND(vehicle.finance.costPerKm)}
              </Text>
              <Text
                position={[0, -0.3, 0]}
                fontSize={0.06}
                color="#94a3b8"
                anchorX="left"
                anchorY="middle"
              >
                Tiền cọc: {formatCurrencyVND(vehicle.finance.userDepositVnd)} ({vehicle.finance.depositStatus === 'HELD' ? 'Đang tạm giữ' : vehicle.finance.depositStatus}) • Mã ref:{' '}
                {vehicle.finance.lastFundDeductionRef}
              </Text>
            </group>
          )}
        </group>

        {/* 3D ACTION BUTTON: TRIGGER PIPELINE RE-SYNC FROM BACKEND */}
        <group
          position={[0, -0.58, 0.02]}
          onClick={(e) => {
            e.stopPropagation();
            if (!isSyncing) syncFromBackend(vehicle.vehicleId);
          }}
        >
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[2.8, 0.22]} />
            <meshStandardMaterial
              color="#0f2b48"
              emissive="#0284c7"
              emissiveIntensity={0.5}
            />
          </mesh>
          <Text
            position={[0, 0, 0.01]}
            fontSize={0.065}
            color="#38bdf8"
            anchorX="center"
            anchorY="middle"
          >
            {isSyncing
              ? '⏳ ĐANG ĐỒNG BỘ TỪ MÁY CHỦ...'
              : '[ 🔄 ĐỒNG BỘ LẠI BẢN SAO SỐ TỪ MÁY CHỦ ]'}
          </Text>
        </group>
      </group>
    </group>
  );
};
