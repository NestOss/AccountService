package com.nestos.accountservice.repository;

import com.nestos.accountservice.domain.Partition;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for Partition. Uses Spring Data.
 * @author Roman Osipov.
 */
public interface PartitionRepository  extends CrudRepository<Partition, Integer>{
  //-------------------Logger---------------------------------------------------

  //-------------------Constants------------------------------------------------
  //-------------------Fields---------------------------------------------------
  //-------------------Constructors---------------------------------------------
  //-------------------Getters and setters--------------------------------------
  //-------------------Methods--------------------------------------------------
}
