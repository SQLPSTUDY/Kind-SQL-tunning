package org.example.jpapaging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jpapaging.dto.EmployeeDTO;
import org.example.jpapaging.dto.PerformanceResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceTestService {

    private final JdbcPagingService jdbcPagingService;
    private final JpaPagingService jpaPagingService;

    public List<PerformanceResult> runAllTests(int offset, int limit) {
        List<PerformanceResult> results = new ArrayList<>();

        log.info("========== Starting Performance Tests ==========");
        log.info("Offset: {}, Limit: {}", offset, limit);
        log.info("===============================================");

        // JDBC Tests
        results.add(measurePerformance(
                "JDBC - ROWNUM (Oracle Traditional)",
                () -> jdbcPagingService.findWithRownum(offset, limit)
        ));

        results.add(measurePerformance(
                "JDBC - OFFSET FETCH (Oracle 12c+)",
                () -> jdbcPagingService.findWithOffsetFetch(offset, limit)
        ));

        results.add(measurePerformance(
                "JDBC - ROW_NUMBER() Window Function",
                () -> jdbcPagingService.findWithRowNumber(offset, limit)
        ));

        results.add(measurePerformance(
                "JDBC - ANSI JOIN + OFFSET FETCH",
                () -> jdbcPagingService.findWithAnsiJoin(offset, limit)
        ));

        // JPA Tests - Basic
        results.add(measurePerformance(
                "JPA - JPQL with setFirstResult/setMaxResults",
                () -> jpaPagingService.findWithJpql(offset, limit)
        ));

        results.add(measurePerformance(
                "JPA - Native Query",
                () -> jpaPagingService.findWithNativeQuery(offset, limit)
        ));

        results.add(measurePerformance(
                "JPA - Criteria API",
                () -> jpaPagingService.findWithCriteria(offset, limit)
        ));

        results.add(measurePerformance(
                "JPA - JPQL with LEFT JOIN",
                () -> jpaPagingService.findWithLeftJoin(offset, limit)
        ));

        // JPA Tests - Spring Data Pageable
        int page = offset / limit;

        results.add(measurePerformance(
                "JPA - Pageable (Page<T>)",
                () -> {
                    Page<EmployeeDTO> pageResult = jpaPagingService.findWithPageable(PageRequest.of(page, limit));
                    return pageResult.getContent();
                }
        ));

        results.add(measurePerformance(
                "JPA - Slice (No Count Query)",
                () -> {
                    Slice<EmployeeDTO> sliceResult = jpaPagingService.findWithSlice(PageRequest.of(page, limit));
                    return sliceResult.getContent();
                }
        ));

        results.add(measurePerformance(
                "JPA - Pageable with Sort",
                () -> {
                    Page<EmployeeDTO> pageResult = jpaPagingService.findWithPageableAndSort(PageRequest.of(page, limit));
                    return pageResult.getContent();
                }
        ));

        results.add(measurePerformance(
                "JPA - Cursor Pagination (Keyset)",
                () -> jpaPagingService.findWithCursorPagination(0L, limit)
        ));

        results.add(measurePerformance(
                "JPA - Native Query with Pageable",
                () -> {
                    Page<EmployeeDTO> pageResult = jpaPagingService.findWithNativeQueryAndPageable(PageRequest.of(page, limit));
                    return pageResult.getContent();
                }
        ));

        // Print summary
        printSummary(results);

        return results;
    }

    private PerformanceResult measurePerformance(String methodName, Supplier<List<EmployeeDTO>> operation) {
        log.info("Testing: {}", methodName);

        // Warm-up
        try {
            operation.get();
        } catch (Exception e) {
            log.error("Warm-up failed for {}: {}", methodName, e.getMessage());
        }

        // Actual measurement
        long startTime = System.currentTimeMillis();
        List<EmployeeDTO> result = null;
        String errorMsg = null;

        try {
            result = operation.get();
        } catch (Exception e) {
            errorMsg = e.getMessage();
            log.error("Error in {}: {}", methodName, e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        int resultCount = result != null ? result.size() : 0;
        String additionalInfo = errorMsg != null ? "ERROR: " + errorMsg : "SUCCESS";

        PerformanceResult performanceResult = new PerformanceResult(
                methodName,
                executionTime,
                resultCount,
                additionalInfo
        );

        log.info("  ⏱️  Execution Time: {} ms", executionTime);
        log.info("  📊 Result Count: {}", resultCount);
        log.info("  ✅ Status: {}", additionalInfo);
        log.info("-------------------------------------------");

        return performanceResult;
    }

    private void printSummary(List<PerformanceResult> results) {
        log.info("========== Performance Test Summary ==========");

        // Sort by execution time
        results.stream()
                .sorted((a, b) -> Long.compare(a.getExecutionTimeMs(), b.getExecutionTimeMs()))
                .forEach(result -> {
                    log.info("{}: {} ms (Count: {})",
                            result.getMethod(),
                            result.getExecutionTimeMs(),
                            result.getResultCount());
                });

        log.info("=============================================");

        // Find fastest and slowest
        PerformanceResult fastest = results.stream()
                .filter(r -> r.getAdditionalInfo().equals("SUCCESS"))
                .min((a, b) -> Long.compare(a.getExecutionTimeMs(), b.getExecutionTimeMs()))
                .orElse(null);

        PerformanceResult slowest = results.stream()
                .filter(r -> r.getAdditionalInfo().equals("SUCCESS"))
                .max((a, b) -> Long.compare(a.getExecutionTimeMs(), b.getExecutionTimeMs()))
                .orElse(null);

        if (fastest != null) {
            log.info("🏆 FASTEST: {} ({} ms)", fastest.getMethod(), fastest.getExecutionTimeMs());
        }

        if (slowest != null) {
            log.info("🐌 SLOWEST: {} ({} ms)", slowest.getMethod(), slowest.getExecutionTimeMs());
        }

        if (fastest != null && slowest != null && fastest.getExecutionTimeMs() > 0) {
            double ratio = (double) slowest.getExecutionTimeMs() / fastest.getExecutionTimeMs();
            log.info("📈 Performance Gap: {:.2f}x", ratio);
        }

        log.info("=============================================");
    }
}