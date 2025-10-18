# Redo

- 오라클은 데이터파일과 컨트롤 파일에 가해지는 모든 변경 사항은 Redo Log 에 저장
- Redo Log의 목적
    - Database Recovery → Archived Redo Log 사용
    - Cache Recovery → Online Redo Log 사용
    - Fast Commit
- `Online Redo`
    - Redo 로그 버퍼에 로그 엔트리를 기록
    - `roll forward` → `rollback`
        - `roll forward` : 인스턴스가 비정상 종료되면 마지막 체크포인트 이후부터 트랜잭션 재현, 시스템 셧다운 이전 버퍼 캐시 원상태 복구
        - `rollback` : 트랜잭션 롤백
    - 최소 두 개 이상의 파일로 구성
    - 현재 사용중인 파일이 다 차면 log switching 발생, 주기가 너무 짧으면 백업 미완료된 Online Redo 로그로 스위칭 → DB Hang
    - 로그를 쓰다 모든 파일이 차면 첫 파일부터 재사용(Round-Robin)
- `Archived Redo`
    - 물리적인 저장 매체에 문제가 생겼을 때 데이터베이스 복구를 위해 사용
- Fast Commit
    - 블록은 Random Access, 로그는 Append 방식으로 기록 → 로그가 빠름
    - 메모리 데이터 블록과 데이터 파일 간 동기화는 DBWR 로 Batch 처리
- 데이터 블록 버퍼를 변경하기 전 항상 Redo 로그 버퍼에 먼저 기록 후 일정 시점마다 LGWR 프로세스에 의해 Redo 로그 버퍼에 있는 내용을 Redo 로그 파일에 기록
    - 3초마다 DBWR 프로세스로부터 신호를 받을 때
    - 로그 버퍼의 1/3이 차거나 기록된 Redo 레코드량이 1MB를 넘을 때
    - 사용자가 커밋 또는 롤백 명령을 날릴 때
- Redo 로그 메커니즘
    - Log Force at commit
        - 트랜잭션이 영속성을 보장 받기 위해서는 최소 커밋 시점에는 로그를 메모리가 아닌 데이터파일에 안전하게 기록 필요
    - Write Ahead Logging
        - 버퍼 캐시 블록 갱신 전 Redo 로그 엔트리를 로그 버퍼에 기록
        - DBWR 가 버퍼캐시의 Dirty 블록들을 데이터파일에 기록하기 전에 LGWR가 해당 Redo 엔트리를 모두 Redo 로그 파일에 기록했음이 보장
    - Fast Commit 과정
        1. 사용자가 커밋 명령
        2. 서버 프로세스는 커밋 레코드를 Redo 로그 버퍼에 기록
        3. LGWR는 즉시 트랜잭션 로그 엔트리와 함께 redo 로그파일에 저장
        4. 커밋을 수행한 서버 프로세스에 “success code” 리턴
    - 커밋 & 롤백 시 `log file sync` 이벤트 발생
        - LGWR가 로그 버퍼 내용을 Redo 로그 파일에 기록할 때까지 서버 프로세스가 대기하는 현상

# Undo

- Undo 목적
    - Transaction Rollback
    - Transaction Recovery
    - Read Consistency

## Undo 세그먼트 트랜잭션 테이블 슬롯

- Undo 세그먼트 헤더에는 트랜잭션 테이블 슬롯이 위치
    - 트랜잭션 ID : [USN# + Slot# + Wrap#]
    - Transaction Status
    - 커밋 SCN
    - Last UBA (Undo Block Address)
    - 기타
- USN : User Segment Number
- 트랜잭션을 시작하려면 Undo 세그먼트에 있는 트랜잭션 테이블로부터 슬롯을 할당 받아야 함
- 변경 사항은 Undo 로그에 기록
    - INSERT : 추가된 레코드의 rowid
    - UPDATE : 변경되는 컬럼에 대한 before image
    - DELETE : 지워지는 로우의 모든 컬럼에 대한 before image
- Last UBA는 트랜잭션의 기록 사항을 가장 마지막 Undo 레코드 뒤에 계속 추가해 나가려고 유지하는 일종의 포인터

## 블록 헤더 ITL 슬롯

- 각 데이터 블록과 인덱스 블록 헤더에는 ITL(Interested Transaction List) 슬롯이 존재
    - ITL 슬롯 번호
    - 트랜잭션 ID
    - UBA
    - 커밋 Flag
    - Locking 정보
    - 커밋 SCN
 
## Lock Byte
- 오라클은 레코드가 저장되는 로우마다 그 헤더에 Lock Byte를 할당해 해당 로우를 갱신 중인 트랜잭션의 ITL 슬롯 번호를 기록
- 로우 Lock = 로우 단위 Lock + 트랜잭션 Lock
