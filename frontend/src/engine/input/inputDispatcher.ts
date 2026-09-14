import { SpatialHitTarget } from './inputTypes';

export interface InteractableCallbacks {
  onClick?: (hit: SpatialHitTarget) => void;
  onPointerOver?: (hit: SpatialHitTarget) => void;
  onPointerOut?: () => void;
  onPointerDown?: (hit: SpatialHitTarget) => void;
  onPointerUp?: () => void;
  onDrag?: (delta: [number, number]) => void;
}

class InputDispatcherClass {
  private interactables = new Map<string, InteractableCallbacks>();
  private actionListeners = new Map<string, Set<(action: string) => void>>();
  private keyTypedListeners = new Set<(char: string) => void>();

  public registerInteractable(id: string, callbacks: InteractableCallbacks): () => void {
    this.interactables.set(id, callbacks);
    return () => {
      this.interactables.delete(id);
    };
  }

  public getInteractable(id: string): InteractableCallbacks | undefined {
    return this.interactables.get(id);
  }

  public dispatchClick(hit: SpatialHitTarget): void {
    const target = this.interactables.get(hit.objectId);
    target?.onClick?.(hit);
  }

  public dispatchPointerOver(hit: SpatialHitTarget): void {
    const target = this.interactables.get(hit.objectId);
    target?.onPointerOver?.(hit);
  }

  public dispatchPointerOut(id: string): void {
    const target = this.interactables.get(id);
    target?.onPointerOut?.();
  }

  public onAction(action: string, callback: (action: string) => void): () => void {
    if (!this.actionListeners.has(action)) {
      this.actionListeners.set(action, new Set());
    }
    this.actionListeners.get(action)?.add(callback);
    return () => {
      this.actionListeners.get(action)?.delete(callback);
    };
  }

  public dispatchAction(action: string): void {
    this.actionListeners.get(action)?.forEach((cb) => cb(action));
    this.actionListeners.get('*')?.forEach((cb) => cb(action));
  }

  public onKeyTyped(callback: (char: string) => void): () => void {
    this.keyTypedListeners.add(callback);
    return () => {
      this.keyTypedListeners.delete(callback);
    };
  }

  public dispatchKeyTyped(char: string): void {
    this.keyTypedListeners.forEach((cb) => cb(char));
  }

  public clear(): void {
    this.interactables.clear();
    this.actionListeners.clear();
    this.keyTypedListeners.clear();
  }
}

export const InputDispatcher = new InputDispatcherClass();
