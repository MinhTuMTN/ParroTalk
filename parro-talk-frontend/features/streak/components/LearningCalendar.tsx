"use client";

import { ChevronDown } from "lucide-react";
import type { CalendarDay } from "@/features/streak/types/streak";

type LearningCalendarProps = {
  days: CalendarDay[];
  selectedYear: number;
  availableYears: number[];
  onYearChange: (year: number) => void;
};

type CalendarCell =
  | {
      date: string;
      studied: boolean;
      isFuture: boolean;
    }
  | null;

const toLocalDate = (value: string) => new Date(`${value}T00:00:00`);

const getMonday = (date: Date) => {
  const copy = new Date(date);
  const day = copy.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  copy.setDate(copy.getDate() + diff);
  return copy;
};

const getSunday = (date: Date) => {
  const copy = getMonday(date);
  copy.setDate(copy.getDate() + 6);
  return copy;
};

const toIsoDate = (date: Date) =>
  [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");

const formatTooltip = (day: Exclude<CalendarCell, null>) => {
  const status = day.isFuture ? "Future day" : day.studied ? "Studied" : "Not studied";
  return `${status} on ${toLocalDate(day.date).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  })}`;
};

export default function LearningCalendar({
  days,
  selectedYear,
  availableYears,
  onYearChange,
}: LearningCalendarProps) {
  const firstDate = new Date(selectedYear, 0, 1);
  const lastDate = new Date(selectedYear, 11, 31);
  const rangeStart = getMonday(firstDate);
  const rangeEnd = getSunday(lastDate);
  const dayMap = new Map(days.map((day) => [day.date, day]));
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const weeks: CalendarCell[][] = [];
  const monthLabels: { key: string; label: string; index: number }[] = [];
  const cursor = new Date(rangeStart);
  let weekIndex = 0;

  while (cursor <= rangeEnd) {
    const week: CalendarCell[] = [];

    for (let dayIndex = 0; dayIndex < 7; dayIndex++) {
      const iso = toIsoDate(cursor);
      const insideRange = cursor >= firstDate && cursor <= lastDate;
      const calendarDay = dayMap.get(iso);
      week.push(
        insideRange
          ? {
              date: iso,
              studied: calendarDay?.studied ?? false,
              isFuture: cursor > today,
            }
          : null,
      );

      const monthKey = `${cursor.getFullYear()}-${cursor.getMonth()}`;
      const isFirstVisibleDayOfMonth =
        insideRange &&
        cursor.getDate() <= 7 &&
        !monthLabels.some((month) => month.key === monthKey);

      if (isFirstVisibleDayOfMonth) {
        monthLabels.push({
          key: monthKey,
          label: cursor.toLocaleDateString("en-US", { month: "short" }),
          index: weekIndex,
        });
      }

      cursor.setDate(cursor.getDate() + 1);
    }

    weeks.push(week);
    weekIndex++;
  }

  if (!monthLabels.some((month) => month.index === 0)) {
    monthLabels.unshift({
      key: `${firstDate.getFullYear()}-${firstDate.getMonth()}`,
      label: firstDate.toLocaleDateString("en-US", { month: "short" }),
      index: 0,
    });
  }

  return (
    <section className="min-w-0 rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-base font-bold text-slate-900">Learning Calendar</h2>
        <label className="relative block">
          <span className="sr-only">Select year</span>
          <select
            value={selectedYear}
            onChange={(event) => onYearChange(Number(event.target.value))}
            className="appearance-none rounded-xl border border-gray-200 bg-white py-2 pl-3 pr-9 text-sm font-semibold text-slate-700 outline-none transition focus:border-green-400 focus:ring-2 focus:ring-green-100"
          >
            {availableYears.map((year) => (
              <option key={year} value={year}>
                {year}
              </option>
            ))}
          </select>
          <ChevronDown
            size={16}
            className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"
          />
        </label>
      </div>

      <div className="mt-6 overflow-x-auto pb-2">
        <div className="min-w-max">
          <div className="mb-2 flex h-5 items-start">
            {weeks.map((_, index) => {
              const month = monthLabels.find((label) => label.index === index);
              return (
                <div key={index} className="w-4 text-xs font-medium text-slate-500">
                  {month?.label}
                </div>
              );
            })}
          </div>

          <div className="flex gap-1">
            {weeks.map((week, index) => (
              <div key={index} className="grid grid-rows-7 gap-1">
                {week.map((day, dayIndex) =>
                  day ? (
                    <div
                      key={day.date}
                      title={formatTooltip(day)}
                      className={`h-3.5 w-3.5 rounded-[4px] ${
                        day.isFuture ? "bg-slate-200" : day.studied ? "bg-green-500" : "bg-gray-100"
                      }`}
                    />
                  ) : (
                    <div key={`blank-${index}-${dayIndex}`} className="h-3.5 w-3.5" />
                  ),
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-5 text-xs font-medium text-slate-600">
        <span className="inline-flex items-center gap-2">
          <span className="h-3.5 w-3.5 rounded-[4px] bg-gray-100" />
          Not studied
        </span>
        <span className="inline-flex items-center gap-2">
          <span className="h-3.5 w-3.5 rounded-[4px] bg-green-500" />
          Studied
        </span>
        <span className="inline-flex items-center gap-2">
          <span className="h-3.5 w-3.5 rounded-[4px] bg-slate-200" />
          Future
        </span>
      </div>
    </section>
  );
}
