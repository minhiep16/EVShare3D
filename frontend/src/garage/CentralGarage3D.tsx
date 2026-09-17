import React, { useEffect } from 'react';
import { GarageFloor3D } from './GarageFloor3D';
import { ParkingBay3D } from './ParkingBay3D';
import { ChargingStation3D } from './ChargingStation3D';
import { DigitalTwinVehicle3D } from './DigitalTwinVehicle3D';
import { VehicleInspectionTerminal3D } from './VehicleInspectionTerminal3D';
import { useGarageStore } from './useGarageStore';

export const CentralGarage3D: React.FC = () => {
  const vehicles = useGarageStore((s) => s.vehicles);
  const parkingBays = useGarageStore((s) => s.parkingBays);
  const chargingStations = useGarageStore((s) => s.chargingStations);
  const fetchGarageData = useGarageStore((s) => s.fetchGarageData);

  useEffect(() => {
    // Fetch live backend vehicles and ownership groups on mount
    fetchGarageData();
  }, [fetchGarageData]);

  return (
    <group name="CentralGarage3D">
      {/* 1. Showroom & Hub Floor */}
      <GarageFloor3D />

      {/* 2. Parking Stall Bays */}
      {parkingBays.map((bay) => (
        <ParkingBay3D key={bay.id} bay={bay} />
      ))}

      {/* 3. Fast Supercharger Stations */}
      {chargingStations.map((station) => (
        <ChargingStation3D key={station.id} station={station} />
      ))}

      {/* 4. Digital Twin Vehicles */}
      {vehicles.map((vehicle) => (
        <DigitalTwinVehicle3D key={vehicle.id} vehicle={vehicle} />
      ))}

      {/* 5. In-World Holographic Inspection Terminal for Selected Vehicle */}
      <VehicleInspectionTerminal3D />
    </group>
  );
};
