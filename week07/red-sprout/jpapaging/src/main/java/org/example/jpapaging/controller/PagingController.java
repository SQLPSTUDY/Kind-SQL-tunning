package org.example.jpapaging.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jpapaging.dto.EmployeeDTO;
import org.example.jpapaging.dto.PerformanceResult;
import org.example.jpapaging.service.JdbcPagingService;
import org.example.jpapaging.service.JpaPagingService;
import org.example.jpapaging.service.PerformanceTestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class PagingController {

    private final JdbcPagingService jdbcPagingService;
    private final JpaPagingService jpaPagingService;
    private final PerformanceTestService performanceTestService;

    // ==================== JDBC Endpoints ====================

    @GetMapping("/jdbc/rownum")
    public List<EmployeeDTO> jdbcRownum(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return jdbcPagingService.findWithRownum(offset, limit);
    }

    @GetMapping("/jdbc/offset-fetch")
    public List<EmployeeDTO> jdbcOffsetFetch(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return jdbcPagingService.findWithOffsetFetch(offset, limit);
    }

    @GetMapping("/jdbc/row-number")
    public List<EmployeeDTO> jdbcRowNumber(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return jdbcPagingService.findWithRowNumber(offset, limit);
    }

    @GetMapping("/jdbc/ansi-join")
    public List<EmployeeDTO> jdbcAnsiJoin(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return jdbcPagingService.findWithAnsiJoin(offset, limit);
    }

    // ==================== JPA Endpoints ====================

    @GetMapping("/jpa/jpql")
    public List<EmployeeDTO> jpaJpql(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return jpaPagingService.findWithJpql(offset, limit);
    }

    @GetMapping("/jpa/native")
    public List<EmployeeDTO> jpaNative(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return jpaPagingService.findWithNativeQuery(offset, limit);
    }

    @GetMapping("/jpa/criteria")
    public List<EmployeeDTO> jpaCriteria(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return jpaPagingService.findWithCriteria(offset, limit);
    }

    @GetMapping("/jpa/left-join")
    public List<EmployeeDTO> jpaLeftJoin(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return jpaPagingService.findWithLeftJoin(offset, limit);
    }

    // ==================== Spring Data JPA Pageable Endpoints ====================

    @GetMapping("/jpa/pageable")
    public Page<EmployeeDTO> jpaPageable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return jpaPagingService.findWithPageable(PageRequest.of(page, size));
    }

    @GetMapping("/jpa/slice")
    public Slice<EmployeeDTO> jpaSlice(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return jpaPagingService.findWithSlice(PageRequest.of(page, size));
    }

    @GetMapping("/jpa/pageable-sort")
    public Page<EmployeeDTO> jpaPageableWithSort(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "employeeId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return jpaPagingService.findWithPageableAndSort(pageRequest);
    }

    @GetMapping("/jpa/cursor")
    public List<EmployeeDTO> jpaCursor(
            @RequestParam(defaultValue = "0") Long lastEmployeeId,
            @RequestParam(defaultValue = "10") int limit) {
        return jpaPagingService.findWithCursorPagination(lastEmployeeId, limit);
    }

    @GetMapping("/jpa/native-pageable")
    public Page<EmployeeDTO> jpaNativePageable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return jpaPagingService.findWithNativeQueryAndPageable(PageRequest.of(page, size));
    }

    // ==================== Performance Test Endpoint ====================

    @GetMapping("/performance-test")
    public List<PerformanceResult> performanceTest(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return performanceTestService.runAllTests(offset, limit);
    }
}