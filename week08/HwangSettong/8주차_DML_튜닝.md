# 6장 DML 튜닝

## 6.1 기본 DML 튜닝

### 6.1.1 DML 성능에 영향을 미치는 요소

- 인덱스
    
    인덱스는 정렬된 자료구조이므로 INSERT, DELETE 시 과정이 더 복잡.
    특히, UPDATE 시에는 삭제 후 삽입하는 방식으로처리함. 
    
- 무결성제약
    
    DBMS에서 PK, FK, Check, Not Null같은 제약을 설정하여 데이터 무결성을 지켜낼 수 있음.
    
    그 중 특히 PK,FK는 실제 데이터를 조회해야 하기 때문에 성능에 더 큰 영향을 미침.
    
- 조건절
    
    2장, 3장의 인덱스 튜닝 원리 적용
    

- 서브쿼리
    
    4장의 조인 튜닝 (4.4 서브쿼리 조인) 원리 적용
    
- Redo 로깅
    
    DML을 수행할 때 마다 Redo 로그를 생성해야 하므로 성능에 영향을 미침.
    
    (때문에 INSERT 작업에 대해서는 Redo 로깅 생략 기능 제공)
    
- Undo 로깅 (Rollback)
    
    DML을 수행할 때 마다 Undo 로그를 생성해야 하므로 성능에 영향을 미침.
    

- Lock
    
    Lock을 자주, 길게, 레벨을 높인다면, 데이터 품질을 높아지지만 DML 성능 느려짐.
    고성능 고품질을 위해서는 동시성 제어가 필요. (*6.4절 참고)
    
- Commit
    
    DML 자체는 데이터를 변경하고 Redo/Undo에 기록됨. 
    Commit 시점에서 Redo 로그를 디스크에 기록하고 트랜잭션 종료. 
    이 작업은 디스크 I/O. 너무 자주 Commit하면 오히려 성능 저하될 수 있음.

<br>
<br>

### 6.1.2 데이터베이스 Call과 성능

**⌾ SQL 실행 단계에 따른 Call 구분**

1. Parse Call
    
    SQL 파싱과 최적화를 수행하는 단계.
    SQL과 실행계획을 라이브러리 캐시에서 찾으면 최적화 단계는 생략 가능
    
2. Execute Call
    
    SQL 실행 단계.
    
    이 단계에서 DML 과정이 끝나지만, SELECT 문은 Fetch 단계를 거쳐야 함.
    
3. Fetch Call
    
    데이터를 읽어서 사용자에게 결과 집합을 전송하는 단계.
    
    전송할 데이터가 많으면 Fetch Call이 여러 번 발생.
    

<br>

**⌾ 발생 지점에 따른 Call 구분**

<img width="708" height="299" alt="image" src="https://github.com/user-attachments/assets/2c88f890-7387-4870-9aa4-4122840b28cf" />


- User Call
    
    네트워크를 경유해 DBMS 외부로부터 인입되는 Call.
    
    3-tier 아키텍처에서는 WAS나 AP서버에서 발생하는 Call.
    
- Recursive Call
    
    DBMS 내부에서 발생하는 Call.
    
    예: 데이터 딕셔너리 조회, 인덱스 관리, 사용자 정의 함수/프로시저/트리거 내부 SQL
    

<br>


**⌾ 절차적 루프 처리**

```sql
FOR rec IN (SELECT empno, sal FROM emp) LOOP
    UPDATE emp SET sal = sal * 1.1 WHERE empno = rec.empno;
END LOOP;
```

위 쿼리는 루프문을 돌며 Recursive Call이 반복 실행됨.

위 쿼리의 루프 처리를 Java에서 한다면? 네트워크를 경유하는 User Call이 많아지게 되면서 성능에 더 안좋은 영향 미침.


<br>


**⌾ One SQL의 중요성**

```sql
insert into target
select * from source;
```

만약 source 테이블에 100만건의 데이터가 있더라도 단 한번의 Call로 처리하여 수행 시간을 줄일 수 있음.

이처럼 One SQL로 처리할 수 있도록 하고 Insert Into Select , 수정가능 조인 뷰(*6.1.5절), Merge문(*6.1.6절) 등을 활용.


