package com.mfcalculator.repository;

import com.mfcalculator.model.CryptoWalletUpload;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CryptoWalletUploadRepository extends JpaRepository<CryptoWalletUpload, Long> {
  Optional<CryptoWalletUpload> findTopByUserIdOrderByUploadedAtDesc(Long userId);
}
