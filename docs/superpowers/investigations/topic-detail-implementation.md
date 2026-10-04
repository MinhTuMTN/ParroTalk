# Topic Detail Implementation Summary

## 1. Backend Scope
- Added `GET /api/vocabulary/topics/{uuid}` endpoint for topic summary.
- Added `GET /api/vocabulary/topics/{uuid}/words` endpoint for paginated words in a topic.
- Changed topic word count logic to `COUNT(DISTINCT vocabulary id)` in `TopicRepository` to match the words list.
- Passed correct user from `@AuthenticationPrincipal` into services to provide per-word learning statuses (NEW/LEARNING/MASTERED).
- Ensured `public-endpoints` in `application.yml` exposes `/api/vocabulary/topics/**` correctly for unauthenticated guests.
- Verified Flyway index (`idx_vocabulary_senses_topic_id`) already exists for performance.

## 2. Frontend Scope
- Next.js page at `app/(main)/vocabulary/topics/[id]/page.tsx` handles parallel data-fetching for the topic summary and paginated word list.
- Implemented page-based pagination with bounds checking, placing Prev/Next buttons gracefully.
- Replaced mock words (`DEFAULT_TRAVEL_WORDS`) with real data, and explicit handling for loading skeletons and errors (e.g. 404 Not Found).
- Safely handles unauthenticated sessions where word `status` might be null.

## 3. Verification
- `mvn clean test-compile` passes cleanly.
- `npx tsc --noEmit` validates the frontend code (ignoring unrelated errors in `admin` sections).
- The prior logic of `VocabularyRepository.searchVocabularies` was properly replaced with Criteria/Specification API in `PublicVocabularyService.java`, preventing the Postgres null binding issue entirely.

Next step is code review.
