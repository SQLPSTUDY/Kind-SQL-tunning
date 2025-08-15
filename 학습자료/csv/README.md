# Import 유의사항
- 제약 조건으로 인해서 다음 순서를 지켜 Import 할 것을 권장

## 1. 참조 테이블
```sql
-- 1. titles (다른 테이블에서 참조됨)
titles.csv → titles 테이블
-- 2. departments (다른 테이블에서 참조됨)
departments.csv → departments 테이블
```

## 2. 기본 데이터 테이블
```sql
-- 3. employees (titles를 참조, 다른 테이블에서 참조됨)
employees.csv → employees 테이블
```

### 주의 사항
- 데이터 형식으로 인해서 날짜 부분 import error 발생
- 아래와 같이 타입 해제 후 다시 import 한 후 타입 재지정
    ```sql
    -- 날짜 컬럼을 임시로 VARCHAR로 변경
    ALTER TABLE employees MODIFY birth_date VARCHAR2(20);
    ALTER TABLE employees MODIFY hire_date VARCHAR2(20);

    -- CSV Import 수행

    -- Import 후 다시 DATE로 변환
    ALTER TABLE employees ADD birth_date_new DATE;
    ALTER TABLE employees ADD hire_date_new DATE;

    UPDATE employees 
    SET birth_date_new = TO_DATE(birth_date, 'MM/DD/YYYY'),
        hire_date_new = TO_DATE(hire_date, 'MM/DD/YYYY');

    ALTER TABLE employees DROP COLUMN birth_date;
    ALTER TABLE employees DROP COLUMN hire_date;
    ALTER TABLE employees RENAME COLUMN birth_date_new TO birth_date;
    ALTER TABLE employees RENAME COLUMN hire_date_new TO hire_date;
    ```

### 3. 연결 테이블들
```sql
-- 4. salaries (employees를 참조)
salaries.csv → salaries 테이블

-- 5. dept_emp (employees와 departments를 참조)
dept_emp.csv → dept_emp 테이블

-- 6. dept_manager (employees와 departments를 참조)
dept_manager.csv → dept_manager 테이블
```