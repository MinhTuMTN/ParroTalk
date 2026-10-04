# Topic Detail Implementation Summary

## Changed Files
### Backend
- `VocabularyRepository.java`: Removed buggy native query `findRandomVocabularies` and made the interface extend `VocabularyRepositoryCustom`.
- `VocabularyRepositoryCustom.java`: Created custom interface for vocabulary dynamic queries.
- `VocabularyRepositoryImpl.java`: Implemented `findRandomVocabularies` using `EntityManager` to safely build dynamic queries without the NULL param bug.
- `PublicVocabularyController.java`: Added two new endpoints `GET /api/vocabulary/topics/{id}` and `GET /api/vocabulary/topics/{id}/words`.
- `PublicVocabularyService.java`: Made `getTopicIcon` public so it can be used by `VocabularyTopicService`.

### Frontend
- `features/vocabulary/services/api.ts`: Added `getTopicSummary` and `getTopicWords` API fetchers.
- `app/(main)/vocabulary/topics/[id]/page.tsx`: Rewrote the page to:
  - Fetch topic summary and paginated words in parallel.
  - Render explicit Not Found and Error states with retry logic.
  - Display page-based pagination controls above the list.
  - Safely extract word status from backend responses and remove all hardcoded mock data.

## Endpoints
- `GET /api/vocabulary/topics/{uuid}` -> returns topic summary (word count, name, description, learned count).
- `GET /api/vocabulary/topics/{uuid}/words?page={page}&size={size}` -> returns a Page of `TopicWordDto` including learning statuses.

## Verification Evidence
- `mvn clean test-compile` passes without errors.
- `npx tsc --noEmit` passes for touched frontend files.
- The repository was restructured to fix the `NULL` binding bug robustly, rather than just bypassing it with hacks.
