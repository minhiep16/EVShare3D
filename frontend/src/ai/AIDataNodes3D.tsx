import React, { useState } from 'react';
import { Text, Line } from '@react-three/drei';
import * as THREE from 'three';
import { useAIStore } from './useAIStore';
import { AI_LAYOUT } from './aiLayout';
import type { AIDataNodeModel } from './aiTypes';

export const AIDataNodes3D: React.FC = () => {
  const dataNodes = useAIStore((s) => s.dataNodes);
  const selectedNodeId = useAIStore((s) => s.selectedNodeId);
  const selectNode = useAIStore((s) => s.selectNode);

  return (
    <group name="AIDataNodes">
      {dataNodes.map((node) => {
        const isSelected = selectedNodeId === node.id;
        return (
          <SingleDataNode
            key={node.id}
            node={node}
            isSelected={isSelected}
            onSelect={() => selectNode(node.id)}
          />
        );
      })}
    </group>
  );
};

interface SingleDataNodeProps {
  node: AIDataNodeModel;
  isSelected: boolean;
  onSelect: () => void;
}

const METRIC_LABEL_MAP: Record<string, string> = {
  'State of Health (SOH)': 'Tình trạng pin (SOH)',
  'Avg Energy Efficiency': 'Hiệu suất năng lượng TB',
  'Odometer Telemetry': 'Quãng đường đã đi',
  'Tire Pressure (PSI)': 'Áp suất lốp (PSI)',
  'Syndicate Fairness Index': 'Chỉ số công bằng nhóm',
  'Evaluation Window': 'Chu kỳ đánh giá',
  'Peak Hour Multiplier': 'Hệ số giờ cao điểm',
  'Priority Co-Owner': 'Thành viên ưu tiên',
  'Monthly Reserve Pool': 'Quỹ dự phòng hàng tháng',
  'Reserve Floor Ratio': 'Tỷ lệ sàn dự phòng',
  'Settlement Latency': 'Thời gian quyết toán',
  'Forecasted Expense': 'Chi phí dự báo',
  'Active Syndicate Proposals': 'Đề xuất đang hiệu lực',
  'Average Quorum Turnout': 'Tỷ lệ túc số TB',
  'Digital Contract Hash': 'Mã băm hợp đồng số',
  'Advisory Boundary': 'Ranh giới cố vấn AI',
  'Total Group Usage': 'Tổng thời gian sử dụng',
  'Fairness Imbalance': 'Mức mất cân bằng',
  'Gini Coefficient': 'Hệ số bất bình đẳng Gini',
  'Equitable Distribution': 'Mức độ công bằng',
};

const METRIC_VALUE_MAP: Record<string, string> = {
  'Nominal (-0.2%/mo)': 'Bình thường (-0.2%/tháng)',
  '+4% Regenerative': '+4% Tái sinh',
  'Active Fleet': 'Đang hoạt động',
  'Balanced': 'Cân bằng',
  'Standard': 'Tiêu chuẩn',
  'Active': 'Đang áp dụng',
  'Eligible': 'Đủ điều kiện',
  'Healthy': 'Ổn định',
  'Compliant': 'Tuân thủ',
  'Instant VietQR': 'Tức thì qua VietQR',
  'Battery Service': 'Bảo dưỡng pin',
  'In Session': 'Đang họp bàn',
  'Quorum Met': 'Đạt túc số',
  '4/4 Signed': '4/4 Đã ký',
  'Advisory Only': 'Chỉ cố vấn',
  '3 Active Deliberations': '3 Đề xuất đang thảo luận',
  'SHA-256 Verifiable': 'Xác thực SHA-256',
  'STRICTLY ENFORCED': 'BẮT BUỘC TUÂN THỦ',
};

function formatMetricLabel(label: string): string {
  return METRIC_LABEL_MAP[label] || label;
}

function formatMetricValue(value: string): string {
  if (METRIC_VALUE_MAP[value]) return METRIC_VALUE_MAP[value];
  return value
    .replace(/\bHours\b/g, 'Giờ')
    .replace(/\bDays\b/g, 'Ngày')
    .replace(/\bFront\b/g, 'Trước')
    .replace(/\bRear\b/g, 'Sau');
}

