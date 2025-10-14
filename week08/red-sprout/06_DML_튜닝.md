# 기본 DML 튜닝

## DML 성능에 영향을 미치는 요소

- 인덱스, 무결성 제약, 조건절, 서브쿼리, Redo 로깅, Undo 로깅, Lock, 커밋

### 인덱스와 DML 성능

- 테이블은 Freelist를 통해 입력 블록을 할당
    - Freelist - 테이블마다 데이터 입력이 가능한 블록 목록을 관리
    - 인덱스는 수직적 탐색을 통해 입력, 변경, 삭제할 블록을 찾아야 하는 복잡한 과정이 존재
    - 핵심 트랜잭션 테이블에서 인덱스를 하나라도 줄이면 TPS가 향상

### 무결성 제약과 DML 성능

- PK, FK 제약은 성능에 큰 영향
    - 빅테크에서는 FK 제약을 전혀 사용하지 않음!!!
    - FK 제약의 장점
        - 데이터 무결성 보장
        - 관계 명확화
        - 부모 데이터가 삭제되면 자식 데이터도 삭제되는 자동 연계 작업 가능(ON DELETE CASCADE)
        - 쿼리 작성 용이
    - FK 제약의 단점
        - 성능
        - 관리 복잡성, 특히 순환 참조에서
        - 제약 위반 조건
        - 유연성 부족
    - FK가 없으면 애플리케이션 레벨에서 모든 것을 처리
        - 데이터 삽입 시 관계를 확인 - 자식 데이터 추가 전 부모 데이터 존재여부 확인
        - 삭제시 연관 데이터 처리 - 부모 데이터 삭제시 자식 데이터 수동으로 삭제
        - 트랜잭션 활용 - 여러 작업이 연결되어 있다면 하나의 트랜잭션으로 관리

### 조건절과 DML 성능

- SELECT 절과 비슷하기에 인덱스 튜닝 원리 적용

### 서브쿼리와 DML 성능

- SELECT 절과 비슷하기에 조인 튜닝 원리 적용

### Redo 로깅과 DML 성능

- 오라클은 데이터파일과 컨트롤 파일에 가해지는 모든 변경사항을 Redo 로그에 기록
- INSERT 작업에 대해 Redo 로깅 기능을 생략 가능 → `Direct Path Insert`

### Undo 로깅과 DML 성능

- Redo는 트랜잭션을 재현함으로써 과거를 현재 상태로 되돌리는데 사용
- Undo는 트랜잭션을 롤백함으로써 현재를 과거 상태로 되돌리는데 사용

### Lock과 DML 성능

- Lock을 필요 이상으로 자주, 길게 또는 레벨을 높일수록 DML 성능이 느려짐

### 커밋과 DML 성능

- DML을 끝내려면 커밋이 필요
- Lock에 의해 blocking이 되는 경우 이를 푸는 열쇠가 커밋
- 커밋은 무거운 작업
    - DB 버퍼캐시
        - 버퍼캐시에서 변경된 블록을 모아 주기적으로 데이터파일에 일괄 기록
        - 배치 처리는 DBWR(Database Writer) 프로세스가 맡음
    - Redo 로그버퍼
        - 버퍼 캐시 데이터가 유실되어도 Redo 복구를 통해 복구 가능
        - 기본적으로 파일 - Disk I/O
            - 로그버퍼에 기록 후 LGWR(Log Writer) 가 배치처리
    - 트랜잭션 데이터 저장 과정
        - Redo 로그에 변경 사항 기록
        - 버퍼블록에서 데이터를 변경(레코드 추가/수정/삭제)
        - 커밋
        - LGWR → Redo 로그 내용을 로그파일에 일괄 저장
        - DBWR → 변경된 버퍼블록들을 데이터파일에 일괄 저장
    - 커밋 == 저장
        - Sync 방식 - 저장 완료 전까지 프로세스가 다른 작업 진행 불가능
        - 커밋이 너무 적으면 - Undo 공간이 부족해져 시스템 장애 가능성
        - 커밋이 너무 많으면 - 프로그램 자체 성능 문제

