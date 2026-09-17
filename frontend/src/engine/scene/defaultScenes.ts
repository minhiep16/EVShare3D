import { SceneDefinition } from './sceneTypes';
import { DemoSecurityGateScene } from './DemoSecurityGateScene';
import { CentralGarage3D } from '../../garage/CentralGarage3D';
import { CoOwnershipAmphitheater3D } from '../../ownership/CoOwnershipAmphitheater3D';
import { BookingChamber3D } from '../../booking/BookingChamber3D';
import { FinanceCenter3D } from '../../finance/FinanceCenter3D';
import { SharedFundVault3D } from '../../vault/SharedFundVault3D';
import { DigitalContractRoom3D } from '../../contracts/DigitalContractRoom3D';
import { DecisionChamber3D } from '../../decision/DecisionChamber3D';
import { AIIntelligenceCenter3D } from '../../ai/AIIntelligenceCenter3D';
import { OperationsCenter3D } from '../../operations/OperationsCenter3D';
import { ServiceWorkshop3D } from '../../workshop/ServiceWorkshop3D';
import { DisputeRoom3D } from '../../dispute/DisputeRoom3D';
import { AdminCommandCenter3D } from '../../admin/AdminCommandCenter3D';
import { SceneRegistry } from './SceneRegistry';

export const DEFAULT_SECURITY_CHECKPOINT_SCENE: SceneDefinition = {
  id: 'SECURITY_CHECKPOINT',
  name: 'Security Checkpoint Gateway',
  description: 'Biometric authentication barrier and entry nexus',
  environment: {
    lightingProfile: 'CYBER_NEON',
    backgroundColor: '#05070c',
    fogColor: '#05070c',
    fogNear: 15,
    fogFar: 55,
    ambientIntensity: 0.6,
    ambientColor: '#080d1a',
    keyLightIntensity: 1.8,
    keyLightColor: '#ffffff',
    keyLightPosition: [6, 12, 6],
    fillColor: '#00e5ff',
    fillIntensity: 0.8,
    fillPosition: [-6, 5, -5],
    rimColor: '#00e5ff',
    rimIntensity: 0.5,
    rimPosition: [0, 8, -8],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 3, 7.5],
    target: [0, 1.8, 0],
    fov: 45,
    minDistance: 2,
    maxDistance: 25,
  },
  Component: DemoSecurityGateScene,
  onEnter: async () => {
    console.info('[Scene:SecurityCheckpoint] Initialized security barrier.');
  },
  onExit: async () => {
    console.info('[Scene:SecurityCheckpoint] Deactivated security barrier.');
  },
};

export const DEFAULT_GARAGE_SCENE: SceneDefinition = {
  id: 'CENTRAL_GARAGE',
  name: 'EV Central Garage & Showroom',
  description: 'Digital twin vehicle showroom, dynamic equity rings, and charging stalls',
  environment: {
    lightingProfile: 'CLEAN_DAYLIGHT',
    backgroundColor: '#0c0f17',
    fogColor: '#0c0f17',
    fogNear: 20,
    fogFar: 75,
    ambientIntensity: 0.9,
    ambientColor: '#121824',
    keyLightIntensity: 2.0,
    keyLightColor: '#ffffff',
    keyLightPosition: [10, 16, 10],
    fillColor: '#ffab00',
    fillIntensity: 0.5,
    fillPosition: [-8, 6, -8],
    rimColor: '#ffffff',
    rimIntensity: 0.6,
    rimPosition: [0, 10, -12],
    enableGroundGrid: true,
  },
  camera: {
    position: [7, 5, 8],
    target: [0, 1.2, 0],
    fov: 42,
    minDistance: 3,
    maxDistance: 35,
  },
  Component: CentralGarage3D,
  onEnter: async () => {
    console.info('[Scene:CentralGarage] Showroom floor activated.');
  },
  onExit: async () => {
    console.info('[Scene:CentralGarage] Showroom floor powered down.');
  },
};

export const DEFAULT_CO_OWNERSHIP_HALL_SCENE: SceneDefinition = {
  id: 'CO_OWNERSHIP_HALL',
  name: 'Co-Ownership Amphitheater & Governance Hall',
  description: 'Grand amphitheater with equity core visualization, member pedestals, contract console & rules stela',
  environment: {
    lightingProfile: 'AMBER_WARM',
    backgroundColor: '#040711',
    fogColor: '#040711',
    fogNear: 15,
    fogFar: 60,
    ambientIntensity: 0.8,
    ambientColor: '#140f09',
    keyLightIntensity: 2.2,
    keyLightColor: '#fff5e6',
    keyLightPosition: [6, 14, 8],
    fillColor: '#d4af37',
    fillIntensity: 0.7,
    fillPosition: [-8, 6, -6],
    rimColor: '#ffb300',
    rimIntensity: 0.6,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 8.5, 14.5],
    target: [0, 1.2, 0],
    fov: 42,
    minDistance: 3,
    maxDistance: 40,
  },
  Component: CoOwnershipAmphitheater3D,
  onEnter: async () => {
    console.info('[Scene:CoOwnershipHall] Co-ownership amphitheater initialized.');
  },
  onExit: async () => {
    console.info('[Scene:CoOwnershipHall] Co-ownership amphitheater powered down.');
  },
};

