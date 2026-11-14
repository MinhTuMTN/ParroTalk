export type UiLanguage = "vi" | "en";
export type TranslationLanguage = "vi" | "en" | "ja" | "ko" | "zh";
export type ThemePreference = "light" | "dark" | "system";
export type PreferredAccent = "US" | "UK";
export type UsVoiceOption =
  | "en-US-JennyNeural"
  | "en-US-GuyNeural"
  | "en-US-AriaNeural";
export type UkVoiceOption =
  | "en-GB-SoniaNeural"
  | "en-GB-RyanNeural"
  | "en-GB-LibbyNeural";
export type DictationReplayKey = "Escape" | "Tab" | "Control";
export type DefaultWritingMode = "SUGGESTION" | "GUIDANCE";
export type DefaultDifficultyScale = "CEFR" | "IELTS";
export type DefaultFlashcardDirection = "EN_VI" | "VI_EN";

export interface UserSettings {
  uiLanguage: UiLanguage;
  translationLanguage: TranslationLanguage;
  theme: ThemePreference;
  preferredAccent: PreferredAccent;
  preferredUsVoice: UsVoiceOption;
  preferredUkVoice: UkVoiceOption;
  playbackSpeed: number;
  autoPlayVocabAudio: boolean;
  dictationAutoAdvance: boolean;
  dictationShowTranslation: boolean;
  dictationAutoReplay: boolean;
  dictationReplayKey: DictationReplayKey;
  defaultWritingMode: DefaultWritingMode;
  defaultDifficultyScale: DefaultDifficultyScale;
  defaultTargetLevel: string;
  vocabSessionSize: number;
  defaultFlashcardDirection: DefaultFlashcardDirection;
  dailyGoal: number;
}

export const DEFAULT_USER_SETTINGS: UserSettings = {
  uiLanguage: "vi",
  translationLanguage: "vi",
  theme: "light",
  preferredAccent: "US",
  preferredUsVoice: "en-US-JennyNeural",
  preferredUkVoice: "en-GB-SoniaNeural",
  playbackSpeed: 1.0,
  autoPlayVocabAudio: false,
  dictationAutoAdvance: false,
  dictationShowTranslation: true,
  dictationAutoReplay: false,
  dictationReplayKey: "Escape",
  defaultWritingMode: "SUGGESTION",
  defaultDifficultyScale: "CEFR",
  defaultTargetLevel: "B2",
  vocabSessionSize: 10,
  defaultFlashcardDirection: "EN_VI",
  dailyGoal: 20,
};

export interface VoiceOptionItem<T extends string> {
  value: T;
  label: string;
  gender: Record<UiLanguage, string>;
  note: Record<UiLanguage, string>;
}

export const US_VOICE_OPTIONS: VoiceOptionItem<UsVoiceOption>[] = [
  {
    value: "en-US-JennyNeural",
    label: "Jenny (en-US-JennyNeural)",
    gender: { vi: "Nữ", en: "Female" },
    note: { vi: "Tự nhiên, rõ ràng", en: "Natural, crisp" },
  },
  {
    value: "en-US-GuyNeural",
    label: "Guy (en-US-GuyNeural)",
    gender: { vi: "Nam", en: "Male" },
    note: { vi: "Trầm ấm, chuẩn Mỹ", en: "Warm, standard US" },
  },
  {
    value: "en-US-AriaNeural",
    label: "Aria (en-US-AriaNeural)",
    gender: { vi: "Nữ", en: "Female" },
    note: { vi: "Truyền cảm, linh hoạt", en: "Expressive, clear" },
  },
];

export const UK_VOICE_OPTIONS: VoiceOptionItem<UkVoiceOption>[] = [
  {
    value: "en-GB-SoniaNeural",
    label: "Sonia (en-GB-SoniaNeural)",
    gender: { vi: "Nữ", en: "Female" },
    note: { vi: "Chuẩn Anh - Anh, dễ nghe", en: "Clear British English" },
  },
  {
    value: "en-GB-RyanNeural",
    label: "Ryan (en-GB-RyanNeural)",
    gender: { vi: "Nam", en: "Male" },
    note: { vi: "Lịch lãm, mạch lạc", en: "Articulate, warm" },
  },
  {
    value: "en-GB-LibbyNeural",
    label: "Libby (en-GB-LibbyNeural)",
    gender: { vi: "Nữ", en: "Female" },
    note: { vi: "Trẻ trung, tự nhiên", en: "Bright, conversational" },
  },
];

export const PLAYBACK_SPEED_OPTIONS = [0.75, 0.9, 1.0, 1.15, 1.25, 1.5] as const;

