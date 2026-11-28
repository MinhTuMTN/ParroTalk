const OUTER_PUNCTUATION = /^[^a-zA-Z0-9']+|[^a-zA-Z0-9']+$/g;
const MULTI_WHITESPACE = /\s+/g;

/**
 * Contractions whose un-apostrophed form does not collide with another distinct English word.
 * Ambiguous contractions (it's/its, we're/were, we'll/well, he'll/hell, I'll/ill,
 * she'll/shell, she'd/shed, we'd/wed) are intentionally excluded.
 */
const UNAMBIGUOUS_CONTRACTIONS = new Set<string>([
  "don't",
  "doesn't",
  "didn't",
  "isn't",
  "aren't",
  "wasn't",
  "weren't",
  "haven't",
  "hasn't",
  "hadn't",
  "wouldn't",
  "couldn't",
  "shouldn't",
]);

const UNAMBIGUOUS_STRIPPED_TO_CANONICAL = new Map<string, string>(
  Array.from(UNAMBIGUOUS_CONTRACTIONS, (contraction) => [
    contraction.replace(/'/g, ""),
    contraction,
  ])
);

const NUMBER_WORDS = new Map<string, string>([
  ["0", "zero"],
  ["1", "one"],
  ["2", "two"],
  ["3", "three"],
  ["4", "four"],
  ["5", "five"],
  ["6", "six"],
  ["7", "seven"],
  ["8", "eight"],
  ["9", "nine"],
  ["10", "ten"],
  ["11", "eleven"],
  ["12", "twelve"],
  ["13", "thirteen"],
  ["14", "fourteen"],
  ["15", "fifteen"],
  ["16", "sixteen"],
  ["17", "seventeen"],
  ["18", "eighteen"],
  ["19", "nineteen"],
  ["20", "twenty"],
  ["30", "thirty"],
  ["40", "forty"],
  ["50", "fifty"],
  ["60", "sixty"],
  ["70", "seventy"],
  ["80", "eighty"],
  ["90", "ninety"],
  ["100", "hundred"],
  ["1000", "thousand"],
]);

const NUMBER_WORD_VALUES = new Set<string>(NUMBER_WORDS.values());
const WORD_TO_NUMBER = new Map<string, string>(
  Array.from(NUMBER_WORDS, ([digit, word]) => [word, digit])
);

const VALID_TENS = new Set<string>([
  "twenty",
  "thirty",
  "forty",
  "fifty",
  "sixty",
  "seventy",
  "eighty",
  "ninety",
]);

const VALID_ONES = new Set<string>([
  "one",
  "two",
  "three",
  "four",
  "five",
  "six",
  "seven",
  "eight",
  "nine",
]);

export type BlankLiveState = "empty" | "typing" | "wrong" | "correct";

export interface BlankValidationSlot {
  wordIndex: number;
  acceptableAnswers?: string[] | null;
}

export interface SegmentBlanksValidationResult {
  allCorrect: boolean;
  results: Record<number, boolean>;
}

/**
 * Normalizes a learner or expected blank string: curly-to-straight apostrophes,
 * trim, lowercase, whitespace collapse, and outer punctuation stripping.
 */
export function normalizeBlankInput(input: string | null | undefined): string {
  if (input == null) {
    return "";
  }
  const normalized = input
    .replace(/\u2019/g, "'")
    .replace(/\u2018/g, "'")
    .trim()
    .toLowerCase()
    .replace(MULTI_WHITESPACE, " ")
    .replace(OUTER_PUNCTUATION, "");
  return normalized.trim();
}

function matchesUnambiguousContraction(
  userNorm: string,
  expectedNorm: string
): boolean {
  if (
    UNAMBIGUOUS_CONTRACTIONS.has(expectedNorm) &&
    userNorm === expectedNorm.replace(/'/g, "")
  ) {
    return true;
  }
  const canonicalFromUser = UNAMBIGUOUS_STRIPPED_TO_CANONICAL.get(userNorm);
  return canonicalFromUser !== undefined && canonicalFromUser === expectedNorm;
}

function expandHyphenVariants(value: string): Set<string> {
  return new Set<string>([
    value,
    value.replace(/-/g, ""),
    value.replace(/-/g, " ").replace(MULTI_WHITESPACE, " ").trim(),
    value.replace(/ /g, "-"),
    value.replace(/ /g, ""),
  ]);
}

function matchesHyphenVariant(userNorm: string, expectedNorm: string): boolean {
  if (!expectedNorm.includes("-") && !userNorm.includes("-")) {
    return false;
  }
  const expectedVariants = expandHyphenVariants(expectedNorm);
  const userVariants = expandHyphenVariants(userNorm);
  for (const variant of userVariants) {
    if (expectedVariants.has(variant)) {
      return true;
    }
  }
  return false;
}

function parseSimpleNumeric(value: string): number | null {
  if (!/^\d{1,3}$/.test(value)) {
    return null;
  }
  const parsed = Number.parseInt(value, 10);
  return Number.isNaN(parsed) ? null : parsed;
}

function numberToWords(n: number): string | null {
  if (n < 0 || n > 99) {
    return NUMBER_WORDS.get(String(n)) ?? null;
  }
  if (n <= 20 || n % 10 === 0) {
    return NUMBER_WORDS.get(String(n)) ?? null;
  }
  const tens = Math.floor(n / 10) * 10;
  const ones = n % 10;
  const tensWord = NUMBER_WORDS.get(String(tens));
  const onesWord = NUMBER_WORDS.get(String(ones));
  if (tensWord === undefined || onesWord === undefined) {
    return null;
  }
  return `${tensWord}-${onesWord}`;
}

function parseCompoundWordNumber(value: string): string | null {
  const parts = value.split("-");
  if (parts.length !== 2) {
    return null;
  }
  const [tens, ones] = parts;
  return VALID_TENS.has(tens) && VALID_ONES.has(ones)
    ? `${tens}-${ones}`
    : null;
}

function isCompoundNumberWord(value: string): boolean {
  return parseCompoundWordNumber(value) !== null;
}

function toCanonicalNumber(value: string): string | null {
  const directWord = NUMBER_WORDS.get(value);
  if (directWord !== undefined) {
    return directWord;
  }
  if (NUMBER_WORD_VALUES.has(value)) {
    return value;
  }
  const hyphenNormalized = value.replace(/ /g, "-");
  if (isCompoundNumberWord(hyphenNormalized)) {
    return hyphenNormalized;
  }
  const parsed = parseSimpleNumeric(value);
  if (parsed !== null) {
    const word = numberToWords(parsed);
    if (word !== null) {
      return word;
    }
  }
  return parseCompoundWordNumber(hyphenNormalized);
}

function matchesNumberEquivalent(
  userNorm: string,
  expectedNorm: string
): boolean {
  const userCanonical = toCanonicalNumber(userNorm);
  const expectedCanonical = toCanonicalNumber(expectedNorm);
  return userCanonical !== null && userCanonical === expectedCanonical;
}

function matchesCandidate(userNorm: string, expectedNorm: string): boolean {
  if (expectedNorm.length === 0) {
    return false;
  }
  if (userNorm === expectedNorm) {
    return true;
  }
  if (matchesUnambiguousContraction(userNorm, expectedNorm)) {
    return true;
  }
  if (matchesHyphenVariant(userNorm, expectedNorm)) {
    return true;
  }
  return matchesNumberEquivalent(userNorm, expectedNorm);
}

function canonicalNumberToDigits(canonical: string): string | null {
  const direct = WORD_TO_NUMBER.get(canonical);
  if (direct !== undefined) {
    return direct;
  }
  const parts = canonical.split("-");
  if (parts.length !== 2) {
    return null;
  }
  const tensDigit = WORD_TO_NUMBER.get(parts[0]);
  const onesDigit = WORD_TO_NUMBER.get(parts[1]);
  if (tensDigit === undefined || onesDigit === undefined) {
    return null;
  }
  const sum = Number.parseInt(tensDigit, 10) + Number.parseInt(onesDigit, 10);
  return Number.isNaN(sum) ? null : String(sum);
}

function addLenientForm(candidates: Set<string>, form: string): void {
  if (form.length === 0) {
    return;
  }
  candidates.add(form);
  const noApos = form.replace(/'/g, "");
  if (noApos.length > 0) {
    candidates.add(noApos);
  }
  for (const base of [form, noApos]) {
    if (base.includes("-")) {
      const noHyphen = base.replace(/-/g, "");
      const spaceHyphen = base.replace(/-/g, " ").replace(MULTI_WHITESPACE, " ").trim();
      if (noHyphen.length > 0) candidates.add(noHyphen);
      if (spaceHyphen.length > 0) candidates.add(spaceHyphen);
    }
    if (base.includes(" ")) {
      const hyphenated = base.replace(/ /g, "-");
      const squashed = base.replace(/ /g, "");
      if (hyphenated.length > 0) candidates.add(hyphenated);
      if (squashed.length > 0) candidates.add(squashed);
    }
  }
}

function buildLenientLiveCandidates(
  expectedWord: string,
  acceptableAnswers?: string[] | null
): Set<string> {
  const candidates = new Set<string>();
  const rawList: string[] = [];
  if (expectedWord != null && expectedWord.trim().length > 0) {
    rawList.push(expectedWord);
  }
  if (acceptableAnswers != null) {
    for (const alt of acceptableAnswers) {
      if (alt != null && alt.trim().length > 0) {
        rawList.push(alt);
      }
    }
  }

  for (const raw of rawList) {
    const norm = normalizeBlankInput(raw);
    if (norm.length === 0) {
      continue;
    }
    addLenientForm(candidates, norm);

    const canonicalNum = toCanonicalNumber(norm);
    if (canonicalNum !== null) {
      addLenientForm(candidates, canonicalNum);
      const digits = canonicalNumberToDigits(canonicalNum);
      if (digits !== null) {
        candidates.add(digits);
      }
    }
  }

  return candidates;
}

function normalizeUserForPrefix(value: string | null | undefined): string {
  if (value == null) {
    return "";
  }
  const base = value
    .replace(/\u2019/g, "'")
    .replace(/\u2018/g, "'")
    .toLowerCase()
    .replace(MULTI_WHITESPACE, " ")
    .replace(/^[^a-z0-9']+/, "");
  const strippedTrailing = base.replace(/[^a-z0-9'\s]+$/, "");
  return strippedTrailing.length > 0 ? strippedTrailing : base;
}

/**
 * Computes real-time validation state for a single blank input while typing.
 */
export function getBlankLiveState(
  value: string,
  expected: string,
  acceptableAnswers?: string[] | null
): BlankLiveState {
  const normalizedUser = normalizeUserForPrefix(value);
  if (normalizedUser.length === 0) {
    return "empty";
  }

  if (isBlankAnswerCorrect(value, expected, acceptableAnswers)) {
    return "correct";
  }

  const candidates = buildLenientLiveCandidates(expected, acceptableAnswers);
  const normalizedUserSpace = normalizedUser.replace(/-/g, " ");
  for (const candidate of candidates) {
    const candidateMatchesPrefix =
      candidate.startsWith(normalizedUser) ||
      ((candidate.includes("-") || candidate.includes(" ")) &&
        candidate.replace(/-/g, " ").startsWith(normalizedUserSpace));
    if (
      candidateMatchesPrefix &&
      normalizedUser.length < candidate.length
    ) {
      return "typing";
    }
  }

  return "wrong";
}

/**
 * Checks whether a single user blank submission matches the expected word
 * or any optional acceptable alternative.
 */
export function isBlankAnswerCorrect(
  userAnswer: string | null | undefined,
  expectedWord: string,
  acceptableAnswers?: string[] | null
): boolean {
  const normalizedUser = normalizeBlankInput(userAnswer);
  if (normalizedUser.length === 0) {
    return false;
  }

  const expectedCandidates: string[] = [];
  if (expectedWord != null && expectedWord.trim().length > 0) {
    expectedCandidates.push(expectedWord);
  }
  if (acceptableAnswers != null) {
    for (const alt of acceptableAnswers) {
      if (alt != null && alt.trim().length > 0) {
        expectedCandidates.push(alt);
      }
    }
  }

  for (const candidate of expectedCandidates) {
    if (matchesCandidate(normalizedUser, normalizeBlankInput(candidate))) {
      return true;
    }
  }
  return false;
}

/**
 * Validates all blanks for a segment independently by wordIndex.
 */
export function validateSegmentBlanks(
  blanks: BlankValidationSlot[],
  wordTextByIndex: Map<number, string>,
  userValues: Record<number, string>
): SegmentBlanksValidationResult {
  if (!blanks || blanks.length === 0) {
    return {
      allCorrect: false,
      results: {},
    };
  }

  const results: Record<number, boolean> = {};
  let correctCount = 0;

  for (const slot of blanks) {
    const expectedWord = wordTextByIndex.get(slot.wordIndex) ?? "";
    const submitted = userValues[slot.wordIndex];
    const correct = isBlankAnswerCorrect(
      submitted,
      expectedWord,
      slot.acceptableAnswers
    );
    results[slot.wordIndex] = correct;
    if (correct) {
      correctCount++;
    }
  }

  return {
    allCorrect: correctCount === blanks.length,
    results,
  };
}