<br>
<br>


### 6.1.3 Array Processing 활용

실무에서는 로직이 복잡한 경우가 많아 절차적 프로그램을 One SQL로 처리하기 쉽지 않다.

그럴 때 Array Processing 기능을 활용하여 Call을 줄일 수 있다.

```sql
FOR rec IN (SELECT empno, sal FROM emp) LOOP
    UPDATE emp SET sal = sal * 1.1 WHERE empno = rec.empno;
END LOOP;
```

이 코드는 행 단위 반복 처리(절차적 루프 처리)를 위해  Recursive Call을 여러번 하여 성능이 비효율적임.

PL/SQL 에서 Array Processing 처리

```sql
DECLARE
    TYPE emp_arr IS TABLE OF emp.empno%TYPE;
    emp_ids emp_arr;
BEGIN
    SELECT empno BULK COLLECT INTO emp_ids FROM emp;

    FORALL i IN emp_ids.FIRST .. emp_ids.LAST
        UPDATE emp SET sal = sal * 1.1 WHERE empno = emp_ids(i);
END;
```

Java(JDBC)에서 Array Processing 처리

```sql
PreparedStatement pstmt = conn.prepareStatement(
    "UPDATE emp SET sal = sal * 1.1 WHERE empno = ?"
);

for(int empno : empList) {
    pstmt.setInt(1, empno);
    pstmt.addBatch();
}

pstmt.executeBatch();   // 딱 1번의 User Call로 전송됨
```

<br>
<br>




### 6.1.4 인덱스 및 제약 해제를 통한 대량 DML 튜닝

대량 DML 수행 시, 인덱스와 제약조건이 많으면 성능 저하가 발생함.

따라서 일시적으로 제약/인덱스를 해제하거나 옵션을 활용하면 성능을 크게 개선할 수 있음.

- **PK 제약에 Unique 인덱스를 사용한 경우**
    
    PK는 기본적으로 Unique 인덱스를 생성함. 대량 DML 시 매번 Unique 체크와 인덱스 유지가 비용 발생.
    
    대량 INSERT 시 PK 제약과 인덱스를 잠시 제거 후, 완료 후 다시 생성.
    
    이때 `NOVALIDATE 옵션` 사용하면 기존 데이터에 대해서는 제약 검증을 생략하고, 새로 들어오는 데이터만 체크 (* 데이터 무결성에 대한 확신이 있을 때 사용하기)
    
- **PK 제약에 Non-Unique 인덱스를 사용한 경우**
PK 인덱스가 UNUSABLE 상태가 되면 INSERT 불가
****PK 인덱스가 UNUSABLE 상태를 유지하면서 데이터를 넣으려면 PK 인덱스에 `Non-Unique 인덱스`를 활용.
- PK 제약 비활성화 상태면 무결성 검증을 안할텐데 PK 인덱스 UNUSABLE이면 INSERT 불가라는게 이해 안됨
    - Oracle은 UNUSABLE 인덱스를 가진 상태에서 테이블에 INSERT를 할 수 있는 경우가 있지만, 조건이 있습니다.
        - **UNIQUE 인덱스**가 UNUSABLE 상태이면, **해당 인덱스를 기반으로 INSERT가 제한될 수 있음**
        - 왜? Oracle은 UNIQUE 인덱스가 UNUSABLE 상태면 **중복 체크를 수행할 수 없기 때문**입니다.
            
            → Oracle이 INSERT를 진행할 때 **인덱스를 사용해야 하는데 인덱스가 UNUSABLE이라 처리 불가**
            
            → 그래서 PK 인덱스가 UNUSABLE이면 PK 제약 비활성화에도 INSERT가 **에러 발생 가능**
            
    - 반면 **Non-Unique 인덱스**는 UNUSABLE 상태여도 중복 체크가 필요 없기 때문에 INSERT 가능.
    
- PK에 non unique 인덱스 제약으로 설정가능?
    
    ```sql
    -- 책 423
    create index target_pk on target(no, empno); -- non unique index
    
    alter table target add
    constraint target_pk primary key (no, empno)
    using index target_pk;
    ```
    

<br>
<br>

### 6.1.5 수정가능 조인 뷰

