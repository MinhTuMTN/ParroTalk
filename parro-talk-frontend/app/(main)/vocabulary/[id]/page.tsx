"use client";

import React, { useState } from 'react';
import { useParams } from 'next/navigation';
import { VocabularyDetailView } from '../../../../features/vocabulary/components/VocabularyDetailView';
import { ReportModal } from '../../../../features/vocabulary/components/ReportModal';
import { useVocabularyDetail, useBookmarkVocabulary } from '../../../../features/vocabulary/hooks';
import * as api from '../../../../features/vocabulary/services/api';

export default function VocabularyDetailPage() {
  const params = useParams();
  const id = Array.isArray(params.id) ? params.id[0] : params.id;
  const { detail, loading } = useVocabularyDetail(id || '');
  const { bookmark } = useBookmarkVocabulary();
  const [reportOpen, setReportOpen] = useState(false);
  
  if (loading || !detail) return <div className="p-8 text-center text-gray-500 dark:text-gray-400">Loading vocabulary details...</div>;
  
  return (
    <div className="p-8">
      <VocabularyDetailView 
        vocab={detail} 
        onSave={() => bookmark(detail.id)} 
        onReport={() => setReportOpen(true)} 
      />
      <ReportModal 
        isOpen={reportOpen} 
        onClose={() => setReportOpen(false)} 
        onSubmit={(type, desc) => api.reportVocabulary(detail.id, { reportType: type, reason: type, description: desc })}
      />
    </div>
  );
}