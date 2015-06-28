package com.nestos.accountservice.service;

/**
 * Account service.
 * @author Roman Osipov
 */
public interface AccountService {

    /**
     * Retrieves current balance or zero if addAmount() method was not called before for specified.
     *
     * @param id balance identifier.
     * @return current balance or zero if addAmount() method was not called before for specified.
     */
    Long getAmount(Integer id);

    /**
     * Increases balance or set if addAmount() method was called first time.
     *     
     * @param id balance identifier.
     * @param value positive or negative value, which must be added to current balance.
     */
    void addAmount(Integer id, Long value);
}