수정 가능 조인 뷰: 뷰를 통해 `INSERT`, `UPDATE`, `DELETE`가 가능한 조인 뷰.

단순 SELECT용 조인 뷰와 달리, 조건을 만족하면 뷰를 통해 실제 테이블 데이터를 변경 가능.

<br>

**⌾  수정 가능 조건**

1. 뷰가 하나의 기본 테이블만 수정하는 경우
    - 여러 테이블 조인 시, 수정 가능한 테이블이 1개여야 함
2. DISTINCT, GROUP BY, 집계 함수, UNION, 서브쿼리, ROWNUM 등은 없어야 함
3. 조인 시 OUTER JOIN 포함하면 수정 불가
4. 기본 키 포함: 변경 대상 테이블 PK 또는 NOT NULL 컬럼은 반드시 존재
5. 뷰 컬럼에 계산식, 함수, 서브쿼리 포함 금지

예시)

```sql
CREATE VIEW emp_dept_v AS
SELECT e.empno, e.ename, e.deptno, d.dname
FROM emp e
JOIN dept d ON e.deptno = d.deptno;

-- emp 테이블만 변경 가능
UPDATE emp_dept_v
SET ename = '홍길동'
WHERE empno = 1001;

-- One SQL (MERGE)
MERGE INTO emp_dept_v v
USING (SELECT 1001 empno, '김철수' ename FROM dual) s
ON (v.empno = s.empno)
WHEN MATCHED THEN UPDATE SET v.ename = s.ename;
```

- 키보존 테이블: 조인 후 PK가 유지되는 테이블 → 수정 가능
- 비키보존 테이블: 조인 후 PK가 깨지는 테이블 → 수정 불가

Oracle은 뷰를 통해 데이터를 변경할 때, 기본키가 보존되는 테이블만 안전하게 한 행을 특정 가능

PK가 깨지면 어느 테이블의 어느 로우를 변경해야 하는지 알 수 없으므로 비키보존 테이블은 수정 불가

EMP 테이블는 키보존 테이블, DEPT 테이블은 비키보존 테이블.

<br>
<br>

### 6.1.6 MERGE 문 활용

oracle 9i부터 가능

```sql
merge into customer t using customer_delta s on (t.cust_id = s.cust_id)
where matched then update
	set t.cust_nm=s.cust_nm, t.email=s.email 
when not matched then insert
	(cust_id, cust_nm, email, tel_no, addr, reg_dt) values
	(s.cust_id, s.cust_nm, s.email, s.tel_no, s.addr, s.reg_dt);
```

선택적으로 update와 insert를 처리할 수 있음. → 수정가능 조인 뷰 기능 대체 가능.

<br>
<br>
<br>


## 6.2 Direct Path I/O 활용

### 6.2.1 Direct Path I/O

일반적인 블록 I/O는 DB 버퍼캐시를 경유함. (반복적인 I/O Call을 줄여 시스템 전반적인 성능을 높이기 위함)

하지만, 대량 데이터를 읽고 쓸 때 건건이 버퍼캐시를 탐색한다면 개별 프로그램 성능에는 오히려 좋지 않음. 버퍼캐시 히트율이 낮기 때문.

때문에 버퍼캐시를 경유하지 않고 데이터 블록을 읽고 쓸 수 있는 기능을 제공함 = `Direct Path I/O` 

<br>

**⌾ Direct Path I/O가 작동하는 경우**

1. 병렬 쿼리로 full scan 수행할 때
    
    ```sql
    -- 병렬 쿼리 예시 ) parallel 힌트 사용 
    select /*+ full(t) parallel(t 4) */ * 
      from big_table t;
    
    -- 병렬 쿼리 예시 ) parallel_index 힌트 사용
    select /*+ index_ffs(t bing_table_x1) parallel(t bing_table_x1 4) */  count(*)
      from big_table t;
    ```
    
2. 병렬 DML을 수행할 때 (*6.2.3절)
3. Direct Path Insert 수행할 때 (*6.2.2절)
4. Temp 세그먼트 블록들을 읽고 쓸 때
5. direct 옵션을 지정하고 export할 때
6. nocache 옵션을 지정한 LOB 컬럼을 읽을 때

