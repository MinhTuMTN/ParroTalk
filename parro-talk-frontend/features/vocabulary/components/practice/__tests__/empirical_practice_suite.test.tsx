import React from "react";
import { renderToString } from "react-dom/server";
import {
  LearnWordsCard,
  type LearnWordData,
} from "../LearnWordsCard";
import {
  MultipleChoiceCard,
  type MultipleChoiceQuestionData,
} from "../MultipleChoiceCard";
import {
  TypeWordCard,
  normalizeWord,
  type TypeWordQuestionData,
} from "../TypeWordCard";
import {
  FillBlankCard,
  type FillBlankQuestionData,
} from "../FillBlankCard";
import {
  FALLBACK_LEARN_WORDS,
  FALLBACK_MULTIPLE_CHOICE,
  FALLBACK_TYPE_WORD,
  FALLBACK_FILL_BLANK,
} from "../../../services/vocabularyPracticeService";

// Test assertion helper
function assert(condition: boolean, message: string) {
  if (!condition) {
    throw new Error(`[ASSERTION FAILED]: ${message}`);
  }
  console.log(`  ✓ ${message}`);
}

console.log("==================================================================");
console.log("EMPIRICAL TEST SUITE: Vocabulary Topic Details Practice Suite");
console.log("==================================================================");

const sampleWord: LearnWordData = {
  id: "w-1",
  word: "mitigate",
  partOfSpeech: "verb",
  cefrLevel: "C1",
  topic: "Environment",
  phonetic: "/ˈmɪt.ɪ.ɡeɪt/",
  phoneticUs: "/ˈmɪt̬.ə.ɡeɪt/",
  phoneticUk: "/ˈmɪt.ɪ.ɡeɪt/",
  audioUsUrl: "https://audio.parrotalk.fun/us/mitigate.mp3",
  audioUkUrl: "https://audio.parrotalk.fun/uk/mitigate.mp3",
  primaryEnglishDefinition: "To make something less severe, harmful, or painful.",
  primaryVietnameseMeaning: "Làm giảm nhẹ, làm dịu bớt",
  conversationExample: {
    english: "Governments must implement long-term policies to mitigate the adverse impact of climate change.",
    vietnamese: "Các chính phủ cần thực hiện các chính sách dài hạn để giảm thiểu tác động bất lợi của biến đổi khí hậu.",
  },
  collocations: ["mitigate risk", "mitigate the effects"],
  synonyms: ["alleviate", "lessen"],
};

const htmlEnVi = renderToString(
  <LearnWordsCard
    wordData={sampleWord}
    direction="EN_VI"
    isFlipped={false}
    onRate={() => {}}
  />
);

const htmlEnViFlipped = renderToString(
  <LearnWordsCard
    wordData={sampleWord}
    direction="EN_VI"
    isFlipped={true}
    onRate={() => {}}
  />
);

const sampleMcQuestion: MultipleChoiceQuestionData = {
  id: "mc-1",
  word: "compromise",
  topic: "Business",
  question: "What is the primary meaning of 'compromise'?",
  sentence: "Both parties agreed to make a compromise to finalize the joint venture agreement.",
  partOfSpeech: "noun / verb",
  phonetic: "/ˈkɒmprəmaɪz/",
  explanation: "'Compromise' means finding a middle ground where both parties accept some terms.",
  options: [
    { key: "A", text: "To reach an agreement by mutual concession", isCorrect: true },
    { key: "B", text: "To refuse any discussion or change", isCorrect: false },
    { key: "C", text: "To demand full payment immediately", isCorrect: false },
    { key: "D", text: "To terminate a signed agreement", isCorrect: false },
  ],
};

const htmlMcUnanswered = renderToString(
  <MultipleChoiceCard
    questionData={sampleMcQuestion}
    selectedOptionKey={null}
    onSelectOption={() => {}}
  />
);

const htmlMcAnsweredCorrect = renderToString(
  <MultipleChoiceCard
    questionData={sampleMcQuestion}
    selectedOptionKey="A"
    onSelectOption={() => {}}
  />
);

const sampleTypeWordQuestion: TypeWordQuestionData = {
  id: "tw-1",
  targetWord: "negotiate",
  topic: "Business",
  partOfSpeech: "verb",
  definition: "To have formal discussions with someone in order to reach an agreement.",
  definitionVn: "Đàm phán, thương lượng",
  firstLetter: "n",
  charCount: 9,
  exampleWithMask: "The committee will ________ a new contract with the supplier.",
  exampleSentence: "The committee will negotiate a new contract with the supplier.",
  phonetic: "/nəˈɡoʊ.ʃi.eɪt/",
};

const htmlTwUnchecked = renderToString(
  <TypeWordCard
    questionData={sampleTypeWordQuestion}
    checkedState={null}
    onCheckAnswer={() => {}}
  />
);

