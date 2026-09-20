import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDisputeStore } from './useDisputeStore';
import { DISPUTE_STATIONS, DISPUTE_THEME } from './disputeLayout';
import { formatStatusVN } from '@/i18n';

export const FloatingDisputeCrystal3D: React.FC = () => {
  const crystalOuterRef = useRef<THREE.Mesh>(null);
  const crystalInnerRef = useRef<THREE.Mesh>(null);
  const ring1Ref = useRef<THREE.Mesh>(null);
  const ring2Ref = useRef<THREE.Mesh>(null);

  const { activeDispute } = useDisputeStore();

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ESCALATED':
        return '#ff1744'; // Vibrant Crimson
      case 'UNDER_REVIEW':
        return '#f59e0b'; // Amber
      case 'RESOLVED':
        return '#10b981'; // Emerald
      case 'OPEN':
      default:
        return '#ea580c'; // Orange-Red
    }
  };

  const statusColor = getStatusColor(activeDispute.status);

  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (crystalOuterRef.current) {
      crystalOuterRef.current.rotation.y = t * 0.4;
      crystalOuterRef.current.rotation.x = Math.sin(t * 0.3) * 0.2;
      crystalOuterRef.current.position.y = 2.2 + Math.sin(t * 1.5) * 0.08;
    }
    if (crystalInnerRef.current) {
      crystalInnerRef.current.rotation.y = -t * 0.8;
      crystalInnerRef.current.rotation.z = Math.cos(t * 0.5) * 0.3;
      crystalInnerRef.current.position.y = 2.2 + Math.sin(t * 1.5) * 0.08;
    }
    if (ring1Ref.current) {
      ring1Ref.current.rotation.x = Math.PI / 2 + Math.sin(t * 0.6) * 0.2;
      ring1Ref.current.rotation.y = t * 0.5;
    }
    if (ring2Ref.current) {
      ring2Ref.current.rotation.x = Math.PI / 3 + Math.cos(t * 0.4) * 0.2;
      ring2Ref.current.rotation.z = -t * 0.6;
    }
  });

  return (
    <group name="FloatingDisputeCrystal">
      {/* 1. Outer Translucent Polyhedral Crystal */}
      <mesh ref={crystalOuterRef} position={[0, 2.2, 0]}>
        <octahedronGeometry args={[0.72, 0]} />
        <meshStandardMaterial
          color={statusColor}
          emissive={statusColor}
          emissiveIntensity={0.8}
          roughness={0.1}
          metalness={0.9}
          transparent
          opacity={0.75}
        />
      </mesh>

      {/* 2. Inner Concentrated Synaptic Core */}
      <mesh ref={crystalInnerRef} position={[0, 2.2, 0]}>
        <icosahedronGeometry args={[0.38, 0]} />
        <meshStandardMaterial
          color="#ffffff"
          emissive={statusColor}
          emissiveIntensity={1.8}
          roughness={0.1}
        />
      </mesh>

      {/* 3. Orbiting Gyroscopic Energy Rings */}
      <mesh ref={ring1Ref} position={[0, 2.2, 0]}>
        <torusGeometry args={[1.15, 0.02, 16, 64]} />
        <meshBasicMaterial color={statusColor} transparent opacity={0.7} />
      </mesh>
      <mesh ref={ring2Ref} position={[0, 2.2, 0]}>
        <torusGeometry args={[1.35, 0.018, 16, 64]} />
        <meshBasicMaterial color={DISPUTE_THEME.secondary} transparent opacity={0.55} />
      </mesh>

      {/* Crystal Local Point Light */}
      <pointLight position={[0, 2.2, 0]} color={statusColor} intensity={2.2} distance={10} />

      {/* 4. Overhead Floating Status Badge */}
      <group position={[0, 3.4, 0]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[2.8, 0.62]} />
          <meshBasicMaterial color="#0b0202" opacity={0.9} transparent />
        </mesh>
        <lineSegments position={[0, 0, 0.01]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.8, 0.62)]} />
          <lineBasicMaterial color={statusColor} />
        </lineSegments>
        <Text
          position={[0, 0.16, 0.02]}
          fontSize={0.075}
          color={statusColor}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.05}
        >
          TRANH CHẤP #{activeDispute.id} // {formatStatusVN(activeDispute.status)}
        </Text>
        <Text
          position={[0, 0.01, 0.02]}
          fontSize={0.056}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
        >
          {activeDispute.complainantUserName} tranh chấp {activeDispute.respondentUserName}
        </Text>
        <Text
          position={[0, -0.16, 0.02]}
          fontSize={0.048}
          color="#cbd5e1"
          anchorX="center"
          anchorY="middle"
        >
          {activeDispute.title.slice(0, 38)}...
        </Text>
      </group>
    </group>
  );
};
