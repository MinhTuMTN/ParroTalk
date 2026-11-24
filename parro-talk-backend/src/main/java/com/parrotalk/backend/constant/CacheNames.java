package com.parrotalk.backend.constant;

/**
 * Constants for Redis cache names.
 *
 * @author MinhTuMTN
 */
public final class CacheNames {

    private CacheNames() {
    }

    /** Cache for lesson search and filter results **/
    public static final String LESSON_SEARCH_CACHE = "lessonSearchCache";

    /** Cache for detailed lesson information **/
    public static final String LESSON_DETAIL_CACHE = "lessonDetailCache_v2";

    /** Cache for featured lessons on public landing page **/
    public static final String FEATURED_LESSONS_CACHE = "featuredLessonsCache";

    /** Cache for authenticated user profile data **/
    public static final String USER_CACHE = "userCache";

    /** Cache for admin detailed user profile **/
    public static final String ADMIN_USER_DETAIL_CACHE = "adminUserDetailCache";

    /** Cache for library sections **/
    public static final String LIBRARY_SECTIONS_CACHE = "librarySectionsCache";

    /** Cache for Cloudflare query embeddings in semantic lesson search **/
    public static final String SEMANTIC_QUERY_EMBEDDING_CACHE = "semanticQueryEmbeddingCache";
}
