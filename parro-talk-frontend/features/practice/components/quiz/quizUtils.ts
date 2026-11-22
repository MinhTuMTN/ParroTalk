import type { QuizGradingStatus, QuizMode } from "../../types/quiz";

export const QUIZ_MODE_LABELS: Record<
  QuizMode,
  { title: string; subtitle: string; badge: string }
> = {
  MULTIPLE_CHOICE: {
    title: "Trắc nghiệm",
    subtitle: "4 lựa chọn A/B/C/D kiểm tra nắm bắt ý chính & chi tiết",
    badge: "4 lựa chọn",
  },
  FILL_IN_THE_BLANK: {
    title: "Điền khuyết",
    subtitle: "Nghe hiểu & tự điền câu trả lời — AI chấm điểm theo ngữ nghĩa",
    badge: "AI chấm",
  },
  MIXED: {
    title: "Kết hợp cả hai",
    subtitle: "Phối hợp trắc nghiệm và điền khuyết cho thử thách toàn diện",
    badge: "Trộn 2 dạng",
  },
};

export function formatQuizTimestamp(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) {
    return "00:00";
  }
  const rounded = Math.floor(seconds);
  const mins = Math.floor(rounded / 60);
  const secs = rounded % 60;
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
}

export function formatQuizTimeRange(start: number, end: number): string {
  const safeStart = Math.max(0, start);
  const safeEnd = Math.max(safeStart, end);
  return `${formatQuizTimestamp(safeStart)} - ${formatQuizTimestamp(safeEnd)}`;
}

export function formatCooldownDuration(totalSeconds: number): string {
  const secs = Math.max(0, Math.ceil(totalSeconds));
  const mins = Math.floor(secs / 60);
  const remSecs = secs % 60;
  if (mins <= 0) {
    return `${remSecs}s`;
  }
  return `${mins}:${String(remSecs).padStart(2, "0")}`;
}

export function formatQuizDate(isoString: string | null | undefined): string {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function getGradingStatusMeta(status: QuizGradingStatus): {
  label: string;
  badgeClass: string;
  cardClass: string;
} {
  switch (status) {
    case "CORRECT":
      return {
        label: "Đúng",
        badgeClass:
          "bg-emerald-100 text-emerald-700 border-emerald-200 dark:bg-emerald-950/70 dark:text-emerald-300 dark:border-emerald-800",
        cardClass:
          "border-emerald-200/90 bg-emerald-50/50 dark:border-emerald-800/70 dark:bg-emerald-950/25",
      };
    case "PARTIAL":
      return {
        label: "Gần đúng",
        badgeClass:
          "bg-amber-100 text-amber-800 border-amber-200 dark:bg-amber-950/70 dark:text-amber-300 dark:border-amber-800",
        cardClass:
          "border-amber-200/90 bg-amber-50/50 dark:border-amber-800/70 dark:bg-amber-950/25",
      };
    case "INCORRECT":
    default:
      return {
        label: "Chưa đúng",
        badgeClass:
          "bg-rose-100 text-rose-700 border-rose-200 dark:bg-rose-950/70 dark:text-rose-300 dark:border-rose-800",
        cardClass:
          "border-rose-200/90 bg-rose-50/50 dark:border-rose-800/70 dark:bg-rose-950/25",
      };
  }
}

export function extractQuizErrorMessage(error: unknown): {
  message: string;
  isRateLimited: boolean;
  retryAfterSeconds?: number;
} {
  if (typeof error === "object" && error !== null) {
    const errObj = error as {
      response?: {
        status?: number;
        headers?: Record<string, string | undefined>;
        data?: { message?: string; error?: string };
      };
      message?: string;
    };
    const status = errObj.response?.status;
    const serverMsg = errObj.response?.data?.message || errObj.response?.data?.error;
    const retryHeader = errObj.response?.headers?.["retry-after"];
    const retryAfterSeconds = retryHeader ? Number(retryHeader) : undefined;

    if (status === 429) {
      return {
        message:
          serverMsg ||
          "Bạn vừa tạo bộ câu hỏi mới gần đây. Vui lòng đợi hết thời gian chờ trước khi tạo lại.",
        isRateLimited: true,
        retryAfterSeconds: Number.isFinite(retryAfterSeconds)
          ? retryAfterSeconds
          : undefined,
      };
    }

    if (serverMsg) {
      return { message: serverMsg, isRateLimited: false };
    }
    if (errObj.message) {
      return { message: errObj.message, isRateLimited: false };
    }
  }

  return {
    message: "Không thể kết nối máy chủ Quiz. Vui lòng thử lại.",
    isRateLimited: false,
  };
}
