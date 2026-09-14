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

## 5. 3D Interactive States Lifecycle & Visual State System (08-AC)

Every interactive element implements a uniform 8-state state machine resolved by the central `VisualStateEngine`:

```text
        ┌─────────────┐
        │    IDLE     │ ◄─── Rest state, neutral emission, standard scale
        └──────┬──────┘
               │ onPointerOver
               ▼
        ┌─────────────┐
        │    HOVER    │ ◄─── Levitation (+0.025m), 1.04x scale, glow expansion, UI_HOVER chirp
        └──────┬──────┘
               │ onPointerDown
               ▼
        ┌─────────────┐
        │   ACTIVE    │ ◄─── Tactile Z-depression (-0.018m), emissive pulse (2.5), UI_CLICK snap
        └──────┬──────┘
               │ onPointerUp (Selected)
               ▼
        ┌─────────────┐
        │  SELECTED   │ ◄─── High elevation (+0.035m), 1.06x scale, persistent amber gold glow
        └──────┬──────┘
               │ Async Action Triggered
               ▼
        ┌─────────────┐
        │   LOADING   │ ◄─── Revolving holographic 3D spinner ring, pulsing opacity, WAIT cursor
        └──────┬──────┘
               ├─────────────────────────┐
               ▼ (success)               ▼ (failure)
        ┌─────────────┐           ┌─────────────┐
        │   SUCCESS   │           │    ERROR    │
        │ Emerald     │           │ Crimson     │
        │ Burst       │           │ Jitter      │
        │ (+0.015m,   │           │ (0.02m shake│
        │ 1.08x,      │           │ 35Hz,       │
        │ NOTIF_CHIME)│           │ NOTIF_ERROR)│
        └─────────────┘           └─────────────┘
```

### Shared 8-State Specification Matrix

| Visual State | Elevation $\Delta Z$ | Scale | Emissive | Emissive Color | Cursor | Transition / Easing | Audio Trigger | Interactivity |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`IDLE`** | `0.000m` | `1.00x` | `0.35` | Variant Glow | `POINTER` | $180\text{ms}$ Spring Damp | — | Interactive |
| **`HOVER`** | `+0.025m` | `1.04x` | `1.60` | Variant Glow | `POINTER` | $120\text{ms}$ Spring Damp | `UI_HOVER` | Interactive |
| **`ACTIVE`** | `-0.018m` | `1.00x` | `2.50` | `#ffffff` / Variant | `POINTER` | $60\text{ms}$ Fast Snap | `UI_CLICK` | Interactive |
| **`SELECTED`** | `+0.035m` | `1.06x` | `2.00` | `#ffab00` (Amber) | `POINTER` | $200\text{ms}$ Spring Damp | — | Interactive |
| **`DISABLED`** | `0.000m` | `1.00x` | `0.00` | `#000000` (Matte) | `NOT_ALLOWED`| $150\text{ms}$ Linear Fade | — | **Blocked** |
| **`LOADING`** | `0.000m` | `1.00x` | `0.80` | Variant Glow | `WAIT` | $6.0\text{rad/s}$ Spinner Orbit | — | **Blocked** |
| **`SUCCESS`** | `+0.015m` | `1.08x` | `2.40` | `#00e676` (Emerald)| `POINTER` | $350\text{ms}$ Punch & Settle | `NOTIF_SUCCESS`| Interactive |
| **`ERROR`** | `0.000m` | `1.00x` | `2.40` | `#ff1744` (Crimson)| `POINTER` | $35\text{Hz}$ Harmonic Shake | `NOTIF_ERROR` | Interactive |

### State Priority Hierarchy
When multiple flags are simultaneously set on a component, the `VisualStateEngine` resolves states strictly by precedence:
$$\text{controlledState} \succ \text{DISABLED} \succ \text{LOADING} \succ \text{ACTIVE} \succ \text{SELECTED} \succ \text{HOVER} \succ \text{IDLE}$$

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

---

## 8. Multi-Tier Adaptive Performance & Quality Control (08-AD)

To guarantee fluid 60 FPS performance across diverse hardware without sacrificing visual fidelity on high-end GPUs, the engine implements three calibrated performance profiles managed by `usePerformanceStore`:

