package com.example.eventflow.reporting;

public class ReportSummary {

  private int paidOrderCount;
  private int soldQty;
  private long salesFen;
  private long refundFen;
  private int usedTicketCount;

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
