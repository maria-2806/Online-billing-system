package com.example.datapoolservice.controller;

import com.example.datapoolservice.model.Bill;
import com.example.datapoolservice.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @PostMapping
    public ResponseEntity<?> createBill(@RequestParam Long customerId, @RequestBody Bill bill) {
        try {
            Bill created = billService.createBill(customerId, bill);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bill> getBillById(@PathVariable Long id) {
        return billService.getBillById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Bill>> getBills(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(billService.getBills(customerId, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bill> updateBill(@PathVariable Long id, @RequestBody Bill billDetails) {
        return billService.updateBill(id, billDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBill(@PathVariable Long id) {
        if (billService.deleteBill(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
