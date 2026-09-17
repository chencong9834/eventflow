package com.example.eventflow.notification.web;

import com.example.eventflow.notification.SiteNotice;
import com.example.eventflow.notification.SiteNoticeMapper;
import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.shared.time.Utc;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

  private final SiteNoticeMapper noticeMapper;

  public NoticeController(SiteNoticeMapper noticeMapper) {
    this.noticeMapper = noticeMapper;
  }

  @GetMapping
  public ApiResponse<List<NoticeResponse>> mine(@AuthenticationPrincipal AuthPrincipal principal) {
    List<NoticeResponse> rows = new ArrayList<>();
    for (SiteNotice notice : noticeMapper.findByUser(principal.getUserId())) {
      NoticeResponse dto = new NoticeResponse();
      dto.setId(notice.getId());
      dto.setTitle(notice.getTitle());
      dto.setBody(notice.getBody());
      dto.setCreatedAt(Utc.toInstant(notice.getCreatedAt()));
      rows.add(dto);
    }
    return ApiResponse.ok(rows);
  }
}
