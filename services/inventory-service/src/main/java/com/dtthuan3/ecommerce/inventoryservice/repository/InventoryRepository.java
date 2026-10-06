package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.repository.customrepo.InventoryRepositoryCustom;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository  extends JpaRepository<Inventory,String>, InventoryRepositoryCustom {

}