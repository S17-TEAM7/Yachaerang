import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { sendEmailCodeApi, verifyEmailCodeApi, signupApi } from '@/api/auth';
import { useToastStore } from '@/stores/toast';

export function useSignupForm() {
  const router = useRouter();
  const toastStore = useToastStore();

  const email = ref('');
  const password = ref('');
  const passwordConfirm = ref('');
  const name = ref('');
  const nickname = ref('');

  const emailSent = ref(false);
  const emailCode = ref('');
  const emailVerified = ref(false);

  const sendEmailCode = async () => {
    if (!email.value) {
      toastStore.show('이메일을 입력해주세요.', 'warning');
      return;
    }
    try {
      await sendEmailCodeApi({
        mail: email.value,
      });
      emailSent.value = true;
      toastStore.show('입력하신 이메일로 인증번호를 발송했습니다.', 'success');
    } catch (error) {
      const errorCode = error.response?.data?.code;
      if (errorCode === 'MEMBER_002') {
        toastStore.show('이미 가입된 이메일입니다.', 'error');
      } else {
        toastStore.show(error.response?.data?.message || '이메일 인증번호 발송 중 오류가 발생했습니다.', 'error');
      }
    }
  };

  const verifyEmailCode = async () => {
    if (!emailCode.value) {
      toastStore.show('인증번호를 입력해주세요.', 'warning');
      return;
    }

    try {
      const { data } = await verifyEmailCodeApi({
        mail: email.value,
        code: emailCode.value,
      });

      if (data?.data === true) {
        emailVerified.value = true;
        toastStore.show('이메일 인증이 완료되었습니다.', 'success');
      } else {
        toastStore.show('인증번호가 올바르지 않습니다.', 'error');
      }
    } catch (error) {
      console.error('이메일 인증 중 오류:', error);
      toastStore.show(error.response?.data?.message || '이메일 인증 중 오류가 발생했습니다.', 'error');
    }
  };

  const isPasswordValid = computed(() =>
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/.test(password.value)
  );

  const isPasswordMatch = computed(() => password.value === passwordConfirm.value);

  const isFormValid = computed(
    () => email.value && isPasswordValid.value && isPasswordMatch.value && name.value && nickname.value
  );

  const handleSignup = async () => {
    if (!emailVerified.value) {
      toastStore.show('이메일 인증을 완료해주세요.', 'warning');
      return;
    }
    try {
      await signupApi({
        email: email.value,
        password: password.value,
        name: name.value,
        nickname: nickname.value,
      });
      toastStore.show('회원가입이 완료되었습니다.', 'success');
      router.push({ name: 'login' });
    } catch (error) {
      const errorCode = error.response?.data?.code;
      if (errorCode === 'MEMBER_002') {
        toastStore.show('이미 가입된 이메일입니다.', 'error');
      } else {
        toastStore.show(error.response?.data?.message || '회원가입 중 오류가 발생했습니다.', 'error');
      }
    }
  };

  return {
    email,
    password,
    passwordConfirm,
    name,
    nickname,

    emailSent,
    emailCode,
    emailVerified,

    isPasswordValid,
    isPasswordMatch,
    isFormValid,

    sendEmailCode,
    verifyEmailCode,
    handleSignup,
  };
}