<br>
<br>

### 6.2.2 Direct Path Insert

**⌾  일반적인 Insert vs Direct Path Insert**

| **순서** | **일반적인 Insert** | **Direct Path Insert** |
| --- | --- | --- |
| 1 | FreeList에서 데이터를 입력할 수 있는 블록을 찾음 | FreeList를 찾아보지 않고, 맨 뒤에 순차적으로 쌓음 |
| 2 | FreeList에서 할당받은 블록을 버퍼 캐시에서 찾음 | 블록을 버퍼 캐시에서 탐색하지 않음 |
| 3 | 버퍼 캐시에 없으면 데이터 파일에서 읽어 버퍼 캐시에 적재한다. | 버퍼 캐시에 적재하지 않고, 데이터 파일에 직접 기록 |
| 4 | Undo 기록 | Undo 기록하지 않음 |
| 5 | Redo 기록 | Redo를 하지 않도록 할 수 있음 |

버퍼 캐시를 참조하지 않음 + Undo, Redo를 로깅 생략 가능 =  매우 빠름

<br>

**⌾  Direct Path Insert주의할 점**

1. Exclusive 모드 TM Lock(*6.4.1절)이 걸리게 됨 .
    
    커밋 전까지다른 트랜잭션은 해당 테이블에 DML을 수행할 수 없으므로, 트랜잭션이 빈번한 곳에는 사용 X.
    
2. 테이블에 여유 공간이 있어도 재활용 하지 않음. 무조건 맨 뒤에 순차적으로 쌓음.

<br>
<br>

### 6.2.3 병렬 DML

```sql
-- 병렬 DML 활성화 
ALTER SESSION SET ENABLE PARALLEL DML

-- 추가
INSERT /*+ parallel(c 4) */ 고객 c INTO 고객 c
select /*+ full(t) parallel(t 4) */ * from 외부가입고객 t;

-- 변경
UPDATE /*+ full(c) parallel(c 4) */ 고객 c set 고객상태 = 'WD'
WHERE 최종거래일시 < '20200101';

-- 삭제
DELETE /*+ full(c) parallel(c 4) */ FROM 고객 c
WHERE 탈퇴일시 < '20200101';
```

병렬 DML 주의할 점 :  Exclusive 모드 TM Lock(*6.4.1절)이 걸리게 됨 .

<br>
<br>
<br>

## 6.3 파티션을 활용한 DML 튜닝

### 6.3.1 테이블 파티션

**⌾  파티션이 필요한 이유**

- 관리적 측면 : 파티션 단위 백업, 추가, 삭제 , 변경 → 가용성 향상
- 성능적 측면 : 파티션 단위 조회 및 DML, 경합 또는 부하 분산

<br>

**⌾  Range 파티션**

값의 범위에 따라 파티셔닝

```sql
create table (주문번호 number, 주문일자 varchar2(8), 고객ID varchar2(5), 배송일자 varchar2(8), 주문금액 number)
partition by range(주문일자) (
	  partition P2017_Q1 values less than ('20170401')
	, partition P2017_Q2 values less than ('20170701')
	, partition P2017_Q3 values less than ('20170101')
	, partition P2017_Q4 values less than ('20180101')
	, partition P2018_Q1 values less than ('20180401')
	, partition P9999_MX values less than (MAXVALUE)
);
```

<br>

**⌾  Hash 방식** 

파티션 키 값을 해시 함수에 입력. 파티션 개수를 사용자가 결정

```sql
create table ...

partition by hash(고객ID) partitions 4;
```

<br>

**⌾  List 방식**

그룹핑 기준에 따라 데이터를 분할 저장하는 방식

```sql
create table ...

partition by list(지역분류) (
	partition P_지역1 values ('서울')
	, partition P_지역2 values ('경기', '인천')
	, partition P_기타 values (DEFALUT)
);
```

<br>
<br>

### 6.3.2 인덱스 파티션

**⌾  테이블과 인덱스 파티션 구분**

<img width="708" height="507" alt="image" src="https://github.com/user-attachments/assets/de8c42c2-c36e-4e99-b355-62a7be352ccc" />


<br>

**⌾  로컬 파티션 인덱스** 

