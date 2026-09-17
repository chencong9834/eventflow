package com.example.eventflow.reporting.web;

import java.time.Instant;

public class ReportResponse {

  private Instant from;
  private Instant to;
  private int paidOrderCount;
  private int soldQty;
  private long salesFen;
  private long refundFen;
  private int usedTicketCount;

  public Instant getFrom() {
    return from;
  }

  public void setFrom(Instant from) {
    this.from = from;
  }

  public Instant getTo() {
    return to;
  }

  public void setTo(Instant to) {
    this.to = to;
  }

  public int getPaidOrderCount() {
    return paidOrderCount;
  }

  public void setPaidOrderCount(int paidOrderCount) {
    this.paidOrderCount = paidOrderCount;
  }

  public int getSoldQty() {
    return soldQty;
  }

  public void setSoldQty(int soldQty) {
    this.soldQty = soldQty;
  }

  public long getSalesFen() {
    return salesFen;
  }

  public void setSalesFen(long salesFen) {
    this.salesFen = salesFen;
  }

  public long getRefundFen() {
    return refundFen;
  }

  public void setRefundFen(long refundFen) {
    this.refundFen = refundFen;
  }

  public int getUsedTicketCount() {
    return usedTicketCount;
  }

  public void setUsedTicketCount(int usedTicketCount) {
    this.usedTicketCount = usedTicketCount;
  }
}
