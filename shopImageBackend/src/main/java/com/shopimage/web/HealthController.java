package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "UP");
        return ApiResponse.ok(m);
    }
}