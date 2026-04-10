import { Sentence } from "@/types";

const MOCK_DATA: Sentence[] = [
  {
    id: "1",
    audioUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    text: "The quick brown fox jumps over the lazy dog."
  },
  {
    id: "2",
    audioUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
    text: "Practicing English dictation improves your listening skills significantly."
  }
];

export async function fetchSentences(): Promise<Sentence[]> {
  // Simulate network delay
  await new Promise(resolve => setTimeout(resolve, 800));
  return MOCK_DATA;
}

export async function submitResult(result: any) {
    // Simulate API call
    console.log("Submitting result:", result);
    await new Promise(resolve => setTimeout(resolve, 500));
    return { success: true };
}