export const DEFAULT_BOOKING_CHAMBER_SCENE: SceneDefinition = {
  id: 'BOOKING_CHAMBER',
  name: 'Booking Chamber & Chrono-Spatial Timeline',
  description: 'Sweeping 3D calendar arc, 24-hour interactive timeline, and autonomous reservation terminal',
  environment: {
    lightingProfile: 'CYBER_NEON',
    backgroundColor: '#040813',
    fogColor: '#040813',
    fogNear: 15,
    fogFar: 65,
    ambientIntensity: 0.85,
    ambientColor: '#081224',
    keyLightIntensity: 2.2,
    keyLightColor: '#ffffff',
    keyLightPosition: [6, 14, 8],
    fillColor: '#00e5ff',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#38bdf8',
    rimIntensity: 0.6,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 3.5, 9.5],
    target: [0, 1.4, 3.0],
    fov: 40,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: BookingChamber3D,
  onEnter: async () => {
    console.info('[Scene:BookingChamber] Chrono-spatial booking chamber activated.');
  },
  onExit: async () => {
    console.info('[Scene:BookingChamber] Chrono-spatial booking chamber powered down.');
  },
};

export const DEFAULT_ENERGY_FINANCE_CENTER_SCENE: SceneDefinition = {
  id: 'ENERGY_FINANCE_CENTER',
  name: 'Energy & Finance Center',
  description: 'Surgical cleanroom financial laboratory, floating expense crystals, cost allocation bars, and contactless payment',
  environment: {
    lightingProfile: 'CYBER_NEON',
    backgroundColor: '#030813',
    fogColor: '#030813',
    fogNear: 15,
    fogFar: 65,
    ambientIntensity: 0.85,
    ambientColor: '#05121f',
    keyLightIntensity: 2.2,
    keyLightColor: '#ffffff',
    keyLightPosition: [6, 14, 8],
    fillColor: '#00e676',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#00e5ff',
    rimIntensity: 0.6,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 9.5, -66.0],
    target: [0, 1.5, -79.0],
    fov: 42,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: FinanceCenter3D,
  onEnter: async () => {
    console.info('[Scene:FinanceCenter] Energy & finance center activated.');
  },
  onExit: async () => {
    console.info('[Scene:FinanceCenter] Energy & finance center powered down.');
  },
};

export const DEFAULT_SHARED_FUND_VAULT_SCENE: SceneDefinition = {
  id: 'SHARED_FUND_VAULT',
  name: 'Shared Fund Vault',
  description: 'Syndicate treasury with cybernetic titanium locking rings, liquid reserve column, and immutable transaction ribbon',
  environment: {
    lightingProfile: 'CYBER_NEON',
    backgroundColor: '#040912',
    fogColor: '#040912',
    fogNear: 15,
    fogFar: 65,
    ambientIntensity: 0.85,
    ambientColor: '#07101e',
    keyLightIntensity: 2.2,
    keyLightColor: '#ffecb3',
    keyLightPosition: [6, 14, 8],
    fillColor: '#ffb300',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#ff8f00',
    rimIntensity: 0.6,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 8.5, 13.5],
    target: [0, 1.5, 0.5],
    fov: 42,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: SharedFundVault3D,
  onEnter: async () => {
    console.info('[Scene:SharedFundVault] Shared fund vault activated.');
  },
  onExit: async () => {
    console.info('[Scene:SharedFundVault] Shared fund vault powered down.');
  },
};

export const DEFAULT_DIGITAL_CONTRACT_ROOM_SCENE: SceneDefinition = {
  id: 'DIGITAL_CONTRACT_ROOM',
  name: 'Digital Contract Room',
  description: 'Executive legal chamber with 3D holographic agreement lectern, biometric signature dais, and version tree',
  environment: {
    lightingProfile: 'CYBER_NEON',
    backgroundColor: '#040813',
    fogColor: '#040813',
    fogNear: 15,
    fogFar: 65,
    ambientIntensity: 0.85,
    ambientColor: '#091224',
    keyLightIntensity: 2.2,
    keyLightColor: '#f1f5f9',
    keyLightPosition: [6, 14, 8],
    fillColor: '#3b82f6',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#60a5fa',
    rimIntensity: 0.6,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 8.5, 13.5],
    target: [0, 1.5, 0.5],
    fov: 42,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: DigitalContractRoom3D,
  onEnter: async () => {
    console.info('[Scene:DigitalContractRoom] Digital contract room activated.');
  },
  onExit: async () => {
    console.info('[Scene:DigitalContractRoom] Digital contract room powered down.');
  },
};

