package com.parrotalk.backend;

import com.parrotalk.backend.service.VocabularyImporterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("import-run") // Will only run if the profile is active
@RequiredArgsConstructor
public class ImportRunner implements CommandLineRunner {

    private final VocabularyImporterService importerService;

    @Override
    public void run(String... args) throws Exception {
        String filePath = "d:/Projects/english-dictation/vocab-data/new/dictionary_cambridge_enhanced_v1.json";
        log.info("AUTO STARTING IMPORT FOR FILE: {}", filePath);
        importerService.importFromJson(filePath);
        log.info("AUTO IMPORT FINISHED");
    }
}
