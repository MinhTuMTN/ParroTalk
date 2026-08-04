package com.parrotalk.backend.constant;

/**
 * SM-2 algorithm ratings for vocabulary recall.
 */
public enum Sm2Rating {
    AGAIN(0),
    HARD(1),
    GOOD(2),
    EASY(3);

    private final int value;

    Sm2Rating(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
