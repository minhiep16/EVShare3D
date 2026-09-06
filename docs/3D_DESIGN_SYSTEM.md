# EVShare 3D – PURE 3D DESIGN SYSTEM SPECIFICATION

## 1. Visual Aesthetics & Design Language

The design language of EVShare 3D is **Cyber-Industrial Luxury & Automotive Futurism**. It synthesizes clean architectural lines, physically based rendering (PBR), glowing holographic interfaces, and deep obsidian/carbon surfaces with vibrant luminous accents.

---

## 2. 3D Material Palette

Every mesh in the virtual metaverse adheres to standard physically based or custom GLSL shader materials:

| Material Key | Base Three.js Class | Properties | Visual Effect & Usage |
| :--- | :--- | :--- | :--- |
| `GlassPhysical` | `MeshPhysicalMaterial` | `roughness: 0.1`, `transmission: 0.95`, `thickness: 1.2`, `ior: 1.52` | Semi-transparent floating HUD panels, terminal enclosures, and windshields. |
| `CyberMetal` | `MeshStandardMaterial` | `metalness: 0.85`, `roughness: 0.25`, `color: #1a1d24` | Structural beams, pedestal bases, charging station frames. |
| `HologramGlow` | `ShaderMaterial` (Custom) | Vertex wave distortion, Fresnel edge glow, scanline raster | Floating notifications, AI recommendations, contract documents. |
| `EmissiveCyan` | `MeshStandardMaterial` | `emissive: #00e5ff`, `emissiveIntensity: 2.5` | Active interactive triggers, available parking bays, normal states. |
| `EmissiveAmber`| `MeshStandardMaterial` | `emissive: #ffab00`, `emissiveIntensity: 2.2` | Reserved slots, pending actions, caution indicators. |
| `EmissiveRed`  | `MeshStandardMaterial` | `emissive: #ff1744`, `emissiveIntensity: 3.0` | In-use / maintenance status, disputes, error states. |
| `EmissiveGreen`| `MeshStandardMaterial` | `emissive: #00e676`, `emissiveIntensity: 2.5` | Success confirmations, approved votes, fully charged batteries. |
| `CarbonFiber`  | `MeshStandardMaterial` | Normal map with woven micro-texture, `metalness: 0.3`, `roughness: 0.6` | Vehicle trim, high-performance pedestals, terminal backings. |

---

## 3. Spatial Typography & Text Rendering

* **Primary Engine**: `@react-three/drei` `Text` leveraging Signed Distance Field (SDF) glyph rendering for razor-sharp legibility at any camera distance and angle.
* **Typefaces**:
  * **Headings / Status**: `Orbitron` / `Space Grotesk` (futuristic, geometric, clean).
  * **Body / Telemetry Data**: `Inter` / `JetBrains Mono` (high readability for numbers and tabular metrics).
* **Dynamic Textures**: Complex multi-line charts and interactive SVG graphics are rendered to off-screen HTML5 Canvases and projected onto 3D quad meshes via `CanvasTexture`.

---

## 4. Reusable 3D UI Components (`frontend/src/ui3d/`)

Every UI component is a genuine Three.js group composed of 3D meshes, collider volumes, and R3F event listeners.

