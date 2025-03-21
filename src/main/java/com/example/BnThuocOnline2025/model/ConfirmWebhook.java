package com.example.BnThuocOnline2025.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmWebhook {
  private String webhookUrl;

  public ConfirmWebhook(String webhookUrl) {
    this.webhookUrl = webhookUrl;
  }
}
