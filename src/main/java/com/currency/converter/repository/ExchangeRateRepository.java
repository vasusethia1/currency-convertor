package com.currency.converter.repository;

import com.currency.converter.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
        @Query("SELECT er FROM ExchangeRate er WHERE er.baseCurrency = :baseCurrency " +
                        "AND er.targetCurrency = :targetCurrency " +
                        "AND er.date = :date " +
                        "ORDER BY er.timestamp DESC LIMIT 1")
        Optional<ExchangeRate> findByBaseCurrencyAndTargetCurrencyAndDate(
                        @Param("baseCurrency") String baseCurrency,
                        @Param("targetCurrency") String targetCurrency,
                        @Param("date") LocalDate date);

        @Query("SELECT er FROM ExchangeRate er WHERE er.baseCurrency = :baseCurrency " +
                        "AND er.targetCurrency = :targetCurrency " +
                        "AND er.date <= :date " +
                        "ORDER BY er.date DESC, er.timestamp DESC LIMIT 1")
        Optional<ExchangeRate> findLatestRateBeforeDate(
                        @Param("baseCurrency") String baseCurrency,
                        @Param("targetCurrency") String targetCurrency,
                        @Param("date") LocalDate date);
}