export const REPLAY_KEY_OPTIONS: {
  value: DictationReplayKey;
  label: string;
  shortLabel: string;
}[] = [
  { value: "Escape", label: "Escape (Esc)", shortLabel: "Esc" },
  { value: "Tab", label: "Tab", shortLabel: "Tab" },
  { value: "Control", label: "Control (Ctrl)", shortLabel: "Ctrl" },
];

export const TRANSLATION_LANGUAGE_OPTIONS: {
  value: TranslationLanguage;
  label: Record<UiLanguage, string>;
}[] = [
  { value: "vi", label: { vi: "Tiếng Việt", en: "Vietnamese" } },
  { value: "en", label: { vi: "Tiếng Anh", en: "English" } },
  { value: "ja", label: { vi: "Tiếng Nhật", en: "Japanese" } },
  { value: "ko", label: { vi: "Tiếng Hàn", en: "Korean" } },
  { value: "zh", label: { vi: "Tiếng Trung", en: "Chinese" } },
];

export const VOCAB_SESSION_SIZE_OPTIONS = [5, 10, 15, 20] as const;
export const DAILY_GOAL_OPTIONS = [5, 10, 15, 20, 30, 45, 60] as const;

export interface UpdateUserProfilePayload {
  fullName: string;
  username?: string;
  avatarUrl?: string;
}

export interface ChangePasswordPayload {
  currentPassword?: string;
  newPassword: string;
  confirmPassword?: string;
}

export interface UserProfileUpdateResult {
  id?: string;
  email?: string;
  fullName?: string;
  username?: string;
  avatarUrl?: string | null;
  role?: string;
  emailVerified?: boolean;
  hasPassword?: boolean;
}

