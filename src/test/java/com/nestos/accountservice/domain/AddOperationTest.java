package com.nestos.accountservice.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Roman Osipov
 */
public class AddOperationTest {
    
    private static final int INVALID_ID = -43;
    private static final int VALID_ID = 43;
    private static final long VALUE = 67;
    
    public AddOperationTest() {
    }
    
    @Test
    public void constructorShouldWork() {
        AddOperation addOperation = new AddOperation(VALID_ID, VALUE);
        assertEquals("Wrong id.", VALID_ID, addOperation.getId());
        assertEquals("Wrong value.", VALUE, addOperation.getValue());
    }
    
    @Test
    public void constructorShouldThrowExceptionForNegativeId() {
        try {
             AddOperation addOperation = new AddOperation(INVALID_ID, VALUE);
            fail("constructor pass with negative id argument value.");
        } catch (IllegalArgumentException e) {
            assertEquals(AddOperation.ID_IAE_MESSAGE, e.getMessage());
        }
    }
}