## 데이터베이스 Call과 성능

### 데이터베이스 Call

- SQL 트레이스 Call
    - Parse Call : SQL 파싱과 최적화를 수행하는 단계
    - Execute Call : SQL을 실행하는 단계
    - Fetch Call : 데이터를 읽어서 사용자에게 결과 집합을 전송하는 과정
- Call 발생 위치
    - User Call : DBMS 외부로부터 오는 Call, 네트워크를 경유하기에 느림
    - Recursize Call : DBMS 내부로부터 오는 Call

⇒ Call 을 줄여서 One SQL 로 작성하자

### Array Processing

- 복잡한 비즈니스 로직을 포함하는 경우가 많음 → One SQL의 어려움 → Array Processing 으로 해결
- JDBC
    - `addBatch()` : Insert 할 값들을 배열에 저장
    - `executeBatch()` : 배열에 저장된 값을 한번에 Insert
- MyBatis
    - `SqlSessionFactory` 의 속성을 제어, `ExecutorType.BATCH` 적용
        
        ```java
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ```
        
    - `SqlSession` Insert 후 반드시 아래와 같은 작업 진행
        
        ```java
        sqlSession.flushStatements(); // 쿼리전송
        sqlSession.commit(); // 커밋
        sqlSession.close(); // 닫기
        sqlSession.clearCache(); // 캐시비우기
        ```
        
- JPA
    - `@Modifying` , `@Query` 를 통해서 쿼리를 직접 입력
    - JPA 1차 캐시 등 영속성 컨텍스트 무시 → 일관성 유지 X

## 인덱스 및 제약 해제를 통한 대량 DML 튜닝

- OLTP 환경에서는 부적합, batch 환경에서 사용

### PK 제약과 인덱스 해제

- PK - Unique Index
    
    ```sql
    -- pk drop
    alter table target modify constraint target_pk disable drop index;
    -- index unusable
    alter index target_x1 unusable;
    -- unusable 상태에서 데이터 입력시 설정, default true
    alter session set skip_unusable_indexes = true;
    -- pk 재생성, 무결성 체크 하지 않으려면 NOVALIDATE
    alter table target modify constraint target_pk enable NOVALIDATE;
    -- index 활성화
    alter index target_x1 rebuild;
    ```
    
- PK - Non-Unique Index
    
    ```sql
    -- pk 제약 해제
    alter table target modify constraint target_pk disable keep index;
    -- pk unusable
    alter index target_pk unusable;
    -- index unusable
    alter index target_x1 unusable;
    -- pk 활성화
    alter index target_pk rebuild;
    -- index 활성화
    alter index target_x1 rebuild;
    -- pk 재생성, 무결성 체크 하지 않으려면 NOVALIDATE
    alter table target modify constraint target_pk enable NOVALIDATE;
    ```
    

## 수정 가능 조인 뷰

- 조인 뷰 : FROM 절에 두 개 이상 테이블을 가진 뷰
- 1 : M 중 M 쪽 집합에서만 입력, 수정, 삭제 가능
- 1쪽 집합에 PK 제약 or Unique 인덱스 생성 필요
    - 1쪽 집합 : Non Key-Preserved Table
    - M쪽 집합 : Key-Preserved Table, 뷰에 rowid 를 제공하는 중복 값이 없는 테이블

## Merge 문 활용

```sql
merge into target t using source s on (t.id = s.id)
when matched then update
	set t.attr1 = s.attr1, ...
when not matched then insert
	(attr1, ...) values (s.attr1, ...)
```

- 저장하려는 레코드가 이미 있으면 UPDATE, 없으면 INSERT 와 같은 복합 작업을 하고자 할 때, MERGE 문을 사용하면 1회의 SQL 실행으로 처리 가능
- 수정 가능 조인 뷰와 달리, 검증용 SELECT 문을 따로 만들지 않아 직관적이지는 않은 단점 존재

