import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useOperationsStore } from './useOperationsStore';
import { OPERATIONS_STATIONS, OPERATIONS_THEME } from './operationsLayout';

export const OperationalNotifications3D: React.FC = () => {
  const { alerts, acknowledgeAlert, dismissAllAlerts } = useOperationsStore();
  const [hoveredAlertId, setHoveredAlertId] = useState<string | null>(null);
  const [hoverDismissAll, setHoverDismissAll] = useState(false);

  const getSeverityColor = (sev: string) => {
    switch (sev) {
      case 'CRITICAL':
        return '#ef4444';
      case 'WARNING':
        return '#f59e0b';
      case 'INFO':
      default:
        return '#06b6d4';
    }
  };

  const activeCount = alerts.filter((a) => !a.isAcknowledged).length;

  return (
    <group
      name="OperationalNotifications"
      position={OPERATIONS_STATIONS.NOTIFICATION_BOARD.relativePosition}
    >
      {/* 1. Pedestal / Floating Support Column */}
      <mesh position={[0, 0.6, 0]} castShadow>
        <cylinderGeometry args={[0.2, 0.3, 1.2, 16]} />
        <meshStandardMaterial color="#0f172a" metalness={0.7} roughness={0.3} />
      </mesh>
      <mesh position={[0, 0.05, 0]}>
        <cylinderGeometry args={[0.4, 0.45, 0.1, 16]} />
        <meshStandardMaterial
          color={OPERATIONS_THEME.primary}
          emissive={OPERATIONS_THEME.primary}
          emissiveIntensity={0.5}
        />
      </mesh>

      {/* 2. Holographic Board Backing */}
      <group position={[0, 1.95, 0]}>
        <mesh position={[0, 0, 0]}>
          <boxGeometry args={[3.2, 2.2, 0.06]} />
          <meshStandardMaterial
            color="#050811"
            metalness={0.8}
            roughness={0.2}
          />
        </mesh>
        <lineSegments position={[0, 0, 0.035]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(3.2, 2.2)]} />
          <lineBasicMaterial color={OPERATIONS_THEME.primary} />
        </lineSegments>

        {/* Board Title & Active Badge */}
        <group position={[0, 0.9, 0.04]}>
          <Text
            position={[-0.45, 0, 0]}
            fontSize={0.095}
            color={OPERATIONS_THEME.primary}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.06}
          >
            OPERATIONAL ALERTS STREAM
          </Text>
          {/* Active Count Pill */}
          <mesh position={[0.95, 0, 0]}>
            <planeGeometry args={[0.82, 0.18]} />
            <meshStandardMaterial
              color={activeCount > 0 ? '#b91c1c' : '#15803d'}
            />
          </mesh>
          <Text
            position={[0.95, 0, 0.01]}
            fontSize={0.065}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            {activeCount > 0 ? `${activeCount} ACTIVE` : 'ALL CLEAR'}
          </Text>
        </group>

        {/* 3. Alert Cards Stack (up to 3 items) */}
        {alerts.slice(0, 3).map((alert, idx) => {
          const y = 0.55 - idx * 0.52;
          const isHovered = hoveredAlertId === alert.id;
          const sevColor = getSeverityColor(alert.severity);

          return (
            <group key={alert.id} position={[0, y, 0.04]}>
              {/* Alert Card Plate */}
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[2.9, 0.44]} />
                <meshStandardMaterial
                  color={
                    alert.isAcknowledged
                      ? '#090d16'
                      : isHovered
                      ? '#1e293b'
                      : '#0f172a'
                  }
                  roughness={0.5}
                />
              </mesh>
              <lineSegments position={[0, 0, 0.005]}>
                <edgesGeometry args={[new THREE.PlaneGeometry(2.9, 0.44)]} />
                <lineBasicMaterial
                  color={alert.isAcknowledged ? '#334155' : sevColor}
                />
              </lineSegments>

              {/* Severity Pill */}
              <mesh position={[-1.15, 0.12, 0.01]}>
                <planeGeometry args={[0.42, 0.12]} />
                <meshStandardMaterial color={sevColor} />
              </mesh>
              <Text
                position={[-1.15, 0.12, 0.02]}
                fontSize={0.055}
                color="#000000"
                anchorX="center"
                anchorY="middle"
              >
                {alert.severity}
              </Text>

              {/* Title & Target */}
              <Text
                position={[-0.85, 0.12, 0.01]}
                fontSize={0.065}
                color="#f8fafc"
                anchorX="left"
                anchorY="middle"
              >
                {alert.title}
              </Text>

              {/* Message */}
              <Text
                position={[-1.35, -0.06, 0.01]}
                fontSize={0.055}
                color={alert.isAcknowledged ? '#64748b' : '#cbd5e1'}
                anchorX="left"
                anchorY="middle"
                maxWidth={2.0}
              >
                {alert.message}
              </Text>

              {/* Acknowledge Button */}
              <group
                position={[1.05, 0, 0.01]}
                onClick={(e) => {
                  e.stopPropagation();
                  acknowledgeAlert(alert.id);
                }}
                onPointerOver={(e) => {
                  e.stopPropagation();
                  setHoveredAlertId(alert.id);
                  document.body.style.cursor = 'pointer';
                }}
                onPointerOut={() => {
                  setHoveredAlertId(null);
                  document.body.style.cursor = 'auto';
                }}
              >
                <mesh>
                  <planeGeometry args={[0.65, 0.22]} />
                  <meshStandardMaterial
                    color={alert.isAcknowledged ? '#1e293b' : '#0369a1'}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.052}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                >
                  {alert.isAcknowledged ? '✓ ACK' : 'ACKNOWLEDGE'}
                </Text>
              </group>
            </group>
          );
        })}

        {/* 4. Bottom Dismiss All Alerts Button */}
        <group
          position={[0, -0.88, 0.04]}
          onClick={(e) => {
            e.stopPropagation();
            dismissAllAlerts();
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            setHoverDismissAll(true);
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            setHoverDismissAll(false);
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <planeGeometry args={[2.8, 0.2]} />
            <meshStandardMaterial
              color={hoverDismissAll ? '#ea580c' : '#1e293b'}
            />
          </mesh>
          <Text
            position={[0, 0, 0.01]}
            fontSize={0.062}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            🗙 ACKNOWLEDGE ALL OPERATIONAL ALERTS
          </Text>
        </group>
      </group>
    </group>
  );
};
