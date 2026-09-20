import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import { useDisputeStore } from './useDisputeStore';
import { DISPUTE_THEME, DISPUTE_STATIONS } from './disputeLayout';
import { formatStatusVN } from '@/i18n';

export const StaffMediationConsole3D: React.FC = () => {
  const {
    activeDispute,
    userRole,
    staffNotesInput,
    proposedResolutionInput,
    setStaffNotesInput,
    setProposedResolutionInput,
    recordStaffMediationNotes,
    proposeStaffResolution,
    escalateToAdminArbitration,
    isSubmitting,
  } = useDisputeStore();

  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  const isAuthorized = userRole === 'ROLE_STAFF' || userRole === 'ROLE_ADMIN';
  const consolePos = DISPUTE_STATIONS.STAFF_CONSOLE.relativePosition;

  return (
    <group name="StaffMediationStation" position={consolePos}>
      {/* Station Header */}
      <Text
        position={[0, 3.2, 0]}
        fontSize={0.2}
        color={DISPUTE_THEME.secondary}
        anchorX="center"
        anchorY="middle"
      >
        BÀN HÒA GIẢI & ĐÁNH GIÁ CỦA NHÂN VIÊN
      </Text>
      <Text
        position={[0, 2.94, 0]}
        fontSize={0.11}
        color={DISPUTE_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Điều tra tranh chấp • Ghi chú sự thật • Đề xuất hòa giải
      </Text>

      {/* Desk Base Structure */}
      <mesh position={[0, 0.4, 0]} receiveShadow castShadow>
        <boxGeometry args={[3.2, 0.8, 1.4]} />
        <meshStandardMaterial
          color="#120606"
          roughness={0.5}
          metalness={0.8}
        />
      </mesh>

      {/* Amber Accent Edge */}
      <mesh position={[0, 0.81, 0]}>
        <boxGeometry args={[3.24, 0.04, 1.44]} />
        <meshStandardMaterial
          color={DISPUTE_THEME.secondary}
          emissive={DISPUTE_THEME.secondary}
          emissiveIntensity={0.6}
        />
      </mesh>

      {/* Primary Terminal Display Screen */}
      <group position={[0, 1.85, -0.1]} rotation={[-0.15, 0, 0]}>
        <mesh position={[0, 0, -0.02]} receiveShadow>
          <planeGeometry args={[3.1, 1.95]} />
          <meshStandardMaterial
            color="#140606"
            roughness={0.3}
            metalness={0.8}
            transparent
            opacity={0.96}
          />
        </mesh>
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[3.14, 1.99]} />
          <meshBasicMaterial
            color={isAuthorized ? DISPUTE_THEME.secondary : DISPUTE_THEME.alertRed}
            wireframe
          />
        </mesh>

        {/* RBAC Authorization Badge */}
        <group position={[0, 0.82, 0.02]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[2.8, 0.16]} />
            <meshBasicMaterial
              color={isAuthorized ? '#14532d' : '#7f1d1d'}
            />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.056}
            color={isAuthorized ? '#86efac' : '#fca5a5'}
            anchorX="center"
            anchorY="middle"
          >
            {isAuthorized
              ? `✓ HÒA GIẢI VIÊN ĐƯỢC ỦY QUYỀN: Phiên hoạt động (${userRole})`
              : `🔒 HẠN CHẾ RBAC: Yêu cầu ROLE_STAFF hoặc ROLE_ADMIN (Hiện tại: ${userRole})`}
          </Text>
        </group>

        {/* SECTION 1: Case Dossier Summary */}
        <group position={[-1.35, 0.62, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.072}
            color={DISPUTE_THEME.cyberCyan}
            anchorX="left"
            anchorY="middle"
          >
            VỤ VIỆC #{activeDispute.id}: {activeDispute.title.slice(0, 48)}...
          </Text>
          <Text
            position={[0, -0.12, 0]}
            fontSize={0.056}
            color={DISPUTE_THEME.textMuted}
            anchorX="left"
            anchorY="middle"
          >
            Nguyên đơn: {activeDispute.complainantUserName} vs Bị đơn:{' '}
            {activeDispute.respondentUserName} • Trạng thái: {formatStatusVN(activeDispute.status)}
          </Text>
        </group>

        {/* SECTION 2: Staff Mediation Notes Log */}
        <group position={[-1.35, 0.28, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.066}
            color={DISPUTE_THEME.secondary}
            anchorX="left"
            anchorY="middle"
          >
            Nhật ký hòa giải & Ghi chú chứng cứ:
          </Text>

          {/* Notes display frame */}
          <mesh position={[1.35, -0.18, 0]}>
            <planeGeometry args={[2.7, 0.26]} />
            <meshStandardMaterial color="#210808" />
          </mesh>
          <Text
            position={[0.08, -0.18, 0.02]}
            fontSize={0.054}
            color="#f1f5f9"
            anchorX="left"
            anchorY="middle"
            maxWidth={2.55}
          >
            {staffNotesInput}
          </Text>

          {/* Quick presets for notes */}
          {isAuthorized && (
            <group position={[0, -0.38, 0]}>
              <group
                position={[0.5, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setStaffNotesInput(
                    'Dữ liệu viễn thông xác nhận va chạm/tốc độ bất thường khớp với thời điểm trầy xước.'
                  );
                }}
                onPointerOver={() => setHoveredBtn('note-telematics')}
                onPointerOut={() => setHoveredBtn(null)}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[1.0, 0.12]} />
                  <meshBasicMaterial
                    color={
                      hoveredBtn === 'note-telematics' ? '#451a03' : '#271007'
                    }
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.046}
                  color={DISPUTE_THEME.secondary}
                  anchorX="center"
                  anchorY="middle"
                >
                  [ + Xác nhận viễn thông ]
                </Text>
              </group>

              {/* Commit Notes Button */}
              <group
                position={[2.0, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  if (!isSubmitting) recordStaffMediationNotes();
                }}
                onPointerOver={() => setHoveredBtn('commit-notes')}
                onPointerOut={() => setHoveredBtn(null)}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[1.3, 0.14]} />
                  <meshStandardMaterial
                    color={
                      hoveredBtn === 'commit-notes'
                        ? DISPUTE_THEME.secondary
                        : '#b45309'
                    }
                    emissive={DISPUTE_THEME.secondary}
                    emissiveIntensity={hoveredBtn === 'commit-notes' ? 0.7 : 0.3}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.052}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                >
                  [ 📝 GHI NHẬN BIÊN BẢN ]
                </Text>
              </group>
            </group>
          )}
        </group>

        {/* SECTION 3: Proposed Settlement Resolution */}
        <group position={[-1.35, -0.26, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.066}
            color={DISPUTE_THEME.secondary}
            anchorX="left"
            anchorY="middle"
          >
            Đề xuất phương án hòa giải / Thỏa thuận:
          </Text>

          {/* Proposed resolution display frame */}
          <mesh position={[1.35, -0.16, 0]}>
            <planeGeometry args={[2.7, 0.22]} />
            <meshStandardMaterial color="#210808" />
          </mesh>
          <Text
            position={[0.08, -0.16, 0.02]}
            fontSize={0.054}
            color="#f1f5f9"
            anchorX="left"
            anchorY="middle"
            maxWidth={2.55}
          >
            {proposedResolutionInput}
          </Text>

          {/* Quick settlement options */}
          {isAuthorized && (
            <group position={[0, -0.34, 0]}>
              <group
                position={[0.5, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setProposedResolutionInput(
                    'Hai bên thống nhất phân chia 50/50 chi phí sửa chữa 500.000 đ qua quỹ chung.'
                  );
                }}
                onPointerOver={() => setHoveredBtn('split-50')}
                onPointerOut={() => setHoveredBtn(null)}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[1.0, 0.12]} />
                  <meshBasicMaterial
                    color={hoveredBtn === 'split-50' ? '#451a03' : '#271007'}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.046}
                  color={DISPUTE_THEME.secondary}
                  anchorX="center"
                  anchorY="middle"
                >
                  [ Phân chia 50/50 ]
                </Text>
              </group>

              {/* Submit Resolution Proposal Button */}
              <group
                position={[2.0, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  if (!isSubmitting) proposeStaffResolution();
                }}
                onPointerOver={() => setHoveredBtn('propose-btn')}
                onPointerOut={() => setHoveredBtn(null)}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[1.3, 0.14]} />
                  <meshStandardMaterial
                    color={
                      hoveredBtn === 'propose-btn'
                        ? DISPUTE_THEME.secondary
                        : '#b45309'
                    }
                    emissive={DISPUTE_THEME.secondary}
                    emissiveIntensity={hoveredBtn === 'propose-btn' ? 0.7 : 0.3}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.052}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                >
                  [ 🤝 GỬI ĐỀ XUẤT HÒA GIẢI ]
                </Text>
              </group>
            </group>
          )}
        </group>

        {/* SECTION 4: Escalation to Admin Arbitration (Bottom Row) */}
        <group position={[0, -0.76, 0.02]}>
          <group
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isSubmitting) {
                escalateToAdminArbitration();
              }
            }}
            onPointerOver={() => {
              if (isAuthorized) {
                setHoveredBtn('escalate-btn');
                document.body.style.cursor = 'pointer';
              }
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[2.7, 0.22]} />
              <meshStandardMaterial
                color={
                  !isAuthorized
                    ? '#374151'
                    : hoveredBtn === 'escalate-btn'
                    ? DISPUTE_THEME.primary
                    : '#991b1b'
                }
                emissive={isAuthorized ? DISPUTE_THEME.primary : '#000000'}
                emissiveIntensity={hoveredBtn === 'escalate-btn' ? 0.8 : 0.4}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.062}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ ⚖ CHUYỂN TRANH CHẤP LÊN TRỌNG TÀI TỐI CAO ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