# Direct Path I/O 활용

## Direct Path I/O

- 병렬 쿼리로 Full Scan 진행 시
    - `/*+ ... parallel(t 4) */` , `/*+ ... parallel_index(t_x1 4) */`
- 병렬 DML을 수행 시(Direct Path Read, Direct Path Insert)
- Temp 세그먼트 블록들을 읽고 쓸 때
- direct 옵션을 지정하고 export를 수행할 때
- nocache 옵션을 지정한 LOB 컬럼을 읽을 때

## Direct Path Insert

- Insert 가 느린 이유
    - 입력할 수 있는 블록을 Freelist 에서 찾음
        - **Freelist** : 테이블 HWM(High-Water Mark) 아래쪽에 있는 블록 중 데이터 입력이 가능한 블록을 관리한 목록
    - Freelist 에서 할당받은 블록을 버퍼캐시에서 찾음
    - 버퍼캐시에 없으면 File I/O
    - Insert 내용을 Undo 세그먼트에 기록
    - Insert 내용을 Redo 로그에 기록
- Direct Path Insert 입력 방법
    - `append` 힌트
    - `parallel` 힌트
    - direct 옵션 지정 후 SQL Loader(sqlldr)로 데이터 적재
    - CTAS(create table … as select) 문 수행
- Direct Path Insert 가 빠른 이유
    - Freelist 미참조
    - 버퍼캐시 미탐색, 미적재
    - 데이터 파일에 직접 기록
    - Undo, Redo 로깅 최소화
- 주의할 점
    - Exclusive 모드 TM Lock 발생 → 다른 트랜잭션은 해당 테이블에 DML을 수행 불가
    - 테이블 HWM(High-Water Mark) 바깥 영역에 입력, 테이블에 여유 공간이 있어도 재활용 X

## 병렬 DML

- 병렬 DML 활성화 필요
    
    ```sql
    alter session enable parallel dml;
    ```
    
    - 비활성화 상태인데 힌트 사용시
        - 레코드 찾는 과정은 병렬
        - 추가/변경/삭제는 QC(Query Coordinator)가 혼자 담당 → 병목
    - 기본적인 오라클 DML 전략
        - Consistent 모드로 대상 레코드 검색
        - Current 모드로 추가/변경/삭제
- `append` 힌트를 지정하지 않아도 Direct Path Insert 방식

### 병렬 DML 이 잘 작동하는지 확인하는 방법

```
PX COORDINATOR
	...
	UPDATE
```

- COORDINATOR 아래쪽 UPDATE 발생 → 각 병렬 프로세스가 UPDATE 진행

```
UPDATE
	...
	PX COORDINATOR
```

- UPDATE 아래쪽 COORDINATOR → QC 가 직접 처리

# 파티션을 활용한 튜닝

## 파티셔닝(Partitioning)

- 물리적으로 여러 테이블로 분산 저장
- 사용자는 한 테이블로 접근하는 것과 같이 사용

### Range 파티션

- 특정 범위로 분할 시 사용, 주로 날짜에 사용

```sql
create table t (...)
partition by range(날짜) (
	partition P2025_Q1 values less than ('20250101'), 
	...
	partition P9999_MX values less than (MAXVALUE)	
);
```

### Hash 파티션

- 해시 함수를 사용해서 해시값이 같은 데이터를 같은 세그먼트에 저장
- 사용자는 파티션 개수만 결정하는 방식
- 변별력이 좋고 데이터 분포가 고른게 효과적
- 해시 함수를 만드는 id → partition key

```sql
create table t (...)
partition by hash(id) partitions 4;
```

### 리스트 파티션

- 사용자 정의 그룹핑 기준
- 데이터 값이 특정 목록에 포함된 경우

