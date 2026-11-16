"use client";

import { useEffect, useState } from "react";
import { Check, Sparkles, Volume2, X } from "lucide-react";
import type { VocabularyListItemResponse } from "../../types/customList";
import {
  formatDataSourceBadge,
  getDefaultSelectedSenseIds,
} from "./customListUtils";

export function SenseSelectionDrawer({
  item,
  isOwner,
  onClose,
  onSaveSelection,
  onGenerateMoreForSense,
}: {
  item: VocabularyListItemResponse | null;
  isOwner: boolean;
  onClose: () => void;
  onSaveSelection: (
    itemId: string,
    payload: {
      selectedSenseIds: string[];
      selectedIdiomIds: string[];
      selectedPhrasalVerbIds: string[];
    }
  ) => Promise<void>;
  onGenerateMoreForSense?: (senseId: string) => Promise<void>;
}) {
  const [selectedSenseIds, setSelectedSenseIds] = useState<string[]>(
    item?.selectedSenseIds ?? []
  );
  const [selectedIdiomIds, setSelectedIdiomIds] = useState<string[]>(
    item?.selectedIdiomIds ?? []
  );
  const [selectedPhrasalVerbIds, setSelectedPhrasalVerbIds] = useState<
    string[]
  >(item?.selectedPhrasalVerbIds ?? []);
  const [saving, setSaving] = useState(false);
  const [generatingSenseId, setGeneratingSenseId] = useState<string | null>(
    null
  );

  useEffect(() => {
    setSelectedSenseIds(item?.selectedSenseIds ?? []);
    setSelectedIdiomIds(item?.selectedIdiomIds ?? []);
    setSelectedPhrasalVerbIds(item?.selectedPhrasalVerbIds ?? []);
  }, [item?.id]);

  if (!item) return null;

  const toggleId = (
    id: string,
    list: string[],
    setter: (next: string[]) => void
  ) => {
    if (!isOwner) return;
    if (list.includes(id)) {
      setter(list.filter((x) => x !== id));
    } else {
      setter([...list, id]);
    }
  };

  const handleSelectDefaultCefr = () => {
    if (!isOwner) return;
    setSelectedSenseIds(getDefaultSelectedSenseIds(item.availableSenses));
  };

  const handleSelectAllSenses = () => {
    if (!isOwner) return;
    setSelectedSenseIds(item.availableSenses.map((s) => s.id));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await onSaveSelection(item.id, {
        selectedSenseIds,
        selectedIdiomIds,
        selectedPhrasalVerbIds,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  };

  const playAudio = (url?: string) => {
    if (!url) return;
    const audio = new Audio(url);
    audio.play().catch(() => {});
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-ink-900/40 backdrop-blur-xs">
      <div className="flex h-full w-full max-w-2xl flex-col bg-white shadow-2xl">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-ink-100 px-6 py-4">
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-xl font-extrabold text-ink-900">
                {item.word || item.rawInputWord}
              </h2>
              {item.dataSource && (
                <span className="rounded-full bg-ocean-50 px-2.5 py-0.5 text-[11px] font-semibold text-ocean-700 border border-ocean-200">
                  Nguồn: {formatDataSourceBadge(item.dataSource)}
                </span>
              )}
            </div>
            <div className="mt-1 flex items-center gap-4 text-xs text-ink-500">
              {item.phoneticUk && (
                <button
                  type="button"
                  onClick={() => playAudio(item.audioUkUrl)}
                  className="inline-flex items-center gap-1 hover:text-brand-600"
                >
                  <Volume2 className="h-3.5 w-3.5" /> UK {item.phoneticUk}
                </button>
              )}
              {item.phoneticUs && (
                <button
                  type="button"
                  onClick={() => playAudio(item.audioUsUrl)}
                  className="inline-flex items-center gap-1 hover:text-brand-600"
                >
                  <Volume2 className="h-3.5 w-3.5" /> US {item.phoneticUs}
                </button>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 space-y-6 overflow-y-auto px-6 py-5">
          {/* Senses Section */}
          <div>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h3 className="text-sm font-bold text-ink-900">
                  Chọn nét nghĩa (Senses) cần học ({selectedSenseIds.length}/
                  {item.availableSenses.length})
                </h3>
                <p className="text-xs text-ink-500">
                  Mặc định chọn các nghĩa phổ biến (A1–B2). Bạn có thể bật thêm
                  nghĩa C1–C2.
                </p>
              </div>
              {isOwner && (
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={handleSelectDefaultCefr}
                    className="rounded-lg border border-ink-200 px-2.5 py-1 text-xs font-semibold text-ink-700 hover:bg-ink-50"
                  >
                    Chỉ chọn A1–B2
                  </button>
                  <button
                    type="button"
                    onClick={handleSelectAllSenses}
                    className="rounded-lg border border-ink-200 px-2.5 py-1 text-xs font-semibold text-ink-700 hover:bg-ink-50"
                  >
                    Chọn tất cả
                  </button>
                </div>
              )}
            </div>

            <div className="mt-3 space-y-3">
              {item.availableSenses.map((sense) => {
                const checked = selectedSenseIds.includes(sense.id);
                return (
                  <div
                    key={sense.id}
                    onClick={() =>
                      toggleId(sense.id, selectedSenseIds, setSelectedSenseIds)
                    }
                    className={`cursor-pointer rounded-xl border p-4 transition-all ${
                      checked
                        ? "border-brand-500 bg-brand-50/40"
                        : "border-ink-200 bg-white hover:border-ink-300"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-2">
                        <span
                          className={`flex h-5 w-5 items-center justify-center rounded-md border text-white ${
                            checked
                              ? "border-brand-600 bg-brand-600"
                              : "border-ink-300 bg-white"
                          }`}
                        >
                          {checked && <Check className="h-3.5 w-3.5" />}
                        </span>
                        {sense.cefrLevel && (
                          <span className="rounded-md bg-brand-100 px-2 py-0.5 text-xs font-bold text-brand-700">
                            {sense.cefrLevel}
                          </span>
                        )}
                        {sense.partOfSpeech && (
                          <span className="text-xs font-semibold italic text-ink-600">
                            {sense.partOfSpeech}
                          </span>
                        )}
                        <span className="text-[11px] text-ink-400">
                          • {sense.questionCount ?? 0} câu hỏi trong kho
                        </span>
                      </div>

                      {isOwner && onGenerateMoreForSense && (
                        <button
                          type="button"
                          disabled={generatingSenseId === sense.id}
                          onClick={async (e) => {
                            e.stopPropagation();
                            setGeneratingSenseId(sense.id);
                            try {
                              await onGenerateMoreForSense(sense.id);
                            } finally {
                              setGeneratingSenseId(null);
                            }
                          }}
                          className="inline-flex items-center gap-1 rounded-lg border border-brand-200 bg-white px-2.5 py-1 text-[11px] font-semibold text-brand-700 hover:bg-brand-50 disabled:opacity-50"
                        >
                          <Sparkles className="h-3 w-3" />
                          {generatingSenseId === sense.id
                            ? "Đang tạo..."
                            : "+3 câu hỏi AI"}
                        </button>
                      )}
                    </div>

                    <p className="mt-2 text-sm font-semibold text-ink-900">
                      {sense.definitionEn}
                    </p>
                    {sense.definitionVn && (
                      <p className="mt-0.5 text-xs font-medium text-brand-700">
                        → {sense.definitionVn}
                      </p>
                    )}

                    {(sense.exampleEn || (sense.examplesEn && sense.examplesEn.length > 0)) && (
                      <div className="mt-2 border-l-2 border-ink-200 pl-2.5 text-xs italic text-ink-600">
                        <p>&ldquo;{sense.exampleEn || sense.examplesEn?.[0]}&rdquo;</p>
                        {sense.exampleVn && (
                          <p className="mt-0.5 not-italic text-ink-500">{sense.exampleVn}</p>
                        )}
                      </div>
                    )}

                    <div className="mt-2.5 flex flex-wrap gap-1.5">
                      {sense.synonyms?.slice(0, 5).map((syn) => (
                        <span
                          key={syn}
                          className="rounded-md bg-emerald-50 px-2 py-0.5 text-[11px] text-emerald-700"
                        >
                          ≈ {syn}
                        </span>
                      ))}
                      {sense.antonyms?.slice(0, 4).map((ant) => (
                        <span
                          key={ant}
                          className="rounded-md bg-coral-50 px-2 py-0.5 text-[11px] text-coral-700"
                        >
                          ≠ {ant}
                        </span>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Idioms Section */}
          {item.availableIdioms.length > 0 && (
            <div>
              <h3 className="text-sm font-bold text-ink-900">
                Idioms ({selectedIdiomIds.length}/{item.availableIdioms.length})
              </h3>
              <div className="mt-2 space-y-2">
                {item.availableIdioms.map((idm) => {
                  const checked = selectedIdiomIds.includes(idm.id);
                  return (
                    <div
                      key={idm.id}
                      onClick={() =>
                        toggleId(
                          idm.id,
                          selectedIdiomIds,
                          setSelectedIdiomIds
                        )
                      }
                      className={`cursor-pointer rounded-xl border p-3 ${
                        checked
                          ? "border-brand-500 bg-brand-50/40"
                          : "border-ink-200 bg-white"
                      }`}
                    >
                      <div className="font-semibold text-xs text-ink-900">
                        {idm.phrase}
                      </div>
                      <div className="text-xs text-ink-600">
                        {idm.definitionEn || idm.meaningEn}
                      </div>
                      {(idm.definitionVn || idm.meaningVn) && (
                        <div className="text-xs text-brand-700">
                          → {idm.definitionVn || idm.meaningVn}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Phrasal Verbs Section */}
          {item.availablePhrasalVerbs.length > 0 && (
            <div>
              <h3 className="text-sm font-bold text-ink-900">
                Phrasal Verbs ({selectedPhrasalVerbIds.length}/
                {item.availablePhrasalVerbs.length})
              </h3>
              <div className="mt-2 space-y-2">
                {item.availablePhrasalVerbs.map((pv) => {
                  const checked = selectedPhrasalVerbIds.includes(pv.id);
                  return (
                    <div
                      key={pv.id}
                      onClick={() =>
                        toggleId(
                          pv.id,
                          selectedPhrasalVerbIds,
                          setSelectedPhrasalVerbIds
                        )
                      }
                      className={`cursor-pointer rounded-xl border p-3 ${
                        checked
                          ? "border-brand-500 bg-brand-50/40"
                          : "border-ink-200 bg-white"
                      }`}
                    >
                      <div className="font-semibold text-xs text-ink-900">
                        {pv.phrase}
                      </div>
                      <div className="text-xs text-ink-600">
                        {pv.definitionEn || pv.meaningEn}
                      </div>
                      {(pv.definitionVn || pv.meaningVn) && (
                        <div className="text-xs text-brand-700">
                          → {pv.definitionVn || pv.meaningVn}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 border-t border-ink-100 px-6 py-4">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl px-4 py-2 text-xs font-semibold text-ink-600 hover:bg-ink-100"
          >
            Đóng
          </button>
          {isOwner && (
            <button
              type="button"
              disabled={saving || selectedSenseIds.length === 0}
              onClick={handleSave}
              className="rounded-xl bg-brand-600 px-5 py-2 text-xs font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
            >
              {saving ? "Đang lưu..." : "Lưu lựa chọn nét nghĩa"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