| Setting / Metric | `HIGH` Tier (Dedicated GPU) | `MEDIUM` Tier (Integrated GPU / Tablet) | `LOW` Tier (Mobile / Safe Mode) |
| :--- | :--- | :--- | :--- |
| **Target Frame Rate** | 60 FPS | 60 FPS | 30–60 FPS |
| **Device Pixel Ratio (DPR)** | `[1.0, 2.0]` (Native Retina) | `[0.85, 1.5]` (Balanced) | `[0.65, 1.0]` (Clamped) |
| **Shadow Quality** | PCF Soft Shadows (2048x2048) | Standard PCF Shadows (1024x1024) | Shadows Disabled (Ambient Occlusion only) |
| **Anisotropic Filtering** | $16\times$ Anisotropy | $4\times$ Anisotropy | $1\times$ (Bilinear / Trilinear) |
| **Geometry LOD Bias** | $1.0\times$ Full Geometry Detail | $1.2\times$ Moderate Simplification | $1.5\times$ Aggressive Polygon Reduction |
| **Post-Processing** | Full Bloom, SSAO, Motion Blur | Subtle Bloom Only | Disabled |

### Dynamic Degradation Engine
The engine continuously samples render frame times via a moving average buffer ($N=60$). If sustained framerate drops below $28\text{fps}$ for $\ge 3$ consecutive seconds, the engine automatically downgrades the tier (`HIGH -> MEDIUM -> LOW`) and recalculates DPR without triggering a full page reload or interrupting the user.

---

## 9. WebGL Diagnostics, Failure Detection & Safe Mode Recovery (08-AE)

In strict adherence to the **Pure 3D Mandate**, when WebGL encounters critical errors or hardware incompatibility, the application **NEVER silently degrades into a traditional 2D dashboard website**. Instead, the platform deploys a dedicated cybernetic WebGL diagnostics and recovery cockpit:

```text
┌─────────────────────────────────────────────────────────────┐
│                 WEBGL RECOVERY ENGINE                       │
│                                                             │
│  [Status Alert]  CONTEXT_LOST / INIT_FAILED / RENDER_ERROR   │
│  [Diagnostics]   GPU Vendor, Renderer, WebGL2, Max Textures │
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────┐  │
│  │ RETRY INIT (3x)  │  │ SAFE MODE (LOW)  │  │ FULL BOOT │  │
│  └──────────────────┘  └──────────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────────┘
```

1. **Hardware & Capability Probe (`webglDetector.ts`)**:
   * Evaluates WebGL2 and WebGL1 context availability.
   * Queries unmasked GPU vendor and renderer via `WEBGL_debug_renderer_info`.
   * Flags software rasterizers (SwiftShader, llvmpipe, softpipe, VirtualBox).
   * Generates a copyable JSON diagnostic telemetry report.
2. **Context Loss & Restoration Hooks**:
   * Direct listeners on canvas `webglcontextlost` and `webglcontextrestored`.
   * Captures context exhaustion and frees GPU textures and geometries.
3. **React 3D Error Boundary (`ErrorBoundary3D.tsx`)**:
   * Catches runtime shader compilation, buffer overflow, and R3F lifecycle errors.
4. **Safe Mode Profile**:
   * Strips dynamic shadow maps, disables post-processing, clamps DPR to 1.0, and boots the 3D scene in lightweight mode.

---

## 10. Mobile & Tablet 3D Touch Interaction Architecture (08-AF)

EVShare 3D is fully operable on touchscreen devices (smartphones, tablets, iPads) without compromising the spatial 3D paradigm:

1. **Virtual Touch Joystick (`VirtualTouchJoystick.tsx`)**:
   * Positioned on the lower-left corner of the viewport.
   * Computes normalized 360-degree analog vectors `[strafe, forward]`.
   * Maps displacement magnitude to avatar velocity:
     * $|\vec{v}| \le 0.85 \implies$ `WALKING` mode.
     * $|\vec{v}| > 0.85 \implies$ `SPRINTING` mode.
   * Auto-recenters with spring physics on finger release.
2. **Touch Tap Selection Raycasting (`TouchGestureController.tsx`)**:
   * Discriminates rapid taps from camera drags ($t \le 250\text{ms}$ and $\Delta d \le 10\text{px}$).
   * Projects tap coordinates directly into NDC space `[-1, 1]` to trigger 3D raycast selection on vehicles, terminals, and buttons.
3. **Camera Touch Drag & Pinch-to-Zoom**:
   * Single-finger swipe across the right 65% of the viewport orbits or pitches the spatial camera.
   * Two-finger pinch computes inter-touch euclidean distance delta and adjusts camera zoom/FOV smoothly.
4. **Responsive Viewport Adaptation (`ResponsiveViewportController.tsx`)**:
   * Automatically monitors screen aspect ratios ($W/H$).
   * For narrow portrait viewports ($aspect < 1.0$), scales camera vertical FOV up to $1.4\times$ to guarantee that vehicle showrooms and terminals remain completely visible.
   * Clamps DPR to $\le 1.25$ on mobile phones and $\le 1.5$ on tablets to prevent thermal throttling.
