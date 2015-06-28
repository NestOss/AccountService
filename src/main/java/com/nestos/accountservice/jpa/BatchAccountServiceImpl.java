package com.nestos.accountservice.jpa;

import com.nestos.accountservice.domain.Account;
import com.nestos.accountservice.domain.AddOperation;
import com.nestos.accountservice.domain.Partition;
import com.nestos.accountservice.repository.AccountRepository;
import com.nestos.accountservice.repository.PartitionRepository;
import com.nestos.accountservice.service.BatchAccountService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of account service batch interface.
 *
 * @author Roman Osipov
 */
@Service("batchAccountService")
public class BatchAccountServiceImpl implements BatchAccountService {
    //-------------------Logger---------------------------------------------------

    //-------------------Constants------------------------------------------------
    //-------------------Fields---------------------------------------------------
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PartitionRepository partitionRepository;

    //-------------------Constructors---------------------------------------------
    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    /**
     * Executes batch of add operations.
     *
     * @param addOperations list of add operations.
     * @param partitionId kafka working partition.
     * @param lastReadedOffset partition last read offset.
     * @return map of updated accounts. Key - account id, value - account.
     */
    @Override
    @Transactional
    public Map<Integer, Account> addAmounts(List<AddOperation> addOperations, int partitionId,
            long lastReadedOffset) {
        // result map
        Map<Integer, Account> accountMap = new HashMap<>();
        if (addOperations == null) {
            throw new NullPointerException("addOperations can't be null");
        }
        if (addOperations.isEmpty()) {
            return accountMap;
        }
        // ids for request existing accounts. Not use lambda because project source version 1.7.
        Set<Integer> ids = new HashSet<>();
        for (AddOperation addOperation : addOperations) {
            ids.add(addOperation.getId());
        }
        List<Account> accounts = accountRepository.findByIdIn(ids);

        for (Account account : accounts) {
            accountMap.put(account.getId(), account);
        }
        for (AddOperation addOperation : addOperations) {
            Account findAccount = accountMap.get(addOperation.getId());
            if (findAccount == null) {
                findAccount = accountRepository.newAccountInstance();
                findAccount.setId(addOperation.getId());
                findAccount.setAmount(addOperation.getValue());
                accountMap.put(addOperation.getId(), findAccount);
            } else {
                findAccount.setAmount(findAccount.getAmount() + addOperation.getValue());
            }
        }
        accountRepository.save(accountMap.values());
        // update read offset in kafka partition
        Partition partition = new Partition();
        partition.setId(partitionId);
        partition.setOffset(lastReadedOffset);
        partitionRepository.save(partition);
        return accountMap;
    }
}
