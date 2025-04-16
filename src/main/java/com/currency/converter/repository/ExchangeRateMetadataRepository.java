package com.currency.converter.repository;


import com.currency.converter.entity.ExchangeRateMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRateMetadataRepository extends JpaRepository<ExchangeRateMetadata, Long> {

  ExchangeRateMetadata findTopByOrderByLastSuccessfulSyncTimeDesc();
}
