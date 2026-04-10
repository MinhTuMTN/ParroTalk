"use client";

import { useRef, useEffect, useState } from "react";

interface DictationInputProps {
  onSubmit: (text: string) => void;
  disabled?: boolean;
}

export default function DictationInput({ onSubmit, disabled = false }: DictationInputProps) {
  const [value, setValue] = useState("");
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (!disabled) {
      inputRef.current?.focus();
    }
  }, [disabled]);

  const handleSubmit = () => {
    if (value.trim()) {
      onSubmit(value);
      setValue("");
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="w-full max-w-2xl flex flex-col gap-4">
      <textarea
        ref={inputRef}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        placeholder="Type what you hear..."
        className="w-full h-32 p-6 rounded-3xl border-2 border-gray-100 focus:border-green-400 focus:ring-0 resize-none text-lg transition-all outline-none bg-white shadow-sm placeholder:text-gray-300"
      />
      
      <button
        onClick={handleSubmit}
        disabled={disabled || !value.trim()}
        className={`
          self-end px-10 py-4 rounded-2xl font-bold text-white transition-all
          ${disabled || !value.trim() 
            ? "bg-gray-200 cursor-not-allowed" 
            : "bg-green-500 hover:bg-green-600 shadow-lg shadow-green-100 active:transform active:scale-95"}
        `}
      >
        Check Answer
      </button>
    </div>
  );
}
