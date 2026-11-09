package com.parrotalk.backend.dto.vocablist;

public record ShareVocabularyListRequest(
        String emailOrUsername,
        String recipientEmailOrUsername,
        Boolean enableShareLink
) {
    public ShareVocabularyListRequest(String emailOrUsername, Boolean enableShareLink) {
        this(emailOrUsername, null, enableShareLink);
    }

    public String resolveTarget() {
        if (emailOrUsername != null && !emailOrUsername.isBlank()) {
            return emailOrUsername.trim();
        }
        if (recipientEmailOrUsername != null && !recipientEmailOrUsername.isBlank()) {
            return recipientEmailOrUsername.trim();
        }
        return null;
    }
}
