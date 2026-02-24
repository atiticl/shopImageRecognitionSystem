package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {
    private final CacheService cacheService;

    @GetMapping("/check")
    public ApiResponse<String> check(@RequestParam("md5") String md5) {
        String cached = cacheService.getResult(md5);
        return ApiResponse.ok(cached);
    }
}