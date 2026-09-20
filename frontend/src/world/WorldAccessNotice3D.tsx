import React, { useEffect, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group } from 'three';
import { useNavigationStore } from './useNavigationStore';
import { usePlayerStore } from '@/stores/usePlayerStore';

export const WorldAccessNotice3D: React.FC = () => {
  const notice = useNavigationStore((state) => state.deniedAccessNotice);
  const clearNotice = useNavigationStore((state) => state.clearDeniedNotice);
  const playerPosition = usePlayerStore((state) => state.position);

  const panelRef = useRef<Group>(null);

  useEffect(() => {
    if (!notice) return;

    const timer = setTimeout(() => {
      clearNotice();
    }, 3200);

    return () => clearTimeout(timer);
  }, [notice, clearNotice]);

  useFrame(() => {
    if (!notice || !panelRef.current) return;
    // Position floating slightly in front and above the player avatar
    panelRef.current.position.set(
      playerPosition[0],
      playerPosition[1] + 2.8,
      playerPosition[2] - 1.2
    );
    // Pulsate opacity or scale
    const pulse = 1.0 + Math.sin(performance.now() * 0.008) * 0.05;
    panelRef.current.scale.setScalar(pulse);
  });

  if (!notice) return null;

  return (
    <group ref={panelRef} name="WorldAccessNotice3D">
      {/* Background Warning Plate */}
      <mesh position={[0, 0, -0.02]}>
        <planeGeometry args={[3.6, 1.2]} />
        <meshBasicMaterial color="#1a0507" transparent opacity={0.9} />
      </mesh>

      {/* Red Hazard Border Wireframe */}
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[3.65, 1.25]} />
        <meshBasicMaterial color="#ff1744" wireframe />
      </mesh>

      {/* Top Banner Text */}
      <Text
        position={[0, 0.32, 0.02]}
        fontSize={0.2}
        color="#ff1744"
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
      >
        TỪ CHỐI TRUY CẬP AN NINH
      </Text>

      {/* Main Notice Message */}
      <Text
        position={[0, -0.05, 0.02]}
        fontSize={0.14}
        color="#ffffff"
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
      >
        {(() => {
          if (notice.message.includes('Biometric Authentication') || notice.message.includes('UNAUTHORIZED') || notice.message.includes('PERIMETER VIOLATION')) {
            return 'VI PHẠM VÀNH ĐAI: Vui lòng hoàn tất xác thực sinh trắc học để vào Metaverse';
          }
          const roleVN: Record<string, string> = {
            ROLE_CO_OWNER: 'ĐỒNG SỞ HỮU (ROLE_CO_OWNER)',
            ROLE_STAFF: 'NHÂN VIÊN (ROLE_STAFF)',
            ROLE_ADMIN: 'QUẢN TRỊ VIÊN (ROLE_ADMIN)',
          };
          const roleName = (notice.requiredRole && roleVN[notice.requiredRole]) || notice.requiredRole || 'CẤP ĐỘ CAO HƠN';
          return `TRUY CẬP BỊ GIỚI HẠN: Khu vực yêu cầu quyền ${roleName}`;
        })()}
      </Text>

      {/* Subtitle / Tip */}
      <Text
        position={[0, -0.32, 0.02]}
        fontSize={0.11}
        color="#f87171"
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/jetbrainsmono/v18/tDbY2o-flEEny0FZhsfKu5WU4zr3E_ad56U.woff"
      >
        LIÊN HỆ QUẢN TRỊ VIÊN ĐỂ ĐƯỢC CẤP QUYỀN TRUY CẬP
      </Text>
    </group>
  );
};
