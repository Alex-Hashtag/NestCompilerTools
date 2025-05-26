package org.nest.tokenization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordinatesTest {
    
    @Test
    void testCoordinatesCreation() {
        Coordinates coordinates = new Coordinates(10, 20);
        assertEquals(10, coordinates.line());
        assertEquals(20, coordinates.column());
    }
    
    @Test
    void testCoordinatesEquality() {
        Coordinates coordinates1 = new Coordinates(10, 20);
        Coordinates coordinates2 = new Coordinates(10, 20);
        Coordinates coordinates3 = new Coordinates(5, 15);
        
        assertEquals(coordinates1, coordinates2);
        assertNotEquals(coordinates1, coordinates3);
    }
    
    @Test
    void testCoordinatesToString() {
        Coordinates coordinates = new Coordinates(10, 20);
        String toString = coordinates.toString();
        
        assertTrue(toString.contains("10"));
        assertTrue(toString.contains("20"));
    }
}
