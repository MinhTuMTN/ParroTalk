import React, { useState } from 'react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (type: string, desc: string) => void;
}

export const ReportModal: React.FC<Props> = ({ isOpen, onClose, onSubmit }) => {
  const [type, setType] = useState('TRANSLATION');
  const [desc, setDesc] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(type, desc);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="bg-white dark:bg-gray-900 rounded-3xl p-8 max-w-lg w-full shadow-2xl border border-gray-100 dark:border-gray-800">
        <h2 className="text-2xl font-bold mb-4 text-gray-900 dark:text-white">Báo cáo sai phạm nội dung</h2>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold mb-2 dark:text-gray-300">Loại báo cáo</label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full p-3 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 dark:text-white"
            >
              <option value="TRANSLATION">Nghĩa tiếng Việt không chính xác</option>
              <option value="AUDIO">Âm thanh phát âm bị lỗi</option>
              <option value="DEFINITION">Định nghĩa tiếng Anh bị sai</option>
              <option value="OTHER">Lỗi khác</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold mb-2 dark:text-gray-300">Chi tiết mô tả</label>
            <textarea
              rows={4}
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
              placeholder="Vui lòng mô tả chi tiết lỗi bạn gặp phải..."
              className="w-full p-3 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 dark:text-white"
              required
            />
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800"
            >
              Hủy
            </button>
            <button
              type="submit"
              className="px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white font-semibold rounded-xl shadow-md"
            >
              Gửi báo cáo
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};