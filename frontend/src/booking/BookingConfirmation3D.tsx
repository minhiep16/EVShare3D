import React from 'react';
import { Text } from '@react-three/drei';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_THEME } from './bookingLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const BookingConfirmation3D: React.FC = () => {
  const vehicles = useBookingStore((s) => s.vehicles);
  const selectedVehicleId = useBookingStore((s) => s.selectedVehicleId);
  const selectedDate = useBookingStore((s) => s.selectedDate);
  const startHour = useBookingStore((s) => s.startHour);
  const endHour = useBookingStore((s) => s.endHour);
  const durationHours = useBookingStore((s) => s.durationHours);
  const estimatedCostVnd = useBookingStore((s) => s.estimatedCostVnd);
  const isSubmitting = useBookingStore((s) => s.isSubmitting);
  const submissionError = useBookingStore((s) => s.submissionError);

  const submitBooking = useBookingStore((s) => s.submitBooking);
  const setActiveStep = useBookingStore((s) => s.setActiveStep);

  const { CHRONO_CYAN, AVAILABLE_GREEN, BUFFER_PURPLE, CONFLICT_RED } = BOOKING_CHAMBER_THEME;

  const vehicle = vehicles.find((v) => v.id === selectedVehicleId) || vehicles[0];

  const startTimeStr = `${startHour.toString().padStart(2, '0')}:00`;
  const endTimeStr = `${endHour.toString().padStart(2, '0')}:00`;
  const bufferTimeStr = `${endHour.toString().padStart(2, '0')}:30`;

  return (
    <group name="BookingConfirmation3D" position={[0, 0, 0]}>
      {/* 1. Header Title */}
      <group position={[0, 1.25, 0]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.13}
          color={CHRONO_CYAN}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          CONFIRM VEHICLE RESERVATION
        </Text>
        <Text
          position={[0, -0.16, 0]}
          fontSize={0.075}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          Review scheduled interval, turnaround buffer, and syndicate billing authorization
        </Text>
      </group>

      {/* 2. Structured Information Grid */}
      <group position={[-1.75, 0.75, 0.02]}>
        {/* Row 1: Vehicle */}
        <Text position={[0, 0, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          VEHICLE TARGET:
        </Text>
        <Text position={[1.4, 0, 0]} fontSize={0.085} color="#ffffff" anchorX="left" anchorY="middle">
          {`${vehicle.manufacturer} ${vehicle.modelName} (${vehicle.licensePlate})`}
        </Text>

        {/* Row 2: Date */}
        <Text position={[0, -0.18, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          RESERVATION DATE:
        </Text>
        <Text position={[1.4, -0.18, 0]} fontSize={0.085} color={CHRONO_CYAN} anchorX="left" anchorY="middle">
          {selectedDate}
        </Text>

        {/* Row 3: Time Interval */}
        <Text position={[0, -0.36, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          ACCESS WINDOW:
        </Text>
        <Text position={[1.4, -0.36, 0]} fontSize={0.085} color="#f8fafc" anchorX="left" anchorY="middle">
          {`${startTimeStr} – ${endTimeStr} (${durationHours} Hours)`}
        </Text>

        {/* Row 4: Mandatory Buffer Notice */}
        <Text position={[0, -0.54, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          TURNAROUND BUFFER:
        </Text>
        <Text position={[1.4, -0.54, 0]} fontSize={0.075} color={BUFFER_PURPLE} anchorX="left" anchorY="middle">
          {`Enforced to ${bufferTimeStr} (+30m cleaning/inspection)`}
        </Text>

        {/* Row 5: Estimated Cost */}
        <Text position={[0, -0.72, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          ESTIMATED CHARGE:
        </Text>
        <Text
          position={[1.4, -0.72, 0]}
          fontSize={0.095}
          color={AVAILABLE_GREEN}
          anchorX="left"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {`${(estimatedCostVnd || 0).toLocaleString()} VND`}
        </Text>

        {/* Row 6: Syndicate */}
        <Text position={[0, -0.9, 0]} fontSize={0.075} color="#64748b" anchorX="left" anchorY="middle">
          SYNDICATE GROUP:
        </Text>
        <Text position={[1.4, -0.9, 0]} fontSize={0.08} color="#94a3b8" anchorX="left" anchorY="middle">
          {vehicle.groupName}
        </Text>
      </group>

      {/* Error Message if any */}
      {submissionError && (
        <Text position={[0, -0.4, 0.02]} fontSize={0.075} color={CONFLICT_RED} anchorX="center" anchorY="middle">
          {submissionError}
        </Text>
      )}

      {/* 3. Action Buttons */}
      <group position={[0, -0.65, 0.02]}>
        {/* Back / Adjust Button */}
        <group
          position={[-1.2, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            AudioEngine.play('UI_CLICK');
            setActiveStep('TIME_SELECT');
          }}
        >
          <mesh>
            <boxGeometry args={[1.3, 0.24, 0.03]} />
            <meshStandardMaterial color="#334155" />
          </mesh>
          <Text position={[0, 0, 0.02]} fontSize={0.075} color="#cbd5e1" anchorX="center" anchorY="middle">
            {'◄ ADJUST TIME'}
          </Text>
        </group>

        {/* Confirm Reservation Button */}
        <group
          position={[1.2, 0, 0]}
          onClick={async (e) => {
            e.stopPropagation();
            if (!isSubmitting) {
              AudioEngine.getInstance().playClickSnap();
              await submitBooking();
            }
          }}
        >
          <mesh>
            <boxGeometry args={[1.8, 0.24, 0.04]} />
            <meshStandardMaterial
              color={isSubmitting ? '#0284c7' : '#047857'}
              emissive={isSubmitting ? '#00e5ff' : '#10b981'}
              emissiveIntensity={0.65}
            />
          </mesh>
          <Text
            position={[0, 0, 0.03]}
            fontSize={0.085}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {isSubmitting ? 'CONFIRMING...' : '⚡ CONFIRM RESERVATION'}
          </Text>
        </group>
      </group>
    </group>
  );
};
