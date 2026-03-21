package com.whiskritorio.dao;

import com.whiskritorio.model.VendaItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaItemRepository extends JpaRepository<VendaItem, Long> {
}
