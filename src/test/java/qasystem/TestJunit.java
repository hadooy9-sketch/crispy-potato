package qasystem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJunit {
    @Test // Test has to be in the file name or maven looks over it
    public void testAddition() {
        int result = 2 + 3;
        assertEquals(5, result);
    }
}

//confirms JUnit and Maven are wired up correctly. FUN FACT the sibling files have to match up
//assertEquals(expected, actual) fails the test if the two values don't match