-- 1. task_execution
CREATE TABLE IF NOT EXISTS TASK_EXECUTION (
                                              TASK_EXECUTION_ID     BIGINT        NOT NULL PRIMARY KEY,
                                              START_TIME            DATETIME      DEFAULT NULL,
                                              END_TIME              DATETIME      DEFAULT NULL,
                                              TASK_NAME             VARCHAR(100),
    EXIT_CODE             INTEGER,
    EXIT_MESSAGE          VARCHAR(2500),
    ERROR_MESSAGE         VARCHAR(2500),
    LAST_UPDATED          TIMESTAMP,
    EXTERNAL_EXECUTION_ID VARCHAR(255),            -- K8s Pod ID 등 플랫폼 식별자
    PARENT_EXECUTION_ID   BIGINT                   -- Composed Task의 부모 Task ID
    );

-- 2. task_execution_params
CREATE TABLE IF NOT EXISTS TASK_EXECUTION_PARAMS (
                                                     TASK_EXECUTION_ID BIGINT       NOT NULL,
                                                     TASK_PARAM        VARCHAR(2500),
    CONSTRAINT TASK_EXEC_PARAMS_FK FOREIGN KEY (TASK_EXECUTION_ID)
    REFERENCES TASK_EXECUTION (TASK_EXECUTION_ID)
    );

-- 3. task_task_batch  ← Task ↔ Batch Job Execution 연결 핵심 테이블
CREATE TABLE IF NOT EXISTS TASK_TASK_BATCH (
                                               TASK_EXECUTION_ID BIGINT NOT NULL,
                                               JOB_EXECUTION_ID  BIGINT NOT NULL,
                                               CONSTRAINT TASK_EXEC_BATCH_FK FOREIGN KEY (TASK_EXECUTION_ID)
    REFERENCES TASK_EXECUTION (TASK_EXECUTION_ID)
    );

-- 4. task_lock  ← 단일 Task 동시 실행 방지용 락
CREATE TABLE IF NOT EXISTS TASK_LOCK (
                                         LOCK_KEY     CHAR(36)     NOT NULL,
    REGION       VARCHAR(100) NOT NULL,
    CLIENT_ID    CHAR(36),
    CREATED_DATE DATETIME     NOT NULL,
    CONSTRAINT TASK_LOCK_PK PRIMARY KEY (LOCK_KEY, REGION)
    );

-- 5. task_seq  ← TASK_EXECUTION_ID 채번용 시퀀스
CREATE TABLE IF NOT EXISTS TASK_SEQ (
                                        ID         BIGINT NOT NULL,
                                        UNIQUE_KEY CHAR(1) NOT NULL,
    CONSTRAINT TASK_SEQ_UN UNIQUE (UNIQUE_KEY)
    );
INSERT INTO TASK_SEQ (ID, UNIQUE_KEY)
SELECT 0, '0'
    WHERE NOT EXISTS (SELECT * FROM TASK_SEQ);