```sql
create table t (...)
partition by list(지역) (
	partition P_지역1 values ('서울'), 
	partition P_지역2 values ('경기', '인천'), 
	...
	partition P9999_MX values (DEFAULT)	
);
```

### 샤딩(Sharding) - 참고

- 각 파티션들을 **서로 다른 서버**에 저장 → 부하 분산
- 응답 시간 개선, 전체 서비스 중단 방지, 효율적인 크기 조정
- 데이터 핫스팟 문제 발생
    - 데이터 분포가 고르지 않아 일부 샤드가 불균형
    - 최적의 샤드 키 설정 중요

## 인덱스 파티션

- 파티션 인덱스
    - 로컬 파티션 인덱스
    - 글로벌 파티션 인덱스
- 비파티션 인덱스

### 로컬 파티션 인덱스

- 인덱스 생성시 마지막에 `LOCAL` 옵션 추가
- 각 인덱스 파티션은 테이블 파티션을 그대로 상속 받음
- 테이블과 정확히 1:1 대응

### 글로벌 파티션 인덱스

- 인덱스 생성시 마지막에 `GLOBAL` 옵션 추가 후 파티션 정의
- 테이블 파티션 구성 변경시 Unusable, 인덱스 재생성 필요

### 비파티션 인덱스

- 일반 인덱스 생성과 동일

### Prefixed vs Nonprefixed

- Prefixed : 인덱스 파티션 키 컬럼이 인덱스의 첫번째 컬럼
- Nonprefixed : 인덱스 파티션 키 컬럼이 인덱스의 첫번째 컬럼이 아니거나 존재하지 않음
- Global Nonprefixed 는 지원하지 않음

### Unique 인덱스를 파티셔닝하려면 파티션 키가 모두 인덱스 구성 컬럼이여야 한다.

- 그렇지 않은 상황을 가정
    - 중복값이 있는지 인덱스 파티션을 모두 탐색
    - 다른 파티션에 입력하는 현상 막으려면 추가적인 Lock 필요
- 파티션 구조 변경 작업시 인덱스 Unusable → 서비스 중단
    - 서비스 중단없으려면 PK를 포함한 모든 인덱스가 로컬 파티션 인덱스

## 파티션을 활용한 대량 UPDATE 튜닝

- 대용량 데이터 수정시 인덱스를 Drop 후 rebuild 하는 비용이 매우 큼

### 파티션 Exchange를 이용한 대량 데이터 변경

1. 임시 테이블을 생성, 가능하면 nologging 모드로 생성
2. 데이터를 읽어 임시 테이블에 입력하면서 수정할 부분을 수정
3. 임시 테이블에 원본 테이블과 같은 구조로 인덱스를 생성. 할 수 있다면 nologging 모드로 생성
4. 조건에 해당하는 파티션과 임시테이블을 Exchange 한다.
    
    ```sql
    alter table t
    exchange partition p with table 임시_t
    including indexes without validation;
    ```
    
5. 임시 테이블 drop
6. nologging 모드로 작업했다면 파티션을 logging 모드로 전환

## 파티션을 활용한 대량 DELETE 튜닝

### DELETE의 과정, 느린 이유

1. 테이블 레코드 삭제
2. 테이블 레코드 삭제에 대한 Undo Logging
3. 테이블 레코드 삭제에 대한 Redo Logging
4. 인덱스 레코드 삭제
5. 인덱스 레코드 삭제에 대한 Undo Logging
6. 인덱스 레코드 삭제에 대한 Redo Logging
7. Undo에 대한 Redo Logging

### 파티션 Drop을 통한 대량 데이터 삭제

```sql
alter table t drop partition p;
-- oracle 11g 이상
alter table t drop partition for('p');
```

### 파티션 Truncate를 이용한 대량 데이터 삭제

