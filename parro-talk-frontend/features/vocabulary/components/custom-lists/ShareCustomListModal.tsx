"use client";

import { useState } from "react";
import { Check, Copy, Link2, Mail, Shield, X } from "lucide-react";
import type { ShareEntryDto } from "../../types/customList";

export function ShareCustomListModal({
  open,
  listId,
  listTitle,
  shareToken,
  shares = [],
  onClose,
  onShareSubmit,
}: {
  open: boolean;
  listId: string;
  listTitle: string;
  shareToken?: string;
  shares?: ShareEntryDto[];
  onClose: () => void;
  onShareSubmit: (payload: {
    recipientEmailOrUsername?: string;
    enableShareLink?: boolean;
  }) => Promise<void>;
}) {
  const [recipient, setRecipient] = useState("");
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) return null;

  const shareUrl =
    typeof window !== "undefined" && shareToken
      ? `${window.location.origin}/vocabulary/lists/${listId}?shareToken=${shareToken}`
      : "";

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recipient.trim()) return;
    setLoading(true);
    setError(null);
    try {
      await onShareSubmit({
        recipientEmailOrUsername: recipient.trim(),
        emailOrUsername: recipient.trim(),
      });
      setRecipient("");
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Không tìm thấy người dùng này";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleLink = async (enable: boolean) => {
    setLoading(true);
    try {
      await onShareSubmit({ enableShareLink: enable });
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    if (!shareUrl) return;
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/50 p-4 backdrop-blur-xs">
      <div className="w-full max-w-lg rounded-2xl border border-ink-200 bg-white p-6 shadow-xl">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-ink-900">
              Chia sẻ &ldquo;{listTitle}&rdquo;
            </h2>
            <p className="text-xs text-ink-500">
              Người được chia sẻ có quyền Học &amp; Luyện tập (Viewer) hoặc nhân
              bản (Clone) về danh sách riêng.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleInvite} className="mt-4">
          <label className="block text-xs font-semibold text-ink-700">
            Mời theo Email hoặc Username
          </label>
          <div className="mt-1.5 flex gap-2">
            <div className="relative flex-1">
              <Mail className="pointer-events-none absolute top-2.5 left-3 h-4 w-4 text-ink-400" />
              <input
                type="text"
                value={recipient}
                onChange={(e) => setRecipient(e.target.value)}
                placeholder="nhap-email@example.com hoặc username"
                className="w-full rounded-xl border border-ink-200 py-2 pr-3 pl-9 text-sm text-ink-900 focus:border-brand-500 focus:outline-hidden"
              />
            </div>
            <button
              type="submit"
              disabled={loading || !recipient.trim()}
              className="rounded-xl bg-brand-600 px-4 py-2 text-xs font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
            >
              Chia sẻ
            </button>
          </div>
          {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
        </form>

        {shares.length > 0 && (
          <div className="mt-4 space-y-2">
            <div className="text-xs font-semibold text-ink-700">
              Đã chia sẻ với ({shares.length})
            </div>
            <div className="max-h-36 space-y-1.5 overflow-y-auto rounded-xl border border-ink-100 bg-ink-50/60 p-2.5">
              {shares.map((s) => (
                <div
                  key={s.id}
                  className="flex items-center justify-between text-xs"
                >
                  <div>
                    <span className="font-semibold text-ink-800">
                      {s.sharedWithName || s.sharedWithEmail}
                    </span>
                    <span className="ml-1.5 text-ink-500">
                      ({s.sharedWithEmail})
                    </span>
                  </div>
                  <span className="inline-flex items-center gap-1 rounded-md bg-white px-2 py-0.5 text-[11px] font-medium text-ink-600">
                    <Shield className="h-3 w-3 text-brand-600" /> {s.permission}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="mt-5 border-t border-ink-100 pt-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Link2 className="h-4 w-4 text-brand-600" />
              <span className="text-xs font-semibold text-ink-800">
                Chia sẻ bằng liên kết bí mật (Share Link)
              </span>
            </div>
            {shareToken ? (
              <button
                type="button"
                onClick={() => handleToggleLink(false)}
                className="text-xs font-semibold text-red-600 hover:underline"
              >
                Tắt liên kết
              </button>
            ) : (
              <button
                type="button"
                onClick={() => handleToggleLink(true)}
                className="rounded-lg bg-ink-100 px-3 py-1 text-xs font-semibold text-ink-800 hover:bg-ink-200"
              >
                Bật liên kết chia sẻ
              </button>
            )}
          </div>

          {shareToken && (
            <div className="mt-2.5 flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={shareUrl}
                className="flex-1 rounded-xl border border-ink-200 bg-ink-50 px-3 py-1.5 text-xs text-ink-700"
              />
              <button
                type="button"
                onClick={handleCopy}
                className="inline-flex items-center gap-1 rounded-xl border border-ink-200 bg-white px-3 py-1.5 text-xs font-semibold text-ink-700 hover:bg-ink-50"
              >
                {copied ? (
                  <>
                    <Check className="h-3.5 w-3.5 text-emerald-600" /> Đã chép
                  </>
                ) : (
                  <>
                    <Copy className="h-3.5 w-3.5" /> Sao chép
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
