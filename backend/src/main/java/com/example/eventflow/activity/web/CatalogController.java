package com.example.eventflow.activity.web;

import com.example.eventflow.activity.ActivityService;
import com.example.eventflow.activity.web.ActivitySummaryResponse.ActivityDetailResponse;
import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.api.PageQuery;
import com.example.eventflow.shared.api.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

  private final ActivityService activityService;

  public CatalogController(ActivityService activityService) {
    this.activityService = activityService;
  }

  @PreAuthorize("hasAuthority('catalog:read')")
  @GetMapping("/activities")
  public ApiResponse<PageResult<ActivitySummaryResponse>> list(
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
    return ApiResponse.ok(activityService.listCatalog(PageQuery.of(page, size)));
  }

  @PreAuthorize("hasAuthority('catalog:read')")
  @GetMapping("/activities/{id}")
  public ApiResponse<ActivityDetailResponse> detail(@PathVariable Long id) {
    return ApiResponse.ok(activityService.getCatalog(id));
  }
}