1. 임시 테이블을 생성하고 남길 데이터만 복제
2. 삭제 대상 테이블 파티션을 Truncate
3. 임시 테이블에 복제해 둔 데이터를 원본 테이블에 입력
4. 임시 테이블을 drop
- 서비스 중단 없이 삭제하는 법
    1. 파티션 키와 커팅 기준 컬럼이 일치
    2. 파티션 단위와 커팅 주기가 일치
    3. 모든 인덱스가 로컬 파티션 인덱스

### 파티션을 활용한 대량 INSERT 튜닝

### 비파티션 테이블

1. 가능하면 테이블을 nologging 모드로 전환
2. 인덱스를 unusable 상태로 전환
3. 가능하면 Direct Path Insert 방식으로 대량 데이터 입력
4. 가능하면 nologging 모드로 인덱스를 재생성
5. nologging 모드로 작업했다면 logging 모드로 전환

### 파티션 테이블

- 보통 초대용량 인덱스를 재생성하는 cost 가 높음 - 웬만하면 인덱스는 그대로
- 파티션이 되어 있다면 파티션 단위로 인덱스를 재생성할 수 있음
1. 가능하면 대상 테이블 파티션을 nologging 모드로 전환
2. 매칭되는 인덱스를 unusable 상태로 전환
3. 가능하면 Direct Path Insert 방식으로 대량 데이터 입력
4. 가능하면 nologging 모드로 인덱스 파티션을 재생성
5. nologging 모드로 작업했다면 작업 파티션을 logging 모드로 전환

# Lock과 트랜잭션 동시성 제어

## 오라클 Lock

- 래치
    - SGA에 공유된 각종 자료구조를 보호
- 버퍼 Lock
    - 버퍼 블록에 대한 엑세스를 직렬화
- 라이브러리 캐시 Lock/Pin
    - 라이브러리 캐시에 공유된 SQL 커서와 PL/SQL 프로그램을 보호하기 위해 사용
- **DML Lock**
    - **애플리케이션 개발의 핵심 Lock**
    - 다중 트랜잭션이 동시에 액세스 하는 사용자 데이터의 무결성을 보호
    - 테이블 Lock(== TM Lock), 로우 Lock
- DDL Lock
    - TM Lock, 라이브러리 캐시 Lock/Pin을 이용해 구현한 가상 Lock

### DML 로우 Lock

- 두 개의 동시 트랜잭션이 같은 로우를 변경하는 것을 방지
- DML 로우 Lock에는 배타적 모드를 사용 → 진행 중인 로우를 다른 트랜잭션이 UPDATE/DELETE 할 수 X
- INSERT 로우 Lock 경합 → Unique 인덱스가 있을 때만 발생
- SELECT 는 로우 Lock 사용 X
- Lock이 오래 유지되지 않도록 커밋 시점을 조절

### DML 테이블 Lock

- 오라클은 DML 로우 Lock을 설정하기에 앞서 테이블 Lock을 먼저 실행
- Lock Compatibility
    - RS : row share
    - RX : row exclusive
    - S : share
    - SRX : share row exclusive
    - X : exclusive
    
    |  | Null | RS | RX | S | SRX | X |
    | --- | --- | --- | --- | --- | --- | --- |
    | Null | ○ | ○ | ○ | ○ | ○ | ○ |
    | RS | ○ | ○ | ○ | ○ | ○ |  |
    | RX | ○ | ○ | ○ |  |  |  |
    | S | ○ | ○ |  | ○ |  |  |
    | SRX | ○ | ○ | ○ |  |  |  |
    | X | ○ |  |  |  |  |  |
- 테이블 Lock 은 **테이블 전체 Lock이 아님**!!!
    - 해당 테이블에서 현재 어떤 작업이 수행 중인지 알려주는 flag
    - 후행 트랜잭션의 작업여부를 결정
    - 후행 트랜잭션이 작업을 진행할 수 없는 경우
        - Lock이 해제될 때까지 대기 : `select * from t for update`
        - 일정 시간만 기다리다 포기 : `select * from t for update wait 3`
        - 기다리지 않고 바로 포기 : `select * from t for update nowait`

### Lock 을 푸는 열쇠, 커밋

