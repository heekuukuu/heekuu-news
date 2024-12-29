package heekuu.table.store.repository;

import heekuu.table.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface StoreRepository extends JpaRepository<Store, Long> {

}