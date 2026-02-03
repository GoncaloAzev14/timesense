package com.datacentric.timesense.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>, 
        JpaSpecificationExecutor<Client> {

    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN true ELSE false END " + 
           "FROM Client a " +
           "WHERE a.name = ?1 and a.deleted = false ")
    boolean existsByName(String name);

    @Query("SELECT c FROM Client c WHERE c.name = ?1 ")
    Optional<Client> findByName(String name);

    @Modifying
    @Transactional
    @Query("UPDATE Client c " + 
            "SET c.deleted = true " + 
            "WHERE c.id = ?1 ")
    void deleteClientById(Long typeId);
    
}
