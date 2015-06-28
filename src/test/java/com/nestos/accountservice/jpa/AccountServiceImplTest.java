package com.nestos.accountservice.jpa;

import com.nestos.accountservice.domain.Account;
import com.nestos.accountservice.domain.AddOperation;
import com.nestos.accountservice.javaconfig.UnitTestConfig;
import com.nestos.accountservice.kafka.KafkaClient;
import com.nestos.accountservice.repository.AccountRepository;
import com.nestos.accountservice.service.AccountService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

/**
 * AccountService tests. This test not true unit test, because depends on Spring container.
 *
 * @author Roman Osipov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UnitTestConfig.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class AccountServiceImplTest {

    private static final int NEGATIVE_INT_VALUE = -43;
    private static final Integer VALID_ID = 47;
    private static final Long VALID_AMOUNT = 113L;
    private static final Long VALID_INC_AMOUNT = 257L;

    @Autowired
    private AccountRepository mockAccountRepository;

    @Autowired
    private AccountService sutAccountService;

    @Autowired
    private KafkaClient mockKafkaClient;

    public AccountServiceImplTest() {
    }

    @Before
    public void setUp() {
        // disable procesing log for test run
        Logger x = Logger.getLogger("com.nestos.processing");
        x.setLevel(Level.OFF);
        LoggerRepository repository = x.getLoggerRepository();
        repository.setThreshold(Level.OFF);
    }

    //---------------getAmmount tests------------------------------------------
    @Test
    public void getAmountShouldThrowExceptionForNegativeId() {
        try {
            sutAccountService.getAmount(NEGATIVE_INT_VALUE);
            fail("getId pass with negative id argument value.");
        } catch (IllegalArgumentException e) {
            assertEquals(AccountServiceImpl.ID_IAE_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void getAmountShouldThrowExceptionForNullId() {
        try {
            sutAccountService.getAmount(null);
            fail("getAmount pass with null id argument value.");
        } catch (NullPointerException e) {
            assertEquals(AccountServiceImpl.ID_NPE_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void getAmountShouldReturnZeroForUnknownId() {
        // arrange 

        // act
        Long realAmount = sutAccountService.getAmount(VALID_ID);
        // assert
        verify(mockAccountRepository, only()).findOne(VALID_ID);
        assertEquals(0, (long) realAmount);
    }

    @Test
    public void getAmountShouldRequestRepositoryIfValueNotCached() {
        // arrange 
        Account stubAccount = new Account();
        stubAccount.setId(VALID_ID);
        stubAccount.setAmount(VALID_AMOUNT);
        when(mockAccountRepository.findOne(VALID_ID)).thenReturn(stubAccount);
        // act
        Long realAmount = sutAccountService.getAmount(VALID_ID);
        // assert
        verify(mockAccountRepository, only()).findOne(VALID_ID);
        assertEquals(VALID_AMOUNT, realAmount);
    }

    @Test
    public void getAmountShouldGetCachedValueIfInvokeTwice() {
        // arrange
        Account stubAccount = new Account();
        stubAccount.setId(VALID_ID);
        stubAccount.setAmount(VALID_AMOUNT);
        when(mockAccountRepository.findOne(VALID_ID))
                .thenReturn(stubAccount);
        // act
        Long realAmount1 = sutAccountService.getAmount(VALID_ID);
        Long realAmount2 = sutAccountService.getAmount(VALID_ID);
        // assert
        verify(mockAccountRepository, only()).findOne(VALID_ID);
        assertEquals(VALID_AMOUNT, realAmount1);
        assertEquals(VALID_AMOUNT, realAmount2);
    }

    /*
     @Test
     public void getAmountShouldGetCachedValueIfInvokeAfterAddAmount() {
     // arrange
     Account stubAccount = new Account();
     stubAccount.setId(VALID_ID);
     stubAccount.setAmount(VALID_AMOUNT);
     when(mockAccountRepository.findOne(VALID_ID))
     .thenReturn(stubAccount);
     // act
     sutAccountService.addAmount(VALID_ID, VALID_INC_AMOUNT);
     Long realAmount = sutAccountService.getAmount(VALID_ID);

     // assert
     // invokes only one times in addAmount 
     verify(mockAccountRepository, times(1)).findOne(VALID_ID);
     assertEquals(VALID_AMOUNT + VALID_INC_AMOUNT, (long) realAmount);
     }
     */
    //---------------addAmmount tests------------------------------------------
    @Test
    public void addAmountShouldThrowExceptionForNegativeId() {
        try {
            sutAccountService.addAmount(NEGATIVE_INT_VALUE, VALID_AMOUNT);
            fail("addAmount pass with negative id argument value.");
        } catch (IllegalArgumentException e) {
            assertEquals(AccountServiceImpl.ID_IAE_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void addAmountShouldThrowExceptionForNullId() {
        try {
            sutAccountService.addAmount(null, VALID_AMOUNT);
            fail("addAmount pass with null id argument value.");
        } catch (NullPointerException e) {
            assertEquals(AccountServiceImpl.ID_NPE_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void addAmountShouldThrowExceptionForNullAmount() {
        try {
            sutAccountService.addAmount(VALID_ID, null);
            fail("addAmount pass with null value argument value.");
        } catch (NullPointerException e) {
            assertEquals(AccountServiceImpl.VALUE_NPE_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void addAmountShouldInvokeKafkaWrite() {
        // arrange
        AddOperation stubAddOperation = new AddOperation(VALID_ID, VALID_INC_AMOUNT);
        // act
        sutAccountService.addAmount(VALID_ID, VALID_INC_AMOUNT);
        // assert
        verify(mockKafkaClient, times(1)).write(stubAddOperation);
    }

    /*
     @Test
     public void addAmountShouldCreateAndSaveNewAccountForUnknownId() {
     // arrange
     Account stubAccount = new Account();

     Account expectedAccount = new Account();
     expectedAccount.setId(VALID_ID);
     expectedAccount.setAmount(VALID_AMOUNT);

     when(mockAccountRepository.newAccountInstance()).thenReturn(stubAccount);

     // act
     sutAccountService.addAmount(VALID_ID, VALID_AMOUNT);

     // assert
     verify(mockAccountRepository, times(1)).newAccountInstance();
     verify(mockAccountRepository, times(1)).save(expectedAccount);
     }

     @Test
     public void addAmountShouldIncreaseAndSaveAccountForId() {
     // arrange
     Account stubInitionalAccount = new Account();
     stubInitionalAccount.setId(VALID_ID);
     stubInitionalAccount.setAmount(VALID_AMOUNT);

     Account stubModifiedAccount = new Account();
     stubModifiedAccount.setId(VALID_ID);
     stubModifiedAccount.setAmount(VALID_AMOUNT + VALID_INC_AMOUNT);

     when(mockAccountRepository.findOne(VALID_ID)).thenReturn(stubInitionalAccount);

     // act
     sutAccountService.addAmount(VALID_ID, VALID_INC_AMOUNT);

     // assert
     verify(mockAccountRepository, never()).newAccountInstance();
     verify(mockAccountRepository, times(1)).save(stubModifiedAccount);
     }
    
     @Test
     @SuppressWarnings("unchecked")
     public void addAmountShouldNotAffectCacheAfterException() {
     // arrange
     Account stubAccount = new Account();

     when(mockAccountRepository.newAccountInstance()).thenReturn(stubAccount);
     when(mockAccountRepository.findOne(VALID_ID)).thenReturn(null)
     .thenReturn(stubAccount);
     when(mockAccountRepository.save(isA(Account.class)))
     .thenReturn(stubAccount).thenThrow(RuntimeException.class);

     // act
     // normal invocation - cache hold VALID_AMOUNT for VALID_ID
     sutAccountService.addAmount(VALID_ID, VALID_AMOUNT);
     // invocation with exception - cache must not modify
     try {
     sutAccountService.addAmount(VALID_ID, VALID_INC_AMOUNT);
     fail("unexpexted normal execution addAmount.");
     } catch (RuntimeException e) {
     }
     Long realAmount = sutAccountService.getAmount(VALID_ID);

     // assert
     assertEquals(VALID_AMOUNT, realAmount);
     }
     */
}
