package com.parrotalk.backend.util;

import java.util.regex.Pattern;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Youtube Util.
 * Utility class for YouTube video operations
 */
public class YoutubeUtil {

    /**
     * Extract video ID from YouTube URL
     * 
     * @param url YouTube URL
     * @return Optional Video ID
     */
    public static Optional<String> extractVideoId(String url) {
        // Regex to extract video ID from various YouTube URL formats
        String regex = "(?:youtube\\.com\\/(?:[^\\/]+\\/.*\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})[\\?&]?.*";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }

        return Optional.empty();
    }

    /**
     * Get thumbnail URL from YouTube URL
     * 
     * @param url YouTube URL
     * @return Thumbnail URL
     */
    public static String getThumbnailUrl(String url) {
        Optional<String> videoId = extractVideoId(url);

        if (videoId.isPresent()) {
            return "https://img.youtube.com/vi/" + videoId.get() + "/hqdefault.jpg";
        }

        return null;
    }

    /**
     * Check if the URL is a YouTube URL
     * 
     * @param url URL to check
     * @return True if the URL is a YouTube URL, false otherwise
     */
    public static boolean isYoutubeUrl(String url) {
        return extractVideoId(url).isPresent();
    }
}
