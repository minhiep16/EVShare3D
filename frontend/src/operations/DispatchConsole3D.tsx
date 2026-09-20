import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useOperationsStore } from './useOperationsStore';
import { OPERATIONS_STATIONS, OPERATIONS_THEME } from './operationsLayout';

export const DispatchConsole3D: React.FC = () => {
  const {
    checkInOutMode,
    setCheckInOutMode,
    fleet,
    selectedVehicleId,
    selectVehicle,
    inputOdometer,
    setInputOdometer,
    inputBattery,
    setInputBattery,
    hasDamageReported,
    setHasDamageReported,
    executeCheckIn,
    executeCheckOut,
    isSubmitting,
    operationMessage,
  } = useOperationsStore();

  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  const selectedVehicle = fleet.find((v) => v.vehicleId === selectedVehicleId) || fleet[0];

  return (
    <group
      name="DispatchConsole"
      position={OPERATIONS_STATIONS.DISPATCH_CONSOLE.relativePosition}
    >
      {/* 1. Heavy Console Base */}
      <mesh position={[0, 0.45, 0]} castShadow>
        <boxGeometry args={[3.2, 0.9, 1.2]} />
        <meshStandardMaterial
          color="#0b0f19"
          metalness={0.7}
          roughness={0.3}
        />
      </mesh>

      {/* Base Hazard Trim */}
      <mesh position={[0, 0.05, 0]}>
        <boxGeometry args={[3.3, 0.1, 1.3]} />
        <meshStandardMaterial
          color={OPERATIONS_THEME.hazardYellow}
          emissive={OPERATIONS_THEME.hazardYellow}
          emissiveIntensity={0.3}
        />
      </mesh>

      {/* 2. Angled Master Desk Surface (Tilted -22 deg) */}
      <group position={[0, 0.95, 0.1]} rotation={[-Math.PI * 0.12, 0, 0]}>
        {/* Desk top panel */}
        <mesh position={[0, 0, 0]}>
          <boxGeometry args={[3.4, 1.6, 0.08]} />
          <meshStandardMaterial
            color="#030712"
            metalness={0.8}
            roughness={0.2}
          />
        </mesh>
        <lineSegments position={[0, 0, 0.045]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(3.4, 1.6)]} />
          <lineBasicMaterial color={OPERATIONS_THEME.primary} />
        </lineSegments>

        {/* Header Title Bar */}
        <Text
          position={[0, 0.65, 0.05]}
          fontSize={0.105}
          color={OPERATIONS_THEME.primary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          BÀN ĐIỀU PHỐI &amp; VẬN HÀNH TRUNG TÂM
        </Text>

        {/* 3. Mode Toggle (Check-In vs Check-Out) */}
        <group position={[0, 0.44, 0.05]}>
          {/* Check-In Tab */}
          <group
            position={[-0.85, 0, 0]}
            onClick={(e) => {
              e.stopPropagation();
              setCheckInOutMode('CHECK_IN');
            }}
            onPointerOver={(e) => {
              e.stopPropagation();
              setHoveredBtn('MODE_IN');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh>
              <planeGeometry args={[1.5, 0.22]} />
              <meshStandardMaterial
                color={
                  checkInOutMode === 'CHECK_IN'
                    ? '#ea580c'
                    : hoveredBtn === 'MODE_IN'
                    ? '#1e293b'
                    : '#0f172a'
                }
                roughness={0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.01]}
              fontSize={0.07}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ NHẬN XE / KHỞI HÀNH ]
            </Text>
          </group>

          {/* Check-Out Tab */}
          <group
            position={[0.85, 0, 0]}
            onClick={(e) => {
              e.stopPropagation();
              setCheckInOutMode('CHECK_OUT');
            }}
            onPointerOver={(e) => {
              e.stopPropagation();
              setHoveredBtn('MODE_OUT');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh>
              <planeGeometry args={[1.5, 0.22]} />
              <meshStandardMaterial
                color={
                  checkInOutMode === 'CHECK_OUT'
                    ? '#0284c7'
                    : hoveredBtn === 'MODE_OUT'
                    ? '#1e293b'
                    : '#0f172a'
                }
                roughness={0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.01]}
              fontSize={0.07}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ TRẢ XE / QUYẾT TOÁN ]
            </Text>
          </group>
        </group>

        {/* 4. Fleet Vehicle Selection Bar */}
        <group position={[0, 0.18, 0.05]}>
          <Text
            position={[-1.2, 0, 0]}
            fontSize={0.07}
            color="#94a3b8"
            anchorX="right"
            anchorY="middle"
          >
            XE ĐIỆN:
          </Text>
          {fleet.map((v, i) => {
            const isSel = v.vehicleId === selectedVehicleId;
            const x = -0.7 + i * 0.98;
            return (
              <group
                key={v.vehicleId}
                position={[x, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  selectVehicle(v.vehicleId);
                }}
                onPointerOver={(e) => {
                  e.stopPropagation();
                  setHoveredBtn(`VEH_${v.vehicleId}`);
                  document.body.style.cursor = 'pointer';
                }}
                onPointerOut={() => {
                  setHoveredBtn(null);
                  document.body.style.cursor = 'auto';
                }}
              >
                <mesh>
                  <planeGeometry args={[0.9, 0.2]} />
                  <meshStandardMaterial
                    color={
                      isSel
                        ? '#ca8a04'
                        : hoveredBtn === `VEH_${v.vehicleId}`
                        ? '#334155'
                        : '#1e293b'
                    }
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.065}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                >
                  {v.modelName.split(' ')[1]} ({v.currentBay.slice(-2)})
                </Text>
              </group>
            );
          })}
        </group>

        {/* 5. Physical Log Inputs: Odometer, Battery SoC, Damage Flag */}
        <group position={[0, -0.1, 0.05]}>
          {/* Odometer Input Group */}
          <group position={[-0.95, 0, 0]}>
            <Text
              position={[0, 0.1, 0]}
              fontSize={0.06}
              color="#94a3b8"
              anchorX="center"
              anchorY="middle"
            >
              CÔNG TƠ MÉT (KM)
            </Text>
            {/* Decrease button */}
            <group
              position={[-0.45, -0.05, 0]}
              onClick={(e) => {
                e.stopPropagation();
                setInputOdometer(inputOdometer - 100);
              }}
            >
              <mesh>
                <boxGeometry args={[0.18, 0.18, 0.02]} />
                <meshStandardMaterial color="#334155" />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.08} color="#ffffff" anchorX="center" anchorY="middle">
                -
              </Text>
            </group>
            {/* Odometer display */}
            <mesh position={[0, -0.05, 0]}>
              <planeGeometry args={[0.62, 0.18]} />
              <meshStandardMaterial color="#020617" />
            </mesh>
            <Text position={[0, -0.05, 0.02]} fontSize={0.075} color="#fbbf24" anchorX="center" anchorY="middle">
              {inputOdometer}
            </Text>
            {/* Increase button */}
            <group
              position={[0.45, -0.05, 0]}
              onClick={(e) => {
                e.stopPropagation();
                setInputOdometer(inputOdometer + 100);
              }}
            >
              <mesh>
                <boxGeometry args={[0.18, 0.18, 0.02]} />
                <meshStandardMaterial color="#334155" />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.08} color="#ffffff" anchorX="center" anchorY="middle">
                +
              </Text>
            </group>
          </group>

          {/* Battery SoC Input Group */}
          <group position={[0.95, 0, 0]}>
            <Text
              position={[0, 0.1, 0]}
              fontSize={0.06}
              color="#94a3b8"
              anchorX="center"
              anchorY="middle"
            >
              DUNG LƯỢNG PIN (%)
            </Text>
            {/* Decrease button */}
            <group
              position={[-0.45, -0.05, 0]}
              onClick={(e) => {
                e.stopPropagation();
                setInputBattery(inputBattery - 5);
              }}
            >
              <mesh>
                <boxGeometry args={[0.18, 0.18, 0.02]} />
                <meshStandardMaterial color="#334155" />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.08} color="#ffffff" anchorX="center" anchorY="middle">
                -
              </Text>
            </group>
            {/* Battery display */}
            <mesh position={[0, -0.05, 0]}>
              <planeGeometry args={[0.62, 0.18]} />
              <meshStandardMaterial color="#020617" />
            </mesh>
            <Text position={[0, -0.05, 0.02]} fontSize={0.075} color="#38bdf8" anchorX="center" anchorY="middle">
              {inputBattery}%
            </Text>
            {/* Increase button */}
            <group
              position={[0.45, -0.05, 0]}
              onClick={(e) => {
                e.stopPropagation();
                setInputBattery(inputBattery + 5);
              }}
            >
              <mesh>
                <boxGeometry args={[0.18, 0.18, 0.02]} />
                <meshStandardMaterial color="#334155" />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.08} color="#ffffff" anchorX="center" anchorY="middle">
                +
              </Text>
            </group>
          </group>
        </group>

        {/* 6. Physical Damage Flag Button */}
        <group
          position={[0, -0.32, 0.05]}
          onClick={(e) => {
            e.stopPropagation();
            setHasDamageReported(!hasDamageReported);
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <planeGeometry args={[2.2, 0.18]} />
            <meshStandardMaterial
              color={hasDamageReported ? '#991b1b' : '#064e3b'}
              emissive={hasDamageReported ? '#ef4444' : '#10b981'}
              emissiveIntensity={0.3}
            />
          </mesh>
          <Text
            position={[0, 0, 0.01]}
            fontSize={0.065}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            {hasDamageReported
              ? '⚠ PHÁT HIỆN HƯ HỎNG: KÍCH HOẠT LỆNH BẢO TRÌ'
              : '✓ KIỂM TRA THÂN VỎ: ĐẠT TIÊU CHUẨN (KHÔNG HỎNG)'}
          </Text>
        </group>

        {/* 7. Master Action Button */}
        <group
          position={[0, -0.55, 0.05]}
          onClick={(e) => {
            e.stopPropagation();
            if (checkInOutMode === 'CHECK_IN') {
              executeCheckIn();
            } else {
              executeCheckOut();
            }
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            setHoveredBtn('ACTION');
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            setHoveredBtn(null);
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <boxGeometry args={[2.8, 0.26, 0.04]} />
            <meshStandardMaterial
              color={
                checkInOutMode === 'CHECK_IN'
                  ? hoveredBtn === 'ACTION'
                    ? '#16a34a'
                    : '#15803d'
                  : hoveredBtn === 'ACTION'
                  ? '#0284c7'
                  : '#0369a1'
              }
              emissive={checkInOutMode === 'CHECK_IN' ? '#22c55e' : '#38bdf8'}
              emissiveIntensity={hoveredBtn === 'ACTION' ? 0.6 : 0.2}
            />
          </mesh>
          <Text
            position={[0, 0, 0.03]}
            fontSize={0.075}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.05}
          >
            {isSubmitting
              ? 'ĐANG TRUYỀN DỮ LIỆU TỪ XA...'
              : checkInOutMode === 'CHECK_IN'
              ? `🟢 XUẤT XE ${selectedVehicle.modelName.toUpperCase()} (NHẬN XE)`
              : `🏁 QUYẾT TOÁN ${selectedVehicle.modelName.toUpperCase()} (TRẢ XE)`}
          </Text>
        </group>
      </group>

      {/* 8. Live Overhead Status Display Plate */}
      <group position={[0, 2.2, 0.1]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[3.2, 0.45]} />
          <meshBasicMaterial color="#020617" opacity={0.9} transparent />
        </mesh>
        <lineSegments position={[0, 0, 0.01]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(3.2, 0.45)]} />
          <lineBasicMaterial color={OPERATIONS_THEME.primary} />
        </lineSegments>
        <Text
          position={[0, 0.08, 0.02]}
          fontSize={0.08}
          color={OPERATIONS_THEME.secondary}
          anchorX="center"
          anchorY="middle"
        >
          XE ĐANG CHỌN: {selectedVehicle.modelName} [{selectedVehicle.licensePlate}]
        </Text>
        <Text
          position={[0, -0.08, 0.02]}
          fontSize={0.068}
          color="#38bdf8"
          anchorX="center"
          anchorY="middle"
        >
          {operationMessage || 'Hệ thống đã sẵn sàng.'}
        </Text>
      </group>
    </group>
  );
};
