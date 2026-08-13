import React, { useState } from 'react';
import { PracticeQuestionDto } from '../types';

interface Props {
  card: PracticeQuestionDto;
  onRate: (rating: number) => void;
}

export const FlashcardDeck: React.FC<Props> = ({ card, onRate }) => {
  const [flipped, setFlipped] = useState(false);

  return (
    <div className="flex flex-col items-center">
      <div
        onClick={() => setFlipped(!flipped)}
        className="w-full max-w-md h-80 cursor-pointer perspective-1000 my-4"
      >
        <div className={`relative w-full h-full duration-500 rounded-3xl shadow-2xl p-8 flex flex-col items-center justify-center text-center transition-all bg-gradient-to-br ${flipped ? 'from-indigo-600 to-blue-700 text-white' : 'from-white to-gray-50 dark:from-gray-900 dark:to-gray-800 text-gray-900 dark:text-white border border-gray-100 dark:border-gray-800'}`}>
          {!flipped ? (
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-blue-500">Mặt trước (Từ vựng)</span>
              <h2 className="text-4xl font-extrabold mt-4 mb-2">{card.prompt}</h2>
              {card.wordHint && <p className="text-sm font-mono text-gray-400">{card.wordHint}</p>}
              <p className="text-xs text-gray-400 mt-8">(Nhấn để xem mặt sau)</p>
            </div>
          ) : (
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-blue-200">Mặt sau (Nghĩa)</span>
              <h3 className="text-3xl font-bold mt-4 mb-3">{card.correctAnswer}</h3>
              {card.explanation && <p className="text-sm text-blue-100 mt-2">{card.explanation}</p>}
            </div>
          )}
        </div>
      </div>

      {flipped && (
        <div className="flex gap-4 mt-6">
          <button onClick={() => { setFlipped(false); onRate(1); }} className="px-5 py-2.5 bg-rose-500 text-white font-bold rounded-xl shadow-lg hover:bg-rose-600">
            Chưa nhớ (Khó)
          </button>
          <button onClick={() => { setFlipped(false); onRate(3); }} className="px-5 py-2.5 bg-amber-500 text-white font-bold rounded-xl shadow-lg hover:bg-amber-600">
            Tương đối (Vừa)
          </button>
          <button onClick={() => { setFlipped(false); onRate(5); }} className="px-5 py-2.5 bg-emerald-500 text-white font-bold rounded-xl shadow-lg hover:bg-emerald-600">
            Đã thuộc (Dễ)
          </button>
        </div>
      )}
    </div>
  );
};