package com.parrotalk.backend.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Translation language enum.
 * 
 * @author MinhTuMTN
 */
@Getter
@RequiredArgsConstructor
public enum TranslationLanguage {

    /** English */
    ENGLISH("en"),
    /** Vietnamese */
    VIETNAMESE("vi"),
    /** Japanese */
    JAPANESE("ja"),
    /** Korean */
    KOREAN("ko"),
    /** Chinese */
    CHINESE("zh");

    /** Language code */
    private final String code;
}
