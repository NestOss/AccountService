package com.nestos.accountservice.repository;

import com.nestos.accountservice.domain.Account;
import java.util.List;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for Account. Uses Spring Data.
 * @author Roman Osipov.
 */
public interface AccountRepository extends CrudRepository<Account, Integer>,
        AccountRepositoryCustom {
    
    List<Account> findByIdIn(Set<Integer> ids);

// Don't do this. Deadlocks possible. https://bugs.mysql.com/bug.php?id=52020
//    @Modifying
//    @Query(value = "INSERT INTO account (id, amount) "
//            + "VALUES (?0,?1)"
//            + "ON DUPLICATE KEY UPDATE "
//            + "amount = amount + ?1", nativeQuery = true)
//    void addAmount(Integer id, Long value);
}
