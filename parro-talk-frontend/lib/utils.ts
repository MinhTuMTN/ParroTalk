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

export function cleanWord(w: string): string {
  return w.toLowerCase().replace(/[.,\/#!$%\^&\*;:{}=\-_`~()]/g, "");
}

export interface DictationMatch {
  isMatched: boolean;
  displayString: string;
}

export function getDictationMatching(userInput: string, targetSentence: string): DictationMatch[] {
  const targetWords = targetSentence.split(" ");
  const userWords = userInput.split(" ");
  
  const result: DictationMatch[] = targetWords.map((targetWord, i) => {
    const userWord = userWords[i] || "";
    const targetClean = cleanWord(targetWord);
    const userClean = cleanWord(userWord);
    
    if (targetClean === userClean && userClean.length > 0) {
      return { isMatched: true, displayString: targetWord };
    }
    
    // Partial character matching
    let displayString = "";
    for (let j = 0; j < targetWord.length; j++) {
      const tChar = targetWord[j];
      const uChar = userWord[j] || "";
      
      if (tChar.match(/[a-zA-Z0-9]/)) {
        if (tChar.toLowerCase() === uChar.toLowerCase()) {
          displayString += tChar;
        } else {
          displayString += "*";
        }
      } else {
        displayString += tChar;
      }
    }
    
    return { isMatched: false, displayString };
  });

  return result;
}
