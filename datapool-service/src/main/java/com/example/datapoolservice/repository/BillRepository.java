package com.example.datapoolservice.repository;

import com.example.datapoolservice.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByCustomerId(Long customerId);
    List<Bill> findByStatus(String status);
    List<Bill> findByCustomerIdAndStatus(Long customerId, String status);
}