export const SETTINGS_I18N = {
  vi: {
    pageTitle: "Cài đặt",
    pageSubtitle: "Cá nhân hóa giọng đọc, chép chính tả, trợ lý viết AI, giao diện và tài khoản của bạn.",
    resetDefaults: "Khôi phục mặc định",
    saving: "Đang lưu...",
    savedNotice: "Đã lưu cài đặt",
    saveErrorNotice: "Không thể đồng bộ cài đặt lên máy chủ, đã lưu tạm trên thiết bị.",
    resetDoneNotice: "Đã khôi phục cài đặt mặc định.",
    tabs: {
      voice: "Giọng đọc & Âm thanh",
      dictation: "Chép chính tả (Dictation)",
      learning: "Học tập & Trợ lý AI",
      appearance: "Ngôn ngữ & Giao diện",
      account: "Hồ sơ & Bảo mật",
    },
    nav: {
      library: "Thư viện",
      vocabulary: "Từ vựng",
      writing: "Luyện viết",
      practice: "Ôn tập",
      admin: "Quản trị",
      upload: "Tải lên",
      settings: "Cài đặt",
      profile: "Hồ sơ",
      logout: "Đăng xuất",
      upgradeToPro: "Nâng cấp Pro",
    },
    voice: {
      title: "Giọng đọc & Âm thanh",
      subtitle: "Tùy chỉnh giọng đọc Edge-TTS ưu tiên và tốc độ phát âm thanh trên toàn hệ thống.",
      preferredAccentLabel: "Giọng ưu tiên mặc định",
      preferredAccentDesc: "Hiển thị trước hoặc phát tự động theo chuẩn Mỹ (US) hoặc Anh (UK).",
      usOption: "US (Giọng Mỹ)",
      ukOption: "UK (Giọng Anh)",
      usVoiceLabel: "Giọng đọc US (Edge-TTS)",
      ukVoiceLabel: "Giọng đọc UK (Edge-TTS)",
      previewBtn: "Nghe thử",
      stopPreviewBtn: "Dừng",
      playbackSpeedLabel: "Tốc độ phát âm thanh",
      playbackSpeedDesc: "Áp dụng cho trình phát audio trong Practice, Writing Assistant và Vocabulary.",
      autoPlayVocabLabel: "Tự động phát âm từ vựng",
      autoPlayVocabDesc: "Tự động đọc từ vựng khi chuyển thẻ mới trong phiên học từ.",
    },
    dictation: {
      title: "Chép chính tả (Dictation)",
      subtitle: "Điều chỉnh nhịp độ làm bài và phím tắt khi luyện nghe - chép chính tả.",
      autoAdvanceLabel: "Tự động chuyển câu tiếp theo",
      autoAdvanceDesc: "Sau khi hoàn thành chính xác câu hiện tại, tự chuyển sang câu kế tiếp sau 350ms.",
      showTranslationLabel: "Hiển thị bản dịch ngay khi hoàn thành",
      showTranslationDesc: "Nếu tắt, bạn có thể nhấn \"Hiện bản dịch\" để chủ động kiểm tra sau.",
      autoReplayLabel: "Tự động phát lại câu (Auto replay)",
      autoReplayDesc: "Đồng bộ với nút Auto replay trong trình phát video luyện tập.",
      replayKeyLabel: "Phím tắt phát lại câu (Replay key)",
      replayKeyDesc: "Nhấn phím này bất cứ lúc nào trong trang Practice để nghe lại câu đang làm.",
    },
    learning: {
      title: "Học tập & Trợ lý AI",
      subtitle: "Thiết lập mặc định cho Writing Assistant, luyện từ vựng và mục tiêu mỗi ngày.",
      defaultWritingModeLabel: "Chế độ Writing Assistant mặc định",
      suggestionMode: "Gợi ý (Suggestion)",
      guidanceMode: "Hướng dẫn (Guidance)",
      defaultScaleLabel: "Thang đo trình độ mặc định",
      defaultTargetLevelLabel: "Mức độ mục tiêu mặc định",
      vocabSessionSizeLabel: "Số từ mỗi phiên học Vocabulary",
      wordsUnit: "từ",
      flashcardDirectionLabel: "Chiều học Flashcard mặc định",
      enViLabel: "Anh -> Việt (EN -> VI)",
      viEnLabel: "Việt -> Anh (VI -> EN)",
      dailyGoalLabel: "Mục tiêu học mỗi ngày",
      minutesUnit: "từ / ngày",
    },
    appearance: {
      title: "Ngôn ngữ & Giao diện",
      subtitle: "Thay đổi ngôn ngữ hiển thị, ngôn ngữ dịch và chủ đề sáng/tối.",
      uiLanguageLabel: "Ngôn ngữ giao diện (UI Language)",
      uiLanguageDesc: "Cập nhật ngay nhãn giao diện trên trang Cài đặt và các điều khiển hỗ trợ.",
      translationLanguageLabel: "Ngôn ngữ dịch mặc định",
      translationLanguageDesc: "Ngôn ngữ đích khi dịch câu và giải thích từ vựng.",
      themeLabel: "Giao diện (Theme)",
      themeLight: "Sáng (Light)",
      themeDark: "Tối (Dark)",
      themeSystem: "Theo hệ thống (System)",
    },
    account: {
      title: "Hồ sơ & Bảo mật",
      subtitle: "Cập nhật thông tin hiển thị và mật khẩu đăng nhập tài khoản của bạn.",
      profileSection: "Thông tin hồ sơ",
      emailLabel: "Email",
      fullNameLabel: "Họ và tên",
      usernameLabel: "Tên người dùng (Username)",
      avatarUrlLabel: "Ảnh đại diện (URL)",
      saveProfileBtn: "Lưu hồ sơ",
      savingProfileBtn: "Đang lưu...",
      profileUpdated: "Đã cập nhật hồ sơ thành công.",
      profileError: "Không thể cập nhật hồ sơ. Vui lòng thử lại.",
      fullNameRequired: "Họ và tên không được để trống.",
      securitySection: "Đổi mật khẩu",
      createPasswordSection: "Tạo mật khẩu",
      oauthNoPasswordHint: "Tài khoản này đăng nhập bằng liên kết ngoài và chưa có mật khẩu riêng. Bạn có thể thiết lập mật khẩu mới bên dưới.",
      currentPasswordLabel: "Mật khẩu hiện tại",
      newPasswordLabel: "Mật khẩu mới",
      confirmPasswordLabel: "Xác nhận mật khẩu mới",
      changePasswordBtn: "Cập nhật mật khẩu",
      createPasswordBtn: "Tạo mật khẩu",
      changingPasswordBtn: "Đang cập nhật...",
      passwordMismatch: "Mật khẩu xác nhận không khớp.",
      passwordTooShort: "Mật khẩu mới cần tối thiểu 6 ký tự.",
      currentPasswordRequired: "Vui lòng nhập mật khẩu hiện tại.",
      passwordUpdated: "Đã đổi mật khẩu thành công.",
      passwordError: "Không thể đổi mật khẩu. Vui lòng kiểm tra lại thông tin.",
    },
  },
  en: {
    pageTitle: "Settings",
    pageSubtitle: "Personalize voices, dictation behavior, AI writing defaults, appearance, and your account.",
    resetDefaults: "Reset to defaults",
    saving: "Saving...",
    savedNotice: "Settings saved",
    saveErrorNotice: "Could not sync to server; changes are saved locally.",
    resetDoneNotice: "Settings reset to defaults.",
    tabs: {
      voice: "Voice & Audio",
      dictation: "Dictation",
      learning: "Learning & AI",
      appearance: "Language & Theme",
      account: "Profile & Security",
    },
    nav: {
      library: "Library",
      vocabulary: "Vocabulary",
      writing: "Writing",
      practice: "Practice",
      admin: "Admin",
      upload: "Upload",
      settings: "Settings",
      profile: "Profile",
      logout: "Logout",
      upgradeToPro: "Upgrade to Pro",
    },
    voice: {
      title: "Voice & Audio",
      subtitle: "Configure your preferred Edge-TTS voices and default playback speed across the app.",
      preferredAccentLabel: "Default preferred accent",
      preferredAccentDesc: "Orders US or UK playback controls first and sets default voice accent.",
      usOption: "US (American)",
      ukOption: "UK (British)",
      usVoiceLabel: "US Voice (Edge-TTS)",
      ukVoiceLabel: "UK Voice (Edge-TTS)",
      previewBtn: "Preview",
      stopPreviewBtn: "Stop",
      playbackSpeedLabel: "Playback speed",
      playbackSpeedDesc: "Applied to audio playback in Practice, Writing Assistant, and Vocabulary.",
      autoPlayVocabLabel: "Auto-play vocabulary audio",
      autoPlayVocabDesc: "Automatically pronounce words when advancing through Learn Words cards.",
    },
    dictation: {
      title: "Dictation",
      subtitle: "Customize pacing, translation reveal, and keyboard shortcuts while practicing.",
      autoAdvanceLabel: "Auto-advance to next sentence",
      autoAdvanceDesc: "Automatically advance 350ms after completing a sentence correctly.",
      showTranslationLabel: "Show translation on sentence completion",
      showTranslationDesc: "When off, click \"Show translation\" to reveal the translated sentence.",
      autoReplayLabel: "Auto-replay sentence",
      autoReplayDesc: "Stays synced with the Auto replay toggle in the Practice video player.",
      replayKeyLabel: "Replay shortcut key",
      replayKeyDesc: "Press this key anytime during Practice to replay the active segment.",
    },
    learning: {
      title: "Learning & AI Assistant",
      subtitle: "Set default options for Writing Assistant, Vocabulary sessions, and your daily study goal.",
      defaultWritingModeLabel: "Default Writing Assistant mode",
      suggestionMode: "Suggestion",
      guidanceMode: "Guidance",
      defaultScaleLabel: "Default proficiency scale",
      defaultTargetLevelLabel: "Default target level",
      vocabSessionSizeLabel: "Words per Vocabulary session",
      wordsUnit: "words",
      flashcardDirectionLabel: "Default flashcard direction",
      enViLabel: "English -> Vietnamese (EN -> VI)",
      viEnLabel: "Vietnamese -> English (VI -> EN)",
      dailyGoalLabel: "Daily study goal",
      minutesUnit: "words / day",
    },
    appearance: {
      title: "Language & Appearance",
      subtitle: "Switch UI language, default translation target, and light/dark theme.",
      uiLanguageLabel: "Interface language",
      uiLanguageDesc: "Immediately updates Settings labels and supported interface controls.",
      translationLanguageLabel: "Default translation language",
      translationLanguageDesc: "Target language used for sentence translations and word lookups.",
      themeLabel: "Theme",
      themeLight: "Light",
      themeDark: "Dark",
      themeSystem: "System",
    },
    account: {
      title: "Profile & Security",
      subtitle: "Update your profile information and login credentials.",
      profileSection: "Profile information",
      emailLabel: "Email",
      fullNameLabel: "Full name",
      usernameLabel: "Username",
      avatarUrlLabel: "Avatar URL",
      saveProfileBtn: "Save profile",
      savingProfileBtn: "Saving...",
      profileUpdated: "Profile updated successfully.",
      profileError: "Failed to update profile. Please try again.",
      fullNameRequired: "Full name is required.",
      securitySection: "Change password",
      createPasswordSection: "Create password",
      oauthNoPasswordHint: "This account uses external sign-in and does not have a local password yet. You can set one below.",
      currentPasswordLabel: "Current password",
      newPasswordLabel: "New password",
      confirmPasswordLabel: "Confirm new password",
      changePasswordBtn: "Update password",
      createPasswordBtn: "Create password",
      changingPasswordBtn: "Updating...",
      passwordMismatch: "Passwords do not match.",
      passwordTooShort: "New password must be at least 6 characters.",
      currentPasswordRequired: "Please enter your current password.",
      passwordUpdated: "Password updated successfully.",
      passwordError: "Failed to update password. Please verify your inputs.",
    },
  },
} as const;

export function getSettingsCopy(language: UiLanguage) {
  return SETTINGS_I18N[language] ?? SETTINGS_I18N.vi;
}
