package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.ClassificationResult;
import com.shopimage.entity.ClassificationTask;
import com.shopimage.entity.SystemLog;
import com.shopimage.category.Category;
import com.shopimage.category.CategoryRepository;
import com.shopimage.repository.ClassificationResultRepository;
import com.shopimage.repository.ClassificationTaskRepository;
import com.shopimage.repository.ModelRepository;
import com.shopimage.repository.ProductImageRepository;
import com.shopimage.repository.SystemLogRepository;
import com.shopimage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.NetworkInterface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserRepository userRepository;
    private final ClassificationTaskRepository classificationTaskRepository;
    private final ProductImageRepository productImageRepository;
    private final ModelRepository modelRepository;
    private final SystemLogRepository systemLogRepository;
    private final ClassificationResultRepository classificationResultRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> getStats() {
        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("totalUsers", userRepository.count());
            stats.put("totalTasks", classificationTaskRepository.count());
            stats.put("totalImages", productImageRepository.count());
            stats.put("totalModels", modelRepository.count());
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            log.error("获取数据看板统计失败", e);
            return ApiResponse.error("获取数据看板统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/system-status")
    public ApiResponse<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("cpu", getCpuUsagePercent());
            status.put("memory", getMemoryUsagePercent());
            status.put("disk", getDiskUsagePercent());
            status.put("network", isNetworkAvailable() ? "normal" : "error");
            return ApiResponse.ok(status);
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return ApiResponse.error("获取系统状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/recent-activities")
    public ApiResponse<List<Map<String, Object>>> getRecentActivities(@RequestParam(defaultValue = "10") int size) {
        try {
            int pageSize = Math.max(1, Math.min(size, 50));
            List<SystemLog> logs = systemLogRepository.findAll(
                    PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createTime"))
            ).getContent();

            List<Map<String, Object>> activities = logs.stream().map(logItem -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", logItem.getId());
                activity.put("type", mapActivityType(logItem));
                activity.put("description", logItem.getMessage());
                activity.put("time", logItem.getCreateTime());
                return activity;
            }).collect(Collectors.toList());

            return ApiResponse.ok(activities);
        } catch (Exception e) {
            log.error("获取最近活动失败", e);
            return ApiResponse.error("获取最近活动失败: " + e.getMessage());
        }
    }

    @GetMapping("/chart-data")
    public ApiResponse<Map<String, Object>> getChartData(@RequestParam(defaultValue = "7d") String period) {
        try {
            int days = parsePeriodDays(period);
            LocalDate today = LocalDate.now();
            LocalDateTime startTime = today.minusDays(days - 1L).atStartOfDay();
            LocalDateTime endTime = today.plusDays(1L).atStartOfDay().minusNanos(1);

            List<ClassificationTask> tasks = classificationTaskRepository.findByCreatedAtBetween(startTime, endTime);

            List<String> dates = new ArrayList<>();
            List<Integer> values = new ArrayList<>();
            Map<LocalDate, Long> taskCountByDay = tasks.stream()
                    .filter(task -> task.getCreatedAt() != null)
                    .collect(Collectors.groupingBy(
                            task -> task.getCreatedAt().toLocalDate(),
                            Collectors.counting()
                    ));

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dates.add(date.toString());
                values.add(taskCountByDay.getOrDefault(date, 0L).intValue());
            }

            Map<String, List<BigDecimal>> modelAccuracyMap = new HashMap<>();
            for (ClassificationTask task : tasks) {
                if (task.getAccuracy() == null || task.getModelName() == null || task.getModelName().isBlank()) {
                    continue;
                }
                modelAccuracyMap.computeIfAbsent(task.getModelName(), key -> new ArrayList<>()).add(task.getAccuracy());
            }

            Map<String, BigDecimal> averageAccuracyByModel = new LinkedHashMap<>();
            modelAccuracyMap.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), average(entry.getValue())))
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                    .limit(6)
                    .forEach(entry -> averageAccuracyByModel.put(entry.getKey(), entry.getValue()));

            Map<String, Object> taskTrend = new HashMap<>();
            taskTrend.put("dates", dates);
            taskTrend.put("values", values);

            Map<String, Object> accuracy = new HashMap<>();
            accuracy.put("categories", new ArrayList<>(averageAccuracyByModel.keySet()));
            accuracy.put("values", averageAccuracyByModel.values().stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));

            Map<String, Object> result = new HashMap<>();
            result.put("taskTrend", taskTrend);
            result.put("accuracy", accuracy);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("获取图表数据失败", e);
            return ApiResponse.error("获取图表数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/operator/overview")
    public ApiResponse<Map<String, Object>> getOperatorOverview(@RequestParam(defaultValue = "5") int recentSize) {
        try {
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            LocalDateTime endOfToday = startOfToday.plusDays(1).minusNanos(1);
            long todayUploads = productImageRepository.countByUploadedAtBetween(startOfToday, endOfToday);
            long processing = classificationTaskRepository.countByStatus("PROCESSING");
            long completed = classificationTaskRepository.countByStatus("COMPLETED");

            List<ClassificationTask> completedTasks = classificationTaskRepository.findByStatus("COMPLETED");
            List<BigDecimal> accuracyValues = completedTasks.stream()
                    .map(ClassificationTask::getAccuracy)
                    .filter(value -> value != null)
                    .collect(Collectors.toList());
            BigDecimal accuracy = average(accuracyValues);

            int size = Math.max(1, Math.min(recentSize, 10));
            List<Map<String, Object>> recentTasks = classificationTaskRepository.findTop10ByOrderByCreatedAtDesc()
                    .stream()
                    .limit(size)
                    .map(task -> {
                        Map<String, Object> item = new HashMap<>();
                        int totalImages = task.getTotalImages() == null ? 0 : task.getTotalImages();
                        int processedImages = task.getProcessedImages() == null ? 0 : task.getProcessedImages();
                        int progress = totalImages <= 0 ? 0 : (int) Math.round((double) processedImages * 100 / totalImages);
                        item.put("id", task.getId());
                        item.put("taskName", task.getTaskName());
                        item.put("imageCount", totalImages);
                        item.put("totalImages", totalImages);
                        item.put("processedImages", processedImages);
                        item.put("status", task.getStatus());
                        item.put("progress", Math.max(0, Math.min(progress, 100)));
                        item.put("createTime", task.getCreatedAt());
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> stats = new HashMap<>();
            stats.put("todayUploads", todayUploads);
            stats.put("processing", processing);
            stats.put("completed", completed);
            stats.put("accuracy", accuracy.doubleValue());

            Map<String, Object> result = new HashMap<>();
            result.put("stats", stats);
            result.put("recentTasks", recentTasks);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("获取运营工作台概览失败", e);
            return ApiResponse.error("获取运营工作台概览失败: " + e.getMessage());
        }
    }

    @GetMapping("/operator/chart-data")
    public ApiResponse<Map<String, Object>> getOperatorChartData(@RequestParam(defaultValue = "7d") String period) {
        try {
            int days = parsePeriodDays(period);
            LocalDate today = LocalDate.now();
            LocalDateTime startTime = today.minusDays(days - 1L).atStartOfDay();
            LocalDateTime endTime = today.plusDays(1L).atStartOfDay().minusNanos(1);
            List<ClassificationTask> tasks = classificationTaskRepository.findByCreatedAtBetween(startTime, endTime);

            List<String> dates = new ArrayList<>();
            List<Integer> values = new ArrayList<>();
            Map<LocalDate, Long> taskCountByDay = tasks.stream()
                    .filter(task -> task.getCreatedAt() != null)
                    .collect(Collectors.groupingBy(
                            task -> task.getCreatedAt().toLocalDate(),
                            Collectors.counting()
                    ));
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dates.add(date.toString());
                values.add(taskCountByDay.getOrDefault(date, 0L).intValue());
            }

            List<Long> taskIds = tasks.stream()
                    .filter(task -> task.getId() != null)
                    .map(ClassificationTask::getId)
                    .collect(Collectors.toList());

            Map<Long, Long> categoryCounts = new HashMap<>();
            if (!taskIds.isEmpty()) {
                List<ClassificationResult> results = classificationResultRepository.findByTaskIdIn(taskIds);
                categoryCounts = results.stream()
                        .filter(result -> result.getPredictedCategoryId() != null)
                        .collect(Collectors.groupingBy(
                                ClassificationResult::getPredictedCategoryId,
                                Collectors.counting()
                        ));
            }

            Map<Long, String> categoryNameMap = categoryRepository.findAll().stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName, (left, right) -> left));

            List<Map.Entry<Long, Long>> sortedCategoryEntries = new ArrayList<>(categoryCounts.entrySet());
            sortedCategoryEntries.sort(Map.Entry.<Long, Long>comparingByValue().reversed());

            List<String> categories = new ArrayList<>();
            List<Integer> categoryValues = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : sortedCategoryEntries.stream().limit(8).collect(Collectors.toList())) {
                categories.add(categoryNameMap.getOrDefault(entry.getKey(), "未知分类"));
                categoryValues.add(entry.getValue().intValue());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("dailyTrend", Map.of("dates", dates, "values", values));
            result.put("categoryDistribution", Map.of("categories", categories, "values", categoryValues));
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("获取运营工作台图表数据失败", e);
            return ApiResponse.error("获取运营工作台图表数据失败: " + e.getMessage());
        }
    }

    private int getCpuUsagePercent() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double systemCpuLoad = osBean.getSystemCpuLoad();
            if (systemCpuLoad < 0) {
                return 0;
            }
            return (int) Math.round(systemCpuLoad * 100);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getMemoryUsagePercent() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long totalMemory = osBean.getTotalPhysicalMemorySize();
            long freeMemory = osBean.getFreePhysicalMemorySize();
            if (totalMemory <= 0) {
                return 0;
            }
            return (int) Math.round((double) (totalMemory - freeMemory) * 100 / totalMemory);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getDiskUsagePercent() {
        try {
            java.io.File[] roots = java.io.File.listRoots();
            long total = 0L;
            long used = 0L;
            for (java.io.File root : roots) {
                long rootTotal = root.getTotalSpace();
                long rootUsable = root.getUsableSpace();
                if (rootTotal <= 0) {
                    continue;
                }
                total += rootTotal;
                used += (rootTotal - rootUsable);
            }
            if (total <= 0) {
                return 0;
            }
            return (int) Math.round((double) used * 100 / total);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isNetworkAvailable() {
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                if (networkInterface.isUp()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private int parsePeriodDays(String period) {
        if (period == null || period.isBlank()) {
            return 7;
        }
        return switch (period.toLowerCase()) {
            case "30d" -> 30;
            case "90d" -> 90;
            default -> 7;
        };
    }

    private String mapActivityType(SystemLog logItem) {
        if (logItem.getLevel() == SystemLog.Level.ERROR || logItem.getLevel() == SystemLog.Level.WARN) {
            return "error";
        }
        if (logItem.getModule() == null) {
            return "system";
        }
        return switch (logItem.getModule()) {
            case USER, AUTH -> "user";
            case TASK, IMAGE -> "task";
            case SYSTEM -> "system";
        };
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
