import React, { useLayoutEffect, useRef } from 'react';
import type { InstancedMesh } from 'three';
import { Color, Matrix4, Vector3 } from 'three';

export interface InstanceTransform {
  position: [number, number, number];
  rotation?: [number, number, number];
  scale?: [number, number, number];
  color?: string;
}

export interface InstancedPropsProps {
  instances: InstanceTransform[];
  geometry: React.ReactNode;
  material: React.ReactNode;
}

const _matrix = new Matrix4();
const _pos = new Vector3();
const _color = new Color();

/**
 * InstancedProps
 * Batches multiple 3D spatial items into a single draw call.
 */
export const InstancedProps: React.FC<InstancedPropsProps> = ({
  instances,
  geometry,
  material,
}) => {
  const meshRef = useRef<InstancedMesh>(null);

  useLayoutEffect(() => {
    if (!meshRef.current) return;

    instances.forEach((inst, i) => {
      _pos.set(...inst.position);
      _matrix.identity().setPosition(_pos);

      if (inst.scale) {
        _matrix.scale(new Vector3(...inst.scale));
      }

      meshRef.current!.setMatrixAt(i, _matrix);

      if (inst.color) {
        _color.set(inst.color);
        meshRef.current!.setColorAt(i, _color);
      }
    });

    meshRef.current.instanceMatrix.needsUpdate = true;
    if (meshRef.current.instanceColor) {
      meshRef.current.instanceColor.needsUpdate = true;
    }
  }, [instances]);

  return (
    <instancedMesh
      ref={meshRef}
      args={[undefined, undefined, instances.length]}
      castShadow
      receiveShadow
    >
      {geometry}
      {material}
    </instancedMesh>
  );
};
