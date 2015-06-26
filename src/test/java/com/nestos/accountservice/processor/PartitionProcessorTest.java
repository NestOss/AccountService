package com.nestos.accountservice.processor;

import com.nestos.accountservice.domain.Account;
import com.nestos.accountservice.domain.AddOperation;
import com.nestos.accountservice.domain.Partition;
import com.nestos.accountservice.jpa.AccountServiceImpl;
import com.nestos.accountservice.kafka.KafkaClient;
import com.nestos.accountservice.repository.PartitionRepository;
import com.nestos.accountservice.service.BatchAccountService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cache.CacheManager;
import static org.mockito.Mockito.*;
import org.springframework.cache.Cache;

/**
 * PartitionProcessor test.
 *
 * @author Roman Osipov
 */
@RunWith(MockitoJUnitRunner.class)
public class PartitionProcessorTest {

    //-------------------Constants------------------------------------------------
    private static final Integer VALID_PARTITION_ID = 3;
    private static final Long VALID_PARTITION_OFFSET = 42L;
    private static final int VALID_ACCOUNT_ID1 = 47;
    private static final int VALID_ACCOUNT_ID2 = 53;
    private static final long VALID_ACCOUNT_VALUE1 = 67;
    private static final long VALID_ACCOUNT_VALUE2 = 73;
    private static final List<AddOperation> EMPTY_ADD_OPERATIONS_LIST
            = new ArrayList<>();

    //--------------------Mocks------------------------------------------------   
    @Mock
    private KafkaClient mockKafkaClient;

    @Mock
    private PartitionRepository mockPartitionRepository;

    @Mock
    private BatchAccountService mockBatchAccountService;

    @Mock
    private CacheManager mockCacheManager;

    @Mock
    private Cache mockCache;

    //--------------------Fields-----------------------------------------------
    private Partition stubPartition;

    List<AddOperation> stubAddOperations;

    Map<Integer, Account> stubAccountMap;

    AtomicBoolean stubIsIdle;

    //-------------------Constructors------------------------------------------
    public PartitionProcessorTest() {
    }

    //-------------------Methods-----------------------------------------------
    @Before
    public void setUp() {

        stubPartition = new Partition();
        stubPartition.setId(VALID_PARTITION_ID);
        stubPartition.setOffset(VALID_PARTITION_OFFSET);

        stubAddOperations = new ArrayList<>();
        stubAddOperations.add(new AddOperation(VALID_ACCOUNT_ID1, VALID_ACCOUNT_VALUE1));
        stubAddOperations.add(new AddOperation(VALID_ACCOUNT_ID2, VALID_ACCOUNT_VALUE2));

        stubAccountMap = new HashMap<>();
        Account account = new Account();
        account.setId(VALID_ACCOUNT_ID1);
        account.setAmount(VALID_ACCOUNT_VALUE2);
        stubAccountMap.put(VALID_ACCOUNT_ID1, account);
        account = new Account();
        account.setId(VALID_ACCOUNT_ID2);
        account.setAmount(VALID_ACCOUNT_VALUE1);
        stubAccountMap.put(VALID_ACCOUNT_ID2, account);

        stubIsIdle = new AtomicBoolean();

        when(mockCacheManager.getCache(AccountServiceImpl.ACCOUNT_CACHE_NAME))
                .thenReturn(mockCache);
    }

    @Test
    public void pullFromPartitionAndSaveToAccountRepositoryShoudWork() {
        // arrange 
        when(mockPartitionRepository.findOne(VALID_PARTITION_ID)).thenReturn(stubPartition);
        when(mockKafkaClient.read(VALID_PARTITION_ID, VALID_PARTITION_OFFSET))
                .thenReturn(stubAddOperations);
        when(mockBatchAccountService.addAmounts(stubAddOperations, VALID_PARTITION_ID,
                VALID_PARTITION_OFFSET + stubAddOperations.size())).thenReturn(stubAccountMap);
        PartitionProcessor sutPartitionProcessor = new PartitionProcessor(VALID_PARTITION_ID,
                mockKafkaClient, mockPartitionRepository, mockBatchAccountService, mockCacheManager,
                stubIsIdle);
        // act
        sutPartitionProcessor.pullFromPartitionAndSaveToAccountRepository();
        // assert
        verify(mockPartitionRepository, only()).findOne(VALID_PARTITION_ID);
        verify(mockKafkaClient, only()).read(VALID_PARTITION_ID, VALID_PARTITION_OFFSET);
        verify(mockBatchAccountService, only()).addAmounts(stubAddOperations, VALID_PARTITION_ID,
                VALID_PARTITION_OFFSET + stubAddOperations.size());
        for (Account account : stubAccountMap.values()) {
            verify(mockCache).put(account.getId(), account.getAmount());
        }
        verifyNoMoreInteractions(mockCache);
        assertFalse("Process do helpful work. It,s not idle", stubIsIdle.get());
    }

    @Test
    public void whenPartitionNotFoundInRepositoryThenKafkaClientMustReadThisZeroOffset() {
        // arrange
        when(mockKafkaClient.read(VALID_PARTITION_ID, 0))
                .thenReturn(stubAddOperations);
        when(mockBatchAccountService.addAmounts(stubAddOperations, VALID_PARTITION_ID,
                VALID_PARTITION_OFFSET + stubAddOperations.size())).thenReturn(stubAccountMap);

        PartitionProcessor sutPartitionProcessor = new PartitionProcessor(VALID_PARTITION_ID,
                mockKafkaClient, mockPartitionRepository, mockBatchAccountService, mockCacheManager,
                stubIsIdle);
        // act
        sutPartitionProcessor.pullFromPartitionAndSaveToAccountRepository();
        // assert 
        verify(mockKafkaClient).read(VALID_PARTITION_ID, 0);
    }

    @Test
    public void whenKafkaClientReturnEmptyListThenMethodMustReturns() {
        // arrange 
        when(mockPartitionRepository.findOne(VALID_PARTITION_ID)).thenReturn(stubPartition);
        when(mockKafkaClient.read(VALID_PARTITION_ID, VALID_PARTITION_OFFSET))
                .thenReturn(EMPTY_ADD_OPERATIONS_LIST);
        PartitionProcessor sutPartitionProcessor = new PartitionProcessor(VALID_PARTITION_ID,
                mockKafkaClient, mockPartitionRepository, mockBatchAccountService, mockCacheManager,
                stubIsIdle);
        // act
        sutPartitionProcessor.pullFromPartitionAndSaveToAccountRepository();
        // assert
        verifyNoMoreInteractions(mockBatchAccountService);
        verifyNoMoreInteractions(mockCache);
        assertTrue("Process not do helpful work. It,s idle", stubIsIdle.get());
    }

}
