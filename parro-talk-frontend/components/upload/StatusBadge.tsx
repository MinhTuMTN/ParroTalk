import React from 'react';

interface StatusBadgeProps {
  status: 'PROCESSING' | 'DONE' | 'FAILED' | 'CONNECTING' | 'IDLE';
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  let dotColor = 'bg-gray-400';
  let bgColor = 'bg-gray-100';
  let textColor = 'text-gray-600';
  let label = status;

  if (status === 'PROCESSING' || status === 'CONNECTING') {
    dotColor = 'bg-blue-500 animate-pulse';
    bgColor = 'bg-blue-50';
    textColor = 'text-blue-700';
  } else if (status === 'DONE') {
    dotColor = 'bg-green-500';
    bgColor = 'bg-green-50';
    textColor = 'text-green-700';
  } else if (status === 'FAILED') {
    dotColor = 'bg-red-500';
    bgColor = 'bg-red-50';
    textColor = 'text-red-700';
  }

  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full ${bgColor}`}>
      <div className={`w-2 h-2 rounded-full ${dotColor}`} />
      <span className={`text-xs font-bold tracking-wider ${textColor}`}>
        {label}
      </span>
    </div>
  );
}
