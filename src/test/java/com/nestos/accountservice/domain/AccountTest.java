package com.nestos.accountservice.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Account tests.
 *
 * @author Roman Osipov
 */
public class AccountTest {

    private static final int NEGATIVE_INT_VALUE = -43;
    private static final int POSITIVE_INT_VALUE = 43;
    private static final long POSITIVE_LONG_VALUE = 67;

    public AccountTest() {
    }

    @Test
    public void defaultConstructorShouldWork() {
        Account sutAccount = new Account();
        assertNull("Wrong id initial value.", sutAccount.getId());
        assertEquals("Wrong amount initial value.", 0, (long) sutAccount.getAmount());
    }

    @Test
    public void setIdShouldWork() {
        Account sutAccount = new Account();
        sutAccount.setId(POSITIVE_INT_VALUE);
        assertEquals(POSITIVE_INT_VALUE, (int) sutAccount.getId());
    }

    @Test
    public void setAmountShouldWork() {
        Account sutAccount = new Account();
        sutAccount.setAmount(POSITIVE_LONG_VALUE);
        assertEquals(POSITIVE_LONG_VALUE, (long) sutAccount.getAmount());
    }

    @Test
    public void setIdShouldThrowExceptionForNegativeId() {
        try {
            Account sutAccount = new Account();
            sutAccount.setId(NEGATIVE_INT_VALUE);
            fail("setId pass with negative id argument value.");
        } catch (IllegalArgumentException e) {
            assertEquals(Account.ID_IAE_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void setAmountShouldThrowExceptionForNullAmount() {
        try {
            Account sutAccount = new Account();
            sutAccount.setAmount(null);
            fail("setAmount pass with null amount argument value.");
        } catch (NullPointerException e) {
            assertEquals(Account.AMOUNT_NPE_MESSAGE, e.getMessage());
        }
    }

}
