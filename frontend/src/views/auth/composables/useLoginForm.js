import { ref, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { loginApi } from "@/api/auth";
import apiClient from "@/api/axios";

export function useLoginForm() {
  const router = useRouter();
  const route = useRoute();
  const { login } = useAuthStore();

  const email = ref("");
  const password = ref("");
  const errorMessage = ref("");

  const isFormValid = computed(() => email.value.trim() !== "" && password.value.trim() !== "");

  const handleLogin = async () => {
    if (!isFormValid.value) return;

    errorMessage.value = "";

    try {
      const res = await loginApi(email.value, password.value);
      const body = res.data;

      if (body?.success === true && body?.code === "200") {
        const { accessToken, refreshToken } = body.data || {};

        if (!accessToken) {
          errorMessage.value = "토큰이 응답에 없습니다. 백엔드 응답 data 구조를 확인해주세요.";
          return;
        }

        // 토큰을 먼저 저장해서 /members 조회 시 Authorization 헤더에 실리도록 함
        localStorage.setItem("accessToken", accessToken);
        if (refreshToken && refreshToken.trim() !== "") {
          localStorage.setItem("refreshToken", refreshToken);
        }

        // 현재 로그인한 회원 정보 조회 (/api/v1/members)
        let profile = null;
        try {
          const profileRes = await apiClient.get("/api/v1/members");
          profile = profileRes.data?.data || null;
        } catch (e) {
          // 프로필 조회 실패해도 로그인 자체는 진행
          // eslint-disable-next-line no-console
          console.error("회원 정보 조회 실패:", e);
        }

        login({
          email: profile?.email || email.value,
          name: profile?.name,
          nickname: profile?.nickname,
          accessToken,
          refreshToken,
        });

        email.value = "";
        password.value = "";

        const targetPath = route.query.redirect || "/";
        window.location.href = targetPath;
        return;
      }

      errorMessage.value = body?.message || "로그인에 실패했습니다.";
    } catch (err) {
      const serverBody = err?.response?.data;
      errorMessage.value = serverBody?.message || "로그인 요청 중 오류가 발생했습니다.";
    }
  };

  return {
    email,
    password,
    isFormValid,
    errorMessage,
    handleLogin,
  };
}
