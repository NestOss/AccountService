package com.nestos.accountservice.javaconfig;

import com.nestos.accountservice.aspect.StatisticHandler;
import com.nestos.accountservice.jpa.AccountServiceImpl;
import com.nestos.accountservice.kafka.KafkaClient;
import com.nestos.accountservice.repository.AccountRepository;
import com.nestos.accountservice.service.AccountService;
import static org.mockito.Mockito.mock;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;

/**
 * Spring Java configuration file.
 *
 * @author Roman Osipov
 */
@Configuration
@EnableCaching
@EnableAspectJAutoProxy
public class UnitTestConfig {
    //-------------------Logger---------------------------------------------------
    //-------------------Constants------------------------------------------------
    //-------------------Fields---------------------------------------------------
    //-------------------Constructors---------------------------------------------
    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    //-------------------Beans----------------------------------------------------
    @Bean
    public KafkaClient kafkaClient() {
        return mock(KafkaClient.class);
    }
    
    @Bean
    public AccountRepository accountRepository() {
        return mock(AccountRepository.class);
    }

    @Bean
    public AccountService accountService() {
        return new AccountServiceImpl();
    }

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

    @Bean
    public StatisticHandler statisticHandler() {
        return new StatisticHandler();
    }
}
