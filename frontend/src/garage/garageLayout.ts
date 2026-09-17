import type { ParkingBay, ChargingStation } from './garageTypes';

export const GARAGE_PARKING_BAYS: ParkingBay[] = [
  {
    id: 'BAY-01',
    label: 'STALL 01',
    position: [-9.5, 0, -6],
    rotation: [0, Math.PI / 4, 0],
    type: 'PARKING',
    isOccupied: false,
  },
  {
    id: 'BAY-02',
    label: 'STALL 02',
    position: [-11.5, 0, 3],
    rotation: [0, Math.PI / 2, 0],
    type: 'PARKING',
    isOccupied: false,
  },
  {
    id: 'BAY-03',
    label: 'STALL 03',
    position: [-9.5, 0, 12],
    rotation: [0, (3 * Math.PI) / 4, 0],
    type: 'PARKING',
    isOccupied: false,
  },
  {
    id: 'BAY-04',
    label: 'STALL 04',
    position: [9.5, 0, -6],
    rotation: [0, -Math.PI / 4, 0],
    type: 'PARKING',
    isOccupied: false,
  },
  {
    id: 'BAY-05',
    label: 'CHG BAY 01',
    position: [11.5, 0, 3],
    rotation: [0, -Math.PI / 2, 0],
    type: 'CHARGING',
    isOccupied: false,
  },
  {
    id: 'BAY-06',
    label: 'CHG BAY 02',
    position: [9.5, 0, 12],
    rotation: [0, -(3 * Math.PI) / 4, 0],
    type: 'CHARGING',
    isOccupied: false,
  },
];

export const GARAGE_CHARGING_STATIONS: ChargingStation[] = [
  {
    id: 'CHG-01',
    stallId: 'BAY-05',
    position: [14.0, 0, 3],
    rotation: [0, -Math.PI / 2, 0],
    maxPowerKw: 250,
    currentPowerKw: 150,
    status: 'CHARGING',
    connectedVehicleId: 5,
  },
  {
    id: 'CHG-02',
    stallId: 'BAY-06',
    position: [12.0, 0, 12],
    rotation: [0, -(3 * Math.PI) / 4, 0],
    maxPowerKw: 250,
    currentPowerKw: 0,
    status: 'IDLE',
  },
];