export const DEFAULT_DECISION_CHAMBER_SCENE: SceneDefinition = {
  id: 'DECISION_CHAMBER',
  name: 'Decision Chamber',
  description: 'Parliamentary governance amphitheater with 3D proposal pods, angled voting console, equity weight pillars, quorum liquid gauge, and authoritative stela',
  environment: {
    lightingProfile: 'PARLIAMENT_INDIGO',
    backgroundColor: '#080a14',
    fogColor: '#080a14',
    fogNear: 15,
    fogFar: 65,
    ambientIntensity: 0.9,
    ambientColor: '#1e1b4b',
    keyLightIntensity: 2.2,
    keyLightColor: '#a5b4fc',
    keyLightPosition: [6, 14, 8],
    fillColor: '#818cf8',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#c084fc',
    rimIntensity: 0.7,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 6.8, 9.8],
    target: [0, 1.2, -1.8],
    fov: 42,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: DecisionChamber3D,
  onEnter: async () => {
    console.info('[Scene:DecisionChamber] Decision chamber activated.');
  },
  onExit: async () => {
    console.info('[Scene:DecisionChamber] Decision chamber powered down.');
  },
};

export const DEFAULT_AI_INTELLIGENCE_CENTER_SCENE: SceneDefinition = {
  id: 'AI_INTELLIGENCE_CENTER',
  name: 'AI Mobility Intelligence Center',
  description: 'Neural mobility nexus with gyroscopic AI holosphere, satellite data nodes, advisory recommendation holograms, and anomaly matrix',
  environment: {
    lightingProfile: 'NEURAL_PURPLE',
    backgroundColor: '#070612',
    fogColor: '#070612',
    fogNear: 15,
    fogFar: 65,
    ambientIntensity: 0.9,
    ambientColor: '#1e1438',
    keyLightIntensity: 2.2,
    keyLightColor: '#c084fc',
    keyLightPosition: [6, 14, 8],
    fillColor: '#06b6d4',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#8b5cf6',
    rimIntensity: 0.7,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 6.5, 9.8],
    target: [0, 2.0, 0],
    fov: 42,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: AIIntelligenceCenter3D,
  onEnter: async () => {
    console.info('[Scene:AIIntelligenceCenter] AI mobility intelligence center activated.');
  },
  onExit: async () => {
    console.info('[Scene:AIIntelligenceCenter] AI mobility intelligence center powered down.');
  },
};

export const DEFAULT_OPERATIONS_CENTER_SCENE: SceneDefinition = {
  id: 'OPERATIONS_CENTER',
  name: 'Operations & Logistics Center',
  description: 'Industrial hangar with optical QR scanner kiosk, telemetry dispatch console, curved fleet stela, and incident alerts',
  environment: {
    lightingProfile: 'OPERATIONS_ORANGE',
    backgroundColor: '#0c0a09',
    fogColor: '#0c0a09',
    fogNear: 15,
    fogFar: 65,
    ambientIntensity: 0.85,
    ambientColor: '#270e06',
    keyLightIntensity: 2.2,
    keyLightColor: '#fb923c',
    keyLightPosition: [6, 14, 8],
    fillColor: '#ea580c',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#f97316',
    rimIntensity: 0.7,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 8.5, 12.0],
    target: [0, 1.2, 0],
    fov: 42,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: OperationsCenter3D,
  onEnter: async () => {
    console.info('[Scene:OperationsCenter] Operations and logistics hangar activated.');
  },
  onExit: async () => {
    console.info('[Scene:OperationsCenter] Operations and logistics hangar powered down.');
  },
};

export const DEFAULT_SERVICE_WORKSHOP_SCENE: SceneDefinition = {
  id: 'SERVICE_WORKSHOP',
  name: 'Service Workshop & Heavy Maintenance Bay',
  description: 'Automotive service bay with dual-column hydraulic lift, multi-system diagnostics, spare parts rack, and immutable work order ledger',
  environment: {
    lightingProfile: 'WORKSHOP_STEEL',
    backgroundColor: '#09090b',
    fogColor: '#09090b',
    fogNear: 15,
    fogFar: 60,
    ambientIntensity: 0.9,
    ambientColor: '#18181b',
    keyLightIntensity: 2.4,
    keyLightColor: '#ffffff',
    keyLightPosition: [6, 14, 8],
    fillColor: '#38bdf8',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#f59e0b',
    rimIntensity: 0.6,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 8.5, 12.0],
    target: [0, 1.2, 0],
    fov: 42,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: ServiceWorkshop3D,
  onEnter: async () => {
    console.info('[Scene:ServiceWorkshop] Service workshop activated.');
  },
  onExit: async () => {
    console.info('[Scene:ServiceWorkshop] Service workshop powered down.');
  },
};

