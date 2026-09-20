import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import { useOwnershipStore } from './useOwnershipStore';
import { CO_OWNERSHIP_HALL_LAYOUT } from './ownershipLayout';
import { formatDateVN, formatStatusVN } from '@/i18n';

export const ContractConsole3D: React.FC = () => {
  const [hoveredTab, setHoveredTab] = useState<number | null>(null);
  const [hoveredAction, setHoveredAction] = useState<string | null>(null);
  const [exitProtocolActive, setExitProtocolActive] = useState(false);
  const [actionFeedback, setActionFeedback] = useState<string | null>(null);

  const groups = useOwnershipStore((s) => s.groups);
  const activeGroupId = useOwnershipStore((s) => s.activeGroupId);
  const activeContract = useOwnershipStore((s) => s.activeContract);
  const inspectedArticleIndex = useOwnershipStore((s) => s.inspectedArticleIndex ?? 0);
  const setInspectedArticle = useOwnershipStore((s) => s.setInspectedArticle);
  const signActiveContract = useOwnershipStore((s) => s.signActiveContract);
  const isSigning = useOwnershipStore((s) => s.isSigning);
  const actionNotice = useOwnershipStore((s) => s.actionNotice);

  const activeGroup = groups.find((g) => g.id === activeGroupId) || groups[0];

  // Robust contract resolution
  const contract = activeContract || {
    id: 10,
    groupId: activeGroup?.id ?? 1,
    groupName: activeGroup?.name ?? 'Apex Syndicate',
    title: `Hợp Đồng Đồng Sở Hữu Xe Điện ${activeGroup?.name ?? 'Apex Syndicate'}`,
    version: 1,
    status: 'ACTIVE' as const,
    effectiveDate: activeGroup?.formationDate ?? '2026-01-15',
    articles: [
      {
        id: 1,
        articleNumber: 'ĐIỀU KHOẢN 01',
        title: 'Phân Bổ Cổ Phần & Tính Bất Biến Sở Hữu',
        summary: 'Quy định nghiêm ngặt tổng tỷ lệ cổ phần sở hữu luôn đạt chính xác 100.00%.',
        fullText:
          'Các đồng sở hữu đồng thuận nắm giữ phần quyền lợi phân đoạn không chia tách đối với Xe Điện như được ghi nhận trong Sổ bộ Nhóm.',
      },
    ],
    signatures: (activeGroup?.members || []).map((m, idx) => ({
      userId: m.userId,
      userName: m.name,
      userEmail: m.email,
      sharePercentage: m.sharePercentage,
      isSigned: idx === 0,
    })),
    totalRequiredSignatures: activeGroup?.members?.length ?? 3,
    totalSubmittedSignatures: 1,
    allSigned: false,
  };

  const articles = contract.articles || [];
  const currentArticle =
    articles[inspectedArticleIndex] ||
    articles[0] || {
      id: 1,
      articleNumber: 'ĐIỀU KHOẢN 01',
      title: 'Thỏa Thuận Chung',
      summary: 'Tổng quan Hợp đồng Đồng sở hữu',
      fullText: 'Các điều khoản tiêu chuẩn về đồng sở hữu xe điện phân đoạn được áp dụng.',
    };

  const { CONTRACT_CONSOLE_POS, CONTRACT_CONSOLE_ROT, THEME } = CO_OWNERSHIP_HALL_LAYOUT;

  // Determine if current demo user (Nguyen Van A, id 1) has signed
  const signatures = contract.signatures || [];
  const currentUserSignature = signatures.find((s) => s.userId === 1);
  const hasUserSigned = currentUserSignature?.isSigned ?? false;

  const handleSignClick = async (e: any) => {
    e.stopPropagation();
    if (!hasUserSigned && !isSigning) {
      await signActiveContract();
      setActionFeedback('DIGITAL SIGNATURE CONFIRMED (SHA-256 RECORDED)');
      setTimeout(() => setActionFeedback(null), 4000);
    }
  };

  const handleExitProtocolClick = (e: any) => {
    e.stopPropagation();
    setExitProtocolActive(!exitProtocolActive);
  };

  return (
    <group
      position={CONTRACT_CONSOLE_POS}
      rotation={CONTRACT_CONSOLE_ROT}
      name="ContractConsole3D"
    >
      {/* 1. Base Terminal Pedestal */}
      <mesh position={[0, 0.45, 0]} receiveShadow>
        <boxGeometry args={[1.4, 0.9, 0.8]} />
        <meshStandardMaterial
          color="#090d16"
          roughness={0.2}
          metalness={0.8}
        />
      </mesh>

      {/* Terminal Base Gold Accent Trim */}
      <mesh position={[0, 0.91, 0]}>
        <boxGeometry args={[1.45, 0.04, 0.85]} />
        <meshStandardMaterial
          color={THEME.GOLD_ACCENT_PRIMARY}
          roughness={0.15}
          metalness={0.9}
        />
      </mesh>

      {/* Terminal Status Beacon Light */}
      <pointLight
        color={THEME.GOLD_ACCENT_PRIMARY}
        intensity={0.8}
        distance={5}
        position={[0, 1.4, 0.4]}
      />

      {/* 2. Main Angled Holographic Manuscript Board */}
      <group position={[0, 1.7, 0]} rotation={[-0.22, 0, 0]}>
        {/* Board Backing */}
        <mesh position={[0, 0, -0.04]}>
          <planeGeometry args={[3.2, 2.2]} />
          <meshStandardMaterial
            color="#040711"
            roughness={0.15}
            metalness={0.85}
            transparent
            opacity={0.92}
          />
        </mesh>

        {/* Outer Gold Frame */}
        <mesh position={[0, 0, -0.035]}>
          <ringGeometry args={[1.58, 1.6, 4]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} />
        </mesh>

        {/* Contract Title & Status */}
        <Text
          position={[0, 0.92, 0]}
          fontSize={0.105}
          color={THEME.TEXT_GOLD_BRIGHT}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          {contract.title.toUpperCase()}
        </Text>

        <Text
          position={[0, 0.78, 0]}
          fontSize={0.065}
          color={contract.status === 'ACTIVE' ? THEME.CYBER_EMERALD : THEME.GOLD_ACCENT_PRIMARY}
          anchorX="center"
          anchorY="middle"
        >
          {`PHIÊN BẢN ${contract.version} • TRẠNG THÁI: ${formatStatusVN(contract.status)} • HIỆU LỰC: ${formatDateVN(contract.effectiveDate)}`}
        </Text>

        {/* Top Divider */}
        <mesh position={[0, 0.69, 0]}>
          <planeGeometry args={[2.9, 0.006]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_GLOW} transparent opacity={0.6} />
        </mesh>

        {/* 3. Interactive Article Tabs */}
        <group position={[0, 0.57, 0]}>
          {articles.map((art, idx) => {
            const isTabActive = inspectedArticleIndex === idx;
            const isTabHovered = hoveredTab === idx;
            const tabWidth = 0.68;
            const spacing = 0.72;
            const xPos = (idx - (articles.length - 1) / 2) * spacing;

            return (
              <group
                key={art.articleNumber || idx}
                position={[xPos, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setInspectedArticle(idx);
                }}
                onPointerEnter={(e) => {
                  e.stopPropagation();
                  setHoveredTab(idx);
                  document.body.style.cursor = 'pointer';
                }}
                onPointerLeave={(e) => {
                  e.stopPropagation();
                  setHoveredTab(null);
                  document.body.style.cursor = 'auto';
                }}
              >
                {/* Tab Surface Box */}
                <mesh>
                  <boxGeometry args={[tabWidth, 0.14, 0.02]} />
                  <meshStandardMaterial
                    color={isTabActive ? THEME.AMBER_DARK : isTabHovered ? '#1e293b' : '#0a0f1d'}
                    emissive={isTabActive ? THEME.GOLD_ACCENT_PRIMARY : '#000000'}
                    emissiveIntensity={isTabActive ? 0.5 : 0}
                    roughness={0.3}
                    metalness={0.7}
                  />
                </mesh>

                {/* Tab Text */}
                <Text
                  position={[0, 0, 0.02]}
                  fontSize={0.052}
                  color={isTabActive ? '#ffffff' : isTabHovered ? THEME.TEXT_GOLD_BRIGHT : THEME.TEXT_MUTED}
                  anchorX="center"
                  anchorY="middle"
                >
                  {`ĐIỀU 0${idx + 1}`}
                </Text>
              </group>
            );
          })}
        </group>

        {/* 4. Active Article Manuscript Details */}
        <group position={[0, 0.16, 0]}>
          {/* Article Title */}
          <Text
            position={[-1.4, 0.22, 0]}
            fontSize={0.075}
            color={THEME.TEXT_GOLD_BRIGHT}
            anchorX="left"
            anchorY="middle"
          >
            {`${currentArticle.articleNumber || 'ĐIỀU KHOẢN'}: ${currentArticle.title}`}
          </Text>

          {/* Article Summary */}
          <Text
            position={[-1.4, 0.1, 0]}
            fontSize={0.058}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
            maxWidth={2.8}
          >
            {currentArticle.summary}
          </Text>

          {/* Clauses List or Full Text */}
          {((currentArticle as any).clauses || [currentArticle.fullText || '']).map((clause: string, cIdx: number) => (
            <group key={cIdx} position={[-1.4, -0.05 - cIdx * 0.14, 0]}>
              <Text fontSize={0.055} color={THEME.GOLD_ACCENT_PRIMARY} anchorX="left" anchorY="middle">
                ◆
              </Text>
              <Text
                position={[0.1, 0, 0]}
                fontSize={0.052}
                color="#94a3b8"
                anchorX="left"
                anchorY="middle"
                maxWidth={2.65}
              >
                {clause}
              </Text>
            </group>
          ))}
        </group>

        {/* 5. Signer Matrix Horizontal Strip */}
        <group position={[0, -0.44, 0]}>
          <Text
            position={[-1.4, 0.06, 0]}
            fontSize={0.055}
            color={THEME.TEXT_MUTED}
            anchorX="left"
            anchorY="middle"
          >
            DANH SÁCH CHỮ KÝ ĐỒNG SỞ HỮU:
          </Text>

          {signatures.map((signer, sIdx) => {
            const spacing = 0.95;
            const xPos = -1.35 + sIdx * spacing;

            return (
              <group key={signer.userId || sIdx} position={[xPos, -0.08, 0]}>
                <mesh position={[0.4, 0, 0]}>
                  <boxGeometry args={[0.88, 0.12, 0.015]} />
                  <meshStandardMaterial
                    color="#090d16"
                    emissive={signer.isSigned ? THEME.CYBER_EMERALD : THEME.AMBER_DARK}
                    emissiveIntensity={0.2}
                  />
                </mesh>

                <Text
                  position={[0.02, 0, 0.02]}
                  fontSize={0.045}
                  color="#ffffff"
                  anchorX="left"
                  anchorY="middle"
                >
                  {(signer.userName || 'Thành viên').split(' ').slice(-2).join(' ')}
                </Text>

                <Text
                  position={[0.78, 0, 0.02]}
                  fontSize={0.045}
                  color={signer.isSigned ? THEME.CYBER_EMERALD : '#f59e0b'}
                  anchorX="right"
                  anchorY="middle"
                >
                  {signer.isSigned ? '✓ ĐÃ KÝ' : 'CHỜ KÝ'}
                </Text>
              </group>
            );
          })}
        </group>

        {/* 6. Interactive Action Row */}
        <group position={[0, -0.74, 0]}>
          {/* Action Button 1: Digital Signing */}
          <group
            position={[-0.75, 0, 0]}
            onClick={handleSignClick}
            onPointerEnter={(e) => {
              e.stopPropagation();
              setHoveredAction('sign');
              document.body.style.cursor = 'pointer';
            }}
            onPointerLeave={(e) => {
              e.stopPropagation();
              setHoveredAction(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh>
              <boxGeometry args={[1.35, 0.18, 0.03]} />
              <meshStandardMaterial
                color={hasUserSigned ? '#064e3b' : isSigning ? '#1e3a5f' : hoveredAction === 'sign' ? THEME.AMBER_DARK : '#0d1527'}
                emissive={hasUserSigned ? THEME.CYBER_EMERALD : isSigning ? THEME.CYBER_CYAN : THEME.GOLD_ACCENT_PRIMARY}
                emissiveIntensity={hoveredAction === 'sign' || hasUserSigned || isSigning ? 0.7 : 0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.03]}
              fontSize={0.052}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              {isSigning
                ? '⏳ ĐANG TRUYỀN CHỮ KÝ SỐ...'
                : hasUserSigned
                ? '✓ ĐÃ KÝ ĐIỆN TỬ'
                : '✍ KÝ HỢP ĐỒNG (SHA-256)'}
            </Text>
          </group>

          {/* Action Button 2: Dispute / Exit Actions */}
          <group
            position={[0.75, 0, 0]}
            onClick={handleExitProtocolClick}
            onPointerEnter={(e) => {
              e.stopPropagation();
              setHoveredAction('exit');
              document.body.style.cursor = 'pointer';
            }}
            onPointerLeave={(e) => {
              e.stopPropagation();
              setHoveredAction(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh>
              <boxGeometry args={[1.35, 0.18, 0.03]} />
              <meshStandardMaterial
                color={exitProtocolActive ? '#7f1d1d' : hoveredAction === 'exit' ? '#334155' : '#0d1527'}
                emissive={exitProtocolActive ? '#ef4444' : '#64748b'}
                emissiveIntensity={hoveredAction === 'exit' ? 0.5 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.03]}
              fontSize={0.052}
              color={exitProtocolActive ? '#fecaca' : '#e2e8f0'}
              anchorX="center"
              anchorY="middle"
            >
              {exitProtocolActive ? 'ẨN ĐIỀU KHOẢN RÚT VỐN' : '⚙ TRANH CHẤP & RÚT VỐN'}
            </Text>
          </group>

          {/* Feedback Toast Message */}
          {(actionNotice || actionFeedback) && (
            <Text
              position={[0, 0.2, 0.05]}
              fontSize={0.06}
              color={THEME.CYBER_EMERALD}
              anchorX="center"
              anchorY="middle"
            >
              {actionNotice || actionFeedback}
            </Text>
          )}
        </group>

        {/* 7. Expanded Dispute & Exit Protocol Panel (When Toggled) */}
        {exitProtocolActive && (
          <group position={[0, -1.35, 0]}>
            <mesh position={[0, 0, -0.02]}>
              <planeGeometry args={[2.9, 0.85]} />
              <meshStandardMaterial
                color="#090510"
                transparent
                opacity={0.96}
                roughness={0.2}
                metalness={0.8}
              />
            </mesh>
            <Text
              position={[0, 0.3, 0]}
              fontSize={0.065}
              color="#f87171"
              anchorX="center"
              anchorY="middle"
            >
              TRỌNG TÀI TRANH CHẤP & QUY TRÌNH RÚT VỐN ĐỒNG SỞ HỮU
            </Text>
            <Text
              position={[0, 0.12, 0]}
              fontSize={0.05}
              color="#e2e8f0"
              anchorX="center"
              anchorY="middle"
              maxWidth={2.6}
            >
              Thành viên muốn rút vốn phải chào bán cổ phần cho các đồng sở hữu hiện hữu theo quyền ưu tiên mua trước. Trường hợp bất đồng vận hành, quy trình trọng tài ràng buộc của nền tảng sẽ tự động kích hoạt.
            </Text>

            {/* Sub Action Buttons */}
            <group position={[-0.65, -0.18, 0]}>
              <mesh>
                <boxGeometry args={[1.1, 0.14, 0.02]} />
                <meshStandardMaterial color="#450a0a" emissive="#dc2626" emissiveIntensity={0.3} />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.045} color="#fca5a5" anchorX="center" anchorY="middle">
                GỬI THÔNG BÁO TRANH CHẤP
              </Text>
            </group>

            <group position={[0.65, -0.18, 0]}>
              <mesh>
                <boxGeometry args={[1.1, 0.14, 0.02]} />
                <meshStandardMaterial color="#1e1b4b" emissive="#6366f1" emissiveIntensity={0.3} />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.045} color="#c7d2fe" anchorX="center" anchorY="middle">
                YÊU CẦU MUA LẠI CỔ PHẦN
              </Text>
            </group>
          </group>
        )}
      </group>
    </group>
  );
};
