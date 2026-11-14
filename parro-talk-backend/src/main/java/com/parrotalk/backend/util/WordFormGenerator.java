package com.parrotalk.backend.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates inflected English word forms (plurals, verb conjugations, comparatives,
 * and common irregular forms) for a normalized vocabulary word.
 */
@Component
public class WordFormGenerator {

    private static final Map<String, List<String>> IRREGULAR_FORMS = Map.ofEntries(
            // Irregular verbs
            Map.entry("be", List.of("am", "is", "are", "was", "were", "been", "being")),
            Map.entry("have", List.of("has", "had", "having")),
            Map.entry("do", List.of("does", "did", "done", "doing")),
            Map.entry("go", List.of("goes", "went", "gone", "going")),
            Map.entry("say", List.of("says", "said", "saying")),
            Map.entry("get", List.of("gets", "got", "gotten", "getting")),
            Map.entry("make", List.of("makes", "made", "making")),
            Map.entry("know", List.of("knows", "knew", "known", "knowing")),
            Map.entry("think", List.of("thinks", "thought", "thinking")),
            Map.entry("take", List.of("takes", "took", "taken", "taking")),
            Map.entry("see", List.of("sees", "saw", "seen", "seeing")),
            Map.entry("come", List.of("comes", "came", "coming")),
            Map.entry("find", List.of("finds", "found", "finding")),
            Map.entry("give", List.of("gives", "gave", "given", "giving")),
            Map.entry("tell", List.of("tells", "told", "telling")),
            Map.entry("feel", List.of("feels", "felt", "feeling")),
            Map.entry("become", List.of("becomes", "became", "becoming")),
            Map.entry("leave", List.of("leaves", "left", "leaving")),
            Map.entry("put", List.of("puts", "putting")),
            Map.entry("mean", List.of("means", "meant", "meaning")),
            Map.entry("keep", List.of("keeps", "kept", "keeping")),
            Map.entry("let", List.of("lets", "letting")),
            Map.entry("begin", List.of("begins", "began", "begun", "beginning")),
            Map.entry("show", List.of("shows", "showed", "shown", "showing")),
            Map.entry("hear", List.of("hears", "heard", "hearing")),
            Map.entry("run", List.of("runs", "ran", "running")),
            Map.entry("hold", List.of("holds", "held", "holding")),
            Map.entry("bring", List.of("brings", "brought", "bringing")),
            Map.entry("write", List.of("writes", "wrote", "written", "writing")),
            Map.entry("sit", List.of("sits", "sat", "sitting")),
            Map.entry("stand", List.of("stands", "stood", "standing")),
            Map.entry("lose", List.of("loses", "lost", "losing")),
            Map.entry("pay", List.of("pays", "paid", "paying")),
            Map.entry("meet", List.of("meets", "met", "meeting")),
            Map.entry("set", List.of("sets", "setting")),
            Map.entry("learn", List.of("learns", "learned", "learnt", "learning")),
            Map.entry("lead", List.of("leads", "led", "leading")),
            Map.entry("understand", List.of("understands", "understood", "understanding")),
            Map.entry("speak", List.of("speaks", "spoke", "spoken", "speaking")),
            Map.entry("read", List.of("reads", "reading")),
            Map.entry("spend", List.of("spends", "spent", "spending")),
            Map.entry("grow", List.of("grows", "grew", "grown", "growing")),
            Map.entry("win", List.of("wins", "won", "winning")),
            Map.entry("buy", List.of("buys", "bought", "buying")),
            Map.entry("send", List.of("sends", "sent", "sending")),
            Map.entry("build", List.of("builds", "built", "building")),
            Map.entry("fall", List.of("falls", "fell", "fallen", "falling")),
            Map.entry("cut", List.of("cuts", "cutting")),
            Map.entry("sell", List.of("sells", "sold", "selling")),
            Map.entry("break", List.of("breaks", "broke", "broken", "breaking")),
            Map.entry("catch", List.of("catches", "caught", "catching")),
            Map.entry("choose", List.of("chooses", "chose", "chosen", "choosing")),
            Map.entry("draw", List.of("draws", "drew", "drawn", "drawing")),
            Map.entry("drive", List.of("drives", "drove", "driven", "driving")),
            Map.entry("eat", List.of("eats", "ate", "eaten", "eating")),
            Map.entry("fly", List.of("flies", "flew", "flown", "flying")),
            Map.entry("forget", List.of("forgets", "forgot", "forgotten", "forgetting")),
            Map.entry("forgive", List.of("forgives", "forgave", "forgiven", "forgiving")),
            Map.entry("freeze", List.of("freezes", "froze", "frozen", "freezing")),
            Map.entry("hide", List.of("hides", "hid", "hidden", "hiding")),
            Map.entry("hit", List.of("hits", "hitting")),
            Map.entry("hurt", List.of("hurts", "hurting")),
            Map.entry("lay", List.of("lays", "laid", "laying")),
            Map.entry("lie", List.of("lies", "lay", "lain", "lying")),
            Map.entry("ride", List.of("rides", "rode", "ridden", "riding")),
            Map.entry("ring", List.of("rings", "rang", "rung", "ringing")),
            Map.entry("rise", List.of("rises", "rose", "risen", "rising")),
            Map.entry("seek", List.of("seeks", "sought", "seeking")),
            Map.entry("shake", List.of("shakes", "shook", "shaken", "shaking")),
            Map.entry("shoot", List.of("shoots", "shot", "shooting")),
            Map.entry("shut", List.of("shuts", "shutting")),
            Map.entry("sing", List.of("sings", "sang", "sung", "singing")),
            Map.entry("sink", List.of("sinks", "sank", "sunk", "sinking")),
            Map.entry("sleep", List.of("sleeps", "slept", "sleeping")),
            Map.entry("steal", List.of("steals", "stole", "stolen", "stealing")),
            Map.entry("stick", List.of("sticks", "stuck", "sticking")),
            Map.entry("strike", List.of("strikes", "struck", "striking")),
            Map.entry("swear", List.of("swears", "swore", "sworn", "swearing")),
            Map.entry("sweep", List.of("sweeps", "swept", "sweeping")),
            Map.entry("swim", List.of("swims", "swam", "swum", "swimming")),
            Map.entry("teach", List.of("teaches", "taught", "teaching")),
            Map.entry("tear", List.of("tears", "tore", "torn", "tearing")),
            Map.entry("throw", List.of("throws", "threw", "thrown", "throwing")),
            Map.entry("wake", List.of("wakes", "woke", "woken", "waking")),
            Map.entry("wear", List.of("wears", "wore", "worn", "wearing")),
            Map.entry("weep", List.of("weeps", "wept", "weeping")),
            // Irregular nouns
            Map.entry("child", List.of("children")),
            Map.entry("person", List.of("people", "persons")),
            Map.entry("man", List.of("men")),
            Map.entry("woman", List.of("women")),
            Map.entry("foot", List.of("feet")),
            Map.entry("tooth", List.of("teeth")),
            Map.entry("goose", List.of("geese")),
            Map.entry("mouse", List.of("mice")),
            Map.entry("ox", List.of("oxen")),
            Map.entry("criterion", List.of("criteria")),
            Map.entry("phenomenon", List.of("phenomena")),
            Map.entry("analysis", List.of("analyses")),
            Map.entry("basis", List.of("bases")),
            Map.entry("crisis", List.of("crises")),
            Map.entry("thesis", List.of("theses")),
            Map.entry("datum", List.of("data")),
            Map.entry("medium", List.of("media")),
            Map.entry("curriculum", List.of("curricula")),
            Map.entry("appendix", List.of("appendices")),
            Map.entry("index", List.of("indices", "indexes")),
            // Irregular adjectives / adverbs
            Map.entry("good", List.of("better", "best")),
            Map.entry("well", List.of("better", "best")),
            Map.entry("bad", List.of("worse", "worst")),
            Map.entry("badly", List.of("worse", "worst")),
            Map.entry("far", List.of("farther", "farthest", "further", "furthest")),
            Map.entry("little", List.of("less", "least")),
            Map.entry("much", List.of("more", "most")),
            Map.entry("many", List.of("more", "most")),
            Map.entry("old", List.of("older", "oldest", "elder", "eldest"))
    );

