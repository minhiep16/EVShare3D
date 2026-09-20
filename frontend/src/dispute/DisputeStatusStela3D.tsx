import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDisputeStore } from './useDisputeStore';
import { DISPUTE_THEME, DISPUTE_STATIONS } from './disputeLayout';
import { formatStatusVN, formatCurrencyVND } from '@/i18n';

export const DisputeStatusStela3D: React.FC = () => {
  const { activeDispute } = useDisputeStore();
  const auraRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    if (auraRef.current) {
      auraRef.current.position.y =
        2.2 + Math.sin(clock.getElapsedTime() * 1.5) * 0.05;
    }
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'OPEN':
        return DISPUTE_THEME.primary;
      case 'UNDER_REVIEW':
        return DISPUTE_THEME.secondary;
      case 'ESCALATED':
        return '#f97316'; // Vivid orange
      case 'RESOLVED':
        return DISPUTE_THEME.verdictGreen;
      case 'DISMISSED':
        return '#64748b';
      default:
        return DISPUTE_THEME.cyberCyan;
    }
  };

  const statusColor = getStatusColor(activeDispute.status);
  const stelaPos = DISPUTE_STATIONS.STATUS_STELA.relativePosition;

  return (
    <group name="DisputeStatusStelaStation" position={stelaPos}>
      {/* Stela Header Marker */}
      <Text
        position={[0, 4.4, 0]}
        fontSize={0.21}
        color={statusColor}
        anchorX="center"
        anchorY="middle"
      >
        BIA ĐÁ KIỂM TOÁN LỊCH TRÌNH & PHÁN QUYẾT
      </Text>
      <Text
        position={[0, 4.12, 0]}
        fontSize={0.11}
        color={DISPUTE_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Biên nhận sổ cái bất biến • Trình tự sự kiện mã hóa
      </Text>

      {/* Monolithic Obsidian Pillar Body */}
      <mesh position={[0, 2.0, 0]} receiveShadow castShadow>
        <boxGeometry args={[3.2, 3.8, 0.45]} />
        <meshStandardMaterial
          color="#0c0202"
          roughness={0.4}
          metalness={0.9}
        />
      </mesh>

      {/* Glowing Outer Runic Trim */}
      <mesh position={[0, 2.0, 0.23]} ref={auraRef}>
        <planeGeometry args={[3.26, 3.86]} />
        <meshBasicMaterial color={statusColor} wireframe transparent opacity={0.6} />
      </mesh>

      {/* Surface Information Panel */}
      <group position={[0, 2.0, 0.24]}>
        {/* Active Status Badge Header */}
        <mesh position={[0, 1.55, 0]}>
          <planeGeometry args={[2.8, 0.28]} />
          <meshBasicMaterial color="#1a0505" />
        </mesh>
        <Text
          position={[0, 1.55, 0.02]}
          fontSize={0.095}
          color={statusColor}
          anchorX="center"
          anchorY="middle"
        >
          TRẠNG THÁI HIỆN TẠI: {formatStatusVN(activeDispute.status)}
        </Text>

        {/* Audit Event Timeline Entries */}
        <group position={[-1.3, 1.15, 0]}>
          {/* Event 1: Creation */}
          <group position={[0, 0, 0]}>
            <mesh position={[-0.05, 0, 0]}>
              <sphereGeometry args={[0.04, 12, 12]} />
              <meshBasicMaterial color={DISPUTE_THEME.cyberCyan} />
            </mesh>
            <Text
              position={[0.1, 0.04, 0]}
              fontSize={0.06}
              color={DISPUTE_THEME.cyberCyan}
              anchorX="left"
              anchorY="middle"
            >
              08:00 • NỘP ĐƠN TRANH CHẤP
            </Text>
            <Text
              position={[0.1, -0.06, 0]}
              fontSize={0.05}
              color="#e2e8f0"
              anchorX="left"
              anchorY="middle"
            >
              {activeDispute.complainantUserName} nộp đơn khiếu nại {activeDispute.respondentUserName} cho Phiên #105.
            </Text>
          </group>

          {/* Event 2: Evidence */}
          <group position={[0, -0.36, 0]}>
            <mesh position={[-0.05, 0, 0]}>
              <sphereGeometry args={[0.04, 12, 12]} />
              <meshBasicMaterial color={DISPUTE_THEME.cyberCyan} />
            </mesh>
            <Text
              position={[0.1, 0.04, 0]}
              fontSize={0.06}
              color={DISPUTE_THEME.cyberCyan}
              anchorX="left"
              anchorY="middle"
            >
              08:15 • GHIM TỌA ĐỘ ĐIỂM HỎNG 3D
            </Text>
            <Text
              position={[0.1, -0.06, 0]}
              fontSize={0.05}
              color="#e2e8f0"
              anchorX="left"
              anchorY="middle"
            >
              2 tập tin chứng cứ bất biến được ghim kèm tọa độ điểm hỏng 3D thực tế.
            </Text>
          </group>

          {/* Event 3: Mediation */}
          <group position={[0, -0.72, 0]}>
            <mesh position={[-0.05, 0, 0]}>
              <sphereGeometry args={[0.04, 12, 12]} />
              <meshBasicMaterial color={DISPUTE_THEME.secondary} />
            </mesh>
            <Text
              position={[0.1, 0.04, 0]}
              fontSize={0.06}
              color={DISPUTE_THEME.secondary}
              anchorX="left"
              anchorY="middle"
            >
              08:30 • NHÂN VIÊN ĐÁNH GIÁ HÒA GIẢI
            </Text>
            <Text
              position={[0.1, -0.06, 0]}
              fontSize={0.05}
              color="#e2e8f0"
              anchorX="left"
              anchorY="middle"
            >
              Đối chiếu dữ liệu viễn thông xe; đề xuất các điều khoản hòa giải.
            </Text>
          </group>

          {/* Event 4: Latest Lifecycle Transition */}
          <group position={[0, -1.08, 0]}>
            <mesh position={[-0.05, 0, 0]}>
              <sphereGeometry args={[0.04, 12, 12]} />
              <meshBasicMaterial color={statusColor} />
            </mesh>
            <Text
              position={[0.1, 0.04, 0]}
              fontSize={0.06}
              color={statusColor}
              anchorX="left"
              anchorY="middle"
            >
              SỰ KIỆN VÒNG ĐỜI MỚI NHẤT: {formatStatusVN(activeDispute.status)}
            </Text>
            <Text
              position={[0.1, -0.06, 0]}
              fontSize={0.05}
              color="#e2e8f0"
              anchorX="left"
              anchorY="middle"
            >
              {activeDispute.status === 'RESOLVED'
                ? 'Phán quyết chung thẩm đã hoàn tất kèm quyết toán quỹ tự động.'
                : activeDispute.status === 'ESCALATED'
                ? 'Vụ việc đã leo thang lên Bục trọng tài tối cao.'
                : 'Vụ việc đang trong quá trình xem xét hòa giải.'}
            </Text>
          </group>
        </group>

        {/* Resolved Settlement Dossier Frame */}
        {activeDispute.status === 'RESOLVED' && (
          <group position={[0, -0.85, 0]}>
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[2.8, 0.55]} />
              <meshStandardMaterial color="#064e3b" roughness={0.3} />
            </mesh>
            <mesh position={[0, 0, 0.005]}>
              <planeGeometry args={[2.82, 0.57]} />
              <meshBasicMaterial color="#34d399" wireframe />
            </mesh>
            <Text
              position={[0, 0.16, 0.02]}
              fontSize={0.068}
              color="#6ee7b7"
              anchorX="center"
              anchorY="middle"
            >
              ✓ PHÁN QUYẾT TRỌNG TÀI RÀNG BUỘC CHUNG THẨM
            </Text>
            <Text
              position={[0, 0.02, 0.02]}
              fontSize={0.052}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
              maxWidth={2.6}
            >
              {activeDispute.resolutionSummary}
            </Text>
            <Text
              position={[0, -0.16, 0.02]}
              fontSize={0.05}
              color={DISPUTE_THEME.secondary}
              anchorX="center"
              anchorY="middle"
            >
              Mã giao dịch: {activeDispute.fundTransactionReference} • Số tiền:{' '}
              {formatCurrencyVND(activeDispute.fundAdjustmentAmount || 0)}
            </Text>
          </group>
        )}
      </group>
    </group>
  );
};
