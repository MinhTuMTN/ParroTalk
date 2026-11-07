/** Decorative passport + ticket illustration for the Continue Learning hero. */
export function TravelIllustration() {
  return (
    <svg viewBox="0 0 220 170" className="h-full w-full" aria-hidden="true">
      <defs>
        <linearGradient id="pt-passport" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#2e9a68" />
          <stop offset="100%" stopColor="#1a6545" />
        </linearGradient>
        <radialGradient id="pt-glow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#ffffff" stopOpacity="0.9" />
          <stop offset="100%" stopColor="#ffffff" stopOpacity="0" />
        </radialGradient>
      </defs>
      <circle cx="115" cy="88" r="74" fill="url(#pt-glow)" />
      <path d="M28 122 C 50 72, 80 60, 102 38" fill="none" stroke="#2e9a68" strokeWidth="1.5" strokeDasharray="2 6" strokeLinecap="round" opacity=".55" />
      <path d="M20 128 h8 M24 124 v8" stroke="#2e9a68" strokeWidth="1.5" strokeLinecap="round" opacity=".6" />
      <circle cx="192" cy="72" r="3" fill="#f0703f" opacity=".8" />
      <circle cx="44" cy="40" r="2.5" fill="#e3a21a" opacity=".8" />
      <path d="M150 28 L192 17 L168 41 L162 32 Z" fill="#ffffff" stroke="#b3e3c9" strokeWidth="1" />
      <g transform="rotate(-12 100 95)">
        <rect x="64" y="38" width="78" height="108" rx="10" fill="#12402e" opacity=".15" transform="translate(4 6)" />
        <rect x="62" y="35" width="78" height="108" rx="10" fill="url(#pt-passport)" />
        <rect x="62" y="35" width="10" height="108" rx="4" fill="#175139" opacity=".6" />
        <circle cx="104" cy="82" r="20" fill="none" stroke="#d8f2e3" strokeWidth="2.5" />
        <ellipse cx="104" cy="82" rx="9" ry="20" fill="none" stroke="#d8f2e3" strokeWidth="2" />
        <line x1="84" y1="82" x2="124" y2="82" stroke="#d8f2e3" strokeWidth="2" />
        <path d="M88 71 H120 M88 93 H120" stroke="#d8f2e3" strokeWidth="1.6" />
        <rect x="88" y="115" width="32" height="4" rx="2" fill="#82cea7" />
      </g>
      <g transform="rotate(-14 160 115)">
        <rect x="122" y="96" width="74" height="42" rx="7" fill="#ffffff" stroke="#dbe3de" />
        <line x1="176" y1="100" x2="176" y2="134" stroke="#c1ccc6" strokeDasharray="3 3" />
        <path d="M134 120 L160 112 L154 118 L162 124 L157 125 L149 120 L140 124 Z" fill="#1f7f54" />
        <path d="M182 106 v20 M187 106 v20" stroke="#c1ccc6" strokeWidth="1.5" />
      </g>
    </svg>
  );
}
