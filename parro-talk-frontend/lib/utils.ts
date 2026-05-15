import { FeedbackToken } from "@/types";

export function compareSentences(userInput: string, targetText: string): FeedbackToken[] {
  const userWords = userInput.trim().toLowerCase().split(/\s+/);
  const targetWords = targetText.trim().toLowerCase().split(/\s+/);
  
  // Clean target words (remove punctuation for comparison)
  const cleanTargetWords = targetWords.map(w => w.replace(/[.,\/#!$%\^&\*;:{}=\-_`~()]/g, ""));
  const cleanUserWords = userWords.map(w => w.replace(/[.,\/#!$%\^&\*;:{}=\-_`~()]/g, ""));

  const tokens: FeedbackToken[] = [];
  
  // This is a simple word-by-word comparison. 
  // For a more robust app, a diff algorithm would be better.
  const maxLength = Math.max(cleanUserWords.length, cleanTargetWords.length);
  
  for (let i = 0; i < maxLength; i++) {
    const userWord = userWords[i] || "";
    const cleanUserWord = cleanUserWords[i] || "";
    const targetWord = targetWords[i] || "";
    const cleanTargetWord = cleanTargetWords[i] || "";

    if (cleanUserWord === cleanTargetWord) {
      tokens.push({
        text: targetWord, // Use original punctuation from target
        isCorrect: true
      });
    } else {
      tokens.push({
        text: userWord || "___",
        isCorrect: false,
        expected: targetWord
      });
    }
  }

  return tokens;
}

export function cleanWord(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, "")
    .split(/\s+/)
    .filter(s => s.length > 0)
    .join(" ");
}

export interface DictationMatch {
  isMatched: boolean;
  displayString: string;
}

export function getDictationMatching(userInput: string, targetSentence: string): DictationMatch[] {
  const targetWords = targetSentence.split(" ");
  const userWords = userInput.split(" ");
  
  return targetWords.map((targetWord, i) => {
    const userWord = userWords[i] || "";

    const targetNorm = cleanWord(targetWord);
    const userNorm = cleanWord(userWord);

    if (targetNorm === userNorm && userNorm.length > 0) {
      return { isMatched: true, displayString: targetWord };
    }

    let displayString = "";
    let uIndex = 0;

    for (let tIndex = 0; tIndex < targetWord.length; tIndex++) {
      const tChar = targetWord[tIndex];

      // Nếu là ký tự chữ/số → mới so sánh
      if (/[a-zA-Z0-9]/.test(tChar)) {
        const uChar = userWord[uIndex] || "";

        if (tChar.toLowerCase() === uChar.toLowerCase()) {
          displayString += tChar;
        } else {
          displayString += "•";
        }

        uIndex++;
      } else {
        displayString += tChar;
      }
    }

    return { isMatched: false, displayString };
  });
}

