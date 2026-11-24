import { describe, expect, it } from "vitest";
import { tokenizeText, tokenizeWithWordIndex } from "../tokenizer";

describe("tokenizer parity with DictationTokenizationService", () => {
  it("keeps straight and curly apostrophes as single word tokens", () => {
    const tokens = tokenizeWithWordIndex("I don't think I’m ready, it’s fine.");
    const words = tokens.filter((t) => t.isWord);

    expect(words.map((w) => w.text)).toEqual([
      "I",
      "don't",
      "think",
      "I’m",
      "ready",
      "it’s",
      "fine",
    ]);
    expect(words.map((w) => w.wordIndex)).toEqual([0, 1, 2, 3, 4, 5, 6]);
    expect(tokens.filter((t) => !t.isWord).every((t) => t.wordIndex === -1)).toBe(
      true
    );
  });

  it("keeps hyphenated compounds and numbers as single word tokens", () => {
    const tokens = tokenizeWithWordIndex("My co-worker checked 2026 reports!");
    const words = tokens.filter((t) => t.isWord);

    expect(words.map((w) => w.text)).toEqual([
      "My",
      "co-worker",
      "checked",
      "2026",
      "reports",
    ]);
    expect(words.map((w) => w.wordIndex)).toEqual([0, 1, 2, 3, 4]);
    expect(tokens[tokens.length - 1]).toMatchObject({
      text: "!",
      isWord: false,
      wordIndex: -1,
    });
  });

  it("assigns distinct 0-based wordIndex values to repeated words", () => {
    const tokens = tokenizeWithWordIndex("Go, go, go!");
    const words = tokens.filter((t) => t.isWord);

    expect(words).toHaveLength(3);
    expect(words.map((w) => w.wordIndex)).toEqual([0, 1, 2]);
  });

  it("reconstructs original text verbatim when joining all tokens", () => {
    const input = '  "Well," she said, "we’ll meet at 10:30—okay?"  ';
    const tokens = tokenizeWithWordIndex(input);
    expect(tokens.map((t) => t.text).join("")).toBe(input);
    expect(tokenizeText(input).map((t) => t.text).join("")).toBe(input);
  });
});
