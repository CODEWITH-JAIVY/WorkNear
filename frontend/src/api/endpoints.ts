import type { ApiClient } from './client';
import type {
  BulkJobFeedResponse, BulkJobInterest, BulkJobPost, BulkJobPostRequest, Category, CategoryWithItems,
  DocType, ItemDto, JobInterest, JobPost, JobPostRequest, KycDocument, LabourProfile, LabourProfileUpdate,
  PublicLabour, Session, UserType,
} from '../types';

export function createEndpoints(api: ApiClient) {
  return {
    auth: {
      signup: (b: { email: string; mobile: string; password: string; userType: UserType }) =>
        api.post<Session>('/api/auth/signup', { body: b, auth: false }),
      login: (b: { email: string; password: string }) =>
        api.post<Session>('/api/auth/login', { body: b, auth: false }),
    },
    catalog: {
      categories: () => api.get<Category[]>('/api/catalog/categories', { auth: false }),
      categoryItems: (code: string) =>
        api.get<CategoryWithItems>(`/api/catalog/categories/${encodeURIComponent(code)}/items`, { auth: false }),
      search: (q: string) => api.get<ItemDto[]>('/api/catalog/items', { query: { search: q }, auth: false }),
      all: () => api.get<ItemDto[]>('/api/catalog/items', { auth: false }),
    },
    jobs: {
      post: (r: JobPostRequest) => api.post<JobPost>('/api/jobs', { body: r }),
      mine: () => api.get<JobPost[]>('/api/jobs/mine'),
      get: (id: number) => api.get<JobPost>(`/api/jobs/${id}`),
      interested: (id: number) => api.get<JobInterest[]>(`/api/jobs/${id}/interested`),
      expressInterest: (id: number, message?: string) =>
        api.post<JobInterest>(`/api/jobs/${id}/interest`, { body: message ? { message } : {} }),
      select: (id: number, labourId: number) =>
        api.post<JobPost>(`/api/jobs/${id}/select`, { query: { labourId } }),
      complete: (id: number) => api.post<JobPost>(`/api/jobs/${id}/complete`),
    },
    bulk: {
      post: (r: BulkJobPostRequest) => api.post<BulkJobPost>('/api/jobs/bulk', { body: r }),
      mine: () => api.get<BulkJobPost[]>('/api/jobs/bulk/mine'),
      feed: (labourType: string, lat: number, lon: number, radiusKm = 10) =>
        api.get<BulkJobFeedResponse>('/api/jobs/bulk/feed', { query: { labourType, lat, lon, radiusKm } }),
      expressInterest: (id: number) => api.post<BulkJobInterest>(`/api/jobs/bulk/${id}/interest`),
      interested: (id: number) => api.get<BulkJobInterest[]>(`/api/jobs/bulk/${id}/interested`),
      close: (id: number) => api.post<BulkJobPost>(`/api/jobs/bulk/${id}/close`),
    },
    labour: {
      me: () => api.get<LabourProfile>('/api/labour/me'),
      update: (u: LabourProfileUpdate) => api.put<LabourProfile>('/api/labour/me', { body: u }),
      publicProfile: (userId: number) => api.get<PublicLabour>(`/api/labour/${userId}/public`),
      setAvailability: (available: boolean) =>
        api.post<void>('/api/labour/me/availability', { query: { available } }),
      updateLocation: (lat: number, lon: number) =>
        api.post<void>('/api/labour/me/location', { body: { lat, lon } }),
    },
    kyc: {
      mine: () => api.get<KycDocument[]>('/api/kyc/documents/mine'),
      submit: (docType: DocType, documentUrl: string) =>
        api.post<KycDocument>('/api/kyc/documents', { body: { docType, documentUrl } }),
      pending: () => api.get<KycDocument[]>('/api/kyc/admin/pending'),
      review: (docId: number, approved: boolean, notes?: string) =>
        api.post<KycDocument>(`/api/kyc/admin/${docId}/review`, { body: { approved, notes } }),
    },
    media: {
      // file: { uri, name, type } from the image picker. Returns { url, key }.
      uploadJobAttachment: (file: { uri: string; name: string; type: string }) => {
        const form = new FormData();
        form.append('folder', 'job-attachments');
        form.append('file', file as unknown as Blob);
        return api.post<{ url: string; key: string }>('/api/media/upload', { form });
      },
      uploadKycDoc: (file: { uri: string; name: string; type: string }) => {
        const form = new FormData();
        form.append('folder', 'kyc-docs');
        form.append('file', file as unknown as Blob);
        return api.post<{ url: string; key: string }>('/api/media/upload', { form });
      },
    },
  };
}

export type Endpoints = ReturnType<typeof createEndpoints>;
