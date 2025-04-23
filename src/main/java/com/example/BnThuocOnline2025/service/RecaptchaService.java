package com.example.BnThuocOnline2025.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Value("${recaptcha.secret.key}")
    private String recaptchaSecret;

    public boolean verifyRecaptcha(String recaptchaResponse) {
        RestTemplate restTemplate = new RestTemplate();
        String params = "?secret=" + recaptchaSecret + "&response=" + recaptchaResponse;
        ResponseEntity<Map> response = restTemplate.getForEntity(RECAPTCHA_VERIFY_URL + params, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            if (body != null) {
                boolean success = (Boolean) body.get("success");
                if (!success) {
                    System.out.println("reCAPTCHA verification failed: " + body);
                }
                return success;
            }
        }
        return false;
    }
}
