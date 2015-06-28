package com.nestos.accountservice.service;

import com.nestos.accountservice.domain.Account;
import com.nestos.accountservice.domain.AddOperation;
import java.util.List;
import java.util.Map;

/**
 * Interface for account service batch processing.
 *
 * @author Roman Osipov
 */
public interface BatchAccountService {

    /**
     * Account service batch operation. 
     *
     * @param addOperations list of add operations.
     * @param partitionId kafka working partition.
     * @param lastReadedOffset  partition last read offset.
     * @return map of updated accounts. Key - account id, value - updated account.
     */
    public Map<Integer, Account> addAmounts(List<AddOperation> addOperations, int partitionId,
            long lastReadedOffset);
}
