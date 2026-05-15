import React from 'react';

interface ProcessingCardProps {
  children: React.ReactNode;
  className?: string;
}

export default function ProcessingCard({ children, className = '' }: ProcessingCardProps) {
  return (
    <div className={`bg-white rounded-3xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-50 flex flex-col items-center justify-center ${className}`}>
      {children}
    </div>
  );
}

