export type TrustTier = 'FOUNDING_MEMBER' | 'VERIFIED_CO_OWNER' | 'STANDARD_MEMBER';

export interface AuthoritativeValidationResult {
  isValid: boolean;
  totalEquity: number;
  validatedBy: string;
  timestamp: string;
}

export interface CoOwnerMember {
  id: number;
  userId: number;
  name: string;
  email: string;
  sharePercentage: number;
  votingPowerPercentage: number;
  isRepresentative: boolean;
  trustTier: TrustTier;
  enrolledDate: string;
  shareCertificateNumber?: string;
  isActive?: boolean;
  color: string;
  avatarGlyph: string;
  pedestalAngle: number;
  totalBookingsLogged: number;
  fairnessScore: number;
}

export interface SyndicateGroup {
  id: number;
  name: string;
  vehicleId: number;
  vehicleModelName: string;
  vehiclePlate: string;
  formationDate: string;
  isActive: boolean;
  totalValuationVnd: number;
  totalSharesPercent: number;
  members: CoOwnerMember[];
  operatingReserveBalanceVnd: number;
  legalRegistrationCode: string;
}

export interface LegalContractArticle {
  id: number;
  articleNumber: string;
  title: string;
  summary: string;
  fullText: string;
}

export interface ContractSignatureRecord {
  userId: number;
  userName: string;
  userEmail: string;
  sharePercentage: number;
  isSigned: boolean;
  signedAt?: string;
  signatureHash?: string;
  ipAddress?: string;
}

export interface CoOwnershipContractModel {
  id: number;
  groupId: number;
  groupName: string;
  title: string;
  version: number;
  status: 'DRAFT' | 'PENDING_SIGNATURE' | 'SIGNED' | 'ACTIVE' | 'EXPIRED';
  effectiveDate: string;
  articles: LegalContractArticle[];
  signatures: ContractSignatureRecord[];
  totalRequiredSignatures: number;
  totalSubmittedSignatures: number;
  allSigned: boolean;
}
