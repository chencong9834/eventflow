package com.example.eventflow.activity.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpsertTierRequest {

  @NotBlank
  @Size(max = 64)
  private String name;

  @NotNull
  @Min(0)
  private Long unitPriceFen;

  @NotNull
  @Min(1)
  private Integer perUserLimit;

  @NotNull
  @Min(1)
  private Integer totalQty;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getUnitPriceFen() {
    return unitPriceFen;
  }

  public void setUnitPriceFen(Long unitPriceFen) {
    this.unitPriceFen = unitPriceFen;
  }

  public Integer getPerUserLimit() {
    return perUserLimit;
  }

  public void setPerUserLimit(Integer perUserLimit) {
    this.perUserLimit = perUserLimit;
  }

  public Integer getTotalQty() {
    return totalQty;
  }

  public void setTotalQty(Integer totalQty) {
    this.totalQty = totalQty;
  }
}