const htmlTwCorrect = renderToString(
  <TypeWordCard
    questionData={sampleTypeWordQuestion}
    checkedState={{ isChecked: true, isCorrect: true, userAnswer: "negotiate" }}
    onCheckAnswer={() => {}}
  />
);

const sampleFillBlankQuestion: FillBlankQuestionData = {
  id: "fb-1",
  word: "mitigate",
  targetForm: "mitigate",
  topic: "Environment",
  partOfSpeech: "verb",
  cefrLevel: "C1",
  definitionVn: "Làm giảm nhẹ, làm dịu bớt mức độ nghiêm trọng",
  definitionEn: "To make something less severe, harmful, or painful.",
  sentenceWithBlank: "Governments must implement proactive policies to ______ the adverse impacts of rising sea levels.",
  completedSentence: "Governments must implement proactive policies to mitigate the adverse impacts of rising sea levels.",
  sentenceVn: "Các chính phủ phải thực hiện các chính sách chủ động để giảm thiểu những tác động bất lợi của mực nước biển dâng cao.",
  grammarContextTip: "Follows infinitive marker 'to' -> requires base form verb.",
  explanation: "'mitigate' is a formal transitive verb meaning to alleviate or reduce the severity of something.",
  source: "DATABASE",
};

const htmlFbUnchecked = renderToString(
  <FillBlankCard
    questionData={sampleFillBlankQuestion}
    checkedState={null}
    onCheckAnswer={() => {}}
  />
);

const htmlFbChecked = renderToString(
  <FillBlankCard
    questionData={sampleFillBlankQuestion}
    checkedState={{ isChecked: true, isCorrect: true, userAnswer: "mitigate" }}
    onCheckAnswer={() => {}}
  />
);

console.log("\n--- AC5: FILL BLANK MEANING CLUE & FALLBACK INTEGRITY ---");
assert(
  htmlFbUnchecked.includes("Làm giảm nhẹ, làm dịu bớt mức độ nghiêm trọng"),
  "FillBlankCard (Unchecked) displays Vietnamese meaning clue (definitionVn) before answering"
);
assert(
  htmlFbUnchecked.includes("To make something less severe, harmful, or painful."),
  "FillBlankCard (Unchecked) displays English definition clue (definitionEn) before answering"
);
assert(
  htmlFbUnchecked.includes("C1"),
  "FillBlankCard (Unchecked) displays CEFR level badge"
);
assert(
  FALLBACK_FILL_BLANK.length >= 6,
  "FALLBACK_FILL_BLANK contains at least 6 rich fallback questions across topics"
);
for (const item of FALLBACK_FILL_BLANK) {
  assert(
    Boolean(item.definitionVn && item.definitionVn.trim().length > 0),
    `FALLBACK_FILL_BLANK item '${item.word}' includes definitionVn`
  );
  assert(
    Boolean(item.definitionEn && item.definitionEn.trim().length > 0),
    `FALLBACK_FILL_BLANK item '${item.word}' includes definitionEn`
  );
  assert(
    !item.grammarContextTip.toLowerCase().includes(item.targetForm.toLowerCase()),
    `FALLBACK_FILL_BLANK item '${item.word}' grammarContextTip does not leak targetForm '${item.targetForm}'`
  );
}

console.log("\n--- AC4: TOUCH TARGET SIZES (>= 44x44px) ---");

const allRendered = [
  { name: "LearnWordsCard (Front)", html: htmlEnVi },
  { name: "LearnWordsCard (Back)", html: htmlEnViFlipped },
  { name: "MultipleChoiceCard (Unanswered)", html: htmlMcUnanswered },
  { name: "MultipleChoiceCard (Answered)", html: htmlMcAnsweredCorrect },
  { name: "TypeWordCard (Unchecked)", html: htmlTwUnchecked },
  { name: "TypeWordCard (Correct)", html: htmlTwCorrect },
  { name: "FillBlankCard (Unchecked)", html: htmlFbUnchecked },
  { name: "FillBlankCard (Checked)", html: htmlFbChecked },
];

for (const item of allRendered) {
  const buttonRegex = /<button([^>]*)>([\s\S]*?)<\/button>/g;
  let m;
  while ((m = buttonRegex.exec(item.html)) !== null) {
    const attrs = m[1];
    const inner = m[2].replace(/<[^>]+>/g, "").trim();
    const classMatch = attrs.match(/class="([^"]*)"/);
    const classes = classMatch ? classMatch[1] : "";

    const hasExplicitMin44 =
      classes.includes("min-h-[44px]") ||
      classes.includes("min-h-[48px]") ||
      classes.includes("min-h-[52px]") ||
      classes.includes("min-h-[64px]");

    console.log(`[${item.name}] Button: "${inner.slice(0, 30)}" | Classes: ${classes.slice(0, 60)}... | hasMin44: ${hasExplicitMin44}`);
  }
}
