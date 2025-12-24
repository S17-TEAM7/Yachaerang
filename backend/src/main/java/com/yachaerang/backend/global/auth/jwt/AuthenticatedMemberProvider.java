package com.yachaerang.backend.global.auth.jwt;

import com.yachaerang.backend.api.member.entity.Member;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedMemberProvider {

    /*
    Context에 등록된 Member의 ID
     */
    public Long getCurrentMemberId() {
        return getMemberByContextHolder().getId();
    }

    /*
    Context에 등록된 Member 객체
     */
    public Member getMemberByContextHolder() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
            throw GeneralException.of(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Member) {
            return (Member) principal;
        } else {
            throw GeneralException.of(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    /*
    Context에 등록된 Member가 있는지 없는지의 여부 확인
     */
    public Boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return true;
        } else {
            return false;
        }
    }
}
