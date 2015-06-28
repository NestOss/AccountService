package com.nestos.accountservice.processor;

import com.nestos.accountservice.domain.Account;
import com.nestos.accountservice.domain.AddOperation;
import com.nestos.accountservice.domain.Partition;
import com.nestos.accountservice.jpa.AccountServiceImpl;
import com.nestos.accountservice.kafka.KafkaClient;
import com.nestos.accountservice.repository.PartitionRepository;
import com.nestos.accountservice.service.BatchAccountService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.apache.commons.lang3.Validate.inclusiveBetween;
import static org.apache.commons.lang3.Validate.notNull;
import org.apache.log4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Partition processor. Periodically in cycle pull partition for new records and apply pulled data
 * to account repository. Pull period defined by CYCLE_PAUSE constant. If pull result list is empty,
 * then set isIdle value to true. Process of each partition confined in separate thread.
 *
 * @author Roman Osipov
 */
public class PartitionProcessor implements Runnable {

    //-------------------Logger---------------------------------------------------
    private final static Logger logger = Logger.getLogger(PartitionProcessor.class.getName());

    //-------------------Constants------------------------------------------------
    // Pause between cycle iterations (ms). TO DO  -  must be set in configuration file. 
    private static final int CYCLE_PAUSE = 100;
    public static final String KAFKA_CLIENT_NPE_MESSAGE = "kafkaClient can't be null.";
    public static final String PARTITION_REPOSITORY_NPE_MESSAGE
            = "partitionRepository can't be null.";
    public static final String CACHE_MANAGER_NPE_MESSAGE = "cacheManager can't be null.";
    public static final String BATCH_ACCOUNT_SERVICE_NPE_MESSAGE
            = "batchAccountService can't be null.";
    public static final String IS_IDLE_NPE_MESSAGE = "isIdle can't be null.";
    public static final String CACHE_NPE_MESSAGE = "cant't find cache "
            + AccountServiceImpl.ACCOUNT_CACHE_NAME;

    //-------------------Fields---------------------------------------------------
    private final int partitionId;
    private final KafkaClient kafkaClient;
    private final PartitionRepository partitionRepository;
    private final BatchAccountService batchAccountService;
    private final Cache cache;
    private final AtomicBoolean isIdle;

    //-------------------Constructors---------------------------------------------
    /**
     * Constructs a new PartitionProcessor.
     *
     * @param partitionId partition id.
     * @param kafkaClient client to Kafka server.
     * @param partitionRepository partition CRUD repository.
     * @param batchAccountService account batch service.
     * @param cacheManager cache manager.
     * @param isIdle value holder for idle flag.
     */
    public PartitionProcessor(int partitionId, KafkaClient kafkaClient,
            PartitionRepository partitionRepository, BatchAccountService batchAccountService,
            CacheManager cacheManager, AtomicBoolean isIdle) {
        // validate arguments
        inclusiveBetween(0, KafkaClient.PARTITIONS_NUM - 1, partitionId);
        notNull(kafkaClient, "kafkaClient can't be null");
        notNull(partitionRepository, "partitionRepository can't be null");
        notNull(cacheManager, "cacheManager can't be null");
        notNull(batchAccountService, "batchAccountService can't be null");
        notNull(isIdle, "isIdle can't be null");
        // apply arguments
        this.partitionId = partitionId;
        this.kafkaClient = kafkaClient;
        this.partitionRepository = partitionRepository;
        this.batchAccountService = batchAccountService;
        this.isIdle = isIdle;
        this.cache = cacheManager.getCache(AccountServiceImpl.ACCOUNT_CACHE_NAME);
        notNull(cache, "cant't find cache " + AccountServiceImpl.ACCOUNT_CACHE_NAME);
    }

    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    private void pullFromPartitionAndSaveToAccountRepository() {
        Partition partition = partitionRepository.findOne(partitionId);
        long offset = (partition == null) ? 0 : partition.getOffset();
        List<AddOperation> addOperations = kafkaClient.read(partitionId, offset);
        if (addOperations.isEmpty()) {
            isIdle.set(true);
            return;
        }
        isIdle.set(false);
        Map<Integer, Account> accountMap = batchAccountService.addAmounts(
                addOperations, partitionId, offset + addOperations.size());
        // evict cache. I see problem this multithreading. In future solve it this read-write lock.
        for (Account account : accountMap.values()) {
            cache.evict(account.getId());
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(CYCLE_PAUSE);
                try {
                    pullFromPartitionAndSaveToAccountRepository();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    isIdle.set(true); // not doing helpful work
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
