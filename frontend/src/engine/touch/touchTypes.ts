export interface TouchDeviceProfile {
  isTouchDevice: boolean;
  isMobileViewport: boolean;
  isTabletViewport: boolean;
  devicePixelRatio: number;
  orientation: 'portrait' | 'landscape';
}

export interface VirtualJoystickState {
  active: boolean;
  origin: [number, number];
  current: [number, number];
  vector: [number, number]; // [x: strafe, y: forward] normalized [-1, 1]
}

export interface TouchTapEvent {
  screenX: number;
  screenY: number;
  ndcX: number;
  ndcY: number;
  timestamp: number;
}
