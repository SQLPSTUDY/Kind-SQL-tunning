package org.example.jpapaging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jpapaging.dto.EmployeeDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcPagingService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<EmployeeDTO> rowMapper = new RowMapper<EmployeeDTO>() {
        @Override
        public EmployeeDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EmployeeDTO(
                    rs.getString("FIRST_NAME"),
                    rs.getString("LAST_NAME"),
                    rs.getString("DEPARTMENT_NAME"),
                    rs.getString("STREET_ADDRESS"),
                    rs.getString("CITY"),
                    rs.getString("COUNTRY_NAME"),
                    rs.getString("REGION_NAME")
            );
        }
    };

    /**
     * Method 1: ROWNUM을 사용한 Oracle 전통 방식
     */
    public List<EmployeeDTO> findWithRownum(int offset, int limit) {
        String sql = """
            SELECT *
            FROM (
                SELECT ROWNUM NO, A.*
                FROM (
                    SELECT E.FIRST_NAME, E.LAST_NAME, D.DEPARTMENT_NAME,
                           L.STREET_ADDRESS, L.CITY, C.COUNTRY_NAME, R.REGION_NAME
                    FROM REGIONS R, COUNTRIES C, LOCATIONS L, DEPARTMENTS D, EMPLOYEES E
                    WHERE D.DEPARTMENT_ID = E.DEPARTMENT_ID
                      AND L.LOCATION_ID = D.LOCATION_ID
                      AND C.COUNTRY_ID = L.COUNTRY_ID
                      AND R.REGION_ID = C.REGION_ID
                ) A
                WHERE ROWNUM <= ?
            )
            WHERE NO >= ?
            """;

        return jdbcTemplate.query(sql, rowMapper, offset + limit, offset + 1);
    }

    /**
     * Method 2: OFFSET FETCH 사용 (Oracle 12c+)
     */
    public List<EmployeeDTO> findWithOffsetFetch(int offset, int limit) {
        String sql = """
            SELECT E.FIRST_NAME, E.LAST_NAME, D.DEPARTMENT_NAME,
                   L.STREET_ADDRESS, L.CITY, C.COUNTRY_NAME, R.REGION_NAME
            FROM REGIONS R, COUNTRIES C, LOCATIONS L, DEPARTMENTS D, EMPLOYEES E
            WHERE D.DEPARTMENT_ID = E.DEPARTMENT_ID
              AND L.LOCATION_ID = D.LOCATION_ID
              AND C.COUNTRY_ID = L.COUNTRY_ID
              AND R.REGION_ID = C.REGION_ID
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """;

        return jdbcTemplate.query(sql, rowMapper, offset, limit);
    }

    /**
     * Method 3: ROW_NUMBER() 윈도우 함수 사용
     */
    public List<EmployeeDTO> findWithRowNumber(int offset, int limit) {
        String sql = """
            SELECT *
            FROM (
                SELECT E.FIRST_NAME, E.LAST_NAME, D.DEPARTMENT_NAME,
                       L.STREET_ADDRESS, L.CITY, C.COUNTRY_NAME, R.REGION_NAME,
                       ROW_NUMBER() OVER (ORDER BY E.EMPLOYEE_ID) AS RN
                FROM REGIONS R, COUNTRIES C, LOCATIONS L, DEPARTMENTS D, EMPLOYEES E
                WHERE D.DEPARTMENT_ID = E.DEPARTMENT_ID
                  AND L.LOCATION_ID = D.LOCATION_ID
                  AND C.COUNTRY_ID = L.COUNTRY_ID
                  AND R.REGION_ID = C.REGION_ID
            )
            WHERE RN > ? AND RN <= ?
            """;

        return jdbcTemplate.query(sql, rowMapper, offset, offset + limit);
    }

    /**
     * Method 4: ANSI 조인 사용 + OFFSET FETCH
     */
    public List<EmployeeDTO> findWithAnsiJoin(int offset, int limit) {
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

        return jdbcTemplate.query(sql, rowMapper, offset, limit);
    }
}