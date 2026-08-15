import { Headphones, SpellCheck, Video, type LucideIcon } from "lucide-react";

export interface NavItem {
  label: string;
  href: string;
}

export interface StatItem {
  value: string;
  label: string;
}

export interface StepItem {
  icon: LucideIcon;
  step: string;
  title: string;
  description: string;
  rotation: string;
}

export interface PwaStepItem {
  step: string;
  title: string;
  description: string;
  image: string;
  alt: string;
  highlight?: string;
}

export const navItems: NavItem[] = [
  { label: "Khám phá", href: "/library" },
  { label: "Tính năng", href: "#how-it-works" },
  { label: "Cài đặt App", href: "#pwa-guide" },
  { label: "Về chúng tôi", href: "#footer" },
];

export const stats: StatItem[] = [
  { value: "10,000+", label: "Lượt luyện tập mỗi ngày" },
  { value: "500+", label: "Bài học đa dạng" },
  { value: "3x", label: "Tốc độ phản xạ" },
  { value: "100%", label: "Phụ đề AI & Song ngữ" },
];

export const steps: StepItem[] = [
  {
    icon: Video,
    step: "Bước 1",
    title: "Chọn bài học",
    description:
      "Chọn video phù hợp trình độ từ kho nội dung phong phú: TED, Phim, Tin tức và Hội thoại đời thường.",
    rotation: "md:rotate-1 md:hover:rotate-0",
  },
  {
    icon: Headphones,
    step: "Bước 2",
    title: "Nghe & Gõ",
    description:
      "Nghe từng câu ngắn, tập trung cao độ và gõ lại chính xác những gì bạn nghe được với bộ điều tốc linh hoạt.",
    rotation: "md:-rotate-1 md:hover:rotate-0",
  },
  {
    icon: SpellCheck,
    step: "Bước 3",
    title: "Kiểm tra lỗi",
    description:
      "AI chấm điểm tức thì, chỉ ra lỗi sai nối âm, từ vựng và giải thích ngữ cảnh chi tiết kèm dịch nghĩa tiếng Việt.",
    rotation: "md:rotate-1 md:hover:rotate-0",
  },
];

export const pwaSteps: PwaStepItem[] = [
  {
    step: "Bước 1",
    title: "Mở trình duyệt",
    description:
      "Mở trình duyệt Safari hoặc Chrome trên thiết bị di động của bạn.",
    image: "/images/pwa/step-1.png",
    alt: "Mở trình duyệt Safari trên iPhone",
  },
  {
    step: "Bước 2",
    title: "Chọn Chia sẻ",
    description:
      "Nhấn vào biểu tượng 'Chia sẻ' (Share) ở thanh điều hướng phía dưới màn hình.",
    image: "/images/pwa/step-2.png",
    alt: "Nhấn biểu tượng Chia sẻ",
    highlight: "Chia sẻ",
  },
  {
    step: "Bước 3",
    title: "Thêm vào MH chính",
    description:
      "Cuộn xuống danh sách tùy chọn và chọn 'Thêm vào MH chính' (Add to Home Screen).",
    image: "/images/pwa/step-3.png",
    alt: "Chọn Thêm vào Màn hình chính",
    highlight: "Thêm vào MH chính",
  },
  {
    step: "Bước 4",
    title: "Xác nhận hoàn tất",
    description:
      "Nhấn nút 'Thêm' ở góc trên bên phải để hoàn tất cài đặt ứng dụng.",
    image: "/images/pwa/step-4.png",
    alt: "Nhấn nút Thêm để hoàn tất",
    highlight: "Thêm",
  },
];