export const DEFAULT_DISPUTE_ROOM_SCENE: SceneDefinition = {
  id: 'DISPUTE_ROOM',
  name: 'Dispute Room & Arbitration Chamber',
  description:
    'Central floating crimson dispute crystal, 3D defect coordinate holotank, immutable evidence staging, staff mediation console, and sovereign admin arbitration dais',
  environment: {
    lightingProfile: 'DISPUTE_CRIMSON',
    backgroundColor: '#0c0202',
    fogColor: '#0c0202',
    fogNear: 15,
    fogFar: 60,
    ambientIntensity: 0.95,
    ambientColor: '#1a0404',
    keyLightIntensity: 2.2,
    keyLightColor: '#ffffff',
    keyLightPosition: [6, 14, 8],
    fillColor: '#ff1744',
    fillIntensity: 0.8,
    fillPosition: [-8, 6, -6],
    rimColor: '#fbbf24',
    rimIntensity: 0.6,
    rimPosition: [0, 10, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 8.5, 12.0],
    target: [0, 1.4, 0],
    fov: 42,
    minDistance: 2,
    maxDistance: 35,
  },
  Component: DisputeRoom3D,
  onEnter: async () => {
    console.info('[Scene:DisputeRoom] Dispute room arbitration chamber activated.');
  },
  onExit: async () => {
    console.info('[Scene:DisputeRoom] Dispute room arbitration chamber powered down.');
  },
};

export const DEFAULT_ADMIN_COMMAND_CENTER_SCENE: SceneDefinition = {
  id: 'ADMIN_COMMAND_CENTER',
  name: 'Admin Command Center & Orbital Oversight Deck',
  description:
    'Apex panopticon deck elevated 25m above the metaverse, housing the 7 sovereign conceptual cores (User, Vehicle, Ownership, Booking, Finance, Dispute, System)',
  environment: {
    lightingProfile: 'COMMAND_HORIZON',
    backgroundColor: '#020617',
    fogColor: '#010309',
    fogNear: 20,
    fogFar: 100,
    ambientIntensity: 0.75,
    ambientColor: '#020617',
    keyLightIntensity: 2.5,
    keyLightColor: '#e0f2fe',
    keyLightPosition: [0, 45, 0],
    fillColor: '#0284c7',
    fillIntensity: 0.9,
    fillPosition: [-8, 28, -6],
    rimColor: '#38bdf8',
    rimIntensity: 0.8,
    rimPosition: [0, 35, -10],
    enableGroundGrid: true,
  },
  camera: {
    position: [0, 36, 18],
    target: [0, 26.5, 0],
    fov: 42,
    minDistance: 2,
    maxDistance: 45,
  },
  Component: AdminCommandCenter3D,
  onEnter: async () => {
    console.info('[Scene:AdminCommandCenter] Sovereign orbital command deck activated.');
  },
  onExit: async () => {
    console.info('[Scene:AdminCommandCenter] Sovereign orbital command deck powered down.');
  },
};

export const registerDefaultScenes = (): void => {
  SceneRegistry.registerScene(DEFAULT_SECURITY_CHECKPOINT_SCENE);
  SceneRegistry.registerScene(DEFAULT_GARAGE_SCENE);
  SceneRegistry.registerScene(DEFAULT_CO_OWNERSHIP_HALL_SCENE);
  SceneRegistry.registerScene(DEFAULT_BOOKING_CHAMBER_SCENE);
  SceneRegistry.registerScene(DEFAULT_ENERGY_FINANCE_CENTER_SCENE);
  SceneRegistry.registerScene(DEFAULT_SHARED_FUND_VAULT_SCENE);
  SceneRegistry.registerScene(DEFAULT_DIGITAL_CONTRACT_ROOM_SCENE);
  SceneRegistry.registerScene(DEFAULT_DECISION_CHAMBER_SCENE);
  SceneRegistry.registerScene(DEFAULT_AI_INTELLIGENCE_CENTER_SCENE);
  SceneRegistry.registerScene(DEFAULT_OPERATIONS_CENTER_SCENE);
  SceneRegistry.registerScene(DEFAULT_SERVICE_WORKSHOP_SCENE);
  SceneRegistry.registerScene(DEFAULT_DISPUTE_ROOM_SCENE);
  SceneRegistry.registerScene(DEFAULT_ADMIN_COMMAND_CENTER_SCENE);
};
