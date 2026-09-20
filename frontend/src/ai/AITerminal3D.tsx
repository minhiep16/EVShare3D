import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAIStore } from './useAIStore';
import { AI_LAYOUT } from './aiLayout';
import type { AISafetyActionType } from './aiTypes';

export const AITerminal3D: React.FC = () => {
  const isAnalyzing = useAIStore((s) => s.isAnalyzing);
  const activePrompt = useAIStore((s) => s.activePrompt);
  const queryResult = useAIStore((s) => s.queryResult);
  const runInteractiveQuery = useAIStore((s) => s.runInteractiveQuery);
  const triggerSafetyAttempt = useAIStore((s) => s.triggerSafetyAttempt);
  const lastBlockedAttempt = useAIStore((s) => s.lastBlockedAttempt);
  const clearBlockedAttempt = useAIStore((s) => s.clearBlockedAttempt);

  const queryPrompts = [
    { label: 'TỶ LỆ CÔNG BẰNG', prompt: 'ANALYZE SYNDICATE FAIRNESS' },
    { label: 'CHẨN ĐOÁN PIN', prompt: 'PREDICT BATTERY HEALTH' },
    { label: 'TỐI ƯU BIỂU GIÁ', prompt: 'OPTIMIZE CHARGING TARIFFS' },
    { label: 'QUÉT BẤT THƯỜNG', prompt: 'SCAN TELEMETRY ANOMALIES' },
  ];

  const disclosureNotice = useAIStore((s) => s.disclosureNotice);
  const modelStatus = useAIStore((s) => s.modelStatus);

  const safetyActionsRow1: { label: string; action: AISafetyActionType }[] = [
    { label: 'THỬ VƯỢT XÁC THỰC', action: 'BYPASS_AUTHENTICATION' },
    { label: 'THỬ VƯỢT PHÂN QUYỀN', action: 'BYPASS_RBAC' },
    { label: 'THỬ SỬA TỶ LỆ VỐN', action: 'ALTER_OWNERSHIP' },
  ];

  const safetyActionsRow2: { label: string; action: AISafetyActionType }[] = [
    { label: 'THỬ TỰ THANH TOÁN', action: 'AUTHORIZE_PAYMENT' },
    { label: 'THỬ TỰ KÝ SỐ', action: 'APPROVE_CONTRACT' },
    { label: 'THỬ ĐÈ BIỂU QUYẾT', action: 'OVERRIDE_VOTING_RULES' },
  ];

  return (
    <group name="AITerminal" position={AI_LAYOUT.interactionTerminalPosition}>
      {/* 1. Terminal Console Base Plinth */}
      <mesh castShadow receiveShadow position={[0, 0.45, 0]}>
        <cylinderGeometry args={[1.6, 1.9, 0.9, 32]} />
        <meshStandardMaterial color="#0b0d1e" roughness={0.4} metalness={0.7} />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
        <ringGeometry args={[1.85, 1.95, 32]} />
        <meshBasicMaterial color={AI_LAYOUT.colors.neuralViolet} opacity={0.6} transparent />
      </mesh>

      {/* 2. Angled Console Faceplate (-22° toward user) */}
      <group position={[0, 1.0, 0]} rotation={[-Math.PI * 0.12, 0, 0]}>
        {/* Main console slab */}
        <mesh receiveShadow position={[0, 0, 0]}>
          <boxGeometry args={[4.0, 2.5, 0.1]} />
          <meshStandardMaterial color="#060814" roughness={0.3} metalness={0.8} />
        </mesh>
        <lineSegments position={[0, 0, 0.055]}>
          <edgesGeometry args={[new THREE.BoxGeometry(4.0, 2.5, 0.1)]} />
          <lineBasicMaterial color={AI_LAYOUT.colors.neuralViolet} />
        </lineSegments>

        {/* Panel Header */}
        <group position={[0, 1.02, 0.06]}>
          <Text
            fontSize={0.14}
            color={AI_LAYOUT.colors.neuralViolet}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.08}
          >
            BÀN ĐIỀU KHIỂN TƯƠNG TÁC AI &amp; KHÓA AN TOÀN
          </Text>
          <Text
            position={[0, -0.16, 0]}
            fontSize={0.085}
            color={modelStatus === 'NOT_AVAILABLE' ? '#f59e0b' : AI_LAYOUT.colors.goldWarning}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.03}
            maxWidth={3.8}
            textAlign="center"
          >
            {disclosureNotice
              .replace('STATUS: NOT_AVAILABLE (AI API UNCONFIGURED) — ADVISORY HEURISTICS ACTIVE', 'TRẠNG THÁI: CHƯA KÍCH HOẠT (CHƯA CẤU HÌNH API AI) — MÔ HÌNH SUY LUẬN TƯ VẤN ĐANG HOẠT ĐỘNG')
              .replace('STATUS: NOT_AVAILABLE (NO DEDICATED AI SERVICE) — ADVISORY HEURISTICS ACTIVE', 'TRẠNG THÁI: CHƯA KÍCH HOẠT (KHÔNG CÓ DỊCH VỤ AI RIÊNG) — MÔ HÌNH SUY LUẬN TƯ VẤN ĐANG HOẠT ĐỘNG')
              .replace('STATUS: ONLINE', 'TRẠNG THÁI: TRỰC TUYẾN')
              .replace('ADVISORY ONLY', 'CHỈ MANG TÍNH TƯ VẤN')}
          </Text>
        </group>

        {/* Query Prompt Chips */}
        <group position={[0, 0.58, 0.06]}>
          <Text
            position={[0, 0.14, 0]}
            fontSize={0.085}
            color={AI_LAYOUT.colors.textMuted}
            anchorX="center"
            anchorY="middle"
          >
            CHỌN YÊU CẦU TỔNG HỢP DỮ LIỆU TƯ VẤN:
          </Text>
          <group position={[0, -0.12, 0]}>
            {queryPrompts.map((p, idx) => {
              const spacing = 0.92;
              const xPos = (idx - 1.5) * spacing;
              const isSelected = activePrompt === p.prompt;

              return (
                <PromptChipButton3D
                  key={p.label}
                  position={[xPos, 0, 0]}
                  label={p.label}
                  isSelected={isSelected}
                  disabled={isAnalyzing}
                  onClick={() => runInteractiveQuery(p.prompt)}
                />
              );
            })}
          </group>
        </group>

        {/* Query Result / Telemetry Readout Feed */}
        <group position={[0, 0.08, 0.06]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[3.7, 0.48]} />
            <meshBasicMaterial color="#02030a" opacity={0.92} transparent side={THREE.DoubleSide} />
          </mesh>
          <lineSegments position={[0, 0, 0.005]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(3.7, 0.48)]} />
            <lineBasicMaterial color="#1e293b" />
          </lineSegments>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.095}
            color={isAnalyzing ? AI_LAYOUT.colors.goldWarning : AI_LAYOUT.colors.textWhite}
            anchorX="center"
            anchorY="middle"
            maxWidth={3.5}
            textAlign="center"
            lineHeight={1.3}
          >
            {isAnalyzing
              ? 'ĐANG TỔNG HỢP DỮ LIỆU TỪ CÁC NÚT THÔNG TIN...'
              : queryResult || 'CHỌN MỘT YÊU CẦU PHÍA TRÊN ĐỂ CHẠY PHÂN TÍCH TƯ VẤN'}
          </Text>
        </group>

        {/* Safety Boundary Test Actions Header */}
        <group position={[0, -0.42, 0.06]}>
          <Text
            position={[0, 0.1, 0]}
            fontSize={0.085}
            color={AI_LAYOUT.colors.anomalyRed}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.04}
          >
            KIỂM TRA GIỚI HẠN BR-AI-SAFE-01 (CẤM CÁC HÀNH ĐỘNG TỰ ĐỘNG):
          </Text>

          {/* Row 1: Auth, RBAC, Ownership */}
          <group position={[0, -0.14, 0]}>
            {safetyActionsRow1.map((act, idx) => {
              const spacing = 1.22;
              const xPos = (idx - 1.0) * spacing;

              return (
                <SafetyTestButton3D
                  key={act.action}
                  position={[xPos, 0, 0]}
                  label={act.label}
                  onClick={() => triggerSafetyAttempt(act.action)}
                />
              );
            })}
          </group>

          {/* Row 2: Payment, Contract, Voting */}
          <group position={[0, -0.46, 0]}>
            {safetyActionsRow2.map((act, idx) => {
              const spacing = 1.22;
              const xPos = (idx - 1.0) * spacing;

              return (
                <SafetyTestButton3D
                  key={act.action}
                  position={[xPos, 0, 0]}
                  label={act.label}
                  onClick={() => triggerSafetyAttempt(act.action)}
                />
              );
            })}
          </group>
        </group>

        {/* Active Blocked Safety Modal Popup in 3D */}
        {lastBlockedAttempt && (
          <group position={[0, 0, 0.22]}>
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[3.6, 1.8]} />
              <meshBasicMaterial color="#050103" opacity={0.98} transparent side={THREE.DoubleSide} />
            </mesh>
            <lineSegments position={[0, 0, 0.005]}>
              <edgesGeometry args={[new THREE.PlaneGeometry(3.6, 1.8)]} />
              <lineBasicMaterial color={AI_LAYOUT.colors.safetyCrimson} linewidth={2} />
            </lineSegments>

            <Text
              position={[0, 0.65, 0.02]}
              fontSize={0.12}
              color={AI_LAYOUT.colors.safetyCrimson}
              anchorX="center"
              anchorY="middle"
              letterSpacing={0.06}
            >
              ⚠ KÍCH HOẠT KHÓA BẢO VỆ CỨNG (BR-AI-SAFE-01)
            </Text>

            <Text
              position={[0, 0.38, 0.02]}
              fontSize={0.105}
              color={AI_LAYOUT.colors.textWhite}
              anchorX="center"
              anchorY="middle"
            >
              {`HÀNH ĐỘNG BỊ CHẶN: ${(() => {
                const map: Record<string, string> = {
                  'Bypass User Authentication / Identity Verification': 'Bỏ qua Xác thực Người dùng / Định danh',
                  'Bypass Role-Based Access Control (RBAC)': 'Bỏ qua Phân quyền Dựa trên Vai trò (RBAC)',
                  'Alter Member Ownership / Equity Shares': 'Thay đổi Cổ phần Sở hữu Thành viên',
                  'Authorize Direct Payment / Fund Withdrawal': 'Tự động Thanh toán / Rút Tiền Quỹ',
                  'Execute / Sign Digital Contract': 'Tự động Thực thi / Ký Hợp đồng Số',
                  'Override Syndicate Voting Rules / Quorum': 'Can thiệp Quy tắc Biểu quyết / Túc số Nhóm',
                  'Irreversible Financial Commitment': 'Cam kết Tài chính Không thể Hoàn tác',
                };
                return map[lastBlockedAttempt.actionLabel] || lastBlockedAttempt.actionLabel;
              })()}`}
            </Text>

            <Text
              position={[0, 0.05, 0.02]}
              fontSize={0.09}
              color="#fca5a5"
              anchorX="center"
              anchorY="middle"
              maxWidth={3.2}
              textAlign="center"
              lineHeight={1.3}
            >
              {(() => {
                const map: Record<string, string> = {
                  BYPASS_AUTHENTICATION: 'VI PHẠM: Thuật toán AI không thể giả mạo hoặc thay thế thông tin xác thực mật mã hoặc token JWT của người dùng. Xác thực bắt buộc do tầng an ninh nền tảng quản lý.',
                  BYPASS_RBAC: 'VI PHẠM: Chính sách an ninh mật mã không thể bị ghi đè bởi thuật toán dự đoán. Mã ủy quyền phải bắt nguồn từ hệ thống xác thực Keycloak/JWT.',
                  ALTER_OWNERSHIP: 'VI PHẠM: AI không thể chỉnh sửa bảng phân bổ cổ phần sở hữu nhóm. Mọi thay đổi cổ phần đòi hỏi biểu quyết chính thức tại Phòng Biểu Quyết (BR-VOT-01).',
                  AUTHORIZE_PAYMENT: 'VI PHẠM: AI chỉ có chức năng cố vấn. Các giao dịch tài chính đòi hỏi xác thực sinh trắc học 2 lớp trực tiếp từ đồng sở hữu tại Trung tâm Tài chính (BR-FIN-03).',
                  APPROVE_CONTRACT: 'VI PHẠM: Thỏa thuận pháp lý yêu cầu chữ ký số cá nhân và chuỗi băm mật mã xác thực tại Phòng Hợp Đồng Số (BR-CON-02).',
                  OVERRIDE_VOTING_RULES: 'VI PHẠM: AI không thể bỏ phiếu, không thể vượt qua yêu cầu túc số 60.00% hoặc ghi đè ngưỡng đồng thuận dân chủ. Kết quả AI chỉ mang tính tham khảo.',
                  IRREVERSIBLE_FINANCIAL_ACTION: 'VI PHẠM: Tái cân bằng quỹ dự phòng hoặc thoái vốn không thể thực hiện tự động bởi AI. Đòi hỏi xác nhận túc số từ các đồng sở hữu.',
                };
                return map[lastBlockedAttempt.attemptedAction] || lastBlockedAttempt.blockedReason;
              })()}
            </Text>

            <Text
              position={[0, -0.32, 0.02]}
              fontSize={0.08}
              color={AI_LAYOUT.colors.goldWarning}
              anchorX="center"
              anchorY="middle"
            >
              {`QUYỀN HẠN YÊU CẦU: ${(() => {
                const map: Record<string, string> = {
                  'CRYPTOGRAPHIC_USER_CREDENTIALS_AND_JWT': 'THÔNG TIN MẬT MÃ NGƯỜI DÙNG & TOKEN JWT',
                  'PLATFORM_SECURITY_ADMINISTRATOR': 'QUẢN TRỊ VIÊN AN NINH NỀN TẢNG',
                  'SYNDICATE_PARLIAMENTARY_CONSENSUS (75% SUPERMAJORITY)': 'ĐỒNG THUẬN BIỂU QUYẾT NHÓM (ĐA SỐ ĐẶC BIỆT 75%)',
                  'HUMAN_CO_OWNER_BIOMETRIC_SIGNATURE': 'CHỮ KÝ SINH TRẮC HỌC CỦA ĐỒNG SỞ HỮU',
                  'AUTHENTICATED_CO_OWNER_LEGAL_SIGNATURE': 'CHỮ KÝ PHÁP LÝ ĐƯỢC XÁC THỰC CỦA ĐỒNG SỞ HỮU',
                  'DEMOCRATIC_CO_OWNER_BALLOT_QUORUM': 'TÚC SỐ BỎ PHIẾU DÂN CHỦ CỦA ĐỒNG SỞ HỮU',
                  'SYNDICATE_CO_OWNERS_QUORUM_APPROVAL': 'PHÊ DUYỆT TÚC SỐ ĐỒNG SỞ HỮU NHÓM',
                };
                return map[lastBlockedAttempt.requiredAuthority] || lastBlockedAttempt.requiredAuthority;
              })()}`}
            </Text>

            {/* Dismiss Button in Pure 3D */}
            <group
              position={[0, -0.62, 0.04]}
              onClick={(e) => {
                e.stopPropagation();
                clearBlockedAttempt();
              }}
            >
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[2.0, 0.25]} />
                <meshStandardMaterial color="#2d0a0e" roughness={0.4} metalness={0.6} />
              </mesh>
              <lineSegments position={[0, 0, 0.005]}>
                <edgesGeometry args={[new THREE.PlaneGeometry(2.0, 0.25)]} />
                <lineBasicMaterial color={AI_LAYOUT.colors.safetyCrimson} />
              </lineSegments>
              <Text
                position={[0, 0, 0.02]}
                fontSize={0.085}
                color="#ffffff"
                anchorX="center"
                anchorY="middle"
              >
                [ XÁC NHẬN GIỚI HẠN AN TOÀN ]
              </Text>
            </group>
          </group>
        )}
      </group>
    </group>
  );
};