- 테이블 파티션과 1:1로 매핑되는 인덱스 파티션. 즉 테이블 파티션 개수만큼 인덱스 파티션이 존재함.
- 테이블 파티션의 키가 동일하게 로컬 인덱스에 상속됨. → 테이블 파티션의 키 = 인덱스 파티션의 키
- 오라클에서 관리해주므로 테이블 파티션 구성을 변경하더라도 서비스에 영향을 거의 주지 않음.

<br>

**⌾  글로벌 파티션 인덱스** 

- 테이블 파티션과는 별개로 인덱스의 파티션을 다른 방식으로 만듦.
- 테이블 파티션이 없어도 생성 가능.
- 테이블 파티션 구성을 변경하는 순간 글로벌 인덱스를 재생성해 줘야 하므로 서비스를 중단해야 함.

<br>

**⌾  비파티션 인덱스** 

- 파티셔닝 하지 않은 인덱스로 일반적인 인덱스.
- 테이블의 파티션 구성을 변경하는 순간 비파티션 인덱스는 재생성되어야 하며 그동안 서비스가 중단됨.

<br>

**⌾  Prefixed vs Nonprefixed**

- Prefixed : 인덱스 파티션 키 컬럼이 인덱스 컬럼 구성에서 왼쪽 선두에 위치할 경우
- Nonprefixed : 인덱스 파티션 키 컬럼이 인덱스 컬럼 구성에서 왼쪽 선두에 위치하지 않거나 인덱스 컬럼에 아예 속하지 않는 경우
- Prefixed/Nonprefixed 기준 파티션 인덱스 유형
    - 로컬 Prefixed
    - 로컬 Nonprefixed
    - 글로벌 Prefixed
    - 비파티션 인덱스

<br>
<br>

### 6.3.3 파티션을 활용한 대량 UPDATE 튜닝

! 대량 데이터 업데이트 시 손익 분기점은 총 5% 이하로 보고, 이를 넘는다면 인덱스 없이 작업한 후 재생성하는게 더 빠름.

<br>

**⌾  파티션 Exchange를 이용한 대량 데이터 변경**

1. 임시테이블 생성
2. 데이터를 읽어 임시테이블에 입력하면서 원하는 부분의 값을 수정
3. 임시테이블에 원본 테이블과 같은 구조로 인덱스 생성
(가능하다면 nologging 모드로 하고 나중에 다시 logging 모드로 전환)
4. 대상 파티션과 임시 테이블을 Exchange
5. 임시테이블 Drop

<br>
<br>

### 6.3.4  파티션을 활용한 대량 DELETE 튜닝

**⌾  DELETE 과정**

1. 테이블 레코드 삭제
2. 테이블 레코드 삭제에 대한 Undo Logging
3. 테이블 레코드 삭제에 대한 Redo Logging
4. 인덱스 레코드 삭제
5. 인덱스 레코드 삭제에 대한 Undo Logging
6. 인덱스 레코드 삭제에 대한 Redo Logging
7. 2번 5번 Undo Logging에 대한 Redo Logging

→ 때문에 DELETE 는 느림.

<br>

**⌾  파티션 Drop**

- **파티션에 할당된 데이터를 모두 삭제할 때**
    
    ```sql
    ALTER TABLE 테이블명 DROP PARTITION p201412;
    ```
    

<br>

**⌾   파티션 Truncate**

- **파티션에 할당된 데이터를 소수만 삭제할 때**
    
    ```sql
    DELETE FROM 테이블명
    WHERE 거래일자 < '20210101' -- 파티션 조건
    AND 상태 = '1' -- 이외 조건
    ```
    

- **파티션에 할당된 데이터를 대다수 삭제할 때**
    1. 임시 테이블 생성하고 남길 데이터만 복제
    (가능하면 nologging 모드로 생성)
    2. 삭제 대상 테이블 파티션을 truncate
    3. 임시 테이블의 복제 데이터를 원본 테이블에 입력
    4. 임시테이블 Drop

```sql
CREATE TABLE 임시 nologging AS SELECT * FROM 실제테이블 WHERE 거래일자 < '20210101' AND 상태 = '1';
ALTER TABLE 실제테이블 TRUNCATE PARTITION p202012;
INSERT INTO 실제테이블 SELECT * FROM 임시;
DROP TABLE 임시;
```

