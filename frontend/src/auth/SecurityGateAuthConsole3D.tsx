import React from 'react';
import { Text } from '@react-three/drei';
import { useAuthStore } from './useAuthStore';
import { Terminal3D, Button3D, Input3D } from '@/engine/ui3d';

interface SecurityGateAuthConsole3DProps {
  position?: [number, number, number];
  rotation?: [number, number, number];
}

export const SecurityGateAuthConsole3D: React.FC<SecurityGateAuthConsole3DProps> = ({
  position = [-3.6, 0, 1.2],
  rotation = [0, Math.PI / 4, 0],
}) => {
  const authMode = useAuthStore((state) => state.authMode);
  const isLoading = useAuthStore((state) => state.isLoading);
  const authError = useAuthStore((state) => state.authError);
  const successNotice = useAuthStore((state) => state.successNotice);

  const email = useAuthStore((state) => state.emailInput);
  const password = useAuthStore((state) => state.passwordInput);
  const fullName = useAuthStore((state) => state.fullNameInput);
  const currentUser = useAuthStore((state) => state.currentUser);
  const isGateUnlocked = useAuthStore((state) => state.isGateUnlocked);

  const setEmail = useAuthStore((state) => state.setEmailInput);
  const setPassword = useAuthStore((state) => state.setPasswordInput);
  const setFullName = useAuthStore((state) => state.setFullNameInput);
  const setAuthMode = useAuthStore((state) => state.setAuthMode);

  const login = useAuthStore((state) => state.login);
  const register = useAuthStore((state) => state.register);
  const logout = useAuthStore((state) => state.logout);
  const fillDemo = useAuthStore((state) => state.fillDemoCredentials);
  const enterWorld = useAuthStore((state) => state.enterWorld);

  const statusLabel = isGateUnlocked ? 'ĐÃ MỞ KHÓA' : 'ĐANG BẢO VỆ';
  const statusVariant = isGateUnlocked ? 'emerald' : 'amber';

  return (
    <Terminal3D
      id="security_gate_auth_terminal"
      title="Bàn Điều Khiển Cổng Xác Thực Sinh Trắc Học"
      statusLabel={statusLabel}
      statusVariant={statusVariant}
      position={position}
      rotation={rotation}
      panelWidth={2.4}
      panelHeight={1.7}
    >
      {/* ─── 1. MODE TABS (LOGIN vs REGISTER) ───────────────────────── */}
      {authMode !== 'AUTHENTICATED' && (
        <group position={[0, 0.58, 0]}>
          <Button3D
            id="tab_login"
            label="ĐĂNG NHẬP"
            width={0.9}
            height={0.18}
            variant={authMode === 'LOGIN' ? 'cyan' : 'dim'}
            position={[-0.52, 0, 0]}
            onClick={() => setAuthMode('LOGIN')}
          />
          <Button3D
            id="tab_register"
            label="ĐĂNG KÝ MỚI"
            width={1.05}
            height={0.18}
            variant={authMode === 'REGISTER' ? 'cyan' : 'dim'}
            position={[0.52, 0, 0]}
            onClick={() => setAuthMode('REGISTER')}
          />
        </group>
      )}

      {/* ─── 2. LOGIN FORM ───────────────────────────────────────────── */}
      {authMode === 'LOGIN' && (
        <group position={[0, 0.1, 0]}>
          <Input3D
            id="auth_login_email"
            label="Tài khoản Email / Tên đăng nhập"
            value={email}
            onChange={setEmail}
            onSubmit={() => login()}
            placeholder="NHẬP EMAIL..."
            width={2.0}
            height={0.22}
            position={[0, 0.28, 0]}
          />

          <Input3D
            id="auth_login_password"
            label="Mật mã sinh trắc học"
            value={password}
            onChange={setPassword}
            onSubmit={() => login()}
            placeholder="NHẬP MẬT MÃ..."
            isPassword
            width={2.0}
            height={0.22}
            position={[0, -0.06, 0]}
          />

          {/* Primary Submit Button */}
          <Button3D
            id="btn_auth_submit"
            label={isLoading ? 'ĐANG XÁC THỰC...' : 'XÁC THỰC TRUY CẬP (ENTER)'}
            width={2.0}
            height={0.24}
            variant="cyan"
            position={[0, -0.36, 0]}
            onClick={() => login()}
          />

          {/* Quick Demo Pre-Fill Row */}
          <group position={[0, -0.58, 0]}>
            <Text
              position={[-0.95, 0, 0.01]}
              fontSize={0.05}
              color="#64748b"
              anchorX="left"
              anchorY="middle"
            >
              TÀI KHOẢN MẪU:
            </Text>
            <Button3D
              id="demo_co_owner"
              label="ĐỒNG SỞ HỮU"
              width={0.48}
              height={0.14}
              variant="amber"
              position={[-0.2, 0, 0]}
              onClick={() => fillDemo('CO_OWNER')}
            />
            <Button3D
              id="demo_staff"
              label="NHÂN VIÊN"
              width={0.42}
              height={0.14}
              variant="amber"
              position={[0.28, 0, 0]}
              onClick={() => fillDemo('STAFF')}
            />
            <Button3D
              id="demo_admin"
              label="QUẢN TRỊ"
              width={0.4}
              height={0.14}
              variant="crimson"
              position={[0.72, 0, 0]}
              onClick={() => fillDemo('ADMIN')}
            />
          </group>
        </group>
      )}

      {/* ─── 3. REGISTRATION FORM ────────────────────────────────────── */}
      {authMode === 'REGISTER' && (
        <group position={[0, 0.15, 0]}>
          <Input3D
            id="auth_reg_name"
            label="Họ và tên đầy đủ"
            value={fullName}
            onChange={setFullName}
            placeholder="NHẬP HỌ TÊN..."
            width={2.0}
            height={0.2}
            position={[0, 0.28, 0]}
          />

          <Input3D
            id="auth_reg_email"
            label="Địa chỉ Email"
            value={email}
            onChange={setEmail}
            placeholder="NHẬP EMAIL..."
            width={2.0}
            height={0.2}
            position={[0, 0.02, 0]}
          />

          <Input3D
            id="auth_reg_password"
            label="Mật khẩu tài khoản"
            value={password}
            onChange={setPassword}
            placeholder="TẠO MẬT KHẨU..."
            isPassword
            width={2.0}
            height={0.2}
            position={[0, -0.24, 0]}
          />

          <Button3D
            id="btn_reg_submit"
            label={isLoading ? 'ĐANG ĐĂNG KÝ...' : 'ĐĂNG KÝ TÀI KHOẢN MỚI'}
            width={2.0}
            height={0.22}
            variant="emerald"
            position={[0, -0.52, 0]}
            onClick={() => register()}
          />
        </group>
      )}

      {/* ─── 4. AUTHENTICATED STATE ──────────────────────────────────── */}
      {authMode === 'AUTHENTICATED' && (
        <group position={[0, 0.15, 0]}>
          {/* Identity Shield Plaque */}
          <mesh position={[0, 0.32, 0]}>
            <boxGeometry args={[2.0, 0.34, 0.02]} />
            <meshStandardMaterial color="#061a14" metalness={0.9} roughness={0.15} />
          </mesh>
          <mesh position={[0, 0.32, 0.012]}>
            <planeGeometry args={[2.02, 0.36]} />
            <meshBasicMaterial color="#00e676" wireframe transparent opacity={0.6} />
          </mesh>

          <Text
            position={[0, 0.4, 0.02]}
            fontSize={0.065}
            color="#00e676"
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            ĐÃ CẤP QUYỀN • DANH TÍNH ĐÃ XÁC THỰC
          </Text>

          <Text
            position={[0, 0.26, 0.02]}
            fontSize={0.075}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            {currentUser?.fullName || currentUser?.email || 'Người dùng đã xác thực'}
          </Text>

          <Text
            position={[0, 0.1, 0.01]}
            fontSize={0.055}
            color="#38bdf8"
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.05}
          >
            VAI TRÒ: {(currentUser?.roles || ['ROLE_CO_OWNER']).map(r => r === 'ROLE_ADMIN' ? 'QUẢN TRỊ VIÊN' : r === 'ROLE_STAFF' ? 'NHÂN VIÊN VẬN HÀNH' : 'ĐỒNG SỞ HỮU').join(' | ')}
          </Text>

          {/* Enter Metaverse Portal Button */}
          <Button3D
            id="btn_enter_world"
            label="VÀO GARAGE TRUNG TÂM ▶"
            width={2.0}
            height={0.28}
            variant="emerald"
            position={[0, -0.15, 0]}
            onClick={() => enterWorld()}
          />

          {/* Logout / Re-arm Barrier Button */}
          <Button3D
            id="btn_logout"
            label="ĐĂNG XUẤT / KHÓA LẠI CỔNG"
            width={1.6}
            height={0.2}
            variant="crimson"
            position={[0, -0.48, 0]}
            onClick={() => logout()}
          />
        </group>
      )}

      {/* ─── 5. STATUS / ERROR NOTICES ───────────────────────────────── */}
      {authError && (
        <group position={[0, -0.74, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.055}
            color="#ff1744"
            anchorX="center"
            anchorY="middle"
            maxWidth={2.2}
            textAlign="center"
          >
            {`CẢNH BÁO AN NINH: ${authError}`}
          </Text>
        </group>
      )}

      {!authError && successNotice && (
        <group position={[0, -0.74, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.055}
            color="#00e676"
            anchorX="center"
            anchorY="middle"
            maxWidth={2.2}
            textAlign="center"
          >
            {successNotice}
          </Text>
        </group>
      )}
    </Terminal3D>
  );
};
