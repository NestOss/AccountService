package com.nestos.accountservice.repository;

import com.nestos.accountservice.domain.Account;

/**
 * This class intend for add functionality to Account repository. 
 * @author Roman Osipov.
 */
public class AccountRepositoryImpl implements AccountRepositoryCustom {

    //-------------------Logger---------------------------------------------------

    //-------------------Constants------------------------------------------------
    //-------------------Fields---------------------------------------------------
    //-------------------Constructors---------------------------------------------
    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    @Override
    public Account newAccountInstance() {
        return new Account();
    }    
}
