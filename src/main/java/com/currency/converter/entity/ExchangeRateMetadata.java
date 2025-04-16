package com.currency.converter.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rate_metadata")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateMetadata {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "last_successful_sync_time", nullable = false)
  private LocalDateTime lastSuccessfulSyncTime;

  @Column(name = "sync_status")
  private String syncStatus; // SUCCESS / FAILURE

  @Column(name = "source")
  private String source;

  @Column(name = "notes")
  private String notes;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

}
