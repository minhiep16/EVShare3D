import React from 'react';
import { Text } from '@react-three/drei';

export const GarageFloor3D: React.FC = () => {
  return (
    <group position={[0, 0, 0]}>
      {/* 1. Main Showroom Floor Disc (24m radius) */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.01, 0]} receiveShadow>
        <circleGeometry args={[23.8, 64]} />
        <meshStandardMaterial
          color="#060913"
          roughness={0.25}
          metalness={0.8}
        />
      </mesh>

      {/* 2. Perimeter Curb Ring */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.04, 0]}>
        <ringGeometry args={[23.5, 23.9, 64]} />
        <meshStandardMaterial
          color="#00e5ff"
          emissive="#00e5ff"
          emissiveIntensity={0.8}
          roughness={0.2}
        />
      </mesh>

      {/* 3. Concentric Showroom Guidance Rings */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
        <ringGeometry args={[14.8, 15.0, 64]} />
        <meshStandardMaterial
          color="#1e293b"
          emissive="#00e5ff"
          emissiveIntensity={0.3}
          roughness={0.5}
        />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
        <ringGeometry args={[6.8, 7.0, 48]} />
        <meshStandardMaterial
          color="#00e5ff"
          emissive="#00e5ff"
          emissiveIntensity={0.4}
          roughness={0.3}
        />
      </mesh>

      {/* 4. Central Circular Pedestal / Hub Centerpiece */}
      <group position={[0, 0.05, 0]}>
        <mesh position={[0, 0.06, 0]}>
          <cylinderGeometry args={[3.2, 3.4, 0.12, 32]} />
          <meshStandardMaterial
            color="#0b1120"
            metalness={0.9}
            roughness={0.2}
          />
        </mesh>
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.13, 0]}>
          <ringGeometry args={[3.1, 3.3, 32]} />
          <meshStandardMaterial
            color="#00e5ff"
            emissive="#00e5ff"
            emissiveIntensity={1.2}
          />
        </mesh>
        {/* Floor Holographic Emblem */}
        <Text
          position={[0, 0.14, 0]}
          rotation={[-Math.PI / 2, 0, 0]}
          fontSize={0.36}
          color="#00e5ff"
          anchorX="center"
          anchorY="middle"
        >
          EVSHARE CENTRAL GARAGE
        </Text>
        <Text
          position={[0, 0.14, 0.45]}
          rotation={[-Math.PI / 2, 0, 0]}
          fontSize={0.16}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          DIGITAL TWIN FLEET & CHARGING HUB
        </Text>
      </group>

      {/* 5. Suspended Neon Luminance Ring at ceiling */}
      <group position={[0, 8.2, 0]}>
        <mesh rotation={[Math.PI / 2, 0, 0]}>
          <torusGeometry args={[12, 0.06, 16, 64]} />
          <meshStandardMaterial
            color="#00e5ff"
            emissive="#00e5ff"
            emissiveIntensity={2.5}
          />
        </mesh>
        <pointLight color="#00e5ff" intensity={15} distance={20} decay={2} position={[0, -0.5, 0]} />
      </group>
    </group>
  );
};
