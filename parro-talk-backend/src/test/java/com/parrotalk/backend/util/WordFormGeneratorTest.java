package com.parrotalk.backend.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordFormGeneratorTest {

    private final WordFormGenerator generator = new WordFormGenerator();

    @Test
    void generateForms_story_containsStoriesAndExcludesStory() {
        List<String> forms = generator.generateForms("story");
        assertTrue(forms.contains("stories"), "Expected 'stories' in " + forms);
        assertFalse(forms.contains("story"), "Expected 'story' to be excluded from " + forms);
    }

    @Test
    void generateForms_find_containsIrregularAndRegularForms() {
        List<String> forms = generator.generateForms("find");
        assertTrue(forms.contains("finds"), "Expected 'finds' in " + forms);
        assertTrue(forms.contains("found"), "Expected 'found' in " + forms);
        assertTrue(forms.contains("finding"), "Expected 'finding' in " + forms);
        assertFalse(forms.contains("find"), "Expected 'find' to be excluded");
    }

    @Test
    void generateForms_child_containsChildren() {
        List<String> forms = generator.generateForms("child");
        assertTrue(forms.contains("children"), "Expected 'children' in " + forms);
        assertFalse(forms.contains("child"), "Expected 'child' to be excluded");
    }

    @Test
    void generateForms_amuse_containsSilentEInflections() {
        List<String> forms = generator.generateForms("amuse");
        assertTrue(forms.contains("amuses"), "Expected 'amuses' in " + forms);
        assertTrue(forms.contains("amused"), "Expected 'amused' in " + forms);
        assertTrue(forms.contains("amusing"), "Expected 'amusing' in " + forms);
        assertFalse(forms.contains("amuse"), "Expected 'amuse' to be excluded");
    }

    @Test
    void generateForms_blankOrNull_returnsEmptyList() {
        assertTrue(generator.generateForms("").isEmpty());
        assertTrue(generator.generateForms("   ").isEmpty());
        assertTrue(generator.generateForms(null).isEmpty());
    }

    @Test
    void generateForms_normalizesCaseAndDeduplicates() {
        List<String> forms = generator.generateForms("  Stop ");
        assertTrue(forms.contains("stops"), "Expected 'stops' in " + forms);
        assertTrue(forms.contains("stopped"), "Expected 'stopped' in " + forms);
        assertTrue(forms.contains("stopping"), "Expected 'stopping' in " + forms);
        assertFalse(forms.contains("stop"));
        assertEquals(forms.stream().distinct().count(), forms.size(), "Forms must be deduplicated");
        assertTrue(forms.stream().allMatch(f -> f.equals(f.toLowerCase().trim())), "All forms must be lowercase and trimmed");
    }

    @Test
    void generateForms_irregularAdjective_good() {
        List<String> forms = generator.generateForms("good");
        assertTrue(forms.contains("better"), "Expected 'better' in " + forms);
        assertTrue(forms.contains("best"), "Expected 'best' in " + forms);
    }

    @Test
    void generateForms_ieToY_die() {
        List<String> forms = generator.generateForms("die");
        assertTrue(forms.contains("dies"), "Expected 'dies' in " + forms);
        assertTrue(forms.contains("died"), "Expected 'died' in " + forms);
        assertTrue(forms.contains("dying"), "Expected 'dying' in " + forms);
    }

    @Test
    void generateForms_multiWord_returnsEmptyList() {
        assertTrue(generator.generateForms("look up").isEmpty());
    }
}
