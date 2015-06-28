package com.nestos.accountservice.javaconfig;

import com.nestos.accountservice.aspect.StatisticHandler;
import com.nestos.accountservice.kafka.KafkaClient;
import com.nestos.accountservice.processor.PartitionProcessorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

/**
 * Spring java config class.
 *
 * @author Roman Osipov
 */
@Configuration
@EnableCaching
@EnableAspectJAutoProxy
@EnableMBeanExport
@ImportResource({"classpath:datasource-tx-jpa.xml", "classpath:rmi.xml"})
@ComponentScan("com.nestos.accountservice.jpa")
@PropertySource("classpath:kafka.properties")
public class ServiceConfig {
    //-------------------Logger---------------------------------------------------
    //-------------------Constants------------------------------------------------
    //-------------------Fields---------------------------------------------------
    
    @Autowired
    Environment env;
    
    //-------------------Constructors---------------------------------------------
    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    //-------------------Beans----------------------------------------------------
    @Bean
    public EhCacheCacheManager cacheManager() {
        return new EhCacheCacheManager(ehcache().getObject());
    }

    @Bean
    public EhCacheManagerFactoryBean ehcache() {
        EhCacheManagerFactoryBean ecf = new EhCacheManagerFactoryBean();
        ecf.setConfigLocation(new ClassPathResource("service-ehcache.xml"));
        ecf.setCacheManagerName("accountServiceCacheManager");
        return ecf;
    }

    // Aspect for collect statistics.
    @Bean
    public StatisticHandler statisticHandler() {
        return new StatisticHandler();
    }
    
    @Bean
    @Profile("prod")
    public KafkaClient kafkaClient() {
        String zooHost = env.getProperty("zookeeper.host");
        int zooPort = env.getProperty("zookeeper.port", Integer.class);
        String kafkaHost = env.getProperty("kafka.host");
        int kafkaPort = env.getProperty("kafka.port", Integer.class);
        return new KafkaClient(zooHost, zooPort, kafkaHost, kafkaPort, "accountTopic");
    }
    
    @Bean
    @Profile("test")
    public KafkaClient testKafkaClient() {
        String zooHost = env.getProperty("zookeeper.host");
        int zooPort = env.getProperty("zookeeper.port", Integer.class);
        String kafkaHost = env.getProperty("kafka.host");
        int kafkaPort = env.getProperty("kafka.port", Integer.class);
        return new KafkaClient(zooHost, zooPort, kafkaHost, kafkaPort, "testAccountTopic");
    }
    
    @Bean
    public PartitionProcessorPool partitionProcessorPool() {
       return new PartitionProcessorPool();
    }
   
}
