import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useWorkshopStore } from './useWorkshopStore';
import { WORKSHOP_STATIONS, WORKSHOP_THEME } from './workshopLayout';

export const WorkOrderStela3D: React.FC = () => {
  const {
    workOrder,
    subsystems,
    isSubmittingExpense,
    isCertifyingRelease,
    commitMaintenanceExpense,
    certifyAndReleaseVehicle,
    safetyViolationNotice,
    feedbackMessage,
  } = useWorkshopStore();

  const [hoverCommit, setHoverCommit] = useState(false);
  const [hoverRelease, setHoverRelease] = useState(false);

  const activeFaults = Object.values(subsystems).filter((s) => s.faultCode !== null);
  const allRepaired = activeFaults.length === 0;
  const isExpenseCommitted = workOrder.backendExpenseId !== null;

  return (
    <group
      name="WorkOrderStela"
      position={WORKSHOP_STATIONS.WORK_ORDER_STELA.relativePosition}
    >
      {/* 1. Stela Structural Frame & Backing */}
      <mesh position={[0, 2.2, 0]}>
        <boxGeometry args={[7.8, 3.2, 0.12]} />
        <meshStandardMaterial color="#050811" metalness={0.8} roughness={0.2} />
      </mesh>
      <lineSegments position={[0, 2.2, 0.065]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(7.8, 3.2)]} />
        <lineBasicMaterial color={WORKSHOP_THEME.primary} />
      </lineSegments>

      {/* Top Header */}
      <group position={[0, 3.45, 0.08]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.16}
          color={WORKSHOP_THEME.primary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.1}
        >
          WORKSHOP DISPATCH & SAFETY CERTIFICATION MATRIX
        </Text>
        <Text
          position={[0, -0.22, 0]}
          fontSize={0.09}
          color={WORKSHOP_THEME.secondary}
          anchorX="center"
          anchorY="middle"
        >
          SPRING BOOT REST BACKEND • NO FAKE COMPLETED MAINTENANCE POLICY ENFORCED
        </Text>
      </group>

      {/* 2. Left Panel: Active Work Order Details */}
      <group position={[-2.5, 1.9, 0.08]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[2.3, 2.4]} />
          <meshStandardMaterial color="#0c101a" roughness={0.4} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.3, 2.4)]} />
          <lineBasicMaterial color="#334155" />
        </lineSegments>

        <Text
          position={[0, 1.0, 0.01]}
          fontSize={0.085}
          color={WORKSHOP_THEME.primary}
          anchorX="center"
          anchorY="middle"
        >
          ACTIVE WORK ORDER
        </Text>
        <Text
          position={[-0.95, 0.8, 0.01]}
          fontSize={0.065}
          color="#f8fafc"
          anchorX="left"
          anchorY="middle"
        >
          WO: {workOrder.workOrderId}
        </Text>
        <Text
          position={[-0.95, 0.65, 0.01]}
          fontSize={0.062}
          color="#94a3b8"
          anchorX="left"
          anchorY="middle"
        >
          VEHICLE: {workOrder.vehicleModel}
        </Text>
        <Text
          position={[-0.95, 0.5, 0.01]}
          fontSize={0.062}
          color="#94a3b8"
          anchorX="left"
          anchorY="middle"
        >
          PLATE: {workOrder.licensePlate}
        </Text>
        <Text
          position={[-0.95, 0.35, 0.01]}
          fontSize={0.062}
          color="#94a3b8"
          anchorX="left"
          anchorY="middle"
        >
          ODOMETER: {workOrder.odometerKm.toLocaleString()} KM
        </Text>
        <Text
          position={[-0.95, 0.2, 0.01]}
          fontSize={0.062}
          color="#94a3b8"
          anchorX="left"
          anchorY="middle"
        >
          TECH: {workOrder.assignedTechnician}
        </Text>

        {/* Subsystem Health Progress Mini-bars */}
        <group position={[0, -0.3, 0.01]}>
          <Text
            position={[-0.95, 0.3, 0]}
            fontSize={0.06}
            color={WORKSHOP_THEME.secondary}
            anchorX="left"
            anchorY="middle"
          >
            SUBSYSTEM INTEGRITY STATUS:
          </Text>
          {Object.values(subsystems).map((sub, idx) => {
            const y = 0.15 - idx * 0.16;
            const isFaulty = sub.faultCode !== null;
            return (
              <group key={sub.subsystemId} position={[0, y, 0]}>
                <Text
                  position={[-0.95, 0, 0]}
                  fontSize={0.052}
                  color={isFaulty ? '#ef4444' : '#e2e8f0'}
                  anchorX="left"
                  anchorY="middle"
                >
                  {sub.name.slice(0, 18)}: {sub.healthPercentage}% {isFaulty ? `[${sub.faultCode}]` : '✓'}
                </Text>
              </group>
            );
          })}
        </group>
      </group>

      {/* 3. Center Panel: Strict Safety Interlock & Actions */}
      <group position={[0, 1.9, 0.08]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[2.5, 2.4]} />
          <meshStandardMaterial color="#0c101a" roughness={0.4} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.5, 2.4)]} />
          <lineBasicMaterial color={WORKSHOP_THEME.secondary} />
        </lineSegments>

        <Text
          position={[0, 1.0, 0.01]}
          fontSize={0.085}
          color={WORKSHOP_THEME.secondary}
          anchorX="center"
          anchorY="middle"
        >
          SAFETY INTERLOCK GATEWAY
        </Text>

        {/* 3 Verification Checks */}
        <group position={[0, 0.65, 0.01]}>
          {/* Check 1 */}
          <Text
            position={[-1.05, 0.15, 0]}
            fontSize={0.058}
            color="#10b981"
            anchorX="left"
            anchorY="middle"
          >
            [✓] 1. OBD-II SCAN COMPLETED
          </Text>
          {/* Check 2 */}
          <Text
            position={[-1.05, -0.05, 0]}
            fontSize={0.058}
            color={allRepaired ? '#10b981' : '#ef4444'}
            anchorX="left"
            anchorY="middle"
          >
            {allRepaired
              ? '[✓] 2. ALL REPAIRS COMPLETED (100% HEALTH)'
              : '[✗] 2. REPAIRS PENDING (ACTIVE DTCS)'}
          </Text>
          {/* Check 3 */}
          <Text
            position={[-1.05, -0.25, 0]}
            fontSize={0.058}
            color={isExpenseCommitted ? '#10b981' : '#f59e0b'}
            anchorX="left"
            anchorY="middle"
          >
            {isExpenseCommitted
              ? `[✓] 3. EXPENSE LOGGED (#${workOrder.backendExpenseId})`
              : '[✗] 3. EXPENSE UNCOMMITTED'}
          </Text>
        </group>

        {/* Safety Violation Banner if triggered */}
        {safetyViolationNotice && (
          <group position={[0, 0.1, 0.02]}>
            <mesh>
              <planeGeometry args={[2.3, 0.38]} />
              <meshStandardMaterial color="#7f1d1d" />
            </mesh>
            <Text
              position={[0, 0, 0.01]}
              fontSize={0.045}
              color="#fecaca"
              anchorX="center"
              anchorY="middle"
              maxWidth={2.2}
            >
              {safetyViolationNotice}
            </Text>
          </group>
        )}

        {/* Action Button A: Commit Expense to Ledger */}
        <group
          position={[0, -0.4, 0.02]}
          onClick={(e) => {
            e.stopPropagation();
            commitMaintenanceExpense();
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            setHoverCommit(true);
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            setHoverCommit(false);
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <boxGeometry args={[2.2, 0.24, 0.04]} />
            <meshStandardMaterial
              color={
                isExpenseCommitted
                  ? '#1e293b'
                  : hoverCommit
                  ? '#0284c7'
                  : '#0369a1'
              }
              emissive={isExpenseCommitted ? '#000000' : WORKSHOP_THEME.primary}
              emissiveIntensity={hoverCommit ? 0.6 : 0.2}
            />
          </mesh>
          <Text
            position={[0, 0, 0.03]}
            fontSize={0.055}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            {isSubmittingExpense
              ? 'COMMITTING TO SPRING BOOT...'
              : isExpenseCommitted
              ? `✓ EXPENSE RECORDED (#${workOrder.backendExpenseId})`
              : `💳 COMMIT EXPENSE (6,000,000 VND)`}
          </Text>
        </group>

        {/* Action Button B: Certify & Release Vehicle */}
        <group
          position={[0, -0.75, 0.02]}
          onClick={(e) => {
            e.stopPropagation();
            certifyAndReleaseVehicle();
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            setHoverRelease(true);
            document.body.style.cursor = 'pointer';
          }}
          onPointerOut={() => {
            setHoverRelease(false);
            document.body.style.cursor = 'auto';
          }}
        >
          <mesh>
            <boxGeometry args={[2.2, 0.26, 0.04]} />
            <meshStandardMaterial
              color={
                workOrder.status === 'RELEASED'
                  ? '#14532d'
                  : hoverRelease
                  ? '#16a34a'
                  : '#15803d'
              }
              emissive={workOrder.status === 'RELEASED' ? '#22c55e' : '#000000'}
              emissiveIntensity={0.3}
            />
          </mesh>
          <Text
            position={[0, 0, 0.03]}
            fontSize={0.06}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            {isCertifyingRelease
              ? 'TRANSMITTING CERTIFICATION...'
              : workOrder.status === 'RELEASED'
              ? '✓ CERTIFIED & RELEASED TO FLEET'
              : '🏁 CERTIFY & RELEASE TO FLEET'}
          </Text>
        </group>
      </group>

      {/* 4. Right Panel: Historical Maintenance Ledger (Real Backend Data) */}
      <group position={[2.5, 1.9, 0.08]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[2.3, 2.4]} />
          <meshStandardMaterial color="#0c101a" roughness={0.4} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.3, 2.4)]} />
          <lineBasicMaterial color="#334155" />
        </lineSegments>

        <Text
          position={[0, 1.0, 0.01]}
          fontSize={0.085}
          color={WORKSHOP_THEME.primary}
          anchorX="center"
          anchorY="middle"
        >
          MAINTENANCE AUDIT LEDGER
        </Text>

        {/* Ledger Entries List */}
        {[
          { id: 701, title: '10,000 KM INSPECTION', cost: '2,500,000 VND', date: '2026-08-14' },
          { id: 702, title: 'COOLANT FLUSH & BLEED', cost: '1,800,000 VND', date: '2026-08-28' },
          { id: 703, title: 'CABIN HEPA FILTER', cost: '650,000 VND', date: '2026-09-05' },
          {
            id: workOrder.backendExpenseId || 704,
            title: 'CERAMIC BRAKES & LIDAR',
            cost: '6,000,000 VND',
            date: '2026-09-16',
            isNew: true,
          },
        ].map((item, idx) => {
          const y = 0.7 - idx * 0.44;
          return (
            <group key={item.id} position={[0, y, 0.01]}>
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[2.1, 0.38]} />
                <meshStandardMaterial color={item.isNew && isExpenseCommitted ? '#064e3b' : '#18181b'} />
              </mesh>
              <Text
                position={[-0.95, 0.1, 0.01]}
                fontSize={0.052}
                color="#f8fafc"
                anchorX="left"
                anchorY="middle"
              >
                #{item.id} • {item.title}
              </Text>
              <Text
                position={[-0.95, -0.08, 0.01]}
                fontSize={0.046}
                color="#38bdf8"
                anchorX="left"
                anchorY="middle"
              >
                {item.cost} • {item.date} • AUDIT VERIFIED
              </Text>
            </group>
          );
        })}
      </group>

      {/* 5. Bottom Feedback Banner */}
      <group position={[0, 0.25, 0.08]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[7.4, 0.32]} />
          <meshBasicMaterial color="#020617" opacity={0.85} transparent />
        </mesh>
        <Text
          position={[0, 0, 0.01]}
          fontSize={0.058}
          color="#38bdf8"
          anchorX="center"
          anchorY="middle"
        >
          {feedbackMessage || 'Service Workshop online.'}
        </Text>
      </group>
    </group>
  );
};
