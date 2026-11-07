---
name: ParroTalk Sage & Mint Design System
colors:
  surface: '#F8FAF9'
  surface-dim: '#E8EFEA'
  surface-bright: '#FFFFFF'
  surface-container-lowest: '#FFFFFF'
  surface-container-low: '#F2F7F4'
  surface-container: '#EAF3ED'
  surface-container-high: '#DFECE3'
  surface-container-highest: '#D2E3D8'
  on-surface: '#111827'
  on-surface-variant: '#4B5563'
  inverse-surface: '#1F2937'
  inverse-on-surface: '#F9FAFB'
  outline: '#9CA3AF'
  outline-variant: '#E5E7EB'
  surface-tint: '#3B9E66'
  primary: '#3B9E66'
  on-primary: '#FFFFFF'
  primary-container: '#4EAA78'
  on-primary-container: '#064E3B'
  inverse-primary: '#A7F3D0'
  secondary: '#FF7A59'
  on-secondary: '#FFFFFF'
  secondary-container: '#FFE8E0'
  on-secondary-container: '#9A3412'
  tertiary: '#F59E0B'
  on-tertiary: '#FFFFFF'
  tertiary-container: '#FEF3C7'
  on-tertiary-container: '#78350F'
  error: '#EF4444'
  on-error: '#FFFFFF'
  error-container: '#FEE2E2'
  on-error-container: '#991B1B'
  primary-fixed: '#D1FAE5'
  primary-fixed-dim: '#A7F3D0'
  on-primary-fixed: '#064E3B'
  on-primary-fixed-variant: '#047857'
  secondary-fixed: '#FFEDD5'
  secondary-fixed-dim: '#FED7AA'
  on-secondary-fixed: '#7C2D12'
  on-secondary-fixed-variant: '#C2410C'
  tertiary-fixed: '#FEF3C7'
  tertiary-fixed-dim: '#FDE68A'
  on-tertiary-fixed: '#78350F'
  on-tertiary-fixed-variant: '#B45309'
  background: '#F8FAF9'
  on-background: '#111827'
  surface-variant: '#EAF3ED'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 44px
    fontWeight: '800'
    lineHeight: 52px
    letterSpacing: -0.025em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 30px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 26px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.02em
rounded:
  sm: 0.375rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.25rem
  '2xl': 1.5rem
  '3xl': 2rem
  full: 9999px
spacing:
  unit: 4px
  container-max: 1280px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 32px
---

# ParroTalk Redesign Specification: Sage & Mint Educational Aesthetic

## 1. Executive Summary & Aesthetic Direction
This redesign transforms ParroTalk's visual design into a **warm, organic, approachable, and engaging EdTech experience** inspired by modern top-tier learning applications. The design combines:
- **Friendly & Tactile UI**: Organic curves (`rounded-2xl`, `rounded-3xl`), soft ambient drop-shadows, clean border outlines (`border-slate-200/80` / `border-emerald-100`), and interactive micro-animations.
- **Nature-Inspired Palette**: Calming Sage Green (`#3B9E66` / `#4EAA78`), Fresh Mint accents (`#A7F3D0` / `#D1FAE5`), warm Coral/Peach highlights (`#FF7A59` / `#FFE8E0`), and warm Slate neutrals.
- **Delightful Visual Assets**: Clean 2D/3D flat character illustrations representing students engaged in listening, reading, and learning.
- **Content & Brand Preservation**: Preserves 100% of existing ParroTalk copy, SEO metadata, routes, auth flows, and core features (Interactive Dictation Playground, 3-Step Flow, PWA Guide, Statistics, Header/Footer navigation).

---

## 2. Color System & Design Tokens

### Primary Palette (Growth & Focus)
- `primary.50`: `#F0FDF4` (Soft tint background)
- `primary.100`: `#DCFCE7` (Badge background)
- `primary.200`: `#BBF7D0` (Border & highlight)
- `primary.500`: `#22C55E` (Vibrant accent)
- `primary.600`: `#3B9E66` (Primary Brand & CTA default)
- `primary.700`: `#047857` (Hover & dark tone)
- `primary.800`: `#065F46` (High contrast text on mint)

