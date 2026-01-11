package com.stability.coareport.repository;

import com.stability.coareport.entity.ChangeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Long> {
    List<ChangeHistory> findByEntityTypeAndEntityIdOrderByModifiedAtDesc(String entityType, Long entityId);

    Page<ChangeHistory> findByEntityTypeAndEntityIdOrderByModifiedAtDesc(String entityType, Long entityId, Pageable pageable);
}