    public List<String> generateForms(String normalizedWord) {
        if (normalizedWord == null) {
            return List.of();
        }
        String base = normalizedWord.trim().toLowerCase(Locale.ROOT);
        if (base.isEmpty() || base.chars().anyMatch(Character::isWhitespace)) {
            return List.of();
        }

        Set<String> forms = new TreeSet<>();

        List<String> irregulars = IRREGULAR_FORMS.get(base);
        if (irregulars != null) {
            for (String form : irregulars) {
                addNormalized(forms, form, base);
            }
        }

        addPluralAndThirdPersonForms(forms, base);
        addVerbAndAdjectiveInflections(forms, base);

        forms.remove(base);
        return new ArrayList<>(forms);
    }

    private void addPluralAndThirdPersonForms(Set<String> forms, String base) {
        if (endsWithConsonantY(base)) {
            addNormalized(forms, base.substring(0, base.length() - 1) + "ies", base);
        } else if (base.endsWith("s") || base.endsWith("x") || base.endsWith("z")
                || base.endsWith("ch") || base.endsWith("sh")) {
            addNormalized(forms, base + "es", base);
        } else if (base.endsWith("fe")) {
            addNormalized(forms, base.substring(0, base.length() - 2) + "ves", base);
            addNormalized(forms, base + "s", base);
        } else if (base.endsWith("f") && !base.endsWith("ff")) {
            addNormalized(forms, base.substring(0, base.length() - 1) + "ves", base);
            addNormalized(forms, base + "s", base);
        } else if (base.endsWith("o") && base.length() > 1 && !isVowel(base.charAt(base.length() - 2))) {
            addNormalized(forms, base + "es", base);
            addNormalized(forms, base + "s", base);
        } else {
            addNormalized(forms, base + "s", base);
        }
    }

