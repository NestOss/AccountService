package com.nestos.accountservice.repository;

import com.nestos.accountservice.domain.Account;

/**
 * This interface intend for add functionality to Account repository. 
 * @author Roman Osipov.
 */
public interface AccountRepositoryCustom {
    /**
     * Factory method.
     * @return instance of Account class.
     */
    Account newAccountInstance();
}
