package com.nestos.accountservice.jpa;

import com.nestos.accountservice.aspect.StatisticHandler;
import com.nestos.accountservice.domain.Partition;
import com.nestos.accountservice.javaconfig.ServiceConfig;
import com.nestos.accountservice.kafka.KafkaClient;
import com.nestos.accountservice.processor.PartitionProcessorPool;
import com.nestos.accountservice.repository.PartitionRepository;
import com.nestos.accountservice.service.AccountService;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;


/**
 * Integration tests for Account Service.
 * Before test run start Kafka server. 
 *
 * @author Roman Osipov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ServiceConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class AccountServiceIT {

    private static final int NUMBER_THREADS = 100;
    private static final int REPEAT_COUNT = 1000;
    private static final double GET_REQUEST_RATIO = 0.2;
    private static final int AMOUNT_MIN_VALUE = -15000;
    private static final int AMOUNT_MAX_VALUE = 15000;
    private static final int ID_MIN_VALUE = -10;
    private static final int ID_MAX_VALUE = 1000;

    @Autowired
    @Qualifier("remoteAccountService")
    private AccountService remoteAccountService;
    
    @Autowired
    private PartitionRepository partitionRepository;
  
    @Autowired
    private StatisticHandler statisticHandler;
    
    @Autowired
    private KafkaClient kafkaClient;
    
    @Autowired
    private PartitionProcessorPool partitionProcessorPool;
    
    @Autowired
    private CacheManager cacheManager;

    //--------------------Nested classes----------------------------------------
    
    private class AddAmountTask implements Runnable {

        private final Integer id;
        private final Long value;
        private final ConcurrentHashMap<Integer,AtomicLong> total;

        public AddAmountTask(Integer id, Long value, ConcurrentHashMap<Integer,AtomicLong> total) {
            this.id = id;
            this.value = value;
            this.total = total;
        }

        @Override
        public void run() {
            remoteAccountService.addAmount(id, value);
            total.putIfAbsent(id, new AtomicLong());
            AtomicLong amount = total.get(id);
            amount.addAndGet(value);
        }
    }

    private class GetAmountTask implements Callable<Long> {

        private final Integer id;

        public GetAmountTask(Integer id) {
            this.id = id;
        }

        @Override
        public Long call() throws Exception {
            return remoteAccountService.getAmount(id);
        }
    }

    //--------------------Constructors-----------------------------------------
    
    public AccountServiceIT() {
    }

    //--------------------Methods----------------------------------------------
    
    @Before
    public void setUp() {
        // disable procesing log for test run
        Logger x = Logger.getLogger("com.nestos.processing");
        x.setLevel(Level.OFF);
        LoggerRepository repository = x.getLoggerRepository();
        repository.setThreshold(Level.OFF);
        // kafka setup
        for (int i = 0; i < KafkaClient.PARTITIONS_NUM; i++) {
            long offset = kafkaClient.getLastOffset(i);
            Partition partition = new Partition();
            partition.setId(i);
            partition.setOffset(offset);
            partitionRepository.save(partition);
        }
        partitionProcessorPool.start();
    }
 
    @Test
    public void accountServiceMustWorkInMultithreadEnvironmentWithoutClearCache()
            throws InterruptedException {
        // arrange
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_THREADS);
        ConcurrentHashMap<Integer,AtomicLong> total = new ConcurrentHashMap<>();
        long getAmountCount = 0;
        long addAmountCount = 0;
        Random random = new Random();
        
        // act
        for (int i = 0; i < REPEAT_COUNT; i++) {
            int id = random.nextInt((ID_MAX_VALUE - ID_MIN_VALUE) + 1) + ID_MIN_VALUE;
            long amount = random.nextInt((AMOUNT_MAX_VALUE - AMOUNT_MIN_VALUE) + 1) 
                    + AMOUNT_MIN_VALUE;
            if (Math.random() < GET_REQUEST_RATIO) {
                executor.submit(new GetAmountTask(id));
                getAmountCount++;
            } else {
                executor.submit(new AddAmountTask(id, amount, total));
                addAmountCount++;
            }
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
        partitionProcessorPool.awaitIdle(); // block while partitionProcessorPool is work
        // assert
        for (Map.Entry<Integer,AtomicLong> entry : total.entrySet()) {
            Integer id = entry.getKey();
            AtomicLong amount = entry.getValue();
            long realAmount = remoteAccountService.getAmount(id);
            getAmountCount++; // invoke remoteAccountService.getAmount in previous line
            assertEquals("Total ammount for id=" + id + " mismatch:",
                amount.get(), realAmount);
        }
        
        // statistic handler must be tested in separate test method
        // but this approach save test execution time
        assertEquals("Invalid statistic for getAmount method:",
                getAmountCount, (long) statisticHandler.getMethodInvocationCount("getAmount"));
        assertEquals("Invalid statistic for addAmount method:",
                addAmountCount, (long) statisticHandler.getMethodInvocationCount("addAmount"));
    }
    
    @Test
    public void accountServiceMustWorkInMultithreadEnvironmentWithClearCache()
            throws InterruptedException {
        // arrange
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_THREADS);
        ConcurrentHashMap<Integer,AtomicLong> total = new ConcurrentHashMap<>();
        Random random = new Random();
        
        // act
        for (int i = 0; i < REPEAT_COUNT; i++) {
            int id = random.nextInt((ID_MAX_VALUE - ID_MIN_VALUE) + 1) + ID_MIN_VALUE;
            long amount = random.nextInt((AMOUNT_MAX_VALUE - AMOUNT_MIN_VALUE) + 1) 
                    + AMOUNT_MIN_VALUE;
            if (Math.random() < GET_REQUEST_RATIO) {
                executor.submit(new GetAmountTask(id));
            } else {
                executor.submit(new AddAmountTask(id, amount, total));
            }
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
        partitionProcessorPool.awaitIdle(); // block while partitionProcessorPool is work
        cacheManager.getCache(AccountServiceImpl.ACCOUNT_CACHE_NAME).clear();
        // assert
        for (Map.Entry<Integer,AtomicLong> entry : total.entrySet()) {
            Integer id = entry.getKey();
            AtomicLong amount = entry.getValue();
            long realAmount = remoteAccountService.getAmount(id);
            assertEquals("Total ammount for id=" + id + " mismatch:",
                amount.get(), realAmount);
        }
    }
    
}
