package com.example.eventflow.activity.web;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ActivitySummaryResponse {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;

  @JsonSerialize(using = ToStringSerializer.class)
  private Long tenantId;

  private String title;
  private String reviewStatus;
  private String saleStatus;
  private int showCount;
  private Instant updatedAt;
  private String organizerName;
  private String organizerCode;
  private String description;
  private String coverUrl;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public String getSaleStatus() {
    return saleStatus;
  }

  public void setSaleStatus(String saleStatus) {
    this.saleStatus = saleStatus;
  }

  public int getShowCount() {
    return showCount;
  }

  public void setShowCount(int showCount) {
    this.showCount = showCount;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getOrganizerName() {
    return organizerName;
  }

  public void setOrganizerName(String organizerName) {
    this.organizerName = organizerName;
  }

  public String getOrganizerCode() {
    return organizerCode;
  }

  public void setOrganizerCode(String organizerCode) {
    this.organizerCode = organizerCode;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCoverUrl() {
    return coverUrl;
  }

  public void setCoverUrl(String coverUrl) {
    this.coverUrl = coverUrl;
  }

  public static class ActivityDetailResponse extends ActivitySummaryResponse {
    private Instant createdAt;
    private List<ShowResponse> shows = new ArrayList<>();
    private List<AuditLogResponse> audits = new ArrayList<>();

    public Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
    }

    public List<ShowResponse> getShows() {
      return shows;
    }

    public void setShows(List<ShowResponse> shows) {
      this.shows = shows;
    }

    public List<AuditLogResponse> getAudits() {
      return audits;
    }

    public void setAudits(List<AuditLogResponse> audits) {
      this.audits = audits;
    }
  }

  public static class ShowResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;
    private Instant startAt;
    private Instant endAt;
    private Instant saleStartAt;
    private Instant saleEndAt;
    private List<TierResponse> tiers = new ArrayList<>();

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

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

    public List<TierResponse> getTiers() {
      return tiers;
    }

    public void setTiers(List<TierResponse> tiers) {
      this.tiers = tiers;
    }
  }

  public static class TierResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;
    private Long unitPriceFen;
    private Integer perUserLimit;
    private Integer totalQty;
    private Integer availableQty;
    private Integer reservedQty;
    private Integer soldQty;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

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

    public Integer getAvailableQty() {
      return availableQty;
    }

    public void setAvailableQty(Integer availableQty) {
      this.availableQty = availableQty;
    }

    public Integer getReservedQty() {
      return reservedQty;
    }

    public void setReservedQty(Integer reservedQty) {
      this.reservedQty = reservedQty;
    }

    public Integer getSoldQty() {
      return soldQty;
    }

    public void setSoldQty(Integer soldQty) {
      this.soldQty = soldQty;
    }
  }

  public static class AuditLogResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long actorUserId;

    private String fromStatus;
    private String toStatus;
    private String comment;
    private Instant createdAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getActorUserId() {
      return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
      this.actorUserId = actorUserId;
    }

    public String getFromStatus() {
      return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
      this.fromStatus = fromStatus;
    }

    public String getToStatus() {
      return toStatus;
    }

    public void setToStatus(String toStatus) {
      this.toStatus = toStatus;
    }

    public String getComment() {
      return comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }

    public Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
    }
  }
}