- Lock에 Lock을 걸려고 하면 발생
- 같은 데이터를 갱신하는 트랜잭션이 동시 수행되지 않도록 애플리케이션 설계
- 적절한 시점에 커밋
- 커밋의 종류
    
    ```sql
    commit write immediate wait;
    commit write immediate nowait;
    commit write batch wait;
    commit write batch nowait;
    ```
    
    - `WAIT`(default) : LGWR가 로그 버퍼를 파일에 기록했다는 완료 메시지를 받을 때 까지 기다림, sync
    - `NOWAIT` :  LGWR 기다리지 않고 바로 다음 트랜잭션 수행, async
    - `IMMEDIATE`(default) : 커밋 명령 받을 때마다 LGWR가 로그 버퍼를 파일에 기록
    - `BATCH` : 세션 내부에 트랜잭션 데이터를 일정량 버퍼링 후 일괄 처리

## 트랜잭션과 동시성 제어

### 비관적 동시성 제어

- 중간 계산 동안 update 발생시 문제 발생
    
    ```sql
    select 적립포인트, 방문횟수, 최근방문일시, 구매실적 from 고객
    where 고객번호 = :cust_num;
    
    -- 새로운 적립포인트 계산
    
    update 고객 set 적립포인트 = :적립포인트 where 고객번호 = :cust_num;
    ```
    
- `select for update` 문 사용시 고객 레코드에 lock 설정
    
    ```sql
    select 적립포인트, 방문횟수, 최근방문일시, 구매실적 from 고객
    where 고객번호 = :cust_num for update;
    ```
    
- (특히 금융권) 데이터를 변경할 목적으로 조회시 **반드시 Lock**을 걸 것!

### 낙관적 동시성 제어

```sql
select 적립포인트, 방문횟수, 최근방문일시, 구매실적 into :a, :b, :c, :d
from 고객
where 고객번호 = :cust_num;

-- 새로운 적립포인트 계산

update 고객 set 적립포인트 = :적립포인트 
where 고객번호 = :cust_num
and 적립포인트 = :a
and 방문횟수 = :b
and 최근방문일시 = :c
and 구매실적 = :d;
```

```sql
select 적립포인트, 방문횟수, 최근방문일시, 구매실적, 변경일시
into :a, :b, :c, :d, :mod_dt
from 고객
where 고객번호 = :cust_num;

-- 새로운 적립포인트 계산

update 고객 set 적립포인트 = :적립포인트, 변경일시 = SYSDATE
where 고객번호 = :cust_num
and 변경일시 = :mod_dt;
```

## 채번 방식에 따른 INSERT 성능 비교

- 채번 테이블
- 시퀀스 오브젝트
- MAX + 1

### 채번 테이블

- 각 테이블 식별자의 단일컬럼 일련번호 또는 구분 속성별 순번을 채번하기 위해 별도 테이블을 관리하는 방식
- 채번 레코드를 읽어서 1 더한 값으로 변경하고 그 값을 새로운 레코드 입력
- 장점
    - 범용성이 좋음
    - INSERT 과정에서 중복 레코드 발생에 대비한 예외 처리에 크게 신경쓸 필요가 없어 채번 함수만 잘 정의하면 됨
    - INSERT 과정에서 결번을 방지
    - PK가 복합컬럼일 때도 사용
- 단점
    - 다른 채번방식에 비해 성능이 좋지 않음 - 로우 Lock 경합, 동시 INSERT 가 많을 경우
- PL/SQL의 `pragma autonomous_transation`  으로 서브 트렌잭션에서 일부 자원만 Lock 을 해제 가능
    - 내부에서 커밋을 수행해도 메인 트랜잭션은 커밋하지 않은 상태로 남음

### 시퀀스 오브젝트

- 테이블별로 시퀀스 오브젝트를 생성
- 장점
    - 성능이 빠름
    - INSERT 과정에서 중복 레코드 발생에 대한 예외 처리에 크게 신경쓸 필요가 없음