interface PromptChipButton3DProps {
  position: [number, number, number];
  label: string;
  isSelected: boolean;
  disabled: boolean;
  onClick: () => void;
}

const PromptChipButton3D: React.FC<PromptChipButton3DProps> = ({
  position,
  label,
  isSelected,
  disabled,
  onClick,
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        if (!disabled) onClick();
      }}
      onPointerOver={(e) => {
        e.stopPropagation();
        if (!disabled) setHovered(true);
      }}
      onPointerOut={() => setHovered(false)}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[0.82, 0.25]} />
        <meshStandardMaterial
          color={isSelected ? '#4c1d95' : hovered ? '#2e1065' : '#0f172a'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(0.82, 0.25)]} />
        <lineBasicMaterial color={isSelected ? '#c084fc' : hovered ? '#a855f7' : '#475569'} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.068}
        color={isSelected ? '#ffffff' : hovered ? '#ffffff' : '#94a3b8'}
        anchorX="center"
        anchorY="middle"
      >
        {label}
      </Text>
    </group>
  );
};

interface SafetyTestButton3DProps {
  position: [number, number, number];
  label: string;
  onClick: () => void;
}

const SafetyTestButton3D: React.FC<SafetyTestButton3DProps> = ({
  position,
  label,
  onClick,
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
      onPointerOver={(e) => {
        e.stopPropagation();
        setHovered(true);
      }}
      onPointerOut={() => setHovered(false)}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[1.1, 0.22]} />
        <meshStandardMaterial
          color={hovered ? '#450a0a' : '#1e0507'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(1.1, 0.22)]} />
        <lineBasicMaterial color={hovered ? '#ef4444' : '#991b1b'} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.065}
        color={hovered ? '#fecaca' : '#f87171'}
        anchorX="center"
        anchorY="middle"
      >
        {label}
      </Text>
    </group>
  );
};
