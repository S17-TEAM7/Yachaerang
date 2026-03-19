package com.yachaerang.backend.api.member.service;

import com.yachaerang.backend.api.common.MemberStatus;
import com.yachaerang.backend.api.common.Role;
import com.yachaerang.backend.api.member.dto.request.MemberRequestDto;
import com.yachaerang.backend.api.member.dto.response.MemberResponseDto;
import com.yachaerang.backend.api.member.entity.Member;
import com.yachaerang.backend.api.member.repository.MemberMapper;
import com.yachaerang.backend.global.auth.jwt.AuthenticatedMemberProvider;
import com.yachaerang.backend.infrastructure.s3.service.S3FileService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@Transactional
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberMapper memberMapper;

    @Mock
    AuthenticatedMemberProvider authenticatedMemberProvider;

    @Mock
    S3FileService s3FileService;

    @Mock
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    MemberService memberService;

    /*
    테스트 객체 선언
     */
    private MemberRequestDto.MyPageDto mypageRequestDto;
    private Member member;


    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("test")
                .nickname("test")
                .role(Role.ROLE_USER)
                .memberStatus(MemberStatus.ACTIVE)
                .imageUrl("default.png")
                .build();

        mypageRequestDto = MemberRequestDto.MyPageDto.builder()
                .name("test2")
                .nickname("test2")
                .build();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("프로필 조회 성공")
    void 프로필조회_성공() {
        // given
        given(authenticatedMemberProvider.getMemberByContextHolder()).willReturn(member);

        // when
        MemberResponseDto.MyPageDto result = memberService.getProfile();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getNickname()).isEqualTo("test");
        assertThat(result.getImageUrl()).isEqualTo("default.png");

        verify(authenticatedMemberProvider, times(1)).getMemberByContextHolder();
    }

    @Test
    @DisplayName("프로필 조회 실패 - 인증되지 않은 사용자")
    void 프로필조회_실패_인증되지않은사용자() {
        // given
        given(authenticatedMemberProvider.getMemberByContextHolder()).willThrow(GeneralException.of(ErrorCode.UNAUTHORIZED_ACCESS));

        // when & then
        assertThatThrownBy(() -> memberService.getProfile())
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> {
                    GeneralException exception = (GeneralException) e;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);
                });
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void 프로필수정_성공() {
        // given
        Member updateMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name(mypageRequestDto.getName())
                .nickname(mypageRequestDto.getNickname())
                .build();
        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(1L);
        given(memberMapper.updateProfile(anyLong(), anyString(), anyString())).willReturn(1);
        given(memberMapper.findById(1L)).willReturn(updateMember);

        // when
        MemberResponseDto.MyPageDto result = memberService.updateProfile(mypageRequestDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("test2");
        assertThat(result.getNickname()).isEqualTo("test2");

        verify(authenticatedMemberProvider, times(1)).getCurrentMemberId();
        verify(memberMapper, times(1)).updateProfile(1L, "test2", "test2");
        verify(memberMapper, times(1)).findById(1L);
    }

    @Test
    @DisplayName("프로필 수정 실패 - 인증되지 않은 사용자")
    void 프로필수정_실패_인증되지않은사용자() {
        // given
        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(null);

        // when & then
        assertThatThrownBy(() -> memberService.updateProfile(mypageRequestDto))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> {
                    GeneralException exception = (GeneralException) e;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("프로필 수정 실패 시 INTERNAL_SERVER_ERROR 예외")
    void updateProfile_UpdateFailed_예외() {
        // given
        Long memberId = member.getId();

        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(memberId);
        given(memberMapper.updateProfile(
                memberId,
                mypageRequestDto.getName(),
                mypageRequestDto.getNickname()
        )).willReturn(0); // 업데이트 실패

        // when & then
        assertThatThrownBy(() -> memberService.updateProfile(mypageRequestDto))
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException ge = (GeneralException) exception;
                    assertThat(ge.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
                });

        then(memberMapper).should(never()).findById(any());
    }

    @Test
    @DisplayName("이름만 수정 성공")
    void 이름만_수정_성공() {
        // given
        Long memberId = member.getId();
        MemberRequestDto.MyPageDto request = MemberRequestDto.MyPageDto.builder()
                .name("새이름")
                .nickname(null)
                .build();

        Member updatedMember = Member.builder()
                .id(memberId)
                .name("새이름")
                .email(member.getEmail())
                .nickname(member.getNickname())
                .imageUrl(member.getImageUrl())
                .build();

        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(memberId);
        given(memberMapper.updateProfile(memberId, "새이름", null)).willReturn(1);
        given(memberMapper.findById(memberId)).willReturn(updatedMember);

        // when
        MemberResponseDto.MyPageDto result = memberService.updateProfile(request);

        // then
        assertThat(result.getName()).isEqualTo("새이름");
        assertThat(result.getNickname()).isEqualTo(member.getNickname());
        assertThat(result.getImageUrl()).isEqualTo(member.getImageUrl());

        then(memberMapper).should().updateProfile(memberId, "새이름", null);
    }

    @Test
    @DisplayName("변경사항 없는 요청 시 EMPTY_MYPAGE_REQUEST 예외")
    void 변경사항_없는_요청시_예외 () {
        // given
        Long memberId = member.getId();
        MemberRequestDto.MyPageDto emptyRequestDto = MemberRequestDto.MyPageDto.builder().build();
        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(memberId);

        // when & then
        assertThatThrownBy(() -> memberService.updateProfile(emptyRequestDto))
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException ge = (GeneralException) exception;
                    assertThat(ge.getErrorCode()).isEqualTo(ErrorCode.EMPTY_MYPAGE_REQUEST);
                });

        then(memberMapper).should(never()).updateProfile(any(), any(), any());
        then(memberMapper).should(never()).findById(any());
    }

    @Test
    @DisplayName("프로필 사진 업로드 성공 및 기존 S3사진 삭제")
    void 프로필_사진_업로드_성공_및_기존S3사진_삭제() {
        // given
        Long memberId = 1L;
        MultipartFile file = mock(MultipartFile.class); // mock
        given(file.getOriginalFilename()).willReturn("test.png");

        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(memberId);
        given(memberMapper.findImageUrl(memberId)).willReturn("https://s3.old/image.png");
        given(s3FileService.upload(eq(file), anyString())).willReturn("https://s3.new/image.png");
        given(memberMapper.updateProfileImage("https://s3.new/image.png", memberId)).willReturn(1);

        // 동기화 활성화 시작
        TransactionSynchronizationManager.initSynchronization();

        // when
        memberService.uploadProfileImage(file);

        // then
        verify(authenticatedMemberProvider, times(1)).getCurrentMemberId();
        // DB 호출 검증
        verify(memberMapper, times(1)).findImageUrl(memberId);
        verify(s3FileService, times(1)).upload(eq(file), anyString());
        verify(memberMapper, times(1)).updateProfileImage("https://s3.new/image.png", memberId);

        // commit 이전
        verify(s3FileService, never()).deleteByUrl(anyString());

        // afterCommit 호출
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).isNotEmpty();

        syncs.forEach(TransactionSynchronization::afterCommit);

        // commit 이후
        verify(s3FileService, times(1)).deleteByUrl("https://s3.old/image.png");
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("프로필 사진 업로드 성공 및 기존 S3사진 삭제 안 함")
    void 프로필_사진_업로드_성공_및_기존S3사진_삭제안함() {
        // given
        Long memberId = 1L;

        MultipartFile file = mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn("test.png");

        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(memberId);
        given(memberMapper.findImageUrl(memberId)).willReturn(null); // 기존 이미지 없음
        given(s3FileService.upload(eq(file), anyString())).willReturn("https://s3.new/image.png");
        given(memberMapper.updateProfileImage("https://s3.new/image.png", memberId)).willReturn(1);

        TransactionSynchronizationManager.initSynchronization();

        // when
        memberService.uploadProfileImage(file);

        // then
        verify(s3FileService, times(1)).upload(eq(file), anyString());
        verify(memberMapper, times(1)).updateProfileImage("https://s3.new/image.png", memberId);

        // 불필요
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(s3FileService, never()).deleteByUrl(anyString());

        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void 비밀번호_변경_성공() {
        // given
        String oldPassword = "OldPassword123";
        String newPassword = "NewPassword123";
        String encodedOldPassword = "$2a$10$encodedOldPassword";
        String encodedNewPassword = "$2a$10$encodedNewPassword";

        MemberRequestDto.PasswordDto requestDto = MemberRequestDto.PasswordDto
                .builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .build();

        Member testMember = Member.builder()
                .email("test@example.com")
                .password(encodedOldPassword)
                .build();
        given(authenticatedMemberProvider.getMemberByContextHolder()).willReturn(testMember);
        given(bCryptPasswordEncoder.matches(oldPassword, encodedOldPassword)).willReturn(true);
        given(bCryptPasswordEncoder.encode(newPassword)).willReturn(encodedNewPassword);
        given(memberMapper.updatePassword(testMember.getEmail(), encodedNewPassword)).willReturn(1);

        // when
        assertThatCode(() -> memberService.changePassword(requestDto))
                .doesNotThrowAnyException();

        // then
        then(authenticatedMemberProvider).should().getMemberByContextHolder();
        then(bCryptPasswordEncoder).should().matches(oldPassword, encodedOldPassword);
        then(bCryptPasswordEncoder).should().encode(newPassword);
        then(memberMapper).should().updatePassword(testMember.getEmail(), encodedNewPassword);
    }

    @Test
    @DisplayName("확장자 없는 파일 업로드 성공")
    void 프로필_사진_업로드_성공_확장자없음() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn("filename_without_extension");

        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(1L);
        given(memberMapper.findImageUrl(1L)).willReturn(null);
        given(s3FileService.upload(eq(file), anyString())).willReturn("https://s3.new/image");
        given(memberMapper.updateProfileImage(anyString(), eq(1L))).willReturn(1);

        TransactionSynchronizationManager.initSynchronization();

        // when & then
        assertThatCode(() -> memberService.uploadProfileImage(file))
                .doesNotThrowAnyException();

        then(s3FileService).should().upload(eq(file), argThat(filename -> !filename.contains(".")));
    }

    @Test
    @DisplayName("프로필 사진 업로드 실패 - 파일이 null")
    void 프로필_사진_업로드_실패_파일null() {
        // given
        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> memberService.uploadProfileImage(null))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_PROFILE_IMAGE));
    }

    @Test
    @DisplayName("프로필 사진 업로드 실패 - 빈 파일")
    void 프로필_사진_업로드_실패_빈파일() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(true);
        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> memberService.uploadProfileImage(file))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_PROFILE_IMAGE));
    }

    @Test
    @DisplayName("프로필 사진 업로드 실패 - 파일 크기 초과")
    void 프로필_사진_업로드_실패_파일크기초과() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(2 * 1024 * 1024L);
        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> memberService.uploadProfileImage(file))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PROFILE_IMAGE_TOO_MUCH));
    }

    @Test
    @DisplayName("프로필 사진 업로드 실패 - DB 업데이트 실패")
    void 프로필_사진_업로드_실패_DB업데이트실패() {
        // given
        Long memberId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(500 * 1024L);
        given(file.getOriginalFilename()).willReturn("test.png");

        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(memberId);
        given(memberMapper.findImageUrl(memberId)).willReturn(null);
        given(s3FileService.upload(eq(file), anyString())).willReturn("https://s3.new/image.png");
        given(memberMapper.updateProfileImage(anyString(), eq(memberId))).willReturn(0);

        TransactionSynchronizationManager.initSynchronization();

        // when & then
        assertThatThrownBy(() -> memberService.uploadProfileImage(file))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_PROFILE_IMAGE));

        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("프로필 사진 업로드 성공 - 기존 이미지 URL이 빈 문자열이면 S3 삭제 안 함")
    void 프로필_사진_업로드_성공_기존이미지URL빈문자열() {
        // given
        Long memberId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn("test.png");

        given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(memberId);
        given(memberMapper.findImageUrl(memberId)).willReturn("");
        given(s3FileService.upload(eq(file), anyString())).willReturn("https://s3.new/image.png");
        given(memberMapper.updateProfileImage("https://s3.new/image.png", memberId)).willReturn(1);

        TransactionSynchronizationManager.initSynchronization();

        // when
        memberService.uploadProfileImage(file);

        // then
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(s3FileService, never()).deleteByUrl(anyString());

        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 기존 비밀번호 불일치")
    void 비밀번호_변경_실패_기존비밀번호불일치() {
        // given
        String oldPassword = "WrongPassword";
        String encodedOldPassword = "$2a$10$encodedOldPassword";

        MemberRequestDto.PasswordDto requestDto = MemberRequestDto.PasswordDto.builder()
                .oldPassword(oldPassword)
                .newPassword("NewPassword123")
                .build();

        Member testMember = Member.builder()
                .email("test@example.com")
                .password(encodedOldPassword)
                .build();

        given(authenticatedMemberProvider.getMemberByContextHolder()).willReturn(testMember);
        given(bCryptPasswordEncoder.matches(oldPassword, encodedOldPassword)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.changePassword(requestDto))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNMATCHED_PASSWORD));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - DB 업데이트 실패")
    void 비밀번호_변경_실패_DB업데이트실패() {
        // given
        String oldPassword = "OldPassword123";
        String newPassword = "NewPassword123";
        String encodedOldPassword = "$2a$10$encodedOldPassword";
        String encodedNewPassword = "$2a$10$encodedNewPassword";

        MemberRequestDto.PasswordDto requestDto = MemberRequestDto.PasswordDto.builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .build();

        Member testMember = Member.builder()
                .email("test@example.com")
                .password(encodedOldPassword)
                .build();

        given(authenticatedMemberProvider.getMemberByContextHolder()).willReturn(testMember);
        given(bCryptPasswordEncoder.matches(oldPassword, encodedOldPassword)).willReturn(true);
        given(bCryptPasswordEncoder.encode(newPassword)).willReturn(encodedNewPassword);
        given(memberMapper.updatePassword(testMember.getEmail(), encodedNewPassword)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> memberService.changePassword(requestDto))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_PASSWORD_FAILED));
    }
}