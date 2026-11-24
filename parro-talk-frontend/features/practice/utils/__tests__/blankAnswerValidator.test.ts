// @vitest-environment node
import { describe, expect, it } from "vitest";
import {
  isBlankAnswerCorrect,
  normalizeBlankInput,
  validateSegmentBlanks,
} from "../blankAnswerValidator";

describe("blankAnswerValidator", () => {
  describe("normalizeBlankInput", () => {
    it("handles null, undefined, empty, and whitespace-only inputs", () => {
      expect(normalizeBlankInput(null)).toBe("");
      expect(normalizeBlankInput(undefined)).toBe("");
      expect(normalizeBlankInput("")).toBe("");
      expect(normalizeBlankInput("   \t\n  ")).toBe("");
    });

    it("trims, lowercases, normalizes curly apostrophes, collapses whitespace, and strips outer punctuation", () => {
      expect(normalizeBlankInput("  DON\u2019T! ")).toBe("don't");
      expect(normalizeBlankInput("\u2018Hello\u2019")).toBe("'hello'");
      expect(normalizeBlankInput(' "Environment." ')).toBe("environment");
      expect(normalizeBlankInput("  \"  hello   world  \"  ")).toBe("hello world");
      expect(normalizeBlankInput("---co-worker---")).toBe("co-worker");
      expect(normalizeBlankInput("!!!...")).toBe("");
    });
  });

  describe("isBlankAnswerCorrect", () => {
    it("returns false for empty, whitespace, punctuation-only, or empty expected inputs", () => {
      expect(isBlankAnswerCorrect("", "hello")).toBe(false);
      expect(isBlankAnswerCorrect("   ", "hello")).toBe(false);
      expect(isBlankAnswerCorrect(null, "hello")).toBe(false);
      expect(isBlankAnswerCorrect(undefined, "hello")).toBe(false);
      expect(isBlankAnswerCorrect("...", "hello")).toBe(false);
      expect(isBlankAnswerCorrect("hello", "")).toBe(false);
      expect(isBlankAnswerCorrect("hello", "   ")).toBe(false);
      expect(isBlankAnswerCorrect("hello", "!!!")).toBe(false);
    });

    it("accepts case, whitespace, curly apostrophe, and outer punctuation variants", () => {
      expect(isBlankAnswerCorrect("  DON\u2019T! ", "don't")).toBe(true);
      expect(isBlankAnswerCorrect(' "Environment." ', "environment")).toBe(true);
      expect(isBlankAnswerCorrect("I'm", "I\u2019m")).toBe(true);
    });

    it("accepts all 13 unambiguous contractions without apostrophe", () => {
      const unambiguousPairs: Array<[string, string]> = [
        ["dont", "don't"],
        ["doesnt", "doesn't"],
        ["didnt", "didn't"],
        ["isnt", "isn't"],
        ["arent", "aren't"],
        ["wasnt", "wasn't"],
        ["werent", "weren't"],
        ["havent", "haven't"],
        ["hasnt", "hasn't"],
        ["hadnt", "hadn't"],
        ["wouldnt", "wouldn't"],
        ["couldnt", "couldn't"],
        ["shouldnt", "shouldn't"],
      ];

      for (const [stripped, expected] of unambiguousPairs) {
        expect(isBlankAnswerCorrect(stripped, expected)).toBe(true);
        expect(isBlankAnswerCorrect(expected, expected)).toBe(true);
      }
    });

    it("rejects ambiguous contractions when apostrophe is omitted", () => {
      const ambiguousPairs: Array<[string, string]> = [
        ["its", "it's"],
        ["were", "we're"],
        ["well", "we'll"],
        ["hell", "he'll"],
        ["ill", "I'll"],
        ["shell", "she'll"],
        ["shed", "she'd"],
        ["wed", "we'd"],
      ];

      for (const [stripped, expected] of ambiguousPairs) {
        expect(isBlankAnswerCorrect(stripped, expected)).toBe(false);
        expect(isBlankAnswerCorrect(expected, stripped)).toBe(false);
      }
    });

    it("accepts hyphen variants only when at least one side contains a hyphen", () => {
      expect(isBlankAnswerCorrect("coworker", "co-worker")).toBe(true);
      expect(isBlankAnswerCorrect("co worker", "co-worker")).toBe(true);
      expect(isBlankAnswerCorrect("co-worker", "co-worker")).toBe(true);
      expect(isBlankAnswerCorrect("co-worker", "co worker")).toBe(true);
      expect(isBlankAnswerCorrect("state of the art", "state-of-the-art")).toBe(true);

      expect(isBlankAnswerCorrect("to gether", "together")).toBe(false);
    });

    it("supports number-word equivalence for 0-99, 100, and 1000", () => {
      expect(isBlankAnswerCorrect("zero", "0")).toBe(true);
      expect(isBlankAnswerCorrect("0", "zero")).toBe(true);
      expect(isBlankAnswerCorrect("fifteen", "15")).toBe(true);
      expect(isBlankAnswerCorrect("15", "fifteen")).toBe(true);
      expect(isBlankAnswerCorrect("forty", "40")).toBe(true);
      expect(isBlankAnswerCorrect("twenty one", "21")).toBe(true);
      expect(isBlankAnswerCorrect("twenty-one", "21")).toBe(true);
      expect(isBlankAnswerCorrect("21", "twenty-one")).toBe(true);
      expect(isBlankAnswerCorrect("21", "twenty one")).toBe(true);
      expect(isBlankAnswerCorrect("ninety nine", "99")).toBe(true);
      expect(isBlankAnswerCorrect("100", "hundred")).toBe(true);
      expect(isBlankAnswerCorrect("hundred", "100")).toBe(true);
      expect(isBlankAnswerCorrect("1000", "thousand")).toBe(true);
      expect(isBlankAnswerCorrect("thousand", "1000")).toBe(true);

      expect(isBlankAnswerCorrect("fifteen", "16")).toBe(false);
      expect(isBlankAnswerCorrect("101", "one hundred one")).toBe(false);
    });

    it("matches optional acceptableAnswers and applies normalization/variants to them", () => {
      expect(isBlankAnswerCorrect("colour", "color", ["colour"])).toBe(true);
      expect(isBlankAnswerCorrect("  COLOUR! ", "color", ["colour"])).toBe(true);
      expect(isBlankAnswerCorrect("twenty five", "quarter", ["25"])).toBe(true);
      expect(isBlankAnswerCorrect("hue", "color", ["colour"])).toBe(false);
      expect(isBlankAnswerCorrect("hue", "color", [null as unknown as string, "  "])).toBe(false);
      expect(isBlankAnswerCorrect("colour", "color", null)).toBe(false);
    });
  });

  describe("validateSegmentBlanks", () => {
    it("returns allCorrect false and empty results when blanks is empty", () => {
      const result = validateSegmentBlanks([], new Map([[0, "hello"]]), { 0: "hello" });
      expect(result).toEqual({ allCorrect: false, results: {} });
    });

    it("validates each blank independently by wordIndex", () => {
      // Sentence: "The team checked the report before the meeting."
      const wordTextByIndex = new Map<number, string>([
        [0, "The"],
        [1, "team"],
        [2, "checked"],
        [3, "the"],
        [4, "report"],
        [5, "before"],
        [6, "the"],
        [7, "meeting"],
      ]);

      const blanks = [
        { wordIndex: 2 },
        { wordIndex: 3 },
        { wordIndex: 6 },
        { wordIndex: 7, acceptableAnswers: ["gathering"] },
      ];

      const partial = validateSegmentBlanks(blanks, wordTextByIndex, {
        2: "checked",
        3: "the",
        6: "a",
        7: "WRONG",
      });

      expect(partial).toEqual({
        allCorrect: false,
        results: {
          2: true,
          3: true,
          6: false,
          7: false,
        },
      });

      const allPassed = validateSegmentBlanks(blanks, wordTextByIndex, {
        2: "checked",
        3: "THE",
        6: "the",
        7: "gathering",
      });

      expect(allPassed).toEqual({
        allCorrect: true,
        results: {
          2: true,
          3: true,
          6: true,
          7: true,
        },
      });
    });

    it("handles missing userValues and missing wordTextByIndex entries safely", () => {
      const wordTextByIndex = new Map<number, string>([[0, "hello"]]);
      const result = validateSegmentBlanks(
        [{ wordIndex: 0 }, { wordIndex: 1 }],
        wordTextByIndex,
        {}
      );

      expect(result).toEqual({
        allCorrect: false,
        results: {
          0: false,
          1: false,
        },
      });
    });
  });
});