- 단점
    - 시퀀스 채번 과정에서 Lock 발생 가능
        - 로우 캐시 Lock
            - 딕셔너리 정보를 매번 디스크 I/O 하는 것은 느리기에 오라클은 로우 Lock 을 사용
            - 많은 사용자들이 시퀀스에 `nextval` 호출 시 로우 캐시 Lock 경합 발생
            - 경합을 줄이기 위해 `CACHE` 옵션을 증가 가능(기본 20)
        - 시퀀스 캐시 Lock
            - 시퀀스 캐시도 공유 캐시에 위치 → 액세스 직렬화 필요
        - SV Lock
            - RAC 환경에서 ORDER 옵션을 사용하여서 형성
                - RAC(Real Application Cluster) : 2개, 혹은 그 이상의 인스턴스가 하나의 storage를 바라보고 있는 구성
                - HA(High Availability) : 2개의 서버를 이용하여 하나는 Active 상태, 나머지 하나는 Standby 상태로 두는 것
            - RAC 각 노드에서 네트워크를 통해 시퀀스 캐시를 서로 주고 받으면서 공유
    - PK 가 단일컬럼일 때만 사용 가능
    - 신규 데이터를 입력하는 과정에서 결번 발생

### MAX + 1

- 대상 테이블의 최종 일련번호를 조회하고 +1
- 장점
    - 시퀀스와 별도 채번 테이블을 관리하는 부담이 없음
    - 충돌이 많지 않으면 성능이 매우 빠름
    - 복합 컬럼 채번도 사용 가능 - 오히려 값이 많으면 로우 Lock 경합이 줄기에 성능이 좋음
- 단점
    - 중복에 대한 예외처리 필요
    - 다중 트랜잭션의 경우 성능 문제

| 채번 방식 | 식별자 구조 | 주요 경향 | 부수적인 경향 | 비고 |
| --- | --- | --- | --- | --- |
| 채번 테이블 | 일련번호 | (값 변경을 위한) 
로우 Lock 경향 | (동시성이 높다면) 
채번 테이블 블록 경향 | - 채번 테이블 관리 부담 |
|  | 구분+순번 | 단일 일련번호일 때보다 Lock 경향 감소 | 단일 일련번호일 때보다 Lock 경향 감소 |  |
| 시퀀스 오브젝트 | 일련번호 | 시퀀스 경향 | (시퀀스 경향 해소 시) 
인덱스 블록 경향 | - 시퀀스 관리 부담
- INSERT 과정에 결번 가능성 |
| MAX + 1 | 일련번호 | (입력 값 중복 시) 
로우 Lock + 재실행 | (동시성이 매우 높다면) 
인덱스 블록 경향 | - 별도 오브젝트 관리 없음
- 중복 값 발생에 대비한 예외처리 필수
- PK 인덱스 구성에 따른 성능 차이 발생 |
|  | 구분+순번 | 단일 일련번호일 때보다 Lock 경향 감소
(구분 속성 값의 종류 수가 많으면, 현저히 감소) | 단일 일련번호일 때보다 Lock 경향 감소
(구분 속성 값의 종류 수가 많으면, 현저히 감소) | - 별도 오브젝트 관리 없음
- 중복 값 발생에 대비한 예외처리 필수
- PK 인덱스 구성에 따른 성능 차이 발생 |

### 시퀀스보다 좋은 솔루션

- SYSDATE 로도 MAX + 1 방식 가능 - 역시 예외 처리 필요
- ILM(Information Lifecycle Management)를 효과적으로 관리하기 위해 데이터 삭제가 중요

### 인덱스 블록 경합

- 순차적으로 값이 증가하는 경우 인덱스의 최우측 블록만 데이터가 입력 - Right Growing 인덱스
- 구분 속성을 넣는 것도 하나의 방법
- 가장 일반적인 방법은 인덱스 해시 파티셔닝
