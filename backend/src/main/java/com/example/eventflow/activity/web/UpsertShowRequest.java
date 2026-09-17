package com.example.eventflow.activity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class UpsertShowRequest {

  @NotBlank
  @Size(max = 128)
  private String name;

  @NotNull private Instant startAt;
  @NotNull private Instant endAt;
  @NotNull private Instant saleStartAt;
  @NotNull private Instant saleEndAt;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public void setStartAt(Instant startAt) {
    this.startAt = startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public void setEndAt(Instant endAt) {
    this.endAt = endAt;
  }

  public Instant getSaleStartAt() {
    return saleStartAt;
  }

  public void setSaleStartAt(Instant saleStartAt) {
    this.saleStartAt = saleStartAt;
  }

  public Instant getSaleEndAt() {
    return saleEndAt;
  }

  public void setSaleEndAt(Instant saleEndAt) {
    this.saleEndAt = saleEndAt;
  }
}
