import React from 'react';
import type { CoOwnershipGroup } from './ownershipTypes';
import { AmphitheaterFloor3D } from './AmphitheaterFloor3D';
import { EquityDistributionRing3D } from './EquityDistributionRing3D';

interface AmphitheaterCentralDais3DProps {
  group: CoOwnershipGroup;
}

export const AmphitheaterCentralDais3D: React.FC<AmphitheaterCentralDais3DProps> = ({ group }) => {
  return (
    <group name="AmphitheaterCentralDais3D">
      {/* Amphitheater Floor & Tiered Architecture */}
      <AmphitheaterFloor3D />

      {/* Holographic 3D Equity Torus & Statistics Floating Above Central Dais */}
      <EquityDistributionRing3D group={group} position={[0, 1.2, 0]} />
    </group>
  );
};