    private void addVerbAndAdjectiveInflections(Set<String> forms, String base) {
        if (base.endsWith("ie")) {
            String stem = base.substring(0, base.length() - 2);
            addNormalized(forms, base + "d", base);
            addNormalized(forms, stem + "ying", base);
            return;
        }

        if (base.endsWith("ee") || base.endsWith("ye") || base.endsWith("oe")) {
            addNormalized(forms, base + "d", base);
            addNormalized(forms, base + "ing", base);
            addNormalized(forms, base + "r", base);
            addNormalized(forms, base + "st", base);
            return;
        }

        if (base.endsWith("e") && base.length() > 1) {
            String stem = base.substring(0, base.length() - 1);
            addNormalized(forms, base + "d", base);
            addNormalized(forms, stem + "ing", base);
            addNormalized(forms, base + "r", base);
            addNormalized(forms, base + "st", base);
            return;
        }

        if (endsWithConsonantY(base)) {
            String stem = base.substring(0, base.length() - 1);
            addNormalized(forms, stem + "ied", base);
            addNormalized(forms, base + "ing", base);
            addNormalized(forms, stem + "ier", base);
            addNormalized(forms, stem + "iest", base);
            return;
        }

        if (shouldDoubleFinalConsonant(base)) {
            char last = base.charAt(base.length() - 1);
            String doubled = base + last;
            addNormalized(forms, doubled + "ed", base);
            addNormalized(forms, doubled + "ing", base);
            addNormalized(forms, doubled + "er", base);
            addNormalized(forms, doubled + "est", base);
            return;
        }

        addNormalized(forms, base + "ed", base);
        addNormalized(forms, base + "ing", base);
        addNormalized(forms, base + "er", base);
        addNormalized(forms, base + "est", base);
    }

    private boolean endsWithConsonantY(String word) {
        return word.length() > 1
                && word.endsWith("y")
                && !isVowel(word.charAt(word.length() - 2));
    }

    private boolean shouldDoubleFinalConsonant(String word) {
        if (word.length() < 3) {
            return false;
        }
        char last = word.charAt(word.length() - 1);
        char middle = word.charAt(word.length() - 2);
        char first = word.charAt(word.length() - 3);
        if (last == 'w' || last == 'x' || last == 'y') {
            return false;
        }
        return !isVowel(last) && isVowel(middle) && !isVowel(first) && countVowelGroups(word) == 1;
    }

    private int countVowelGroups(String word) {
        int groups = 0;
        boolean inVowelGroup = false;
        for (int i = 0; i < word.length(); i++) {
            boolean vowel = isVowel(word.charAt(i));
            if (vowel && !inVowelGroup) {
                groups++;
                inVowelGroup = true;
            } else if (!vowel) {
                inVowelGroup = false;
            }
        }
        return groups;
    }

    private boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private void addNormalized(Set<String> forms, String candidate, String base) {
        if (candidate == null) {
            return;
        }
        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty() && !normalized.equals(base)) {
            forms.add(normalized);
        }
    }
}
