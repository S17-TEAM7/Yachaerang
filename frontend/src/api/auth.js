import { publicClient } from './axios';

// 로그인
export function loginApi(email, password) {
  return publicClient.post('/api/v1/auth/login', { email, password });
}

// 회원가입
export function signupApi(payload) {
  return publicClient.post('/api/v1/auth/signup', payload);
}

// 이메일 인증
export const sendEmailCodeApi = (payload) => {
  return publicClient.post('/api/v1/mails', payload);
};

export const verifyEmailCodeApi = (payload) => {
  return publicClient.post('/api/v1/mails/verify-code', payload);
};

// 비밀번호 찾기 (인증 코드 발송)
export const sendPasswordResetCodeApi = (payload) => {
  return publicClient.post('/api/v1/mails/password/send-code', payload);
};

// 비밀번호 재설정 (인증 코드 확인 및 임시 비밀번호 발송)
export const resetPasswordApi = (payload) => {
  return publicClient.post('/api/v1/mails/password/reset', payload);
};