<template>
  <main class="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-8">
    <div class="w-full max-w-2xl space-y-6">
      <!-- 헤더 -->
      <div class="mb-10">
        <h1 class="text-3xl font-bold tracking-tight text-gray-900">비밀번호 찾기</h1>
        <p class="mt-2 text-gray-600">가입하신 이메일로 인증번호를 받아 비밀번호를 재설정하세요</p>
      </div>

      <!-- 폼 카드 -->
      <div class="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <form @submit.prevent="handleSubmit" class="space-y-6">
          <!-- 이메일 입력 -->
          <div class="space-y-2">
            <label for="email" class="text-sm font-medium text-gray-900">이메일</label>
            <input
              id="email"
              v-model="email"
              type="email"
              :disabled="emailSent"
              class="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20 transition-colors disabled:bg-gray-100 disabled:cursor-not-allowed"
              placeholder="가입하신 이메일을 입력해주세요"
            />
            <p v-if="emailSent" class="text-xs text-green-600 mt-1">입력하신 이메일로 인증번호를 발송했습니다.</p>
          </div>

          <!-- 인증번호 입력 -->
          <div v-if="emailSent" class="space-y-2">
            <label for="emailCode" class="text-sm font-medium text-gray-900">인증번호</label>
            <input
              id="emailCode"
              v-model="emailCode"
              type="text"
              maxlength="6"
              class="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20 transition-colors"
              placeholder="인증번호 6자리 입력"
            />
          </div>

          <!-- 버튼 -->
          <div class="space-y-3">
            <button
              v-if="!emailSent"
              type="button"
              @click="sendResetCode"
              :disabled="isLoading || !email"
              class="w-full rounded-lg bg-[#F44323] px-4 py-3 font-semibold text-white transition-all hover:bg-[#d63a1f] hover:shadow-md active:scale-[0.98] disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {{ isLoading ? "발송 중..." : "인증번호 보내기" }}
            </button>
            <button
              v-else
              type="button"
              @click="resetPasswordWithCode"
              :disabled="isLoading || !emailCode || emailCode.length !== 6"
              class="w-full rounded-lg bg-[#F44323] px-4 py-3 font-semibold text-white transition-all hover:bg-[#d63a1f] hover:shadow-md active:scale-[0.98] disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {{ isLoading ? "인증 중..." : "인증하기" }}
            </button>
          </div>

          <!-- 로그인으로 돌아가기 -->
          <div class="pt-4 border-t border-gray-100">
            <router-link :to="{ name: 'login' }" class="block text-center text-sm text-gray-600 hover:text-[#F44323] transition-colors">로그인으로 돌아가기</router-link>
          </div>
        </form>
      </div>
    </div>
  </main>
</template>

<script setup>
import { useForgotPasswordForm } from "@/views/auth/composables/useForgotPasswordForm";

const { email, emailSent, emailCode, isLoading, sendResetCode, resetPasswordWithCode } = useForgotPasswordForm();

const handleSubmit = () => {
  if (!emailSent) {
    sendResetCode();
  } else {
    resetPasswordWithCode();
  }
};
</script>
