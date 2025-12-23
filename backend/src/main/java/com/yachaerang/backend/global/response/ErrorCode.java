package com.yachaerang.backend.global.response;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ErrorCode implements BaseCode{

    TEST_ERROR(HttpStatus.BAD_REQUEST, "TESTERROR", "테스트 에러입니다."),

    // request
    UNMATCHED_REQUEST(HttpStatus.BAD_REQUEST, "METHODERROR", "요청 방식이 잘못됐습니다."),
    REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "TIMEOUT", "요청 시간이 초과되었스빈다."),

    // MyBatis
    ENUM_MAPPED_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MYBATIS_001", "DB로부터 Java의 Eum을 매핑하는데에 실패했습니다."),
    MYBATIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MYBATIS_002", "DB로부터 오류 발생하였습니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "회원을 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "MEMBER_002", "이미 존재하는 회원입니다."),
    EMPTY_MYPAGE_REQUEST(HttpStatus.BAD_REQUEST, "MEMBER_003", "유효한 변경사항 요청이 없습니다."),
    MEMBER_PROFILE_IMAGE(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBER_004", "회원 이미지를 DB에 저장 중 오류가 발생했습니다."),
    MEMBER_PASSWORD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBER_005", "비밀번호 초기화 저장 중 오류가 발생했습니다."),
    PROFILE_IMAGE_TOO_MUCH(HttpStatus.BAD_REQUEST, "MEMBER_006", "프로필 이미지의 용량이 너무 큽니다."),

    // login
    UNMATCHED_PASSWORD(HttpStatus.BAD_REQUEST, "LOGIN_001", "잘못된 비밀번호입니다."),
    FAILED_GENERATE_PASSWORD(HttpStatus.INTERNAL_SERVER_ERROR, "LOGIN_002", "새로운 비밀번호 생성에 실패했습니다."),

    // Cookie
    COOKIE_NOT_FOUND(HttpStatus.NOT_FOUND, "COOKIE_001", "쿠키를 찾을 수 없습니다."),

    // token
    TOKEN_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_001", "토큰 생성에 실패하였습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_002", "만료된 토큰입니다"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "TOKEN_003", "유효하지 않은 토큰입니다"),
    TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "TOKEN_004", "지원하지 않는 토큰 형식입니다"),
    TOKEN_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "TOKEN_005", "토큰 서명이 유효하지 않습니다"),
    CLAIM_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_006", "토큰에서 정보 추출에 실패하였습니다."),
    CLAIM_NOT_FOUND(HttpStatus.BAD_REQUEST, "TOKEN_007", "토큰에 필수 정보가 없습니다"),
    CLAIM_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "TOKEN_008", "토큰 정보 형식이 올바르지 않습니다"),
    UNMATCHED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "TOKEN_009", "refresh token이 일치하지 않습니다."),

    // auth
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증된 사용자가 아닙니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 존재하지 않습니다."),

    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "TOKEN_010", "토큰이 존재하지 않습니다"),
    TOKEN_HEADER_INVALID(HttpStatus.BAD_REQUEST, "TOKEN_011", "토큰 헤더 형식이 올바르지 않습니다"),

    UUID_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "UUID_001", "UUID 생성에 실패했습니다."),

    // Date
    WRONG_REQUEST_DATE(HttpStatus.BAD_REQUEST, "DATE_001", "날짜를 올바르게 입력해주세요."),
    INVALID_ISO_WEEK_DATE(HttpStatus.BAD_REQUEST, "DATE_002", "표준 ISO 주간 규칙에 어긋납니다."),

    // article
    ARTICLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "ARTICLE_001", "기사를 찾을 수 없습니다."),

    // favorite
    FAVORITE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FAVORITE_001", "관심 대시보드 등록에 실패했습니다."),
    FAVORITE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FAVORITE_002", "관심 대시보드 해제에 실패했습니다."),
    FAVORITE_DUPLICATED(HttpStatus.BAD_REQUEST, "FAVORITE_003", "이미 등록된 관심사입니다."),

    // farm
    FARM_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "FARM_001", "농장 정보가 이미 존재합니다. 수정으로 요청을 보내주세요."),
    FARM_GRADE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "FARM_002", "농장등급이 유효하지 않습니다."),
    FARM_NOT_FOUND(HttpStatus.NOT_FOUND, "FARM_003", "기존의 농장 정보를 찾을 수 없습니다."),
    FARM_SAVE_FAILED(HttpStatus.BAD_REQUEST, "FARM_004", "농장 정보 저장에 실패했습니다."),
    FARM_UPDATE_FAILED(HttpStatus.BAD_REQUEST, "FARM_005", "농장 정보 수정에 실패하였습니다."),

    // chat
    SESSION_FAILED(HttpStatus.BAD_REQUEST, "CHAT_001", "채팅의 세션을 생성하는 데에 실패하였습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_002", "채팅을 찾을 수 없습니다."),
    SESSION_ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "CHAT_003", "채팅 세션을 읽을 권한이 없습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "CHAT_004", "채팅이 이미 종료되었습니다."),

    // AI
    INVALID_MESSAGE(HttpStatus.BAD_REQUEST, "AI_001", "메시지 형식이 잘못되었습니다."),
    AI_MODEL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI_002", "AI 모델을 불러오는 데에 오류가 생겼습니다."),
    AI_EVALUATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_003", "모델로부터 평가를 불러오는 데에 실패하였습니다."),

    // json
    FAILED_JSON_PARSING(HttpStatus.INTERNAL_SERVER_ERROR, "JSON_001", "json 파싱에 실패했습니다."),

    // s3
    S3_UPLOAD_FAILED(HttpStatus.BAD_REQUEST, "S3_001", "S3에 파일을 업로드 하는 데에 실패하였습니다."),
    S3_KEY_FAILED(HttpStatus.BAD_REQUEST, "S3_002", "S3의 key가 유효하지 않습니다."),
    S3_DELETE_FAILED(HttpStatus.BAD_REQUEST, "S3_003", "S3 파일 삭제 중 오류가 일어났습니다."),

    // smtp
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "MAIL_001", "인증코드가 만료되었거나 존재하지 않습니다."),
    UNMATCHED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "MAIL_002", "인증 코드가 일치하지 않습니다."),
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MAIL_003", "메일 전송에 실패하였습니다."),

    // bookmark
    ALREADY_BOOKMARKED(HttpStatus.BAD_REQUEST, "BOOKMARK_001", "이미 북마크에 등록해두었습니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK_002", "북마크를 조회할 수 없습니다."),
    BOOKMARK_DB_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOKMARK_003", "북마크 DB 실행이 유효하지 않습니다."),

    // reaction
    REACTION_ALREADY(HttpStatus.ALREADY_REPORTED, "REACTION_001", "이미 반응을 달았습니다."),
    REACTION_DB_FAILED(HttpStatus.BAD_REQUEST, "REACTION_002", "리액션을 저장하는 데에 실패하였습니다."),
    REACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "REACTION_003", "저장된 리액션 조회가 불가능합니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500ERROR", "서버에서 장애가 일어났습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
