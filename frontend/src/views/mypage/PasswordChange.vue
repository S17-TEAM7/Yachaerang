<template>
  <main class="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-8">
    <div class="w-full max-w-2xl space-y-6">
      <!-- 헤더 -->
      <div>
        <h1 class="text-3xl font-bold tracking-tight text-gray-900">비밀번호 변경</h1>
        <p class="mt-2 text-gray-600">보안을 위해 정기적으로 비밀번호를 변경해주세요</p>
      </div>

      <!-- 폼 카드 -->
      <div class="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <form @submit.prevent="handleSubmit" class="space-y-6">
          <!-- 현재 비밀번호 -->
          <div class="space-y-2">
            <label for="currentPassword" class="text-sm font-medium text-gray-900">현재 비밀번호</label>
            <input
              id="currentPassword"
              v-model="currentPassword"
              type="password"
              class="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20 transition-colors"
              placeholder="현재 비밀번호를 입력해주세요"
            />
            <p v-if="currentPasswordMsg" :class="currentPasswordClass" class="text-xs mt-1">
              {{ currentPasswordMsg }}
            </p>
          </div>

          <!-- 새 비밀번호 -->
          <div class="space-y-2">
            <label for="newPassword" class="text-sm font-medium text-gray-900">새 비밀번호</label>
            <input
              id="newPassword"
              v-model="newPassword"
              type="password"
              class="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20 transition-colors"
              placeholder="문자, 숫자, 특수문자 포함 8~20자"
            />
            <p v-if="newPassword" :class="newPasswordClass" class="text-xs mt-1">
              {{ newPasswordMsg }}
            </p>
          </div>

          <!-- 새 비밀번호 확인 -->
          <div class="space-y-2">
            <label for="newPasswordConfirm" class="text-sm font-medium text-gray-900">새 비밀번호 확인</label>
            <input
              id="newPasswordConfirm"
              v-model="newPasswordConfirm"
              type="password"
              class="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20 transition-colors"
              placeholder="새 비밀번호를 다시 입력해주세요"
            />
            <p v-if="newPasswordConfirm" :class="confirmClass" class="text-xs mt-1">
              {{ confirmMsg }}
            </p>
          </div>

          <!-- 비밀번호 안내 -->
          <div class="rounded-lg bg-gray-50 border border-gray-200 p-4">
            <p class="text-xs text-gray-500 font-medium mb-1">비밀번호 규칙</p>
            <ul class="text-xs text-gray-500 space-y-1 list-disc list-inside">
              <li>문자, 숫자, 특수문자(@$!%*#?&)를 포함해야 합니다</li>
              <li>8자 이상 20자 이하여야 합니다</li>
              <li>현재 비밀번호와 동일할 수 없습니다</li>
            </ul>
          </div>

          <!-- 버튼 -->
          <button
            type="submit"
            class="w-full rounded-lg bg-[#F44323] px-4 py-3 font-semibold text-white transition-all hover:bg-[#d63a1f] hover:shadow-md active:scale-[0.98]"
          >
            비밀번호 변경
          </button>
        </form>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed } from 'vue';
import { changePasswordApi } from '@/api/member';
import { useAuthStore } from '@/stores/auth';
import { useToastStore } from '@/stores/toast';
import { useAlertStore } from '@/stores/alert';

const authStore = useAuthStore();
const toastStore = useToastStore();
const alertStore = useAlertStore();

const currentPassword = ref('');
const newPassword = ref('');
const newPasswordConfirm = ref('');

const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/;

// 현재 비밀번호와 새 비밀번호 동일 여부
const isSameAsCurrent = computed(() => {
  if (!currentPassword.value || !newPassword.value) return false;
  return currentPassword.value === newPassword.value;
});

// 현재 비밀번호 메시지
const currentPasswordMsg = computed(() => {
  if (!currentPassword.value) return '';
  return '';
});

const currentPasswordClass = computed(() => {
  return 'text-gray-500';
});

// 새 비밀번호 안내 메시지(동일 여부 우선)
const newPasswordMsg = computed(() => {
  if (!newPassword.value) return '';
  if (isSameAsCurrent.value) return '현재 비밀번호와 다른 비밀번호를 입력해주세요.';
  if (!passwordRegex.test(newPassword.value)) return '문자, 숫자, 특수문자를 포함한 8~20자여야 합니다.';
  return '사용 가능한 비밀번호입니다.';
});

const newPasswordClass = computed(() => {
  if (!newPassword.value) return 'text-gray-500';
  if (isSameAsCurrent.value) return 'text-red-600';
  return passwordRegex.test(newPassword.value) ? 'text-green-600' : 'text-red-600';
});

const confirmMsg = computed(() => {
  if (!newPasswordConfirm.value) return '';
  return newPassword.value === newPasswordConfirm.value ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
});

const confirmClass = computed(() => {
  if (!newPasswordConfirm.value) return 'text-gray-500';
  return newPassword.value === newPasswordConfirm.value ? 'text-green-600' : 'text-red-600';
});

const handleSubmit = async () => {
  if (!currentPassword.value) {
    toastStore.show('현재 비밀번호를 입력해주세요.', 'warning');
    return;
  }

  if (!newPassword.value) {
    toastStore.show('새 비밀번호를 입력해주세요.', 'warning');
    return;
  }

  // 동일하면 요청 자체 차단
  if (isSameAsCurrent.value) {
    toastStore.show('현재 비밀번호와 다른 비밀번호를 입력해주세요.', 'warning');
    return;
  }

  if (!passwordRegex.test(newPassword.value)) {
    toastStore.show('새 비밀번호 형식을 확인해주세요.', 'warning');
    return;
  }

  if (newPassword.value !== newPasswordConfirm.value) {
    toastStore.show('새 비밀번호가 일치하지 않습니다.', 'warning');
    return;
  }

  try {
    await changePasswordApi(currentPassword.value, newPassword.value);

    alertStore.showAlert('비밀번호를 수정하였습니다.\n다시 로그인해주세요.', '비밀번호 변경 완료', async () => {
      await authStore.logout();
    });
  } catch (error) {
    const errorResponse = error.response?.data;
    const errorCode = errorResponse?.code;

    if (errorCode === 'LOGIN_001') {
      toastStore.show('현재 비밀번호가 올바르지 않습니다.', 'error');
      return;
    }
    if (errorCode === 'LOGIN_002') {
      toastStore.show(errorResponse?.message || '비밀번호 변경에 실패했습니다.', 'error');
      return;
    }

    toastStore.show('비밀번호 변경 중 오류가 발생했습니다.', 'error');
    console.error('change password error:', error);
  }
};
</script>

<style scoped>
/* 추가 커스텀 스타일이 필요한 경우 여기에 작성 */
</style>