<br>
<br>

### 6.3.5 파티션을 활용한 대량 INSERT 튜닝

- **비파티션 테이블**
    1. (가능하면 작업 대상 테이블 파티션을 nologging 모드로 전환)
    2. 인덱스를 Unusable 상태로 전환
    3. (가능하면 Direct Path Insert 방식으로) 대량 데이터 입력
    4. (가능하면 nologging 모드로) 인덱스 재생성
    5. (nologging모드로 작업했다면 logging 모드로 전환)

- **파티션 테이블**
    1. (가능하면 작업 대상 테이블 파티션을 nologging 모드로 전환)
    2. 작업 대상 테이블 파티션과 매칭되는 인덱스 파티션을 Unusable 상태로 전환
    3. (가능하면 Direct Path Insert 방식으로) 대량 데이터 입력
    4. (가능하면 nologging 모드로) 인덱스 파티션 재생성
    5. (nologging모드로 작업했다면 작업파티션을 logging 모드로 전환)

<br>
<br>
<br>



## 6.4 Lock과 트랜잭션 동시성 제어

### 6.4.1 오라클 Lock

**⌾   DML Lock** 

- **DML 로우 Lock**
    
    다중 트랜잭션이 동시에 같은 로우를 변경하는 것을 방지함.
    
    오라클은 SELECT끼리, DML과 SELECT 간에 락을 사용하지 않음.
    
    DML 진행중일 때에는 DML 로우 LOCK에 의해 성능저하가 발생할 수 있으므로 커밋 시점 조절
    
- **DML 테이블 Lock**
    
    DML 로우 Lock 설정 전 테이블 Lock(TM Lock)을 설정함.
    
    테이블 전체에 Lock이 걸리는 의미가 아님. 테이블 Lock에는 여러 가지 모드가 있고, 어떤 모드를 사용했는지에 따라 후행 트랜잭션이 수행할 수 있는 작업의 범위가 결정됨.
    
    ```sql
    -- Lock 해제까지 기다리기
    SELECT * FROM T FOR UPDATE
    
    -- 일정시간만 기다리고 작업 포기
    SELECT * FROM T FOR UPDATE WAIT 3
    
    -- 기다리지않고 작업 포기
    SELECT * FROM T FOR UPDATE NOWAIT
    ```
    

<br>

**⌾   Commit**

- 블로킹 : 선행 트랜잭션에서 설정한 Lock 때문에 후행 트랜잭션이 작업을 진행 못하고 멈춰있음
- 교착상태(Deadlock) : 두 트랜잭션이 각각 특정 리소스에 Lock을 설정한 상태에서 맞은편 트랜잭션이 Lock을 설정한 리소스에 다시 Lock을 설정하려고 하는 상황
    
    이를 먼저 인지한 트랜잭션이 문장 수준 롤백을 진행
    

→ 이를 풀기 위해서 적절한 시점에 commit 해야함

- 커밋 옵션
    1. WAIT(Default) : LGWR가 로그 버퍼를 파일에 기록했다는 완료 메세지를 받을 때까지 대기
    2. NOWAIT : LGWR의 완료 메세지를 기다리지 않고 바로 다음 트랜잭션을 진행
    3. IMMEDIATE(Default) : 커밋 명령을 받을 때마다 LGWR가 로그 버퍼를 파일에 기록
    4. BATCH : 세션 내부에 트랜잭션 데이터를 일정량 버퍼링 했다가 일괄 처리

<br>
<br>

### 6.4.2 트랜잭션 동시성 제어

- **비관적 동시성 제어**
    
    동시에 같은 데이터를 수정한다고 가정
    
    데이터를 읽는 시점에 Lock을 걸고 처리가 완료될 때 까지 유지.
    
- **낙관적 동시성 제어**
    
    동시에 같은 데이터를 수정하지 않을거라고 가정
    
    락을 바로 걸지 않고, 트랜잭션 커밋 시점에서 충돌 여부를 체크.
    
    충돌이 발생하면 롤백하거나 재시도.
 
