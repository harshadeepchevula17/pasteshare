package com.pasteshare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasteIdGeneratorTest {

    private final PasteIdGenerator generator = new PasteIdGenerator();

    @Test
    void generatesUniqueTwentyTwoCharacterUrlSafeIds() {
        String first = generator.generate();
        String second = generator.generate();

        assertEquals(22, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]{22}"));
        assertNotEquals(first, second);
    }
}
