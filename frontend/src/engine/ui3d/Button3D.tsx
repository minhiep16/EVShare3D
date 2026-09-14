import React from 'react';
import { Button3DProps } from './ui3dTypes';
import { ThreeDButton } from './ThreeDButton';

/**
 * Button3D (Alias for ThreeDButton)
 * Standard WebGL 3D button adhering to docs/3D_DESIGN_SYSTEM.md.
 */
export const Button3D: React.FC<Button3DProps> = (props) => {
  return <ThreeDButton {...props} />;
};