### Secondary & Accent Palette (Warmth, Momentum & Badges)
- `coral.500`: `#FF7A59` (Warm badge & streak indicator)
- `amber.500`: `#F59E0B` (Hot highlights, rating stars)
- `rose.500`: `#EF4444` (Status alert & unpublished tags)

### Neutral & Surface Palette
- `background.page`: `#F8FAF9` (Warm tinted off-white base)
- `background.surface`: `#FFFFFF` (Pure white card surface)
- `background.cardMuted`: `#F2F7F4` (Soft mint tinted container)
- `text.primary`: `#111827` (Slate 900 - High legibility)
- `text.secondary`: `#4B5563` (Slate 600 - Subtitles & descriptions)
- `text.muted`: `#9CA3AF` (Slate 400 - Timestamps & inactive labels)

---

## 3. Component Architecture & UI Elements

### 3.1. Navigation Header
- **Desktop**: Sticky top header with frosted backdrop blur (`bg-white/90 backdrop-blur-md`), ParroTalk logo with clean typography, nav links with active indicator pill, and quick Auth/Profile button.
- **Mobile**: Minimalist top header with brand logo & user avatar + docked/floating bottom navigation pill bar with icons (`Home`, `Library`, `Practice`, `Profile`).

### 3.2. Hero Section (Responsive Multi-Device)
- **Badge**: `🔥 Khám phá phương pháp luyện nghe tương tác mới` with soft amber accent.
- **Headline**: High-impact bold typography with emerald highlight.
- **Action Group**: Primary 3D-styled pill button ("Bắt đầu luyện tập") + secondary pill with play icon ("Xem Demo").
- **Featured Learning Banner Card** (as seen in sample):
  - Split banner layout: Left side features organic character illustration; Right side features live indicator badge (`● Live on after 2 hour`), class title, progress bar (`Step 06/10`), and floating circular play button.
- **Interactive Audio Playground**:
  - Live waveform visualizer with animated emerald equalizer bars.
  - Multi-speed toggle chips (`0.5x`, `1x`, `1.5x`, `2x`).
  - Loop toggle button with active state.
  - Interactive typing box with live Heat Map indicator and accuracy gradient bar.

### 3.3. Social Proof & Statistics Strip
- Horizontal card container with 4 core metrics (`10,000+ Lượt luyện tập`, `500+ Bài học`, `3x Tốc độ phản xạ`, `100% Phụ đề AI`).
- Clean dividers with soft mint icons and high-contrast typography.

### 3.4. 3-Step Methodology Section ("Quy trình 3 bước vàng")
- Cards with subtle rotation or organic elevation, custom pastel icon containers, step badge (`Bước 1`, `Bước 2`, `Bước 3`), and engaging descriptions.

### 3.5. Interactive Schedule & Daily Practice Widget (New Feature Module)
- Horizontal date selector strip (`Fri 30`, `Sat 31`, `Sun 01`, `Mon 02 (Active)`, `Tue 03`...).
- Timeline schedule cards showing curated daily lessons (e.g. `08:00 AM Science Dictation`, `09:00 AM Daily Conversation`).

### 3.6. PWA Installation Guide Section
- 4-step visual cards with screenshots and badge steps.
- Hover lift effect with soft emerald border highlight.

### 3.7. Footer
- Dark modern slate footer (`#0F172A`) with ParroTalk branding, quick navigation links, contact email, and copyright.

---

## 4. Multi-Device Layout Specifications

### Mobile (< 768px)
- Single column layout, generous touch targets (min 44px).
- Floating pill bottom navigation.
- Horizontal scroll for date selector and chips with hidden scrollbars.

### Tablet (768px - 1024px)
- 2-column grid for featured cards and steps.
- Integrated header with collapse menu.

### Desktop (1024px+)
- Max container width 1280px centered.
- Rich 3-column / 4-column grids with expansive whitespace and rich interactive widgets.
