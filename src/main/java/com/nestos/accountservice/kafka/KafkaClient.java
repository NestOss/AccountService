package com.nestos.accountservice.kafka;

import com.nestos.accountservice.domain.AddOperation;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import kafka.admin.AdminUtils;
import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Logger;

/**
 * Kafka client.
 *
 * @author Roman Osipov
 */
public class KafkaClient {
    //-------------------Logger---------------------------------------------------

    private final static Logger logger = Logger.getLogger(KafkaClient.class.getName());

    //-------------------Constants------------------------------------------------
   
    private final static String CLIENT_NAME = "accountService";
    private final static int SESSION_TIMEOUT_MS = 10000;
    private final static int CONNECTION_TIMEOUT_MS = 10000;
    public final static int PARTITIONS_NUM = 5;
    private final static int REPLICATION_FACTOR = 1;
    private final static int CLIENT_BUFFER_SIZE = 64 * 1024;
    private final static int CLIENT_TIMEOUT = 100000;
    private final static int CLIENT_FETCH_SIZE = 100000;

    //-------------------Fields---------------------------------------------------
    private String topicName;
    private ZkClient zkClient;
    private KafkaProducer<String, byte[]> kafkaProducer;
    private SimpleConsumer kafkaConsumer;
    private String zooHost;
    private int zooPort;
    private String kafkaHost;
    private int kafkaPort;

    //-------------------Constructors---------------------------------------------
    public KafkaClient() {
    }

    public KafkaClient(String zooHost, int zooPort, String kafkaHost, int kafkaPort,
            String topicName ) {
        this.zooHost = zooHost;
        this.zooPort = zooPort;
        this.kafkaHost = kafkaHost;
        this.kafkaPort = kafkaPort;
        this.topicName = topicName;
    }

    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    private Properties createProducerConfigProperties() {
        Properties props = new Properties();
        String broker = kafkaHost + ":" + kafkaPort;
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("request.required.acks", "all");
        // use key hashCode for select partition
        props.setProperty("partitioner.class", "kafka.producer.DefaultPartitioner");
        return props;
    }

    /**
     * Submit add operation to kafka server .
     * @param addOperation operation to write.
     */
    public void write(AddOperation addOperation) {
        try {
            ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(
                    topicName, "" + addOperation.getId(), addOperation.toByteArray());
            Future<RecordMetadata> result = kafkaProducer.send(producerRecord);
            // make call synchronous for durability garanties
            result.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Can't write to topic.");
        }
    }

    /**
     * Read from specified partition and return list of add operations.
     *
     * @param partition partition number.
     * @param offset offset in partition.
     * @return list of add operations, consists up to maxItemsToRead elements.
     */
    public List<AddOperation> read(int partition, long offset) {
        List<AddOperation> addOperations = new ArrayList<>();
        FetchRequest req = new FetchRequestBuilder()
                .clientId(CLIENT_NAME)
                .addFetch(topicName, partition, offset, CLIENT_FETCH_SIZE)
                .build();
        FetchResponse fetchResponse = kafkaConsumer.fetch(req);
        if (fetchResponse.hasError()) {
            throw new RuntimeException("Error fetching kafka data.");
        }
        for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(topicName, partition)) {
            ByteBuffer byteBuffer = messageAndOffset.message().payload();
            byte[] bytes = new byte[byteBuffer.limit()];
            byteBuffer.get(bytes);
            addOperations.add(AddOperation.valueOf(bytes));
        }
        return addOperations;
    }

    @PostConstruct
    public void postConstruct() {
        String zooBroker = zooHost + ":" + zooPort;
        try {
            zkClient = new ZkClient(zooBroker, SESSION_TIMEOUT_MS, CONNECTION_TIMEOUT_MS,
                    ZKStringSerializer$.MODULE$);
            String topicPath = ZkUtils.getTopicPath(topicName);
            if (!zkClient.exists(topicPath)) {
                Properties props = new Properties();
                AdminUtils.createTopic(zkClient, topicName,
                        PARTITIONS_NUM, REPLICATION_FACTOR, props);
            }
            // producer creation
            kafkaProducer = new KafkaProducer<>(createProducerConfigProperties());
            // consumer creation - single leader without replication
            kafkaConsumer = new SimpleConsumer(kafkaHost, kafkaPort, CLIENT_TIMEOUT,
                    CLIENT_BUFFER_SIZE, CLIENT_NAME);

        } catch (org.I0Itec.zkclient.exception.ZkTimeoutException ex) {
            System.out.println("Could not establish connection to ZooKeeper server."
                    + "Check resource/kafka.properties settings.");
            logger.error(ex);
            throw ex;
        }
    }

    /**
     * Return last offset in partition.
     * @param partition partition number.
     * @return last offset in partition.
     */
    public long getLastOffset(int partition) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(topicName, partition);
        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>(); 
        requestInfo.put(topicAndPartition,
                new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.LatestTime(), 1));
        kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(
                requestInfo, kafka.api.OffsetRequest.CurrentVersion(), CLIENT_NAME);
        OffsetResponse response = kafkaConsumer.getOffsetsBefore(request);
        if (response.hasError()) {
            logger.error("Error fetching data Offset Data the Broker. Reason: " 
                    + response.errorCode(topicName, partition) );
            return 0;
        }
        long[] offsets = response.offsets(topicName, partition);
        return offsets[0];
    } 
    
    @PreDestroy
    public void preDestroy() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
        if (zkClient != null) {
            zkClient.close();
        }
    }

}
