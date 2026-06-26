export default function StreakSkeleton() {
  return (
    <div className="space-y-5">
      <div className="grid gap-5 xl:grid-cols-[1.15fr_0.82fr_1.25fr]">
        {[1, 2, 3].map((item) => (
          <div key={item} className="h-64 animate-pulse rounded-3xl border border-gray-100 bg-white" />
        ))}
      </div>
      <div className="h-72 animate-pulse rounded-3xl border border-gray-100 bg-white" />
      <div className="grid gap-5 xl:grid-cols-[1.25fr_1fr]">
        {[1, 2].map((item) => (
          <div key={item} className="h-72 animate-pulse rounded-3xl border border-gray-100 bg-white" />
        ))}
      </div>
    </div>
  );
}
