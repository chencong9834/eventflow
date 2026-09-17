package com.example.eventflow.activity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ReviewDecideRequest {

  @NotBlank
  @Pattern(regexp = "APPROVED|REJECTED", message = "决定须为通过或驳回")
  private String decision;

  @Size(max = 512)
  private String comment;

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
