export type ModerationStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'REJECTED' | 'DUPLICATE';

export type ModerationPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type LessonReportType = 'FILE' | 'TRANSLATION';

export type FeedbackCategory =
  | 'LOGIN'
  | 'BUG'
  | 'PERFORMANCE'
  | 'FEATURE_REQUEST'
  | 'PAYMENT'
  | 'UI_UX'
  | 'OTHER';

export interface UserSummary {
  id: string;
  fullName: string;
  email: string;
}

export interface ModerationState {
  status: ModerationStatus;
  priority: ModerationPriority;
  assigneeId?: string | null;
  resolutionNote?: string | null;
  resolvedAt?: string | null;
}

export interface LessonReportResponse {
  id: string;
  lessonId: string;
  segmentId?: string | null;
  type: LessonReportType;
  reason: string;
  description: string;
  reporter?: UserSummary | null;
  moderation: ModerationState;
  createdAt: string;
}

export interface AppFeedbackResponse {
  id: string;
  category: FeedbackCategory;
  title: string;
  description: string;
  submitter?: UserSummary | null;
  moderation: ModerationState;
  createdAt: string;
}

export interface ModerationUpdateRequest {
  status?: ModerationStatus;
  priority?: ModerationPriority;
  assigneeId?: string;
  unassign?: boolean;
  resolutionNote?: string;
  note?: string;
}

export interface ModerationEventResponse {
  id: string;
  field: string;
  oldValue?: string | null;
  newValue?: string | null;
  note?: string | null;
  actor?: UserSummary | null;
  createdAt: string;
}

export interface LessonReportSearchParams {
  status?: ModerationStatus;
  type?: LessonReportType;
  priority?: ModerationPriority;
  lessonId?: string;
  assigneeId?: string;
  page?: number;
  size?: number;
}

export interface AppFeedbackSearchParams {
  status?: ModerationStatus;
  category?: FeedbackCategory;
  priority?: ModerationPriority;
  assigneeId?: string;
  page?: number;
  size?: number;
}
