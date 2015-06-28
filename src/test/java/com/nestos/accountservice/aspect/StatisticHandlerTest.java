package com.nestos.accountservice.aspect;

import com.nestos.accountservice.domain.Account;
import com.nestos.accountservice.javaconfig.UnitTestConfig;
import com.nestos.accountservice.repository.AccountRepository;
import com.nestos.accountservice.service.AccountService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Statistic collector tests. This tests not true unit test, because depends on Spring container.
 *
 * @author Roman Osipov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UnitTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StatisticHandlerTest {

    private static final Integer VALID_ID = 47;
    private static final Long VALID_AMOUNT = 113L;
    private static final Long VALID_INC_AMOUNT = 257L;
    private static final String GET_AMOUNT_METHOD_NAME = "getAmount";
    private static final String ADD_AMOUNT_METHOD_NAME = "addAmount";

    @Autowired
    private AccountRepository mockAccountRepository;

    @Autowired
    private AccountService sutAccountService;

    @Autowired
    private StatisticHandler statisticHandler;

    public StatisticHandlerTest() {
    }

    @Before
    public void setUp() {
        // disable procesing log for test run
        Logger x = Logger.getLogger("com.nestos.processing");
        x.setLevel(Level.OFF);
        LoggerRepository repository = x.getLoggerRepository();
        repository.setThreshold(Level.OFF);
    }

    @Test
    public void getMethodInvocationCountShouldReturnZeroForNoInvocations() {
        assertEquals(0, (long) statisticHandler.getMethodInvocationCount(GET_AMOUNT_METHOD_NAME));
        assertEquals(0, (long) statisticHandler.getMethodInvocationCount(ADD_AMOUNT_METHOD_NAME));
    }

    @Test
    public void getMethodInvocationCountShouldReturnTwoForTwoInvocationsGetAmount() {
        // arrange
        Account stubAccount = new Account();
        stubAccount.setId(VALID_ID);
        stubAccount.setAmount(VALID_AMOUNT);
        when(mockAccountRepository.findOne(VALID_ID))
                .thenReturn(stubAccount);
        // act
        sutAccountService.getAmount(VALID_ID);
        sutAccountService.getAmount(VALID_ID);
        // assert
        assertEquals(2, (long) statisticHandler.getMethodInvocationCount(GET_AMOUNT_METHOD_NAME));
    }

    @Test
    public void getMethodInvocationCountShouldReturnTwoForTwoInvocationsAddAmount() {
        // arrange
        Account stubAccount = new Account();
        Account expectedAccount = new Account();
        expectedAccount.setId(VALID_ID);
        expectedAccount.setAmount(VALID_AMOUNT);
        when(mockAccountRepository.newAccountInstance()).thenReturn(stubAccount);
        // act
        sutAccountService.addAmount(VALID_ID, VALID_AMOUNT);
        sutAccountService.addAmount(VALID_ID, VALID_AMOUNT);
        // assert
        assertEquals(2, (long) statisticHandler.getMethodInvocationCount(ADD_AMOUNT_METHOD_NAME));
    }

    @Test
    public void getMethodInvocationCountShouldReturnOneForOnceInvocationsGetAndSetAmmount() {
        // arrange
        Account stubAccount = new Account();
        stubAccount.setId(VALID_ID);
        stubAccount.setAmount(VALID_AMOUNT);
        when(mockAccountRepository.findOne(VALID_ID))
                .thenReturn(stubAccount);
        // act
        sutAccountService.addAmount(VALID_ID, VALID_INC_AMOUNT);
        sutAccountService.getAmount(VALID_ID);

        // assert
        assertEquals(1, (long) statisticHandler.getMethodInvocationCount(GET_AMOUNT_METHOD_NAME));
        assertEquals(1, (long) statisticHandler.getMethodInvocationCount(ADD_AMOUNT_METHOD_NAME));
    }

    @Test
    public void resetMethodInvocationCounterShouldResetCounter() {
        // arrange
        Account stubAccount = new Account();
        Account expectedAccount = new Account();
        expectedAccount.setId(VALID_ID);
        expectedAccount.setAmount(VALID_AMOUNT);
        when(mockAccountRepository.newAccountInstance()).thenReturn(stubAccount);
        // act
        sutAccountService.addAmount(VALID_ID, VALID_AMOUNT);
        sutAccountService.addAmount(VALID_ID, VALID_AMOUNT);
        statisticHandler.resetMethodInvocationCounter(ADD_AMOUNT_METHOD_NAME);
        // assert
        assertEquals(0, (long) statisticHandler.getMethodInvocationCount(ADD_AMOUNT_METHOD_NAME));
    }

    @Test
    public void resetShouldResetAllStatistic() {
        // arrange
        Account stubAccount = new Account();
        stubAccount.setId(VALID_ID);
        stubAccount.setAmount(VALID_AMOUNT);
        when(mockAccountRepository.findOne(VALID_ID))
                .thenReturn(stubAccount);
        // act
        sutAccountService.addAmount(VALID_ID, VALID_INC_AMOUNT);
        sutAccountService.getAmount(VALID_ID);
        statisticHandler.reset();

        // assert
        assertEquals(0, (long) statisticHandler.getMethodInvocationCount(GET_AMOUNT_METHOD_NAME));
        assertEquals(0, (long) statisticHandler.getMethodInvocationCount(ADD_AMOUNT_METHOD_NAME));
    }
}
