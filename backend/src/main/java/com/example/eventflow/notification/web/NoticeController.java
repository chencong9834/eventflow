package com.example.eventflow.notification.web;

import com.example.eventflow.notification.SiteNotice;
import com.example.eventflow.notification.SiteNoticeMapper;
import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.api.PageQuery;
import com.example.eventflow.shared.api.PageResult;
import com.example.eventflow.shared.api.PageSupport;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.shared.time.Utc;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

  private final SiteNoticeMapper noticeMapper;

  public NoticeController(SiteNoticeMapper noticeMapper) {
    this.noticeMapper = noticeMapper;
  }

  @GetMapping
  public ApiResponse<PageResult<NoticeResponse>> mine(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PageQuery query = PageQuery.of(page, size);
    return ApiResponse.ok(
        PageSupport.query(
            query,
            () -> noticeMapper.findByUser(principal.getUserId()),
            notice -> {
              NoticeResponse dto = new NoticeResponse();
              dto.setId(notice.getId());
              dto.setTitle(notice.getTitle());
              dto.setBody(notice.getBody());
              dto.setCreatedAt(Utc.toInstant(notice.getCreatedAt()));
              return dto;
            }));
  }
}
