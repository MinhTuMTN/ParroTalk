"use client";

import React, { useState } from "react";
import { useParams } from "next/navigation";
import { VocabularyDetailView } from "../../../../features/vocabulary/components/VocabularyDetailView";
import { VocabularyDetailSkeleton } from "../../../../features/vocabulary/components/word-detail";
import { ReportModal } from "../../../../features/vocabulary/components/ReportModal";
import { useVocabularyDetail, useBookmarkVocabulary } from "../../../../features/vocabulary/hooks";
import * as api from "../../../../features/vocabulary/services/api";

export default function VocabularyDetailPage() {
  const params = useParams();
  const id = Array.isArray(params.id) ? params.id[0] : params.id;
  const { detail, loading } = useVocabularyDetail(id || "");
  const { bookmark } = useBookmarkVocabulary();
  const [reportOpen, setReportOpen] = useState(false);

  // When loading and data is not ready, render the polished skeleton
  if (loading && !detail && id?.toLowerCase() !== "issue") {
    return (
      <div className="min-h-screen bg-[#fbfcfb] py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-5xl mx-auto">
          <VocabularyDetailSkeleton />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fbfcfb] py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">
        <VocabularyDetailView
          vocab={detail || undefined}
          onSave={() => detail && bookmark(detail.id)}
          onReport={() => setReportOpen(true)}
        />

        <ReportModal
          isOpen={reportOpen}
          onClose={() => setReportOpen(false)}
          onSubmit={(type, desc) =>
            detail &&
            api.reportVocabulary(detail.id, {
              reportType: type,
              reason: type,
              description: desc,
            })
          }
        />
      </div>
    </div>
  );
}