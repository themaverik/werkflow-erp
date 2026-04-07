package com.werkflow.business.inventory.repository;

import com.werkflow.business.inventory.entity.AssetRequest;
import com.werkflow.business.inventory.entity.AssetRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRequestRepository extends JpaRepository<AssetRequest, Long> {
    Optional<AssetRequest> findByProcessInstanceId(String processInstanceId);
    Page<AssetRequest> findByRequesterUserId(String requesterUserId, Pageable pageable);
    Page<AssetRequest> findByDepartmentCode(String departmentCode, Pageable pageable);
    Page<AssetRequest> findByStatus(AssetRequestStatus status, Pageable pageable);
}
