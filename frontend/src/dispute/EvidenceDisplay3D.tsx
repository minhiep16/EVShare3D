import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDisputeStore } from './useDisputeStore';
import { DISPUTE_THEME, DISPUTE_STATIONS } from './disputeLayout';

export const EvidenceDisplay3D: React.FC = () => {
  const {
    evidenceList,
    selectedEvidenceId,
    selectEvidence,
    selectDefect,
    defects,
    attachEvidence,
    setActiveStation,
    setActiveTab,
    isSubmitting,
  } = useDisputeStore();

  const glowRef = useRef<THREE.Mesh>(null);
  const [hoveredButton, setHoveredButton] = useState<string | null>(null);

  useFrame(({ clock }) => {
    if (glowRef.current) {
      const s = 1 + Math.sin(clock.getElapsedTime() * 2) * 0.04;
      glowRef.current.scale.set(s, s, 1);
    }
  });

  const selectedEvidence =
    evidenceList.find((e) => e.id === selectedEvidenceId) || evidenceList[0];

  const handleSelectEvidence = (id: number) => {
    selectEvidence(id);
    const relatedDefect = defects.find((d) => d.evidenceId === id);
    if (relatedDefect) {
      selectDefect(relatedDefect.id);
    }
  };

  const handlePinpointOnHolotank = () => {
    const relatedDefect = defects.find(
      (d) => d.evidenceId === selectedEvidence?.id
    );
    if (relatedDefect) {
      selectDefect(relatedDefect.id);
    }
    setActiveStation('DEFECT_HOLOTANK');
    setActiveTab('DEFECT_SCAN');
  };

  const handleQuickAddEvidence = () => {
    attachEvidence('Supplementary high-res photo of left bumper trim & headlight gap.', [
      -0.88,
      0.48,
      -2.08,
    ]);
  };

  const stationPos = DISPUTE_STATIONS.EVIDENCE_CAROUSEL.relativePosition;

  return (
    <group name="EvidenceCarouselStation" position={stationPos}>
      {/* Station Title */}
      <Text
        position={[0, 3.2, 0]}
        fontSize={0.2}
        color={DISPUTE_THEME.cyberCyan}
        anchorX="center"
        anchorY="middle"
      >
        BĂNG CHUYỀN TRÌNH DIỄN CHỨNG CỨ BẤT BIẾN
      </Text>
      <Text
        position={[0, 2.94, 0]}
        fontSize={0.11}
        color={DISPUTE_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Chuẩn BR-DIS-01 • Sổ đăng ký băm mật mã vĩnh viễn
      </Text>

      {/* Heavy Base Pedestal */}
      <mesh position={[0, 0.35, 0]} receiveShadow castShadow>
        <boxGeometry args={[3.4, 0.7, 1.6]} />
        <meshStandardMaterial
          color="#0f0202"
          roughness={0.6}
          metalness={0.8}
        />
      </mesh>

      {/* Obsidian Base Trim */}
      <mesh position={[0, 0.71, 0]}>
        <boxGeometry args={[3.44, 0.04, 1.64]} />
        <meshStandardMaterial
          color={DISPUTE_THEME.cyberCyan}
          emissive={DISPUTE_THEME.cyberCyan}
          emissiveIntensity={0.5}
        />
      </mesh>

      {/* Angled Main Display Board */}
      <group position={[0, 1.85, -0.1]} rotation={[-0.15, 0, 0]}>
        {/* Terminal Frame */}
        <mesh position={[0, 0, -0.02]} receiveShadow>
          <planeGeometry args={[3.3, 2.0]} />
          <meshStandardMaterial
            color="#140303"
            roughness={0.3}
            metalness={0.9}
            transparent
            opacity={0.95}
          />
        </mesh>

        {/* Outer Accent Glow Border */}
        <mesh position={[0, 0, -0.015]} ref={glowRef}>
          <planeGeometry args={[3.34, 2.04]} />
          <meshBasicMaterial
            color={DISPUTE_THEME.cyberCyan}
            wireframe
            transparent
            opacity={0.7}
          />
        </mesh>

        {/* LEFT COLUMN: Evidence Selection Carousel Tabs */}
        <group position={[-1.05, 0.45, 0.02]}>
          <Text
            position={[0, 0.35, 0]}
            fontSize={0.082}
            color={DISPUTE_THEME.cyberCyan}
            anchorX="center"
            anchorY="middle"
          >
            HỒ SƠ CHỨNG CỨ ({evidenceList.length})
          </Text>

          {evidenceList.map((item, idx) => {
            const isSelected = item.id === selectedEvidence?.id;
            return (
              <group
                key={item.id}
                position={[0, -idx * 0.45, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  handleSelectEvidence(item.id);
                }}
                onPointerOver={() => {
                  setHoveredButton(`ev-${item.id}`);
                  document.body.style.cursor = 'pointer';
                }}
                onPointerOut={() => {
                  setHoveredButton(null);
                  document.body.style.cursor = 'auto';
                }}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[0.95, 0.36]} />
                  <meshStandardMaterial
                    color={isSelected ? '#311010' : '#1f0808'}
                    roughness={0.4}
                    metalness={0.7}
                  />
                </mesh>
                <mesh position={[0, 0, 0.005]}>
                  <planeGeometry args={[0.96, 0.37]} />
                  <meshBasicMaterial
                    color={
                      isSelected
                        ? DISPUTE_THEME.primary
                        : hoveredButton === `ev-${item.id}`
                        ? DISPUTE_THEME.cyberCyan
                        : '#3b1212'
                    }
                    wireframe
                  />
                </mesh>
                <Text
                  position={[-0.42, 0.08, 0.02]}
                  fontSize={0.065}
                  color={isSelected ? DISPUTE_THEME.secondary : '#ffffff'}
                  anchorX="left"
                  anchorY="middle"
                >
                  TÀI LIỆU #{item.id} • {item.fileUrl.endsWith('.pdf') ? 'PDF' : 'JPG'}
                </Text>
                <Text
                  position={[-0.42, -0.06, 0.02]}
                  fontSize={0.05}
                  color={DISPUTE_THEME.textMuted}
                  anchorX="left"
                  anchorY="middle"
                >
                  Bởi: {item.uploadedByUserName}
                </Text>
              </group>
            );
          })}

          {/* Action Button: Attach New Evidence */}
          <group
            position={[0, -1.05, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (!isSubmitting) handleQuickAddEvidence();
            }}
            onPointerOver={() => {
              setHoveredButton('attach');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredButton(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[0.95, 0.28]} />
              <meshStandardMaterial
                color={
                  hoveredButton === 'attach'
                    ? DISPUTE_THEME.primary
                    : '#4c0d0d'
                }
                emissive={DISPUTE_THEME.primary}
                emissiveIntensity={hoveredButton === 'attach' ? 0.7 : 0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.058}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ ➕ THÊM CHỨNG CỨ ]
            </Text>
          </group>
        </group>

        {/* RIGHT COLUMN: Selected Evidence Details & Spatial Coordinates */}
        {selectedEvidence && (
          <group position={[0.55, 0, 0.02]}>
            {/* Header / Security Badge */}
            <mesh position={[0.45, 0.8, 0]}>
              <planeGeometry args={[0.9, 0.16]} />
              <meshBasicMaterial color="#065f46" />
            </mesh>
            <Text
              position={[0.45, 0.8, 0.02]}
              fontSize={0.054}
              color="#34d399"
              anchorX="center"
              anchorY="middle"
            >
              ✓ BẤT BIẾN • BR-DIS-01
            </Text>

            <Text
              position={[-0.85, 0.8, 0]}
              fontSize={0.09}
              color={DISPUTE_THEME.cyberCyan}
              anchorX="left"
              anchorY="middle"
            >
              BẢN GHI CHỨNG CỨ #{selectedEvidence.id}
            </Text>

            {/* Document / Photo Visual Thumbnail Simulator */}
            <mesh position={[0, 0.35, 0]}>
              <planeGeometry args={[1.8, 0.65]} />
              <meshStandardMaterial
                color="#0a101d"
                roughness={0.2}
                metalness={0.9}
              />
            </mesh>
            <mesh position={[0, 0.35, 0.005]}>
              <planeGeometry args={[1.82, 0.67]} />
              <meshBasicMaterial color={DISPUTE_THEME.cyberCyan} wireframe />
            </mesh>

            {/* Thumbnail Content Mock Graphic */}
            <Text
              position={[0, 0.44, 0.02]}
              fontSize={0.072}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              {selectedEvidence.fileUrl.split('/').pop()}
            </Text>
            <Text
              position={[0, 0.28, 0.02]}
              fontSize={0.054}
              color={DISPUTE_THEME.secondary}
              anchorX="center"
              anchorY="middle"
            >
              SHA-256: 8f49b1a03e...c99d (Chữ ký xác thực)
            </Text>

            {/* 3D Coordinates Specification */}
            <Text
              position={[-0.85, -0.08, 0]}
              fontSize={0.065}
              color={DISPUTE_THEME.secondary}
              anchorX="left"
              anchorY="middle"
            >
              Tọa độ lưới điểm hỏng 3D:
            </Text>
            <Text
              position={[-0.85, -0.22, 0]}
              fontSize={0.062}
              color="#38bdf8"
              anchorX="left"
              anchorY="middle"
            >
              {selectedEvidence.mesh3dDefectCoordinates || 'Chưa xác định tọa độ'}
            </Text>

            {/* Description Text */}
            <Text
              position={[-0.85, -0.38, 0]}
              fontSize={0.06}
              color="#f8fafc"
              anchorX="left"
              anchorY="top"
              maxWidth={1.8}
              lineHeight={1.3}
            >
              "{selectedEvidence.description}"
            </Text>

            {/* Pinpoint on Holotank Button */}
            <group
              position={[0, -0.75, 0]}
              onClick={(e) => {
                e.stopPropagation();
                handlePinpointOnHolotank();
              }}
              onPointerOver={() => {
                setHoveredButton('pinpoint');
                document.body.style.cursor = 'pointer';
              }}
              onPointerOut={() => {
                setHoveredButton(null);
                document.body.style.cursor = 'auto';
              }}
            >
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[1.8, 0.26]} />
                <meshStandardMaterial
                  color={
                    hoveredButton === 'pinpoint'
                      ? DISPUTE_THEME.cyberCyan
                      : '#0369a1'
                  }
                  emissive={DISPUTE_THEME.cyberCyan}
                  emissiveIntensity={hoveredButton === 'pinpoint' ? 0.8 : 0.4}
                />
              </mesh>
              <Text
                position={[0, 0, 0.02]}
                fontSize={0.07}
                color="#ffffff"
                anchorX="center"
                anchorY="middle"
              >
                [ 📍 ĐỊNH VỊ TRÊN BỂ ẢNH 3D ]
              </Text>
            </group>
          </group>
        )}
      </group>
    </group>
  );
};
