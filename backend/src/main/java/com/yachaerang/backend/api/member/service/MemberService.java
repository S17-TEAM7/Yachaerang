package com.yachaerang.backend.api.member.service;

import com.yachaerang.backend.api.member.dto.request.MemberRequestDto;
import com.yachaerang.backend.api.member.dto.response.MemberResponseDto;
import com.yachaerang.backend.api.member.entity.Member;
import com.yachaerang.backend.api.member.repository.MemberMapper;
import com.yachaerang.backend.global.auth.jwt.AuthenticatedMemberProvider;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import com.yachaerang.backend.global.util.LogUtil;
import com.yachaerang.backend.infrastructure.s3.service.S3FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberMapper memberMapper;
    private final AuthenticatedMemberProvider authenticatedMemberProvider;
    private final S3FileService s3FileService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    /*
    나의 정보 조회하기
     */
    @Transactional(readOnly = true)
    public MemberResponseDto.MyPageDto getProfile() {
        // Member 확인
        Member member = authenticatedMemberProvider.getMemberByContextHolder();

        MemberResponseDto.MyPageDto myPageDto = MemberResponseDto.MyPageDto.builder()
                .name(member.getName())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .imageUrl(member.getImageUrl())
                .build();
        return myPageDto;
    }

    /*
    나의 정보 수정하기
     */
    @Transactional
    public MemberResponseDto.MyPageDto updateProfile(MemberRequestDto.MyPageDto myPageDto) {
        // Member 확인
        Long memberId = authenticatedMemberProvider.getCurrentMemberId();
        if (memberId == null) {
            throw new GeneralException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (myPageDto.getName() == null && myPageDto.getNickname() == null) {
            throw new GeneralException(ErrorCode.EMPTY_MYPAGE_REQUEST);
        }

        int result = memberMapper.updateProfile(
                memberId,
                myPageDto.getName(),
                myPageDto.getNickname()
        );

        if (result == 0) {
            throw GeneralException.of(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // return확인
        Member member = memberMapper.findById(memberId);
        return MemberResponseDto.MyPageDto.builder()
                .name(member.getName())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .imageUrl(member.getImageUrl())
                .build();
    }

    /**
     * 프로필 사진 업로드
     */
    @Transactional
    public void uploadProfileImage(MultipartFile file) {
        Long memberId = authenticatedMemberProvider.getCurrentMemberId();

        final long MAX_SIZE = 1 * 1024 * 1024; // 1MB
        if (file == null || file.isEmpty()) {
            throw GeneralException.of(ErrorCode.MEMBER_PROFILE_IMAGE);
        }
        if (file.getSize() > MAX_SIZE) {
            throw GeneralException.of(ErrorCode.PROFILE_IMAGE_TOO_MUCH);
        }

        String oldImageUrl = memberMapper.findImageUrl(memberId);

        // 새로 저장할 이름
        String extension;
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().contains(".")) {
            extension = "";
        } else {
            extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        }
        String filename = memberId.toString() + "/" + UUID.randomUUID() + extension;

        // S3로 사진 업로드
        String imageUrl = s3FileService.upload(file, filename);

        // member DB 업데이트하기
        int result = memberMapper.updateProfileImage(imageUrl, memberId);
        if (result != 1) {
            throw GeneralException.of(ErrorCode.MEMBER_PROFILE_IMAGE);
        } else {
            // S3 이전 이미지 삭제
            if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                s3FileService.deleteByUrl(oldImageUrl);
                            }
                        }
                );
            }
        }
    }


    /*
    비밀번호 변경
     */
    @Transactional
    public void changePassword(MemberRequestDto.PasswordDto requestDto) {

        Member member = authenticatedMemberProvider.getMemberByContextHolder();

        // 비밀번호 검증
        if (!bCryptPasswordEncoder.matches(requestDto.getOldPassword(), member.getPassword())) {
            LogUtil.error("{} 회원의 비밀번호 검증에 실패하였습니다.", member.getEmail());
            throw GeneralException.of(ErrorCode.UNMATCHED_PASSWORD);
        }

        // 비밀번호 업데이트
        String encodedPassword = bCryptPasswordEncoder.encode(requestDto.getNewPassword());

        if (memberMapper.updatePassword(member.getEmail(), encodedPassword) != 1) {
            LogUtil.error("{} 회원의 비밀번호를 변경하는 데에 실패하였습니다.", member.getEmail());
            throw GeneralException.of(ErrorCode.MEMBER_PASSWORD_FAILED);
        }
        LogUtil.info("{} 회원의 비밀번호 변경 성공", member.getEmail());
    }
}
