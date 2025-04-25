package com.webanhang.team_project.repository;

import com.webanhang.team_project.model.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, Long> {
    void deleteById(Long id);
}
