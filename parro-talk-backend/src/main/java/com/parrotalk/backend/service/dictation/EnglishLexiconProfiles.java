package com.parrotalk.backend.service.dictation;

import java.util.Map;
import java.util.Set;

/**
 * Curated offline linguistic profiles for English closed-class words, discourse fillers,
 * weak forms, homophones, phrasal particles, and silent-letter/orthography patterns.
 * Zero runtime AI dependencies.
 */
public final class EnglishLexiconProfiles {

    private EnglishLexiconProfiles() {
    }

    public static final Set<String> ARTICLES_DETERMINERS = Set.of(
            "a", "an", "the", "this", "that", "these", "those", "some", "any", "each", "every", "either", "neither",
            "no", "both", "few", "several", "many", "much", "more", "most", "such");

    public static final Set<String> PRONOUNS = Set.of(
            "i", "me", "my", "mine", "myself",
            "you", "your", "yours", "yourself", "yourselves",
            "he", "him", "his", "himself",
            "she", "her", "hers", "herself",
            "it", "its", "itself",
            "we", "us", "our", "ours", "ourselves",
            "they", "them", "their", "theirs", "themselves",
            "who", "whom", "whose", "which", "what", "whoever", "whatever",
            "something", "anything", "everything", "nothing", "someone", "anyone", "everyone", "noone");

    public static final Set<String> PREPOSITIONS = Set.of(
            "in", "on", "at", "to", "for", "of", "with", "by", "from", "about", "into", "through", "during",
            "before", "after", "above", "below", "between", "among", "under", "over", "around", "against",
            "without", "within", "along", "across", "behind", "beyond", "beside", "near", "off", "out", "up",
            "down", "upon", "toward", "towards", "since", "until", "till", "per", "via", "except", "despite");

    public static final Set<String> CONJUNCTIONS = Set.of(
            "and", "or", "but", "nor", "so", "yet", "for",
            "because", "although", "though", "while", "whereas", "unless", "if", "whether",
            "since", "as", "once", "when", "whenever", "where", "wherever", "than");

    public static final Set<String> AUXILIARIES_MODALS = Set.of(
            "am", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "having",
            "do", "does", "did",
            "will", "would", "shall", "should", "can", "could", "may", "might", "must", "ought", "need");

    public static final Set<String> NEGATORS = Set.of(
            "not", "never", "neither", "nor", "none", "nothing", "nobody", "nowhere", "hardly", "scarcely", "barely");

    public static final Set<String> PHRASAL_PARTICLES = Set.of(
            "up", "down", "out", "off", "in", "on", "away", "back", "over", "through", "along", "around", "apart", "aside");

    public static final Set<String> DISCOURSE_FILLERS = Set.of(
            "uh", "um", "er", "ah", "oh", "hmm", "hm", "well", "actually", "basically", "literally", "like", "yeah", "okay", "ok");

    public static final Set<String> WEAK_FORM_WORDS = Set.of(
            "a", "an", "the", "to", "for", "of", "from", "at", "by", "in", "on", "as", "than", "that",
            "and", "or", "but", "can", "could", "have", "has", "had", "do", "does", "will", "would",
            "am", "are", "is", "was", "were", "be", "been", "you", "your", "he", "his", "him", "her",
            "them", "their", "us", "our", "we", "there", "some", "must", "should");

    public static final Set<String> H_DROPPING_WORDS = Set.of(
            "he", "him", "his", "her", "hers", "have", "has", "had", "here", "how", "who");

    public static final Set<String> HOMOPHONES = Set.of(
            "their", "there", "they're",
            "your", "you're",
            "its", "it's",
            "to", "too", "two",
            "passed", "past",
            "weather", "whether",
            "affect", "effect",
            "accept", "except",
            "principal", "principle",
            "compliment", "complement",
            "stationery", "stationary",
            "here", "hear",
            "wear", "where", "were",
            "know", "no",
            "knew", "new",
            "right", "write",
            "break", "brake",
            "piece", "peace",
            "plain", "plane",
            "wait", "weight",
            "week", "weak",
            "buy", "by", "bye",
            "meet", "meat",
            "hour", "our",
            "one", "won",
            "ate", "eight",
            "for", "four",
            "see", "sea",
            "son", "sun",
            "pair", "pear",
            "tail", "tale",
            "mail", "male",
            "flour", "flower",
            "bare", "bear",
            "fair", "fare",
            "hole", "whole",
            "knight", "night",
            "road", "rode",
            "scene", "seen",
            "steal", "steel",
            "threw", "through",
            "allowed", "aloud");

    public static final Set<String> NUMBER_WORDS = Set.of(
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
            "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety", "hundred", "thousand", "million",
            "first", "second", "third", "fourth", "fifth");

