package com.example.eventflow.activity;

import com.example.eventflow.activity.web.ActivitySummaryResponse;
import com.example.eventflow.activity.web.ActivitySummaryResponse.ActivityDetailResponse;
import com.example.eventflow.activity.web.ActivitySummaryResponse.AuditLogResponse;
import com.example.eventflow.activity.web.ActivitySummaryResponse.ShowResponse;
import com.example.eventflow.activity.web.ActivitySummaryResponse.TierResponse;
import com.example.eventflow.activity.web.ReviewDecideRequest;
import com.example.eventflow.activity.web.UpsertActivityRequest;
import com.example.eventflow.activity.web.UpsertShowRequest;
import com.example.eventflow.activity.web.UpsertTierRequest;
import com.example.eventflow.audit.AuditOperationLog;
import com.example.eventflow.audit.AuditOperationLogMapper;
import com.example.eventflow.identity.SysUser;
import com.example.eventflow.identity.SysUserMapper;
import com.example.eventflow.inventory.Inventory;
import com.example.eventflow.inventory.InventoryMapper;
import com.example.eventflow.inventory.RedisStockService;
import com.example.eventflow.notification.SiteNotice;
import com.example.eventflow.notification.SiteNoticeMapper;
import com.example.eventflow.shared.api.PageQuery;
import com.example.eventflow.shared.api.PageResult;
import com.example.eventflow.shared.api.PageSupport;
import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import com.example.eventflow.shared.id.SnowflakeIdGenerator;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.shared.time.Utc;
import com.example.eventflow.tenant.TenantService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

  public static final String DRAFT = "DRAFT";
  public static final String PENDING = "PENDING";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";
  public static final String CLOSED = "CLOSED";
  public static final String ON_SALE = "ON_SALE";
  public static final String OBJECT_ACTIVITY = "ACTIVITY";

  private final ActivityMapper activityMapper;
  private final ActivityShowMapper showMapper;
  private final TicketTierMapper tierMapper;
  private final InventoryMapper inventoryMapper;
  private final RedisStockService redisStock;
  private final AuditOperationLogMapper auditMapper;
  private final SiteNoticeMapper noticeMapper;
  private final SysUserMapper userMapper;
  private final SnowflakeIdGenerator ids;
  private final Clock clock;

  public ActivityService(
      ActivityMapper activityMapper,
      ActivityShowMapper showMapper,
      TicketTierMapper tierMapper,
      InventoryMapper inventoryMapper,
      RedisStockService redisStock,
      AuditOperationLogMapper auditMapper,
      SiteNoticeMapper noticeMapper,
      SysUserMapper userMapper,
      SnowflakeIdGenerator ids,
      Clock clock) {
    this.activityMapper = activityMapper;
    this.showMapper = showMapper;
    this.tierMapper = tierMapper;
    this.inventoryMapper = inventoryMapper;
    this.redisStock = redisStock;
    this.auditMapper = auditMapper;
    this.noticeMapper = noticeMapper;
    this.userMapper = userMapper;
    this.ids = ids;
    this.clock = clock;
  }

  public int countPending() {
    return activityMapper.countByReviewStatus(PENDING);
  }

  public PageResult<ActivitySummaryResponse> listMine(AuthPrincipal principal, PageQuery page) {
    requireOrganizer(principal);
    return PageSupport.query(
        page,
        () -> activityMapper.findByTenant(principal.getTenantId()),
        ActivityService::toSummary);
  }

  public PageResult<ActivitySummaryResponse> listForReview(String reviewStatus, PageQuery page) {
    String status = reviewStatus == null || reviewStatus.isBlank() ? PENDING : reviewStatus;
    return PageSupport.query(page, () -> activityMapper.findByReviewStatus(status), ActivityService::toSummary);
  }

  public ActivityDetailResponse getMine(AuthPrincipal principal, Long activityId) {
    requireOrganizer(principal);
    Activity activity = requireMine(principal.getTenantId(), activityId);
    return toDetail(activity, true);
  }

  public ActivityDetailResponse getForReview(Long activityId) {
    Activity activity = activityMapper.findById(activityId);
    if (activity == null) {
      throw new BizException(ErrorCode.NOT_FOUND, "活动不存在", HttpStatus.NOT_FOUND);
    }
    return toDetail(activity, true);
  }

  public PageResult<ActivitySummaryResponse> listCatalog(PageQuery page) {
    java.time.LocalDateTime now = Utc.now(clock);
    return PageSupport.query(page, () -> activityMapper.findOnSaleCatalog(now), ActivityService::toSummary);
  }

  public ActivityDetailResponse getCatalog(Long activityId) {
    Activity activity = activityMapper.findById(activityId);
    if (activity == null
        || !APPROVED.equals(activity.getReviewStatus())
        || !ON_SALE.equals(activity.getSaleStatus())) {
      throw new BizException(ErrorCode.NOT_FOUND, "活动不存在或未在售", HttpStatus.NOT_FOUND);
    }
    return toDetail(activity, true);
  }

  @Transactional
  public ActivityDetailResponse create(AuthPrincipal principal, UpsertActivityRequest request) {
    requireOrganizer(principal);
    Activity activity = new Activity();
    activity.setId(ids.nextId());
    activity.setTenantId(principal.getTenantId());
    activity.setTitle(request.getTitle().trim());
    activity.setDescription(blankToNull(request.getDescription()));
    activity.setCoverUrl(blankToNull(request.getCoverUrl()));
    activity.setReviewStatus(DRAFT);
    activity.setSaleStatus(CLOSED);
    activity.setCreatedBy(principal.getUserId());
    activityMapper.insert(activity);
    return getMine(principal, activity.getId());
  }

  @Transactional
  public ActivityDetailResponse update(
      AuthPrincipal principal, Long activityId, UpsertActivityRequest request) {
    Activity activity = requireEditable(principal, activityId);
    activity.setTitle(request.getTitle().trim());
    activity.setDescription(blankToNull(request.getDescription()));
    activity.setCoverUrl(blankToNull(request.getCoverUrl()));
    activityMapper.updateMeta(activity);
    return getMine(principal, activityId);
  }

  @Transactional
  public ActivityDetailResponse addShow(
      AuthPrincipal principal, Long activityId, UpsertShowRequest request) {
    Activity activity = requireEditable(principal, activityId);
    validateShowTimes(request);
    ActivityShow show = new ActivityShow();
    show.setId(ids.nextId());
    show.setTenantId(principal.getTenantId());
    show.setActivityId(activity.getId());
    fillShow(show, request, principal.getUserId());
    showMapper.insert(show);
    return getMine(principal, activityId);
  }

  @Transactional
  public ActivityDetailResponse updateShow(
      AuthPrincipal principal, Long showId, UpsertShowRequest request) {
    ActivityShow show = requireShow(principal.getTenantId(), showId);
    requireEditable(principal, show.getActivityId());
    validateShowTimes(request);
    fillShow(show, request, principal.getUserId());
    showMapper.update(show);
    return getMine(principal, show.getActivityId());
  }

  @Transactional
  public ActivityDetailResponse addTier(
      AuthPrincipal principal, Long showId, UpsertTierRequest request) {
    ActivityShow show = requireShow(principal.getTenantId(), showId);
    requireEditable(principal, show.getActivityId());
    TicketTier tier = new TicketTier();
    tier.setId(ids.nextId());
    tier.setTenantId(principal.getTenantId());
    tier.setShowId(show.getId());
    fillTier(tier, request);
    tier.setCreatedBy(principal.getUserId());
    tierMapper.insert(tier);
    Inventory inventory = new Inventory();
    inventory.setId(ids.nextId());
    inventory.setTenantId(principal.getTenantId());
    inventory.setTicketTierId(tier.getId());
    inventory.setTotalQty(request.getTotalQty());
    inventory.setAvailableQty(request.getTotalQty());
    inventory.setReservedQty(0);
    inventory.setSoldQty(0);
    inventory.setCreatedBy(principal.getUserId());
    inventoryMapper.insert(inventory);
    redisStock.replaceAvailable(tier.getId(), request.getTotalQty());
    return getMine(principal, show.getActivityId());
  }

  @Transactional
  public ActivityDetailResponse updateTier(
      AuthPrincipal principal, Long tierId, UpsertTierRequest request) {
    TicketTier tier = tierMapper.findByIdAndTenant(tierId, principal.getTenantId());
    if (tier == null) {
      throw new BizException(ErrorCode.NOT_FOUND, "票档不存在", HttpStatus.NOT_FOUND);
    }
    ActivityShow show = requireShow(principal.getTenantId(), tier.getShowId());
    requireEditable(principal, show.getActivityId());
    if (!request.getTotalQty().equals(tier.getTotalQty())) {
      int updated =
          inventoryMapper.resetTotalIfUnsold(tier.getId(), request.getTotalQty(), request.getTotalQty());
      if (updated == 0) {
        throw new BizException(ErrorCode.ACTIVITY_LOCKED, "已有预留或售出库存，不能改总量", HttpStatus.CONFLICT);
      }
      redisStock.replaceAvailable(tier.getId(), request.getTotalQty());
    }
    fillTier(tier, request);
    tierMapper.update(tier);
    return getMine(principal, show.getActivityId());
  }

  @Transactional
  public ActivityDetailResponse submit(AuthPrincipal principal, Long activityId) {
    Activity activity = requireEditable(principal, activityId);
    List<ActivityShow> shows = showMapper.findByActivityId(activityId);
    if (shows.isEmpty()) {
      throw new BizException(ErrorCode.BAD_REQUEST, "提交审核前至少添加一个场次", HttpStatus.BAD_REQUEST);
    }
    for (ActivityShow show : shows) {
      List<TicketTier> tiers = tierMapper.findByShowId(show.getId());
      if (tiers.isEmpty()) {
        throw new BizException(ErrorCode.BAD_REQUEST, "每个场次至少需要一个票档", HttpStatus.BAD_REQUEST);
      }
    }
    String from = activity.getReviewStatus();
    activityMapper.updateStatuses(activityId, PENDING, CLOSED);
    writeAudit(principal, activity, from, PENDING, "提交审核");
    return getMine(principal, activityId);
  }

  @Transactional
  public ActivityDetailResponse startRevision(AuthPrincipal principal, Long activityId) {
    requireOrganizer(principal);
    Activity activity = requireMine(principal.getTenantId(), activityId);
    if (!APPROVED.equals(activity.getReviewStatus())) {
      throw new BizException(ErrorCode.BAD_REQUEST, "仅已通过的活动可申请改价改结构", HttpStatus.BAD_REQUEST);
    }
    activityMapper.updateStatuses(activityId, DRAFT, CLOSED);
    writeAudit(principal, activity, APPROVED, DRAFT, "下架并打开改价改结构，已售订单仍以快照为准");
    clearRedisStock(activityId);
    return getMine(principal, activityId);
  }

  @Transactional
  public ActivityDetailResponse onSale(AuthPrincipal principal, Long activityId) {
    requireOrganizer(principal);
    Activity activity = requireMine(principal.getTenantId(), activityId);
    if (!APPROVED.equals(activity.getReviewStatus())) {
      throw new BizException(ErrorCode.BAD_REQUEST, "仅已通过审核的活动可重新开售", HttpStatus.BAD_REQUEST);
    }
    activityMapper.updateStatuses(activityId, APPROVED, ON_SALE);
    writeAudit(principal, activity, activity.getSaleStatus(), ON_SALE, "重新开售");
    replaceRedisStock(activityId);
    return getMine(principal, activityId);
  }

  @Transactional
  public ActivityDetailResponse offSale(AuthPrincipal principal, Long activityId) {
    requireOrganizer(principal);
    Activity activity = requireMine(principal.getTenantId(), activityId);
    if (!APPROVED.equals(activity.getReviewStatus())) {
      throw new BizException(ErrorCode.BAD_REQUEST, "仅已通过审核的活动可下架", HttpStatus.BAD_REQUEST);
    }
    activityMapper.updateStatuses(activityId, APPROVED, CLOSED);
    writeAudit(principal, activity, activity.getSaleStatus(), CLOSED, "主办方下架停售");
    clearRedisStock(activityId);
    return getMine(principal, activityId);
  }

  @Transactional
  public ActivityDetailResponse decide(AuthPrincipal principal, Long activityId, ReviewDecideRequest request) {
    Activity activity = activityMapper.findById(activityId);
    if (activity == null) {
      throw new BizException(ErrorCode.NOT_FOUND, "活动不存在", HttpStatus.NOT_FOUND);
    }
    if (!PENDING.equals(activity.getReviewStatus())) {
      throw new BizException(ErrorCode.REVIEW_NOT_PENDING, "仅待审活动可审批", HttpStatus.CONFLICT);
    }
    String decision = request.getDecision();
    if (REJECTED.equals(decision)
        && (request.getComment() == null || request.getComment().isBlank())) {
      throw new BizException(ErrorCode.BAD_REQUEST, "驳回必须填写意见", HttpStatus.BAD_REQUEST);
    }
    String sale = APPROVED.equals(decision) ? ON_SALE : CLOSED;
    activityMapper.updateStatuses(activityId, decision, sale);
    writeAudit(principal, activity, PENDING, decision, blankToNull(request.getComment()));
    if (APPROVED.equals(decision)) {
      replaceRedisStock(activityId);
    } else {
      clearRedisStock(activityId);
    }
    String title = APPROVED.equals(decision) ? "活动审核通过" : "活动审核驳回";
    String body = activity.getTitle() + (request.getComment() == null || request.getComment().isBlank() ? "" : "：" + request.getComment().trim());
    for (SysUser user : userMapper.findSummariesByTenantId(activity.getTenantId())) {
      SiteNotice notice = new SiteNotice();
      notice.setId(ids.nextId());
      notice.setTenantId(activity.getTenantId());
      notice.setUserId(user.getId());
      notice.setTitle(title);
      notice.setBody(body);
      noticeMapper.insert(notice);
    }
    return getForReview(activityId);
  }

  private void replaceRedisStock(Long activityId) {
    for (ActivityShow show : showMapper.findByActivityId(activityId)) {
      for (TicketTier tier : tierMapper.findByShowId(show.getId())) {
        Inventory inventory = inventoryMapper.findByTicketTierId(tier.getId());
        if (inventory != null) {
          redisStock.replaceAvailable(tier.getId(), inventory.getAvailableQty());
        }
      }
    }
  }

  private void clearRedisStock(Long activityId) {
    for (ActivityShow show : showMapper.findByActivityId(activityId)) {
      for (TicketTier tier : tierMapper.findByShowId(show.getId())) {
        redisStock.delete(tier.getId());
      }
    }
  }

  private Activity requireEditable(AuthPrincipal principal, Long activityId) {
    requireOrganizer(principal);
    Activity activity = requireMine(principal.getTenantId(), activityId);
    if (!DRAFT.equals(activity.getReviewStatus()) && !REJECTED.equals(activity.getReviewStatus())) {
      throw new BizException(ErrorCode.ACTIVITY_LOCKED, "待审或已通过的活动不可改结构", HttpStatus.CONFLICT);
    }
    return activity;
  }

  private Activity requireMine(Long tenantId, Long activityId) {
    Activity activity = activityMapper.findByIdAndTenant(activityId, tenantId);
    if (activity == null) {
      throw new BizException(ErrorCode.NOT_FOUND, "活动不存在", HttpStatus.NOT_FOUND);
    }
    return activity;
  }

  private ActivityShow requireShow(Long tenantId, Long showId) {
    ActivityShow show = showMapper.findByIdAndTenant(showId, tenantId);
    if (show == null) {
      throw new BizException(ErrorCode.NOT_FOUND, "场次不存在", HttpStatus.NOT_FOUND);
    }
    return show;
  }

  private void writeAudit(
      AuthPrincipal principal, Activity activity, String from, String to, String comment) {
    AuditOperationLog log = new AuditOperationLog();
    log.setId(ids.nextId());
    log.setActorTenantId(principal.getTenantId());
    log.setActorUserId(principal.getUserId());
    log.setObjectType(OBJECT_ACTIVITY);
    log.setObjectId(activity.getId());
    log.setObjectTenantId(activity.getTenantId());
    log.setFromStatus(from);
    log.setToStatus(to);
    log.setComment(comment);
    auditMapper.insert(log);
  }

  private void fillShow(ActivityShow show, UpsertShowRequest request, Long actorId) {
    show.setName(request.getName().trim());
    show.setStartAt(Utc.from(request.getStartAt()));
    show.setEndAt(Utc.from(request.getEndAt()));
    show.setSaleStartAt(Utc.from(request.getSaleStartAt()));
    show.setSaleEndAt(Utc.from(request.getSaleEndAt()));
    if (show.getCreatedBy() == null) {
      show.setCreatedBy(actorId);
    }
  }

  private static void fillTier(TicketTier tier, UpsertTierRequest request) {
    tier.setName(request.getName().trim());
    tier.setUnitPriceFen(request.getUnitPriceFen());
    tier.setPerUserLimit(request.getPerUserLimit());
  }

  private static void validateShowTimes(UpsertShowRequest request) {
    if (!request.getEndAt().isAfter(request.getStartAt())) {
      throw new BizException(ErrorCode.BAD_REQUEST, "结束时间必须晚于开演时间", HttpStatus.BAD_REQUEST);
    }
    if (!request.getSaleEndAt().isAfter(request.getSaleStartAt())) {
      throw new BizException(ErrorCode.BAD_REQUEST, "售卖结束必须晚于售卖开始", HttpStatus.BAD_REQUEST);
    }
  }

  private ActivityDetailResponse toDetail(Activity activity, boolean withChildren) {
    ActivityDetailResponse dto = new ActivityDetailResponse();
    copySummary(activity, dto);
    dto.setCreatedAt(Utc.toInstant(activity.getCreatedAt()));
    if (withChildren) {
      for (ActivityShow show : showMapper.findByActivityId(activity.getId())) {
        ShowResponse showDto = new ShowResponse();
        showDto.setId(show.getId());
        showDto.setName(show.getName());
        showDto.setStartAt(Utc.toInstant(show.getStartAt()));
        showDto.setEndAt(Utc.toInstant(show.getEndAt()));
        showDto.setSaleStartAt(Utc.toInstant(show.getSaleStartAt()));
        showDto.setSaleEndAt(Utc.toInstant(show.getSaleEndAt()));
        for (TicketTier tier : tierMapper.findByShowId(show.getId())) {
          showDto.getTiers().add(toTier(tier));
        }
        dto.getShows().add(showDto);
      }
      for (AuditOperationLog log : auditMapper.findByObject(OBJECT_ACTIVITY, activity.getId())) {
        AuditLogResponse item = new AuditLogResponse();
        item.setId(log.getId());
        item.setActorUserId(log.getActorUserId());
        item.setFromStatus(log.getFromStatus());
        item.setToStatus(log.getToStatus());
        item.setComment(log.getComment());
        item.setCreatedAt(Utc.toInstant(log.getCreatedAt()));
        dto.getAudits().add(item);
      }
    }
    return dto;
  }

  private static TierResponse toTier(TicketTier tier) {
    TierResponse dto = new TierResponse();
    dto.setId(tier.getId());
    dto.setName(tier.getName());
    dto.setUnitPriceFen(tier.getUnitPriceFen());
    dto.setPerUserLimit(tier.getPerUserLimit());
    dto.setTotalQty(tier.getTotalQty());
    dto.setAvailableQty(tier.getAvailableQty());
    dto.setReservedQty(tier.getReservedQty());
    dto.setSoldQty(tier.getSoldQty());
    return dto;
  }

  private static ActivitySummaryResponse toSummary(Activity activity) {
    ActivitySummaryResponse dto = new ActivitySummaryResponse();
    copySummary(activity, dto);
    return dto;
  }

  private static void copySummary(Activity activity, ActivitySummaryResponse dto) {
    dto.setId(activity.getId());
    dto.setTenantId(activity.getTenantId());
    dto.setTitle(activity.getTitle());
    dto.setReviewStatus(activity.getReviewStatus());
    dto.setSaleStatus(activity.getSaleStatus());
    dto.setShowCount(activity.getShowCount() == null ? 0 : activity.getShowCount());
    dto.setUpdatedAt(Utc.toInstant(activity.getUpdatedAt()));
    dto.setOrganizerName(activity.getOrganizerName());
    dto.setOrganizerCode(activity.getOrganizerCode());
    dto.setDescription(activity.getDescription());
    dto.setCoverUrl(activity.getCoverUrl());
  }

  private static void requireOrganizer(AuthPrincipal principal) {
    if (!TenantService.TYPE_ORGANIZER.equals(principal.getTenantType())) {
      throw new BizException(ErrorCode.FORBIDDEN, "仅主办方可维护本租户活动", HttpStatus.FORBIDDEN);
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
