import {
  Footer,
  Header,
  HeroSection,
  HowItWorksSection,
  PwaGuideSection,
  SocialProofSection,
} from "@/components/landing";

export default function HomePage() {
  return (
    <main className="min-h-screen bg-slate-50 text-slate-900 selection:bg-emerald-200/80 selection:text-emerald-950 font-sans">
      <Header />
      <HeroSection />
      <SocialProofSection />
      <HowItWorksSection />
      <PwaGuideSection />
      <Footer />
    </main>
  );
}
