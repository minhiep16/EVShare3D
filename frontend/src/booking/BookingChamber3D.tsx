import React, { useEffect } from 'react';
import { useFrame, useThree } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { BookingChamberFloor3D } from './BookingChamberFloor3D';
import { VehicleShowcasePedestal3D } from './VehicleShowcasePedestal3D';
import { BookingTerminal3D } from './BookingTerminal3D';
import { FairUsageHoloPillar3D } from './FairUsageHoloPillar3D';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_LAYOUT, BOOKING_CHAMBER_THEME } from './bookingLayout';

export const BookingChamber3D: React.FC = () => {
  const { camera } = useThree();

  const cameraPreset = useBookingStore((s) => s.cameraPreset);
  const fetchTimeline = useBookingStore((s) => s.fetchTimeline);
  const fetchFairUsage = useBookingStore((s) => s.fetchFairUsage);

  const { CAMERA_PRESETS } = BOOKING_CHAMBER_LAYOUT;
  const { CHRONO_CYAN } = BOOKING_CHAMBER_THEME;

  useEffect(() => {
    // Initial data hydration on mount
    fetchTimeline();
    fetchFairUsage();
  }, [fetchTimeline, fetchFairUsage]);

  // Smooth camera glide to active chamber preset
  useFrame((_, delta) => {
    let preset = CAMERA_PRESETS.CHAMBER_OVERVIEW;

    if (cameraPreset === 'TERMINAL_FOCUS') {
      preset = CAMERA_PRESETS.TERMINAL_FOCUS;
    } else if (cameraPreset === 'CALENDAR_FOCUS') {
      preset = CAMERA_PRESETS.CALENDAR_FOCUS;
    } else if (cameraPreset === 'VEHICLE_INSPECTION') {
      preset = CAMERA_PRESETS.VEHICLE_INSPECTION;
    }

    const targetPos = new THREE.Vector3(...preset.position);
    camera.position.lerp(targetPos, delta * 2.5);

    const lookAtTarget = new THREE.Vector3(...preset.target);
    camera.lookAt(lookAtTarget);
  });

  return (
    <group name="BookingChamber3D">
      {/* 1. Chamber Floor & Chronometer Dial */}
      <BookingChamberFloor3D />

      {/* 2. Central Vehicle Showcase Turntable */}
      <VehicleShowcasePedestal3D />

      {/* 3. Master Holographic Booking Terminal */}
      <BookingTerminal3D />

      {/* 4. Fair Usage & Quota Hologram Pillar */}
      <FairUsageHoloPillar3D />
    </group>
  );
};
