"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Bell, CheckCheck } from "lucide-react";
import type { UserNotificationDto } from "../../types/customList";
import {
  fetchUserNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "../../services/customVocabularyListService";

export function NotificationBellDropdown({
  onRealtimeEvent,
}: {
  onRealtimeEvent?: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<UserNotificationDto[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const onRealtimeEventRef = useRef(onRealtimeEvent);
  const lastLatestIdRef = useRef<string | null>(null);

  useEffect(() => {
    onRealtimeEventRef.current = onRealtimeEvent;
  }, [onRealtimeEvent]);

  const loadNotifications = async (notifyOnNew: boolean = false) => {
    try {
      const data = await fetchUserNotifications();
      const latestId = data.notifications[0]?.id ?? null;
      if (notifyOnNew && latestId && latestId !== lastLatestIdRef.current) {
        onRealtimeEventRef.current?.();
      }
      if (latestId) {
        lastLatestIdRef.current = latestId;
      }
      setNotifications(data.notifications);
      setUnreadCount(data.unreadCount);
    } catch {
      // Ignore if unauthenticated
    }
  };

  useEffect(() => {
    loadNotifications(false);
    const interval = setInterval(() => {
      loadNotifications(true);
    }, 12000);
    return () => clearInterval(interval);
  }, []);

  const handleMarkAll = async () => {
    await markAllNotificationsRead();
    await loadNotifications();
  };

  const handleReadOne = async (id: string) => {
    await markNotificationRead(id);
    await loadNotifications();
  };

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="relative flex h-10 w-10 items-center justify-center rounded-xl border border-ink-200 bg-white text-ink-700 shadow-2xs hover:bg-ink-50"
        title="Thông báo từ vựng & câu hỏi AI"
      >
        <Bell className="h-4.5 w-4.5" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-coral-600 px-1 text-[10px] font-bold text-white">
            {unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-80 sm:w-96 rounded-2xl border border-ink-200 bg-white p-4 shadow-xl">
          <div className="flex items-center justify-between border-b border-ink-100 pb-2.5">
            <span className="text-xs font-bold text-ink-900">
              Thông báo ({unreadCount} chưa đọc)
            </span>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={handleMarkAll}
                className="inline-flex items-center gap-1 text-[11px] font-semibold text-brand-600 hover:underline"
              >
                <CheckCheck className="h-3.5 w-3.5" /> Đánh dấu đã đọc
              </button>
            )}
          </div>

          <div className="mt-2 max-h-80 space-y-2 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="py-6 text-center text-xs text-ink-400">
                Chưa có thông báo nào.
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => {
                    if (!n.isRead) handleReadOne(n.id);
                  }}
                  className={`rounded-xl border p-3 text-xs transition-colors ${
                    n.isRead
                      ? "border-ink-100 bg-white text-ink-600"
                      : "border-brand-200 bg-brand-50/50 text-ink-900"
                  }`}
                >
                  <div className="font-bold">{n.title}</div>
                  <p className="mt-0.5 text-[11px] text-ink-600">{n.message}</p>
                  {n.referenceId && (
                    <Link
                      href={`/vocabulary/lists/${n.referenceId}`}
                      onClick={() => setOpen(false)}
                      className="mt-1.5 inline-block text-[11px] font-semibold text-brand-600 hover:underline"
                    >
                      Mở danh sách →
                    </Link>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