```text
┌─────────────────────────────────────────────────────────────┐
│                       ThreeDButton                          │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ [Mesh] Beveled Rounded Box (Z: 0.05)                │   │
│   │ [Shader] Emissive Border Stroke                     │   │
│   │ [Text] SDF 3D Text (Z: 0.03 relative to base)       │   │
│   │ [Behavior] Physical Z-depression (-0.02) on click   │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Component Inventory
1. **`ThreeDButton`**:
   * Physical pressable button with depth depression on pointer down, dynamic emissive border on hover, and acoustic click response.
2. **`ThreeDInput`**:
   * 3D terminal slot showing input label, blinking spatial cursor mesh, and dynamic text buffer updated via 3D keyboard or hardware keys.
3. **`ThreeDKeyboard`**:
   * Floating, curved 3D array of individual `ThreeDButton` keys (QWERTY + NumPad + Actions). Pressing keys physically animates the keycap and emits key codes to the active `ThreeDInput`.
4. **`ThreeDPanel`**:
   * Floating frosted glass plate with beveled edges, integrated mounting studs, and soft ambient drop-shadow.
5. **`ThreeDWindow` / `ThreeDModal`**:
   * **NOT an HTML popup**. A physical 3D spatial window that unfolds into the scene using spring-physics scaling, oriented orthogonally to the camera with close/minimize interactive pins.
6. **`ThreeDCard`**:
   * Slanted holographic display card displaying vehicle telemetry, member profiles, or expense summaries.
7. **`ThreeDTerminal`**:
   * Free-standing floor kiosk containing an integrated screen panel, status LEDs, and interactive touch controls.
8. **`ThreeDChart`**:
   * True volumetric data visualizer featuring 3D extruded bars, dynamic ribbon lines, and rotating percentage toruses.
9. **`ThreeDDropdown` & `ThreeDMenu`**:
   * Spatial carousel that fans out concentric selection chips along an arc when activated.
10. **`ThreeDSlider`**:
    * 3D rail track with a glowing cylindrical thumb knob draggable along a constrained spatial axis.
11. **`ThreeDProgress` / `ThreeDIndicator`**:
    * Segmented holographic energy bar or circular fluid meter tracking battery SoC, booking time elapsed, or download progress.
12. **`ThreeDNotification`**:
    * Autonomous floating holographic drone / mini-beacon that glides into the user's peripheral viewport, pulses with context color, and unfolds message text.
13. **`ThreeDPortal`**:
    * Luminous archway or floor disc with swirling vortex particle systems indicating sector destination and access authorization.

---

## 5. 3D Interactive States Lifecycle

Every interactive element implements a uniform 8-state state machine:

```text
        ┌─────────────┐
        │    IDLE     │
        └──────┬──────┘
               │ onPointerOver
               ▼
        ┌─────────────┐
        │    HOVER    │ ◄─── Glow expands, subtle scale (1.05x), audio hum
        └──────┬──────┘
               │ onPointerDown
               ▼
        ┌─────────────┐
        │   ACTIVE    │ ◄─── Physical Z-depression, emissive pulse, click sound
        └──────┬──────┘
               │ onPointerUp (Selected)
               ▼
        ┌─────────────┐
        │  SELECTED   │ ◄─── Persistent highlight ring, camera alignment
        └──────┬──────┘
               │ Async Action Triggered
               ▼
        ┌─────────────┐
        │   LOADING   │ ◄─── Pulsing orb animation, disabled collider
        └──────┬──────┘
               ├─────────────────────────┐
               ▼ (success)               ▼ (failure)
        ┌─────────────┐           ┌─────────────┐
        │   SUCCESS   │           │    ERROR    │
        │ Green Burst │           │  Red Jitter │
        └─────────────┘           └─────────────┘
```

* **Disabled State (`DISABLED`)**: Desaturated matte gray material, zero emissive emission, raycast interactions completely ignored.

---

## 6. Lighting Moods by Sector

* **Central Garage**: Industrial elegance. Soft cool skylights (5500K), focused warm downlights over parking bays (3200K), and vibrant cyan charging neon.
* **Finance Center**: Clean surgical luminescence. Crisp white ambient lighting (6000K) paired with emerald and gold data pipelines.
* **AI Intelligence Center**: Cybernetic deep twilight. Deep indigo ambient shadows punctuated by high-contrast magenta and cobalt lasers.
* **Operations & Service Bay**: High-intensity inspection floods. Overhead fluorescent light tubes with realistic tube flicker and metallic floor reflections.
* **Admin Command Deck**: Cinematic cosmic horizon. Dimmed mood lights with the glowing earth / platform model providing ambient bounce.

---

## 7. 3D Spatial Audio Architecture

* Web Audio API synthesized and sample-based soundscapes positioned via Three.js `PositionalAudio`:
  * `ui_hover.mp3`: Subtle high-frequency chime on hovering interactive objects.
  * `ui_click.mp3`: Tactile ceramic/metallic switch click.
  * `vehicle_door_open.mp3`: Pneumatic latch release when inspecting an EV.
  * `portal_teleport.mp3`: Low-frequency whoosh during sector transitions.
  * `payment_success.mp3`: Harmonic chime on ledger balance credit.
  * `alert_dispute.mp3`: Dual-tone warning pulse in the Dispute Room.
* Global audio toggle and volume sliders readily accessible on the user's 3D wristwatch / companion terminal.