    public static final Set<String> INTERJECTION_SINGLE_WORDS = Set.of(
            "uh", "um", "er", "ah", "oh", "hmm", "hm", "yeah", "yes", "no", "ok", "okay", "hey", "hi", "bye", "wow", "music", "applause", "laughter");

    /**
     * Common irregular past / participle forms that cannot be inferred from simple suffix checks alone.
     */
    public static final Set<String> IRREGULAR_INFLECTIONS = Set.of(
            "went", "gone", "came", "saw", "seen", "took", "taken", "made", "gave", "given", "found", "thought",
            "told", "felt", "left", "kept", "began", "begun", "shown", "heard", "ran", "held", "brought",
            "wrote", "written", "sat", "stood", "lost", "paid", "met", "learnt", "led", "understood", "spoke",
            "spoken", "spent", "grew", "grown", "won", "bought", "sent", "built", "fell", "fallen", "sold",
            "broke", "broken", "caught", "chose", "chosen", "drew", "drawn", "drove", "driven", "ate", "eaten",
            "flew", "flown", "forgot", "forgotten", "froze", "frozen", "hid", "hidden", "rode", "ridden",
            "rang", "rung", "rose", "risen", "sought", "shook", "shaken", "shot", "sang", "sung", "slept",
            "stole", "stolen", "swam", "swum", "taught", "tore", "torn", "threw", "thrown", "woke", "woken",
            "wore", "worn", "children", "people", "men", "women", "feet", "teeth", "mice", "better", "best",
            "worse", "worst");

    /**
     * Common English suffixes strongly indicative of open-class words when absent from the DB lexicon.
     */
    public static final Map<String, String> SUFFIX_POS_HINTS = Map.ofEntries(
            Map.entry("tion", "noun"),
            Map.entry("sion", "noun"),
            Map.entry("ment", "noun"),
            Map.entry("ness", "noun"),
            Map.entry("ity", "noun"),
            Map.entry("ance", "noun"),
            Map.entry("ence", "noun"),
            Map.entry("ship", "noun"),
            Map.entry("ology", "noun"),
            Map.entry("able", "adjective"),
            Map.entry("ible", "adjective"),
            Map.entry("ous", "adjective"),
            Map.entry("ful", "adjective"),
            Map.entry("less", "adjective"),
            Map.entry("ive", "adjective"),
            Map.entry("al", "adjective"),
            Map.entry("ly", "adverb"),
            Map.entry("ize", "verb"),
            Map.entry("ise", "verb"),
            Map.entry("ify", "verb"),
            Map.entry("ate", "verb"));

    public static boolean isClosedClass(String normalizedWord) {
        return ARTICLES_DETERMINERS.contains(normalizedWord)
                || PRONOUNS.contains(normalizedWord)
                || PREPOSITIONS.contains(normalizedWord)
                || CONJUNCTIONS.contains(normalizedWord)
                || AUXILIARIES_MODALS.contains(normalizedWord);
    }

    public static boolean isNegativeContraction(String normalizedWord) {
        return normalizedWord != null && normalizedWord.endsWith("n't");
    }

    public static boolean isContraction(String normalizedWord) {
        return normalizedWord != null && normalizedWord.contains("'");
    }

    public static boolean isNumberToken(String normalizedWord) {
        if (normalizedWord == null || normalizedWord.isBlank()) {
            return false;
        }
        return normalizedWord.matches("\\d+(?:\\.\\d+)?") || NUMBER_WORDS.contains(normalizedWord);
    }

    public static boolean hasSilentLetterOrOpaqueOrthography(String normalizedWord) {
        if (normalizedWord == null || normalizedWord.length() < 3) {
            return false;
        }
        return normalizedWord.startsWith("kn")
                || normalizedWord.startsWith("wr")
                || normalizedWord.startsWith("gn")
                || normalizedWord.startsWith("ps")
                || normalizedWord.contains("ough")
                || normalizedWord.contains("augh")
                || normalizedWord.contains("igh")
                || normalizedWord.contains("mb")
                || normalizedWord.contains("bt")
                || normalizedWord.contains("sc")
                || normalizedWord.contains("ph")
                || normalizedWord.contains("que");
    }

    public static boolean startsWithVowelSound(String normalizedWord) {
        if (normalizedWord == null || normalizedWord.isEmpty()) {
            return false;
        }
        char c = normalizedWord.charAt(0);
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    public static boolean endsWithConsonantChar(String normalizedWord) {
        if (normalizedWord == null || normalizedWord.isEmpty()) {
            return false;
        }
        char c = normalizedWord.charAt(normalizedWord.length() - 1);
        return Character.isLetter(c) && !(c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u');
    }
}