const SingleDataNode: React.FC<SingleDataNodeProps> = ({
  node,
  isSelected,
  onSelect,
}) => {
  const [hovered, setHovered] = useState(false);
  const corePos = AI_LAYOUT.aiCorePosition;

  return (
    <group position={node.position}>
      {/* 1. Laser Data Conduit Beam to Central Core */}
      <Line
        points={[
          [0, 0, 0],
          [corePos[0] - node.position[0], corePos[1] - node.position[1], corePos[2] - node.position[2]],
        ]}
        color={isSelected ? '#fbbf24' : node.color}
        lineWidth={isSelected ? 2 : 1}
        transparent
        opacity={isSelected ? 0.75 : 0.35}
      />

      {/* 2. Interactive Satellite Node Object */}
      <group
        onClick={(e) => {
          e.stopPropagation();
          onSelect();
        }}
        onPointerOver={(e) => {
          e.stopPropagation();
          setHovered(true);
          document.body.style.cursor = 'pointer';
        }}
        onPointerOut={() => {
          setHovered(false);
          document.body.style.cursor = 'auto';
        }}
      >
        {/* Node Polyhedron */}
        <mesh castShadow scale={isSelected ? 1.25 : hovered ? 1.15 : 1.0}>
          <octahedronGeometry args={[0.35, 0]} />
          <meshStandardMaterial
            color={isSelected ? '#fbbf24' : node.color}
            emissive={isSelected ? '#d97706' : node.color}
            emissiveIntensity={isSelected ? 1.5 : hovered ? 1.0 : 0.4}
            roughness={0.2}
            metalness={0.8}
          />
        </mesh>

        {/* Outer Pulsing Wireframe Cage */}
        <mesh scale={isSelected ? 1.45 : hovered ? 1.3 : 1.15}>
          <dodecahedronGeometry args={[0.38, 0]} />
          <meshBasicMaterial
            color={isSelected ? '#ffffff' : node.color}
            wireframe
            transparent
            opacity={isSelected ? 0.8 : 0.4}
          />
        </mesh>

        {/* 3. Floating Telemetry Card Above Node */}
        <group position={[0, 1.1, 0]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[2.2, 1.1]} />
            <meshBasicMaterial
              color="#040612"
              opacity={isSelected ? 0.92 : 0.8}
              transparent
              side={THREE.DoubleSide}
            />
          </mesh>
          <lineSegments position={[0, 0, 0.005]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(2.2, 1.1)]} />
            <lineBasicMaterial color={isSelected ? '#fbbf24' : hovered ? '#ffffff' : node.color} />
          </lineSegments>

          {/* Node Header */}
          <Text
            position={[0, 0.38, 0.02]}
            fontSize={0.11}
            color={isSelected ? '#fbbf24' : node.color}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.05}
          >
            {node.id === 'NODE_MOBILITY'
              ? 'DỮ LIỆU DI CHUYỂN'
              : node.id === 'NODE_FAIRNESS'
              ? 'CHỈ SỐ CÔNG BẰNG'
              : node.id === 'NODE_DIAGNOSTICS'
              ? 'CHẨN ĐOÁN PIN XE'
              : node.id === 'NODE_GOVERNANCE'
              ? 'DỮ LIỆU QUẢN TRỊ'
              : node.name.toUpperCase()}
          </Text>

          {/* Metrics List */}
          {node.metrics.slice(0, 3).map((m, idx) => {
            const yOffset = 0.14 - idx * 0.22;
            return (
              <group key={idx} position={[0, yOffset, 0.02]}>
                <Text
                  position={[-0.95, 0, 0]}
                  fontSize={0.08}
                  color={AI_LAYOUT.colors.textMuted}
                  anchorX="left"
                  anchorY="middle"
                >
                  {formatMetricLabel(m.label)}
                </Text>
                <Text
                  position={[0.95, 0, 0]}
                  fontSize={0.085}
                  color={AI_LAYOUT.colors.textWhite}
                  anchorX="right"
                  anchorY="middle"
                >
                  {formatMetricValue(m.value)}
                </Text>
              </group>
            );
          })}
        </group>
      </group>
    </group>
  );
};
