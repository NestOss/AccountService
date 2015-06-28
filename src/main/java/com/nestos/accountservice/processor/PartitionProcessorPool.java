package com.nestos.accountservice.processor;

import com.nestos.accountservice.kafka.KafkaClient;
import com.nestos.accountservice.repository.PartitionRepository;
import com.nestos.accountservice.service.BatchAccountService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

/**
 * Process Kafka partitions. To start process call {@link #start() start} method. 
 * Starts one fetching thread per partition.
 *
 * @author Roman Osipov.
 */
public class PartitionProcessorPool {
  //-------------------Logger---------------------------------------------------

    //-------------------Constants------------------------------------------------
    //-------------------Fields---------------------------------------------------
    @Autowired
    private KafkaClient kafkaClient;

    @Autowired
    private PartitionRepository partitionRepository;

    @Autowired
    private BatchAccountService accountServiceBatch;

    @Autowired
    private CacheManager cacheManager;

    private ExecutorService executorService;

    private AtomicBoolean[] idleFlags;

    //-------------------Constructors---------------------------------------------
    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    /**
     * Blocks thread while at most one PartitionProcessor not idle (has messages to consume).
     *
     * @throws java.lang.InterruptedException
     */
    public void awaitIdle() throws InterruptedException {
        boolean idle;
        do {
            Thread.sleep(1000);
            idle = true;
            for (int i = 0; i < KafkaClient.PARTITIONS_NUM; i++) {
                if (!idleFlags[i].get()) {
                    idle = false;
                    break;
                }
            }
        } while (idle = false);
    }

    /**
     * Start execution of partition processors. 
     */
    public void start() {
        idleFlags = new AtomicBoolean[KafkaClient.PARTITIONS_NUM];
        for (int i = 0; i < KafkaClient.PARTITIONS_NUM; i++) {
            idleFlags[i] = new AtomicBoolean();
            PartitionProcessor partitionProcessor = new PartitionProcessor(i, kafkaClient,
                    partitionRepository, accountServiceBatch, cacheManager, idleFlags[i]);
            executorService.submit(partitionProcessor);
        }
    }

    @PostConstruct
    public void postConstruct() {
        executorService = Executors.newFixedThreadPool(KafkaClient.PARTITIONS_NUM);
    }

    @PreDestroy
    public void preDestroy() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(1, TimeUnit.DAYS);
    }

}
