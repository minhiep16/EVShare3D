import React, { useRef, useState, useMemo } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useOperationsStore } from './useOperationsStore';
import { OPERATIONS_STATIONS, OPERATIONS_THEME } from './operationsLayout';

export const QRScannerStation3D: React.FC = () => {
  const laserRef = useRef<THREE.Mesh>(null);
  const holoMatrixRef = useRef<THREE.Group>(null);

  const {
    qrToken,
    qrExpiresAt,
    qrValidationResult,
    isScanningQr,
    generateQrCode,
    validateQrCode,
  } = useOperationsStore();

  const [hoverBtnGen, setHoverBtnGen] = useState(false);
  const [hoverBtnVal, setHoverBtnVal] = useState(false);

  // Animated laser sweep & floating matrix wobble
  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (laserRef.current) {
      // Oscillate laser position along scanner bed
      laserRef.current.position.y = 1.22 + Math.sin(t * 3.5) * 0.32;
    }
    if (holoMatrixRef.current) {
      holoMatrixRef.current.rotation.y = t * 0.4;
      holoMatrixRef.current.position.y = 1.95 + Math.sin(t * 1.8) * 0.04;
    }
  });

  // Generate a procedural 7x7 holographic QR bit matrix based on token string
  const qrGrid = useMemo(() => {
    const seed = qrToken || 'QR-EVSHARE-DEFAULT';
    const grid: boolean[][] = [];
    for (let r = 0; r < 7; r++) {
      const row: boolean[] = [];
      for (let c = 0; c < 7; c++) {
        // Corners are finder patterns
        if (
          (r < 2 && c < 2) ||
          (r < 2 && c > 4) ||
          (r > 4 && c < 2)
        ) {
          row.push(true);
        } else {
          const charCode = seed.charCodeAt((r * 7 + c) % seed.length);
          row.push((charCode + r * c) % 2 === 0);
        }
      }
      grid.push(row);
    }
    return grid;
  }, [qrToken]);

  return (
    <group
      name="QRScannerStation"
      position={OPERATIONS_STATIONS.QR_DESK.relativePosition}
    >
      {/* 1. Pedestal Stand */}
      <mesh position={[0, 0.55, 0]} castShadow>
        <cylinderGeometry args={[0.35, 0.45, 1.1, 24]} />
        <meshStandardMaterial
          color="#0f172a"
          metalness={0.7}
          roughness={0.3}
        />
      </mesh>

      {/* Base Ring Accent */}
      <mesh position={[0, 0.05, 0]}>
        <cylinderGeometry args={[0.48, 0.5, 0.1, 24]} />
        <meshStandardMaterial
          color={OPERATIONS_THEME.primary}
          emissive={OPERATIONS_THEME.primary}
          emissiveIntensity={0.6}
        />
      </mesh>

      {/* 2. Angled Scanner Bed / Frame */}
      <group position={[0, 1.22, 0.1]} rotation={[-Math.PI * 0.18, 0, 0]}>
        {/* Scanner Backplate */}
        <mesh position={[0, 0, 0]}>
          <boxGeometry args={[1.0, 0.9, 0.08]} />
          <meshStandardMaterial
            color="#020617"
            metalness={0.8}
            roughness={0.2}
          />
        </mesh>

        {/* Optical Glass Bed */}
        <mesh position={[0, 0, 0.045]}>
          <planeGeometry args={[0.88, 0.78]} />
          <meshStandardMaterial
            color="#083344"
            emissive={OPERATIONS_THEME.cyberCyan}
            emissiveIntensity={0.2}
            roughness={0.1}
            metalness={0.9}
            transparent
            opacity={0.85}
          />
        </mesh>

        {/* Laser Sweep Line */}
        <mesh ref={laserRef} position={[0, 0, 0.05]}>
          <planeGeometry args={[0.86, 0.02]} />
          <meshBasicMaterial
            color={isScanningQr ? '#ef4444' : OPERATIONS_THEME.cyberCyan}
            side={THREE.DoubleSide}
          />
        </mesh>

        {/* Scanner Bed Border Wire */}
        <lineSegments position={[0, 0, 0.05]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(0.88, 0.78)]} />
          <lineBasicMaterial color={OPERATIONS_THEME.primary} />
        </lineSegments>
      </group>

      {/* 3. Floating 3D Holographic QR Code Matrix */}
      <group ref={holoMatrixRef} position={[0, 1.95, 0.1]}>
        {/* Glass backing plate */}
        <mesh position={[0, 0, 0]}>
          <boxGeometry args={[0.82, 0.82, 0.02]} />
          <meshBasicMaterial
            color="#030712"
            transparent
            opacity={0.75}
            side={THREE.DoubleSide}
          />
        </mesh>
        <lineSegments position={[0, 0, 0.015]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(0.82, 0.82)]} />
          <lineBasicMaterial color={OPERATIONS_THEME.cyberCyan} />
        </lineSegments>

        {/* Procedural QR Bit Cubes */}
        {qrGrid.map((row, r) =>
          row.map((bit, c) => {
            if (!bit) return null;
            const x = (c - 3) * 0.095;
            const y = (3 - r) * 0.095;
            return (
              <mesh key={`qr-${r}-${c}`} position={[x, y, 0.02]}>
                <boxGeometry args={[0.082, 0.082, 0.02]} />
                <meshStandardMaterial
                  color={OPERATIONS_THEME.cyberCyan}
                  emissive={OPERATIONS_THEME.cyberCyan}
                  emissiveIntensity={0.8}
                />
              </mesh>
            );
          })
        )}
      </group>

      {/* 4. Scanner Info & Readout Board */}
      <group position={[0, 2.55, 0.1]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.14}
          color={OPERATIONS_THEME.primary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          TRẠM QUÉT MÃ QR NHẬN XE
        </Text>
        <Text
          position={[0, -0.16, 0]}
          fontSize={0.085}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          MÃ MẬT MÃ: {qrToken || 'CHƯA PHÁT HÀNH'}
        </Text>
        <Text
          position={[0, -0.3, 0]}
          fontSize={0.075}
          color={qrValidationResult ? '#10b981' : '#f59e0b'}
          anchorX="center"
          anchorY="middle"
        >
          {qrValidationResult
            ? `ĐÃ XÁC THỰC: ${qrValidationResult.userName || 'Người dùng hợp lệ'}`
            : 'TRẠNG THÁI: SẴN SÀNG QUÉT'}
        </Text>
      </group>

      {/* 5. Interactive 3D Action Buttons */}
      <group position={[0, 0.95, 0.55]}>
        {/* Button 1: Issue / Refresh QR */}
        <group
          position={[-0.42, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            generateQrCode(101);
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            setHoverBtnGen(true);
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            setHoverBtnGen(false);
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <boxGeometry args={[0.78, 0.22, 0.05]} />
            <meshStandardMaterial
              color={hoverBtnGen ? '#ea580c' : '#1e293b'}
              emissive={hoverBtnGen ? OPERATIONS_THEME.primary : '#000000'}
              emissiveIntensity={hoverBtnGen ? 0.6 : 0}
            />
          </mesh>
          <Text
            position={[0, 0, 0.035]}
            fontSize={0.062}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            ⚡ TẠO MÃ QR
          </Text>
        </group>

        {/* Button 2: Scan & Validate */}
        <group
          position={[0.42, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            validateQrCode();
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            setHoverBtnVal(true);
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            setHoverBtnVal(false);
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <boxGeometry args={[0.78, 0.22, 0.05]} />
            <meshStandardMaterial
              color={hoverBtnVal ? '#0891b2' : '#1e293b'}
              emissive={hoverBtnVal ? OPERATIONS_THEME.cyberCyan : '#000000'}
              emissiveIntensity={hoverBtnVal ? 0.6 : 0}
            />
          </mesh>
          <Text
            position={[0, 0, 0.035]}
            fontSize={0.06}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            {isScanningQr ? 'ĐANG QUÉT...' : '🔍 XÁC THỰC QR'}
          </Text>
        </group>
      </group>
    </group>
  );
};
