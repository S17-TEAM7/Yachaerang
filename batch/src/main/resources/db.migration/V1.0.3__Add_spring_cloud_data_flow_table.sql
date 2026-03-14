-- 1. app_registration  ← Source/Processor/Sink/Task 앱 등록 정보
CREATE TABLE IF NOT EXISTS APP_REGISTRATION (
                                                ID               BIGINT       NOT NULL PRIMARY KEY,
                                                OBJECT_VERSION   BIGINT,
                                                DEFAULT_VERSION  BIT,
                                                METADATA_URI     LONGTEXT,
                                                NAME             VARCHAR(255),
    TYPE             INTEGER,                      -- 0:APP 1:SOURCE 2:PROCESSOR 3:SINK 4:TASK
    URI              LONGTEXT,
    VERSION          VARCHAR(255)
    );

-- 2. task_definitions , SCDF에 등록된 Task 정의
CREATE TABLE IF NOT EXISTS TASK_DEFINITIONS (
                                                DEFINITION_NAME VARCHAR(255) NOT NULL PRIMARY KEY,
    DEFINITION      LONGTEXT,
    DESCRIPTION     VARCHAR(255)
    );

-- 3. stream_definitions , SCDF에 등록된 Stream 파이프라인 정의 (Stream 미사용 시 생략 가능)
CREATE TABLE IF NOT EXISTS STREAM_DEFINITIONS (
                                                  DEFINITION_NAME VARCHAR(255) NOT NULL PRIMARY KEY,
    DEFINITION      LONGTEXT,
    DESCRIPTION     VARCHAR(255),
    ORIGINAL_DEFINITION LONGTEXT,
    STATUS          VARCHAR(255)
    );

-- 4. audit_records , SCDF 조작 이력 (앱 등록/Task 실행 등 감사 로그)
CREATE TABLE IF NOT EXISTS AUDIT_RECORDS (
    ID                BIGINT       NOT NULL PRIMARY KEY,
    AUDIT_ACTION      BIGINT,
    AUDIT_OPERATION   BIGINT,
    CORRELATION_ID    VARCHAR(255),
    CREATED_BY        VARCHAR(255),
    CREATED_ON        DATETIME,
    DATA              LONGTEXT,
    PLATFORM_NAME     VARCHAR(255)
    );

-- 5. scdf_id_generator , SCDF 내부 ID 채번용 (APP_REGISTRATION, AUDIT_RECORDS 등)
CREATE TABLE IF NOT EXISTS SCDF_ID_GENERATOR (
                                                 ID         BIGINT NOT NULL,
                                                 UNIQUE_KEY CHAR(1) NOT NULL,
    CONSTRAINT SCDF_ID_GEN_UN UNIQUE (UNIQUE_KEY)
    );
INSERT INTO SCDF_ID_GENERATOR (ID, UNIQUE_KEY)
SELECT 0, '0'
    WHERE NOT EXISTS (SELECT * FROM SCDF_ID_GENERATOR);