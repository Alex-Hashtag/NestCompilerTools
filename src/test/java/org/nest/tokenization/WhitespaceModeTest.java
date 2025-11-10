package org.nest.tokenization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


class WhitespaceModeTest
{

    @Test
    void testWhitespaceModeValues()
    {
        // Verify the enum values
        assertEquals(3, WhitespaceMode.values().length);
        assertArrayEquals(new WhitespaceMode[]{
                WhitespaceMode.IGNORE,
                WhitespaceMode.SIGNIFICANT,
                WhitespaceMode.INDENTATION
        }, WhitespaceMode.values());
    }

    @Test
    void testWhitespaceModeNames()
    {
        assertEquals("IGNORE", WhitespaceMode.IGNORE.name());
        assertEquals("SIGNIFICANT", WhitespaceMode.SIGNIFICANT.name());
        assertEquals("INDENTATION", WhitespaceMode.INDENTATION.name());
    }
}
