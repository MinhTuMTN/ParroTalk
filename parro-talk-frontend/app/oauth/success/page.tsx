import { Suspense } from "react";

import OAuthSuccessClient from "./success-client";

function OAuthSuccessFallback() {
  return (
    <div className="min-h-screen bg-[#FDFDFD] flex items-center justify-center p-6">
      <div className="text-sm font-bold text-gray-500">Signing you in...</div>
    </div>
  );
}

export default function OAuthSuccessPage() {
  return (
    <Suspense fallback={<OAuthSuccessFallback />}>
      <OAuthSuccessClient />
    </Suspense>
  );
}
