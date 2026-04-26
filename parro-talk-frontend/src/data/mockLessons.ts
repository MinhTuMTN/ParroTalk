import type { Lesson } from "@/src/types/lesson";

const buildSegments = (lessonId: string, lines: string[]) =>
  lines.map((text, index) => {
    const startTime = index * 18;
    return {
      id: `${lessonId}-seg-${String(index + 1).padStart(2, "0")}`,
      text,
      startTime,
      endTime: startTime + 16,
    };
  });

export const mockLessons: Lesson[] = [
  {
    id: "lsn-001",
    title: "Modern Business Spanish",
    source: "youtu.be/xJ8wQ2L1fPk",
    duration: 860,
    status: "published",
    segments: buildSegments("lsn-001", [
      "Hoy veremos como abrir una reunion con claridad y confianza.",
      "Primero definimos el objetivo y el resultado esperado.",
      "Cerramos con acciones concretas, responsables y fechas limite.",
    ]),
    createdAt: "2023-10-12T09:00:00.000Z",
  },
  {
    id: "lsn-002",
    title: "Tokyo Subway Etiquette",
    source: "subway_tips_audio.mp3",
    duration: 525,
    status: "hidden",
    segments: buildSegments("lsn-002", [
      "Stand to the left only if signs request it in that station.",
      "Let passengers exit first before stepping into the carriage.",
      "Keep phone calls silent and use earphones at low volume.",
    ]),
    createdAt: "2023-10-10T09:00:00.000Z",
  },
  {
    id: "lsn-003",
    title: "French Pronunciation Masterclass",
    source: "youtu.be/FR-0eNf9s8I",
    duration: 1335,
    status: "published",
    segments: buildSegments("lsn-003", [
      "Le son u demande une position precise de la bouche.",
      "Nasal vowels changent totalement le sens des phrases.",
      "Repetez lentement puis accelerez sans perdre la clarté.",
    ]),
    createdAt: "2023-09-28T09:00:00.000Z",
  },
  {
    id: "lsn-004",
    title: "German Grammar: Case Systems",
    source: "grammar_A1_de.pdf",
    duration: 0,
    status: "published",
    segments: buildSegments("lsn-004", [
      "Der Nominativ marks the subject in simple sentences.",
      "Use Akkusativ for direct objects and common travel phrases.",
      "Dativ appears after selected prepositions and verbs.",
    ]),
    createdAt: "2023-09-25T09:00:00.000Z",
  },
  {
    id: "lsn-005",
    title: "IELTS Listening Practice Set 4",
    source: "ielts_listening_set4.mp3",
    duration: 1152,
    status: "hidden",
    segments: buildSegments("lsn-005", [
      "Section one focuses on booking information and names.",
      "Section two requires quick mapping and directional clues.",
      "Check spelling in the final ten seconds before moving on.",
    ]),
    createdAt: "2023-09-18T09:00:00.000Z",
  },
  {
    id: "lsn-006",
    title: "Office English for Meetings",
    source: "youtu.be/eN5sU2A0asY",
    duration: 755,
    status: "published",
    segments: buildSegments("lsn-006", [
      "Let us align on priorities before discussing blockers.",
      "Could you clarify the budget impact for next quarter.",
      "Thanks everyone, I will share minutes after this call.",
    ]),
    createdAt: "2023-09-12T09:00:00.000Z",
  },
  {
    id: "lsn-007",
    title: "Travel Phrases: Airport Edition",
    source: "travel_airport_pack.mp3",
    duration: 390,
    status: "hidden",
    segments: buildSegments("lsn-007", [
      "Where is the check in counter for flight AZ four two.",
      "My luggage is delayed, can you help me file a report.",
      "Is this gate final or will there be a shuttle transfer.",
    ]),
    createdAt: "2023-09-01T09:00:00.000Z",
  },
  {
    id: "lsn-008",
    title: "Academic English: Lecture Notes",
    source: "lecture_notes_intro.wav",
    duration: 1410,
    status: "published",
    segments: buildSegments("lsn-008", [
      "Start with thesis statements that preview your structure.",
      "Signal transitions so listeners can follow each argument.",
      "Conclude by linking evidence back to your main claim.",
    ]),
    createdAt: "2023-08-28T09:00:00.000Z",
  },
];