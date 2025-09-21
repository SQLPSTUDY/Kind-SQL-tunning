package org.example.jpapaging.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jpapaging.dto.EmployeeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaPagingService {

    private final EntityManager entityManager;

    /**
     * Method 1: JPQL with setFirstResult/setMaxResults
     */
    public List<EmployeeDTO> findWithJpql(int offset, int limit) {
        String jpql = """
            SELECT new org.example.jpapaging.dto.EmployeeDTO(
                e.firstName, e.lastName, d.departmentName,
                l.streetAddress, l.city, c.countryName, r.regionName
            )
            FROM Employee e
            JOIN e.department d
            JOIN d.location l
            JOIN l.country c
            JOIN c.region r
            """;

        TypedQuery<EmployeeDTO> query = entityManager.createQuery(jpql, EmployeeDTO.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    /**
     * Method 2: Native Query with pagination
     */
    public List<EmployeeDTO> findWithNativeQuery(int offset, int limit) {
        String sql = """
            SELECT E.FIRST_NAME as firstName, E.LAST_NAME as lastName, D.DEPARTMENT_NAME as departmentName,
                   L.STREET_ADDRESS as streetAddress, L.CITY as city, C.COUNTRY_NAME as countryName, R.REGION_NAME as regionName
            FROM EMPLOYEES E
            INNER JOIN DEPARTMENTS D ON D.DEPARTMENT_ID = E.DEPARTMENT_ID
            INNER JOIN LOCATIONS L ON L.LOCATION_ID = D.LOCATION_ID
            INNER JOIN COUNTRIES C ON C.COUNTRY_ID = L.COUNTRY_ID
            INNER JOIN REGIONS R ON R.REGION_ID = C.REGION_ID
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter(1, offset)
                .setParameter(2, limit)
                .getResultList();

        return results.stream()
                .map(row -> new EmployeeDTO(
                        (String) row[0], // firstName
                        (String) row[1], // lastName
                        (String) row[2], // departmentName
                        (String) row[3], // streetAddress
                        (String) row[4], // city
                        (String) row[5], // countryName
                        (String) row[6]  // regionName
                ))
                .toList();
    }

    /**
     * Method 3: Criteria API with pagination
     */
    public List<EmployeeDTO> findWithCriteria(int offset, int limit) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(EmployeeDTO.class);
        var employee = query.from(org.example.jpapaging.entity.Employee.class);
        var department = employee.join("department");
        var location = department.join("location");
        var country = location.join("country");
        var region = country.join("region");

        query.select(cb.construct(EmployeeDTO.class,
                employee.get("firstName"),
                employee.get("lastName"),
                department.get("departmentName"),
                location.get("streetAddress"),
                location.get("city"),
                country.get("countryName"),
                region.get("regionName")
        ));

        TypedQuery<EmployeeDTO> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(limit);

        return typedQuery.getResultList();
    }

    /**
     * Method 4: JPQL with LEFT JOIN (Outer Join)
     */
    public List<EmployeeDTO> findWithLeftJoin(int offset, int limit) {
        String jpql = """
            SELECT new org.example.jpapaging.dto.EmployeeDTO(
                e.firstName, e.lastName, d.departmentName,
                l.streetAddress, l.city, c.countryName, r.regionName
            )
            FROM Employee e
            LEFT JOIN e.department d
            LEFT JOIN d.location l
            LEFT JOIN l.country c
            LEFT JOIN c.region r
            """;

        TypedQuery<EmployeeDTO> query = entityManager.createQuery(jpql, EmployeeDTO.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    /**
     * Method 5: Pageable 사용 - JPQL
     */
    public Page<EmployeeDTO> findWithPageable(Pageable pageable) {
        String jpql = """
            SELECT new org.example.jpapaging.dto.EmployeeDTO(
                e.firstName, e.lastName, d.departmentName,
                l.streetAddress, l.city, c.countryName, r.regionName
            )
            FROM Employee e
            JOIN e.department d
            JOIN d.location l
            JOIN l.country c
            JOIN c.region r
            """;

        TypedQuery<EmployeeDTO> query = entityManager.createQuery(jpql, EmployeeDTO.class);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<EmployeeDTO> content = query.getResultList();

        // Total count query
        String countJpql = """
            SELECT COUNT(e)
            FROM Employee e
            JOIN e.department d
            JOIN d.location l
            JOIN l.country c
            JOIN c.region r
            """;

        Long total = entityManager.createQuery(countJpql, Long.class).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Method 6: Slice 사용 - 다음 페이지 존재 여부만 확인 (count 쿼리 없음)
     */
    public Slice<EmployeeDTO> findWithSlice(Pageable pageable) {
        String jpql = """
            SELECT new org.example.jpapaging.dto.EmployeeDTO(
                e.firstName, e.lastName, d.departmentName,
                l.streetAddress, l.city, c.countryName, r.regionName
            )
            FROM Employee e
            JOIN e.department d
            JOIN d.location l
            JOIN l.country c
            JOIN c.region r
            """;

        TypedQuery<EmployeeDTO> query = entityManager.createQuery(jpql, EmployeeDTO.class);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize() + 1); // 한 개 더 가져와서 다음 페이지 존재 여부 확인

        List<EmployeeDTO> content = query.getResultList();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content = content.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * Method 7: Pageable with Sort 사용
     */
    public Page<EmployeeDTO> findWithPageableAndSort(Pageable pageable) {
        String jpql = """
            SELECT new org.example.jpapaging.dto.EmployeeDTO(
                e.firstName, e.lastName, d.departmentName,
                l.streetAddress, l.city, c.countryName, r.regionName
            )
            FROM Employee e
            JOIN e.department d
            JOIN d.location l
            JOIN l.country c
            JOIN c.region r
            ORDER BY e.employeeId
            """;

        TypedQuery<EmployeeDTO> query = entityManager.createQuery(jpql, EmployeeDTO.class);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<EmployeeDTO> content = query.getResultList();

        String countJpql = """
            SELECT COUNT(e)
            FROM Employee e
            JOIN e.department d
            JOIN d.location l
            JOIN l.country c
            JOIN c.region r
            """;

        Long total = entityManager.createQuery(countJpql, Long.class).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Method 8: Cursor 기반 페이징 (Keyset Pagination)
     */
    public List<EmployeeDTO> findWithCursorPagination(Long lastEmployeeId, int limit) {
        String jpql = """
            SELECT new org.example.jpapaging.dto.EmployeeDTO(
                e.firstName, e.lastName, d.departmentName,
                l.streetAddress, l.city, c.countryName, r.regionName
            )
            FROM Employee e
            JOIN e.department d
            JOIN d.location l
            JOIN l.country c
            JOIN c.region r
            WHERE e.employeeId > :lastId
            ORDER BY e.employeeId
            """;

        TypedQuery<EmployeeDTO> query = entityManager.createQuery(jpql, EmployeeDTO.class);
        query.setParameter("lastId", lastEmployeeId);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    /**
     * Method 9: Native Query with Pageable
     */
    public Page<EmployeeDTO> findWithNativeQueryAndPageable(Pageable pageable) {
        String sql = """
            SELECT E.FIRST_NAME, E.LAST_NAME, D.DEPARTMENT_NAME,
                   L.STREET_ADDRESS, L.CITY, C.COUNTRY_NAME, R.REGION_NAME
            FROM EMPLOYEES E
            INNER JOIN DEPARTMENTS D ON D.DEPARTMENT_ID = E.DEPARTMENT_ID
            INNER JOIN LOCATIONS L ON L.LOCATION_ID = D.LOCATION_ID
            INNER JOIN COUNTRIES C ON C.COUNTRY_ID = L.COUNTRY_ID
            INNER JOIN REGIONS R ON R.REGION_ID = C.REGION_ID
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter(1, (int) pageable.getOffset())
                .setParameter(2, pageable.getPageSize())
                .getResultList();

        List<EmployeeDTO> content = results.stream()
                .map(row -> new EmployeeDTO(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        (String) row[6]
                ))
                .toList();

        // Count query
        String countSql = """
            SELECT COUNT(*)
            FROM EMPLOYEES E
            INNER JOIN DEPARTMENTS D ON D.DEPARTMENT_ID = E.DEPARTMENT_ID
            INNER JOIN LOCATIONS L ON L.LOCATION_ID = D.LOCATION_ID
            INNER JOIN COUNTRIES C ON C.COUNTRY_ID = L.COUNTRY_ID
            INNER JOIN REGIONS R ON R.REGION_ID = C.REGION_ID
            """;

        Long total = ((Number) entityManager.createNativeQuery(countSql).getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }
}