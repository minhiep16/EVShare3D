import { SceneDefinition } from './sceneTypes';
import { DemoSecurityGateScene } from './DemoSecurityGateScene';
import { DemoShowroomScene } from './DemoShowroomScene';
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
  Component: DemoShowroomScene,
  onEnter: async () => {
    console.info('[Scene:CentralGarage] Showroom floor activated.');
  },
  onExit: async () => {
    console.info('[Scene:CentralGarage] Showroom floor powered down.');
  },
};

export const registerDefaultScenes = (): void => {
  SceneRegistry.registerScene(DEFAULT_SECURITY_CHECKPOINT_SCENE);
  SceneRegistry.registerScene(DEFAULT_GARAGE_SCENE);
};
