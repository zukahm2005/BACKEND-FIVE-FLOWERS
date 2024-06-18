package com.example.backendfiveflowers.service;

import com.example.backendfiveflowers.entity.Payments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PaymentsService {
    List<Payments> findAll();

    Page<Payments> findAll(Pageable pageable); // Thêm phương thức này
    Optional<Payments> findById(Long id);
    Payments save(Payments payments);
    void deleteById(Long id);
}
