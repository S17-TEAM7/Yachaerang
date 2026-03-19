package com.yachaerang.backend.global.auth.jwt;

import com.yachaerang.backend.api.member.entity.Member;
import com.yachaerang.backend.global.auth.config.SecurityConfig;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Import(SecurityConfig.class)
class AuthenticatedMemberProviderTest {

    private AuthenticatedMemberProvider authenticatedMemberProvider;

    @BeforeEach
    void setUp() {
        authenticatedMemberProvider = new AuthenticatedMemberProvider();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정상적으로 인증된 Member를 반환한다")
    void 정상적으로_인증된_Member를_반환_성공() {
        // given
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        member,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Member result = authenticatedMemberProvider.getMemberByContextHolder();

        // then
        assertThat(result).isEqualTo(member);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("UsernamePasswordAuthenticationToken이 아닌 경우 예외")
    void UsernamePasswordAuthenticationToken이_아닌_경우_예외 () {
        // given
        AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymousToken);

        // when & then
        assertThatThrownBy(() -> authenticatedMemberProvider.getMemberByContextHolder())
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException ge = (GeneralException) exception;
                    assertThat(ge.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);
                });
    }

    @Test
    @DisplayName("Authentication이 null이면 예외")
    void Authentication이_null이면_예외() {
        // given
        SecurityContextHolder.getContext().setAuthentication(null);

        // when & then
        assertThatThrownBy(() -> authenticatedMemberProvider.getMemberByContextHolder())
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException ge = (GeneralException) exception;
                    assertThat(ge.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);
                });
    }

    @Test
    @DisplayName("등록된 Principal이 Member 타입이 아니면 예외")
    void 등록된_Principal이_Member타입이_아니면_예외() {
        // given
        String nonMemberPrincipal = "not-a-member";
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(nonMemberPrincipal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when & then
        assertThatThrownBy(() -> authenticatedMemberProvider.getMemberByContextHolder())
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException ge = (GeneralException) exception;
                    assertThat(ge.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("정상적으로 인증된 Member의 ID를 반환")
    void 정상적으로_인증된_Member의_ID를_반환_성공() {
        // given
        Long expectedId = 123L;
        Member member = Member.builder()
                .id(expectedId)
                .email("test@example.com")
                .build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                member,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Long result = authenticatedMemberProvider.getCurrentMemberId();

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("인증되지 않은 경우 예외")
    void 인증되지_않은_경우_예외() {
        // given
        SecurityContextHolder.clearContext();

        // when & then
        assertThatThrownBy(() -> authenticatedMemberProvider.getCurrentMemberId())
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("UsernamePasswordAuthenticationToken이면 isAuthenticated는 true")
    void isAuthenticated_인증된_경우_true() {
        // given
        Member member = Member.builder().id(1L).email("test@example.com").build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                member, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when & then
        assertThat(authenticatedMemberProvider.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("UsernamePasswordAuthenticationToken이 아니면 isAuthenticated는 false")
    void isAuthenticated_미인증_경우_false() {
        // given
        SecurityContextHolder.getContext().setAuthentication(null);

        // when & then
        assertThat(authenticatedMemberProvider.isAuthenticated()).isFalse();
    }
}