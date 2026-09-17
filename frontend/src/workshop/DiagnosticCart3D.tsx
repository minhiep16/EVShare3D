import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useWorkshopStore } from './useWorkshopStore';
import { WORKSHOP_STATIONS, WORKSHOP_THEME } from './workshopLayout';

export const DiagnosticCart3D: React.FC = () => {
  const {
    subsystems,
    isScanningObd,
    runObdDiagnostic,
    isRepairing,
    repairAllFaults,
    feedbackMessage,
  } = useWorkshopStore();

  const [hoverScan, setHoverScan] = useState(false);
  const [hoverRepair, setHoverRepair] = useState(false);

  const activeFaults = Object.values(subsystems).filter((s) => s.faultCode !== null);

  return (
    <group
      name="DiagnosticCart"
      position={WORKSHOP_STATIONS.DIAGNOSTIC_CART.relativePosition}
    >
      {/* 1. Mobile Tool Cart Frame & Shelves */}
      {/* Cart Base Frame */}
      <mesh position={[0, 0.45, 0]} castShadow>
        <boxGeometry args={[1.1, 0.75, 0.85]} />
        <meshStandardMaterial color="#18181b" metalness={0.7} roughness={0.3} />
      </mesh>

      {/* Cart 4 Corner Castors */}
      {[
        [-0.45, 0.06, -0.35],
        [0.45, 0.06, -0.35],
        [-0.45, 0.06, 0.35],
        [0.45, 0.06, 0.35],
      ].map((cPos, idx) => (
        <mesh key={`castor-${idx}`} position={cPos as [number, number, number]}>
          <cylinderGeometry args={[0.06, 0.06, 0.12, 16]} />
          <meshStandardMaterial color="#27272a" metalness={0.9} />
        </mesh>
      ))}

      {/* Intermediate Tool Tray */}
      <mesh position={[0, 0.75, 0]}>
        <boxGeometry args={[1.15, 0.04, 0.9]} />
        <meshStandardMaterial color={WORKSHOP_THEME.primary} metalness={0.6} />
      </mesh>

      {/* 2. Angled Diagnostic Touchscreen Tablet */}
      <group position={[0, 1.15, 0]} rotation={[-Math.PI * 0.18, 0, 0]}>
        <mesh position={[0, 0, 0]}>
          <boxGeometry args={[1.1, 0.85, 0.08]} />
          <meshStandardMaterial color="#09090b" metalness={0.8} roughness={0.2} />
        </mesh>
        <lineSegments position={[0, 0, 0.045]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(1.1, 0.85)]} />
          <lineBasicMaterial color={WORKSHOP_THEME.primary} />
        </lineSegments>

        {/* Title */}
        <Text
          position={[0, 0.32, 0.05]}
          fontSize={0.062}
          color={WORKSHOP_THEME.primary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.05}
        >
          OBD-II DIAGNOSTIC BENCH
        </Text>
        <Text
          position={[0, 0.22, 0.05]}
          fontSize={0.046}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          CAN BUS: 500 KBPS • ISO-14229 UDS
        </Text>

        {/* Active Fault Readout Box */}
        <mesh position={[0, 0.05, 0.05]}>
          <planeGeometry args={[0.98, 0.26]} />
          <meshBasicMaterial color="#020617" />
        </mesh>
        <Text
          position={[0, 0.12, 0.06]}
          fontSize={0.048}
          color={activeFaults.length > 0 ? '#ef4444' : '#10b981'}
          anchorX="center"
          anchorY="middle"
        >
          {activeFaults.length > 0
            ? `ACTIVE FAULTS DETECTED: ${activeFaults.length}`
            : 'ALL SUBSYSTEMS NOMINAL (0 DTCS)'}
        </Text>
        <Text
          position={[0, 0.01, 0.06]}
          fontSize={0.04}
          color="#cbd5e1"
          anchorX="center"
          anchorY="middle"
        >
          {activeFaults.length > 0
            ? activeFaults.map((f) => `[${f.faultCode}] ${f.name}`).join(' | ')
            : 'No diagnostic trouble codes logged in ECU.'}
        </Text>

        {/* Interactive Button 1: Run Full OBD Diagnostic */}
        <group
          position={[-0.26, -0.18, 0.05]}
          onClick={(e) => {
            e.stopPropagation();
            runObdDiagnostic();
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            setHoverScan(true);
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            setHoverScan(false);
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <boxGeometry args={[0.46, 0.16, 0.03]} />
            <meshStandardMaterial
              color={hoverScan ? '#0284c7' : '#1e293b'}
              emissive={hoverScan ? WORKSHOP_THEME.primary : '#000000'}
              emissiveIntensity={hoverScan ? 0.5 : 0}
            />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.042}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            {isScanningObd ? 'SCANNING...' : '🔍 OBD SCAN'}
          </Text>
        </group>

        {/* Interactive Button 2: Execute Repairs & Clear Faults */}
        <group
          position={[0.26, -0.18, 0.05]}
          onClick={(e) => {
            e.stopPropagation();
            repairAllFaults();
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            setHoverRepair(true);
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            setHoverRepair(false);
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <boxGeometry args={[0.46, 0.16, 0.03]} />
            <meshStandardMaterial
              color={hoverRepair ? '#059669' : '#064e3b'}
              emissive={hoverRepair ? '#10b981' : '#000000'}
              emissiveIntensity={hoverRepair ? 0.6 : 0}
            />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.042}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            {isRepairing ? 'REPAIRING...' : '🛠 OVERHAUL'}
          </Text>
        </group>

        {/* Live Status Message at Bottom of Tablet */}
        <Text
          position={[0, -0.32, 0.05]}
          fontSize={0.036}
          color="#38bdf8"
          anchorX="center"
          anchorY="middle"
          maxWidth={0.96}
        >
          {feedbackMessage || 'System ready.'}
        </Text>
      </group>
    </group>
  );
};
