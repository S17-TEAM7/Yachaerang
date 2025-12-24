import { ref } from "vue";
import { useRouter } from "vue-router";
import { sendPasswordResetCodeApi, resetPasswordApi } from "@/api/auth";
import { useToastStore } from "@/stores/toast";

export function useForgotPasswordForm() {
  const router = useRouter();
  const toast = useToastStore();

  const email = ref("");
  const emailSent = ref(false);
  const emailCode = ref("");
  const isLoading = ref(false);

  const sendResetCode = async () => {
    if (!email.value) {
      toast.show("이메일을 입력해주세요.", "warning");
      return;
    }

    try {
      isLoading.value = true;
      await sendPasswordResetCodeApi({
        mail: email.value,
      });
      emailSent.value = true;
      toast.show("입력하신 이메일로 인증번호를 발송했습니다.", "success");
    } catch (error) {
      const errorCode = error.response?.data?.code;
      const message = error.response?.data?.message;

      if (errorCode === "MEMBER_001") {
        toast.show("가입되지 않은 이메일입니다.", "error");
      } else {
        toast.show(message || "인증번호 발송 중 오류가 발생했습니다.", "error");
      }
    } finally {
      isLoading.value = false;
    }
  };

  const resetPasswordWithCode = async () => {
    if (!email.value) {
      toast.show("이메일을 입력해주세요.", "warning");
      return;
    }
    if (!emailCode.value) {
      toast.show("이메일로 받은 인증번호를 입력해주세요.", "warning");
      return;
    }

    try {
      isLoading.value = true;
      await resetPasswordApi({
        mail: email.value,
        code: emailCode.value,
      });

      toast.show("임시 비밀번호가 메일로 전송되었습니다.", "success");
      router.push({ name: "login" });
    } catch (error) {
      const errorCode = error.response?.data?.code;
      const message = error.response?.data?.message;

      if (errorCode === "MAIL_001") {
        toast.show("인증코드가 만료되었습니다. 다시 요청해주세요.", "error");
      } else if (errorCode === "MAIL_002") {
        toast.show("인증 코드가 올바르지 않습니다.", "error");
      } else if (errorCode === "MEMBER_001") {
        toast.show("가입되지 않은 이메일입니다.", "error");
      } else {
        toast.show(message || "비밀번호 재설정 중 오류가 발생했습니다.", "error");
      }
    } finally {
      isLoading.value = false;
    }
  };

  return {
    email,
    emailSent,
    emailCode,
    isLoading,
    sendResetCode,
    resetPasswordWithCode,
  };
}
