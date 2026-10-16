export type UserType = 'CUSTOMER' | 'LABOUR' | 'ADMIN';

export interface Session {
  accessToken: string;
  refreshToken: string;
  userId: number;
  userType: UserType;
}

export interface Category { code: string; name: string; displayOrder: number }
export interface ItemDto { code: string; name: string }
export interface CategoryWithItems {
  categoryCode: string;
  categoryName: string;
  groupedItems: Record<string, ItemDto[]>;
}

export type JobStatus = 'OPEN' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED';

export interface JobPost {
  id: number;
  customerId: number;
  acceptedLabourId: number | null;
  title: string;
  description: string | null;
  requiredLabourType: string;
  genderPreference: string | null;
  minRating: number | null;
  latitude: number;
  longitude: number;
  addressText: string | null;
  status: JobStatus;
  budget: number | null;
  scheduledAt: string | null;
  mediaKeysCsv: string | null;
  createdAt: string;
}

export interface JobPostRequest {
  title: string;
  description?: string;
  requiredLabourType: string;
  genderPreference?: 'MALE' | 'FEMALE';
  latitude: number;
  longitude: number;
  addressText?: string;
  budget?: number;
  radiusKm?: number;
  scheduledAt?: string | null;
  mediaKeys?: string[];
}

export interface JobInterest { id: number; jobId: number; labourId: number; message: string | null; createdAt: string }

export interface PublicLabour {
  userId: number;
  name: string | null;
  labourType: string | null;
  serviceCodes: string[];
  rating: number;
  ratingCount: number;
  kycVerified: boolean;
  city: string | null;
  languages: string[];
  rate: number | null;
  about: string | null;
}

export interface LabourProfile {
  userId: number;
  name: string | null;
  labourType: string | null;
  serviceCodesCsv: string | null;
  gender: 'MALE' | 'FEMALE' | null;
  languagesCsv: string | null;
  rate: number | null;
  about: string | null;
  city: string | null;
  rating: number;
  ratingCount: number;
  kycVerified: boolean;
  availableToday: boolean;
}

export interface LabourProfileUpdate {
  name: string;
  labourType?: string;
  serviceCodes: string[];
  gender?: 'MALE' | 'FEMALE';
  languagesCsv?: string;
  rate?: number;
  about?: string;
  city?: string;
}

export interface BulkJobPostRequest {
  requiredLabourType: string;
  workersNeeded: number;
  description?: string;
  siteAddressText?: string;
  latitude: number;
  longitude: number;
  radiusKm?: number;
  externalFormLink?: string;
}
export interface BulkJobPost extends BulkJobPostRequest {
  id: number;
  contractorId: number;
  status: 'OPEN' | 'CLOSED';
  createdAt: string;
}
export interface BulkJobFeedItem {
  id: number;
  requiredLabourType: string;
  workersNeeded: number;
  description: string | null;
  siteAddressText: string | null;
  externalFormLink: string | null;
  createdAt: string;
}
export interface BulkJobFeedResponse { jobs: BulkJobFeedItem[]; adSlot: unknown | null }
export interface BulkJobInterest { id: number; bulkJobId: number; labourId: number; createdAt: string }

export type DocType = 'AADHAAR' | 'PAN' | 'SELFIE';
export interface KycDocument {
  id: number;
  labourId: number;
  docType: DocType | 'POLICE_VERIFICATION';
  documentUrl: string;
  status: 'PENDING' | 'VERIFIED' | 'REJECTED';
  reviewNotes: string | null;
}

export interface NotificationEvent {
  type: string;
  receivedAt: number;
  jobId?: number;
  bulkJobId?: number;
  labourId?: number;
  [key: string]: unknown;
}
