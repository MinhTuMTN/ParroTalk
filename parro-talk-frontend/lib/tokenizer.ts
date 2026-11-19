export interface TextToken {
  text: string;
  isWord: boolean;
}

export interface IndexedTextToken extends TextToken {
  tokenIndex: number;
  wordIndex: number;
}

export const WORD_TOKEN_REGEX = /([a-zA-Z0-9]+(?:[-'\u2019][a-zA-Z0-9]+)*)/g;

export function tokenizeWithWordIndex(text: string): IndexedTextToken[] {
  if (!text) return [];
  const regex = new RegExp(WORD_TOKEN_REGEX.source, "g");
  const tokens: IndexedTextToken[] = [];
  let lastIndex = 0;
  let tokenIndex = 0;
  let wordIndex = 0;

  let match;
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      tokens.push({
        text: text.substring(lastIndex, match.index),
        isWord: false,
        tokenIndex: tokenIndex++,
        wordIndex: -1,
      });
    }
    tokens.push({
      text: match[0],
      isWord: true,
      tokenIndex: tokenIndex++,
      wordIndex: wordIndex++,
    });
    lastIndex = regex.lastIndex;
  }

  if (lastIndex < text.length) {
    tokens.push({
      text: text.substring(lastIndex),
      isWord: false,
      tokenIndex: tokenIndex++,
      wordIndex: -1,
    });
  }

  return tokens;
}

export function tokenizeText(text: string): TextToken[] {
  // Capture words containing letters, numbers, and internal hyphens/apostrophes (straight or curly)
  // Example: "don't", "don’t", "co-worker", "issue"
  return tokenizeWithWordIndex(text).map(({ text: tokenText, isWord }) => ({
    text: tokenText,
    isWord,
  }));
}
