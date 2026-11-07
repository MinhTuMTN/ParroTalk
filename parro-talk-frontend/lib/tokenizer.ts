export interface TextToken {
  text: string;
  isWord: boolean;
}

export function tokenizeText(text: string): TextToken[] {
  // Capture words containing letters, numbers, and internal hyphens/apostrophes
  // Example: "don't", "co-worker", "issue"
  const regex = /([a-zA-Z0-9]+(?:[-'][a-zA-Z0-9]+)*)/g;
  const tokens: TextToken[] = [];
  let lastIndex = 0;
  
  let match;
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      tokens.push({ 
        text: text.substring(lastIndex, match.index), 
        isWord: false 
      });
    }
    tokens.push({ 
      text: match[0], 
      isWord: true 
    });
    lastIndex = regex.lastIndex;
  }
  
  if (lastIndex < text.length) {
    tokens.push({ 
      text: text.substring(lastIndex), 
      isWord: false 
    });
  }
  
  return tokens;
}
