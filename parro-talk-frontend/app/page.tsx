import {
  Footer,
  Header,
  HeroSection,
  HowItWorksSection,
  MobileBottomNav,
  PwaGuideSection,
  SocialProofSection,
} from "@/components/landing";

export default function HomePage() {
  return (
    <main className="min-h-screen bg-[#F8FAF9] text-slate-900 selection:bg-emerald-200/80 selection:text-emerald-950 font-sans pb-16 md:pb-0">
      <Header />
      <HeroSection />
      <SocialProofSection />
      <HowItWorksSection />
      <PwaGuideSection />
      <Footer />
      <MobileBottomNav />
    </main>
  );
}
