import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDisputeStore } from './useDisputeStore';
import { DISPUTE_THEME, DISPUTE_STATIONS } from './disputeLayout';
import type { DefectMarker3D } from './disputeTypes';

export const DefectHolotank3D: React.FC = () => {
  const {
    defects,
    selectedDefectId,
    selectDefect,
    selectEvidence,
    setActiveStation,
    setActiveTab,
  } = useDisputeStore();

  const holotankGroupRef = useRef<THREE.Group>(null);
  const scanRingRef = useRef<THREE.Mesh>(null);
  const twinGroupRef = useRef<THREE.Group>(null);
  const [hoveredDefectId, setHoveredDefectId] = useState<string | null>(null);

  // Rotate scanner and gently float the digital twin
  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (scanRingRef.current) {
      scanRingRef.current.rotation.y = t * 0.8;
    }
    if (twinGroupRef.current) {
      twinGroupRef.current.position.y = 1.35 + Math.sin(t * 1.5) * 0.03;
    }
  });

  const selectedDefect = defects.find((d) => d.id === selectedDefectId) || defects[0];

  const handleDefectClick = (defect: DefectMarker3D) => {
    selectDefect(defect.id);
    selectEvidence(defect.evidenceId);
  };

  const handleInspectEvidence = (evidenceId: number) => {
    selectEvidence(evidenceId);
    setActiveStation('EVIDENCE_CAROUSEL');
    setActiveTab('EVIDENCE');
  };

  const holotankPos = DISPUTE_STATIONS.DEFECT_HOLOTANK.relativePosition;

  return (
    <group
      name="DefectHolotankStation"
      position={holotankPos}
      ref={holotankGroupRef}
    >
      {/* Station Title Marker */}
      <Text
        position={[0, 3.4, 0]}
        fontSize={0.24}
        color={DISPUTE_THEME.cyberCyan}
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/outfit/v11/QEUw-pXakupjh6eODBs.woff"
      >
        3D DEFECT COORDINATE HOLOTANK
      </Text>
      <Text
        position={[0, 3.12, 0]}
        fontSize={0.12}
        color={DISPUTE_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Real-Time Spatial Damage Twin • Interactive Defect Pins
      </Text>

      {/* Heavy Cylindrical Obsidian Pedestal */}
      <mesh position={[0, 0.3, 0]} receiveShadow castShadow>
        <cylinderGeometry args={[2.5, 2.7, 0.6, 32]} />
        <meshStandardMaterial
          color="#0f0202"
          roughness={0.7}
          metalness={0.8}
        />
      </mesh>

      {/* Metallic Holo Emitting Ring */}
      <mesh position={[0, 0.61, 0]}>
        <ringGeometry args={[1.8, 2.45, 32]} />
        <meshStandardMaterial
          color={DISPUTE_THEME.primary}
          emissive={DISPUTE_THEME.primary}
          emissiveIntensity={0.6}
          roughness={0.3}
          metalness={0.9}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* Rotating Cyber Scan Ring */}
      <mesh
        ref={scanRingRef}
        position={[0, 0.63, 0]}
        rotation={[-Math.PI / 2, 0, 0]}
      >
        <ringGeometry args={[0.3, 2.2, 32]} />
        <meshBasicMaterial
          color={DISPUTE_THEME.cyberCyan}
          wireframe
          transparent
          opacity={0.35}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* Hologram Emitter Grid Base Disc */}
      <mesh position={[0, 0.62, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <circleGeometry args={[2.3, 32]} />
        <meshBasicMaterial
          color="#1e293b"
          wireframe
          transparent
          opacity={0.4}
        />
      </mesh>

      {/* Floating Holographic EV Wireframe Model (Twin) */}
      <group ref={twinGroupRef} position={[0, 1.35, 0]}>
        {/* Main Chassis Box */}
        <mesh position={[0, 0, 0]}>
          <boxGeometry args={[1.7, 0.45, 3.8]} />
          <meshStandardMaterial
            color={DISPUTE_THEME.cyberCyan}
            wireframe
            emissive={DISPUTE_THEME.cyberCyan}
            emissiveIntensity={0.5}
            transparent
            opacity={0.7}
          />
        </mesh>

        {/* Cabin Glass Dome */}
        <mesh position={[0, 0.38, -0.2]}>
          <boxGeometry args={[1.4, 0.4, 1.9]} />
          <meshStandardMaterial
            color="#38bdf8"
            wireframe
            emissive="#0284c7"
            emissiveIntensity={0.6}
            transparent
            opacity={0.6}
          />
        </mesh>

        {/* Wheels (Wireframe Toruses) */}
        {[
          [-0.9, -0.15, 1.1],
          [0.9, -0.15, 1.1],
          [-0.9, -0.15, -1.1],
          [0.9, -0.15, -1.1],
        ].map(([wx, wy, wz], idx) => (
          <mesh key={idx} position={[wx, wy, wz]} rotation={[0, 0, Math.PI / 2]}>
            <cylinderGeometry args={[0.32, 0.32, 0.2, 16]} />
            <meshStandardMaterial
              color="#0284c7"
              wireframe
              transparent
              opacity={0.5}
            />
          </mesh>
        ))}

        {/* Front Grill / Bumper Accent */}
        <mesh position={[0, -0.05, -1.92]}>
          <boxGeometry args={[1.5, 0.2, 0.1]} />
          <meshStandardMaterial
            color={DISPUTE_THEME.alertRed}
            emissive={DISPUTE_THEME.alertRed}
            emissiveIntensity={0.8}
            wireframe
          />
        </mesh>

        {/* Rear Accent */}
        <mesh position={[0, 0.05, 1.92]}>
          <boxGeometry args={[1.5, 0.15, 0.1]} />
          <meshStandardMaterial
            color="#ef4444"
            emissive="#ef4444"
            emissiveIntensity={0.7}
            wireframe
          />
        </mesh>

        {/* Spatial 3D Defect Markers Pinned to Real Coordinates */}
        {defects.map((defect) => {
          const isSelected = defect.id === selectedDefectId;
          const isHovered = defect.id === hoveredDefectId;
          const markerColor =
            defect.severity === 'CRITICAL'
              ? DISPUTE_THEME.primary
              : defect.severity === 'MODERATE'
              ? DISPUTE_THEME.secondary
              : DISPUTE_THEME.cyberCyan;

          return (
            <group
              key={defect.id}
              position={defect.position}
              onClick={(e) => {
                e.stopPropagation();
                handleDefectClick(defect);
              }}
              onPointerOver={(e) => {
                e.stopPropagation();
                setHoveredDefectId(defect.id);
                document.body.style.cursor = 'pointer';
              }}
              onPointerOut={() => {
                setHoveredDefectId(null);
                document.body.style.cursor = 'auto';
              }}
            >
              {/* Vertical Guide Line down to exact mesh surface */}
              <mesh position={[0, 0.25, 0]}>
                <cylinderGeometry args={[0.015, 0.015, 0.5, 8]} />
                <meshBasicMaterial
                  color={markerColor}
                  transparent
                  opacity={0.8}
                />
              </mesh>

              {/* Pulsing Pin Head */}
              <mesh position={[0, 0.5, 0]}>
                <sphereGeometry args={[isSelected ? 0.09 : 0.065, 16, 16]} />
                <meshStandardMaterial
                  color={markerColor}
                  emissive={markerColor}
                  emissiveIntensity={isSelected || isHovered ? 1.5 : 0.8}
                  roughness={0.2}
                />
              </mesh>

              {/* Holographic Pulse Ring */}
              <mesh position={[0, 0.5, 0]} rotation={[Math.PI / 2, 0, 0]}>
                <ringGeometry
                  args={[
                    isSelected ? 0.11 : 0.08,
                    isSelected ? 0.15 : 0.11,
                    16,
                  ]}
                />
                <meshBasicMaterial
                  color={markerColor}
                  transparent
                  opacity={isSelected ? 0.9 : 0.5}
                  side={THREE.DoubleSide}
                />
              </mesh>

              {/* Defect Label Tag in 3D */}
              <group position={[0, 0.75, 0]}>
                <mesh position={[0, 0, -0.01]}>
                  <planeGeometry args={[0.7, 0.22]} />
                  <meshBasicMaterial
                    color="#090101"
                    transparent
                    opacity={0.85}
                  />
                </mesh>
                <Text
                  position={[0, 0.04, 0]}
                  fontSize={0.075}
                  color={markerColor}
                  anchorX="center"
                  anchorY="middle"
                >
                  {defect.id}: {defect.severity}
                </Text>
                <Text
                  position={[0, -0.05, 0]}
                  fontSize={0.055}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                >
                  {defect.label}
                </Text>
              </group>
            </group>
          );
        })}
      </group>

      {/* Floating 3D Defect Inspection Hologram Card (Right Side of Holotank) */}
      {selectedDefect && (
        <group position={[2.5, 1.8, 0]} rotation={[0, -Math.PI / 4, 0]}>
          {/* Card Frame Backing */}
          <mesh position={[0, 0, -0.02]} receiveShadow>
            <planeGeometry args={[2.2, 1.9]} />
            <meshStandardMaterial
              color="#1a0505"
              roughness={0.4}
              metalness={0.8}
              transparent
              opacity={0.92}
            />
          </mesh>

          {/* Border Glow */}
          <mesh position={[0, 0, -0.01]}>
            <planeGeometry args={[2.24, 1.94]} />
            <meshBasicMaterial
              color={
                selectedDefect.severity === 'CRITICAL'
                  ? DISPUTE_THEME.primary
                  : DISPUTE_THEME.secondary
              }
              wireframe
            />
          </mesh>

          {/* Header Title */}
          <Text
            position={[-0.95, 0.76, 0.02]}
            fontSize={0.11}
            color={DISPUTE_THEME.cyberCyan}
            anchorX="left"
            anchorY="middle"
          >
            DEFECT RECORD: {selectedDefect.id}
          </Text>

          {/* Severity Tag */}
          <mesh position={[0.65, 0.76, 0.02]}>
            <planeGeometry args={[0.55, 0.16]} />
            <meshBasicMaterial
              color={
                selectedDefect.severity === 'CRITICAL'
                  ? DISPUTE_THEME.alertRed
                  : DISPUTE_THEME.secondary
              }
            />
          </mesh>
          <Text
            position={[0.65, 0.76, 0.03]}
            fontSize={0.07}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            {selectedDefect.severity}
          </Text>

          {/* Defect Label */}
          <Text
            position={[-0.95, 0.55, 0.02]}
            fontSize={0.1}
            color="#ffffff"
            anchorX="left"
            anchorY="middle"
          >
            {selectedDefect.label}
          </Text>

          {/* 3D Spatial Coordinates */}
          <Text
            position={[-0.95, 0.36, 0.02]}
            fontSize={0.075}
            color={DISPUTE_THEME.cyberCyan}
            anchorX="left"
            anchorY="middle"
          >
            Coordinates: X: {selectedDefect.position[0]}m | Y:{' '}
            {selectedDefect.position[1]}m | Z: {selectedDefect.position[2]}m
          </Text>

          {/* Reporter & Session */}
          <Text
            position={[-0.95, 0.2, 0.02]}
            fontSize={0.07}
            color={DISPUTE_THEME.textMuted}
            anchorX="left"
            anchorY="middle"
          >
            Reported by: {selectedDefect.uploaderName} ({selectedDefect.timestamp})
          </Text>

          {/* Description Text */}
          <Text
            position={[-0.95, -0.05, 0.02]}
            fontSize={0.068}
            color="#f1f5f9"
            anchorX="left"
            anchorY="top"
            maxWidth={1.9}
            lineHeight={1.3}
          >
            "{selectedDefect.description}"
          </Text>

          {/* Linked Evidence Citation */}
          <Text
            position={[-0.95, -0.42, 0.02]}
            fontSize={0.07}
            color={DISPUTE_THEME.secondary}
            anchorX="left"
            anchorY="middle"
          >
            Attached: Evidence File #{selectedDefect.evidenceId} (Verified SHA-256)
          </Text>

          {/* Interactive Button: Jump to Evidence Carousel */}
          <group
            position={[0, -0.68, 0.02]}
            onClick={(e) => {
              e.stopPropagation();
              handleInspectEvidence(selectedDefect.evidenceId);
            }}
            onPointerOver={() => {
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.9, 0.26]} />
              <meshStandardMaterial
                color={DISPUTE_THEME.cyberCyan}
                emissive={DISPUTE_THEME.cyberCyan}
                emissiveIntensity={0.5}
                roughness={0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.08}
              color="#090101"
              anchorX="center"
              anchorY="middle"
            >
              [ 📸 INSPECT EVIDENCE ATTACHMENT ]
            </Text>
          </group>
        </group>
      )}
    </group>
  );
};
