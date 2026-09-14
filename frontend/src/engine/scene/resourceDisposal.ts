import * as THREE from 'three';

/**
 * Traverses a Three.js Object3D hierarchy and disposes all geometries,
 * materials, and textures attached to uniforms to prevent WebGL memory leaks.
 */
export const disposeObject3D = (object: THREE.Object3D | null): void => {
  if (!object) return;

  object.traverse((child) => {
    if (child instanceof THREE.Mesh) {
      // 1. Dispose Geometry
      if (child.geometry) {
        child.geometry.dispose();
      }

      // 2. Dispose Material(s)
      if (child.material) {
        if (Array.isArray(child.material)) {
          child.material.forEach((mat) => disposeMaterial(mat));
        } else {
          disposeMaterial(child.material);
        }
      }
    }
  });

  // Remove from parent if still attached
  if (object.parent) {
    object.parent.remove(object);
  }
};

/**
 * Disposes a Three.js Material and all its attached textures
 */
export const disposeMaterial = (material: THREE.Material): void => {
  if (!material) return;

  const mat = material as unknown as Record<string, unknown>;

  // Traverse possible texture properties
  const textureKeys = [
    'map',
    'lightMap',
    'bumpMap',
    'normalMap',
    'specularMap',
    'envMap',
    'alphaMap',
    'roughnessMap',
    'metalnessMap',
    'emissiveMap',
    'clearcoatMap',
    'clearcoatRoughnessMap',
    'clearcoatNormalMap',
    'transmissionMap',
    'thicknessMap',
  ];

  for (const key of textureKeys) {
    const value = mat[key];
    if (value && typeof value === 'object' && 'dispose' in value) {
      (value as { dispose: () => void }).dispose();
    }
  }

  material.dispose();
};
