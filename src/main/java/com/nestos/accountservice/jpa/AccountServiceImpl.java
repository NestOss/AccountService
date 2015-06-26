package com.nestos.accountservice.jpa;

import com.nestos.accountservice.domain.Account;
import com.nestos.accountservice.domain.AddOperation;
import com.nestos.accountservice.kafka.KafkaClient;
import com.nestos.accountservice.repository.AccountRepository;
import com.nestos.accountservice.service.AccountService;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.apache.commons.lang3.Validate.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;

/**
 * Account service.
 *
 * @author Roman Osipov
 */
@Service("accountService")
public class AccountServiceImpl implements AccountService {

    //-------------------Logger---------------------------------------------------
    //-------------------Constants------------------------------------------------
    public static final String ACCOUNT_CACHE_NAME = "accountServiceCache";
    public static final String ID_NPE_MESSAGE = "id can't be null.";
    public static final String VALUE_NPE_MESSAGE = "value can't be null.";
    public static final String ID_IAE_MESSAGE = "id can't be negative.";

    //-------------------Fields---------------------------------------------------
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private KafkaClient kafkaClient;

    @Autowired
    private CacheManager cacheManager;
    
    private Cache cache;

    //-------------------Constructors---------------------------------------------
    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    /**
     * Retrieves current balance or zero if addAmount() method was not called before for specified.
     *
     * @param id balance identifier.
     * @return current balance or zero if addAmount() method was not called before for specified.
     */
    @Override
    @Transactional(readOnly = true)
    // @Cacheable(ACCOUNT_CACHE_NAME)
    public Long getAmount(Integer id) {
        notNull(id, ID_NPE_MESSAGE);
        inclusiveBetween(0, Integer.MAX_VALUE, id, ID_IAE_MESSAGE);
        Long amount;
        amount = cache.get(id, Long.class);
        if (amount != null) {
            return amount;
        }
        Account account = accountRepository.findOne(id);
        amount = (account == null) ? 0 : account.getAmount();
        cache.putIfAbsent(id, amount);
        return amount;
    }

    /**
     * Increases balance or set if addAmount() method was called first time.
     *
     * @param id balance identifier.
     * @param value positive or negative value, which must be added to current balance.
     *
     */
    @Override
    public void addAmount(Integer id, Long value) {
        notNull(id, ID_NPE_MESSAGE);
        notNull(value, VALUE_NPE_MESSAGE);
        inclusiveBetween(0, Integer.MAX_VALUE, id, ID_IAE_MESSAGE);
        AddOperation addOperation = new AddOperation(id, value);
        kafkaClient.write(addOperation);
    }
    
    @PostConstruct
    private void postConstruct() {
        cache = cacheManager.getCache(ACCOUNT_CACHE_NAME);
    }
}
