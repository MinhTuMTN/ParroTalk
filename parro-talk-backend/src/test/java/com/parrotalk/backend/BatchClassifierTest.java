package com.parrotalk.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.service.LessonCategoryClassificationService;
import com.parrotalk.backend.entity.Lesson;
import java.util.List;

@Disabled("Utility script for manual execution")
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=none",
    "application.ai.api-keys=API_KEY"
})
public class BatchClassifierTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonCategoryClassificationService classificationService;

    @Test
    public void classifyAll() throws Exception {
        System.out.println("Starting batch classification...");
        List<Lesson> lessons = lessonRepository.findAll();
        System.out.println("Found " + lessons.size() + " total lessons.");
        for (Lesson lesson : lessons) {
            boolean hasCategories = false;
            try {
                // Try to get categories via service if there's a getter, or just try to classify directly.
                // Actually, the easiest way is to let classifyLessonCategories check it if it does, 
                // but it might not. We can query the database or just fetch it in a small transaction.
                // Let's just catch LazyInitializationException or avoid using getCategories().
                // I will just use a native query check.
            } catch (Exception e) {}
            
            System.out.println("Classifying lesson: " + lesson.getId() + " - " + lesson.getTitle());
            try {
                classificationService.classifyLessonCategories(lesson.getId());
                System.out.println("Successfully classified: " + lesson.getId());
            } catch (Exception e) {
                System.out.println("Failed to classify " + lesson.getId() + ": " + e.getMessage());
                if (e.getMessage().contains("429")) {
                    System.out.println("Rate limit exceeded. Waiting for 10 seconds...");
                    Thread.sleep(10000);
                }
            }
        }
        System.out.println("Batch classification finished.");
    }
}
