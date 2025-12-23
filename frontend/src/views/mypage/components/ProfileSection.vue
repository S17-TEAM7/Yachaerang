<template>
  <div class="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
    <div class="mb-4">
      <h2 class="text-lg font-semibold text-gray-900">프로필 정보</h2>
      <p class="text-sm text-gray-600">개인 정보를 확인하고 수정하세요.</p>
    </div>

    <div class="space-y-6">
      <div class="flex items-center gap-6">
        <div
          class="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-[#FECC21] text-2xl font-bold text-[#F44323]"
        >
          <img v-if="form.imageUrl" :src="form.imageUrl" alt="프로필 이미지" class="h-full w-full object-cover" />
          <span v-else>{{ '👤' }}</span>
        </div>

        <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileChange" />

        <button class="btn btn-secondary" @click="triggerFileSelect">사진 선택</button>
      </div>

      <div class="space-y-4">
        <div class="grid gap-4 md:grid-cols-2">
          <div class="space-y-2">
            <label for="name" class="text-sm font-medium text-gray-900">이름</label>
            <input
              id="name"
              v-model="form.name"
              :disabled="!isEditing"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20 disabled:bg-gray-50"
            />
          </div>

          <div class="space-y-2">
            <label for="nickname" class="text-sm font-medium text-gray-900">닉네임</label>
            <input
              id="nickname"
              v-model="form.nickname"
              :disabled="!isEditing"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20 disabled:bg-gray-50"
            />
          </div>
        </div>

        <div class="space-y-2">
          <label for="email" class="text-sm font-medium text-gray-900">이메일</label>
          <input
            id="email"
            :value="form.email"
            type="email"
            disabled
            class="w-full rounded-lg border border-gray-300 bg-gray-100 px-3 py-2 text-gray-500 cursor-not-allowed"
          />
          <p v-if="isEditing" class="text-xs text-gray-500">* 이메일은 변경할 수 없습니다.</p>
        </div>

        <div class="flex gap-2">
          <template v-if="isEditing">
            <button class="btn btn-primary" @click="handleSubmit">변경사항 저장하기</button>
            <button class="btn btn-secondary" @click="cancelEdit">취소</button>
          </template>
          <button
            v-else
            class="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 font-medium text-gray-700 transition-colors hover:bg-gray-50 transition-all duration-200 hover:bg-gray-100 hover:border-gray-400 hover:-translate-y-0.5 active:translate-y-0 active:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2"
            @click="startEdit"
          >
            <IconEdit class="h-4 w-4" />
            프로필 수정하기
          </button>
        </div>
      </div>
    </div>
  </div>
  <ConfirmModal
    :show="showCancelModal"
    title="수정을 취소하시겠습니까?"
    message="변경한 내용이 저장되지 않습니다."
    @confirm="confirmCancel"
    @cancel="closeCancelModal"
  />
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { getMyProfile, updateProfile, uploadProfileImage } from '@/api/member';
import IconEdit from '@/components/icons/IconEdit.vue';
import ConfirmModal from '@/components/modal/ConfirmModal.vue';
import { useToastStore } from '@/stores/toast';

const toastStore = useToastStore();

defineProps({
  profile: {
    type: Object,
    required: false,
    default: null,
  },
});

const showCancelModal = ref(false);

const fileInput = ref(null);
const isEditing = ref(false);

// 폼 데이터 초기화
const form = reactive({
  email: '',
  name: '',
  nickname: '',
  imageUrl: '',
});

// 수정 취소 시 복구할 원본 데이터 저장소
const originalForm = reactive({
  name: '',
  nickname: '',
});

// 컴포넌트 마운트 시 내 정보 가져오기
onMounted(async () => {
  // localStorage user
  const storedUser = localStorage.getItem('user');

  if (storedUser) {
    try {
      const parsedUser = JSON.parse(storedUser);

      // localStorage user 데이터 연동
      form.email = parsedUser.email || '';
      form.name = parsedUser.name || '';
      form.nickname = parsedUser.nickname || '';

      form.imageUrl = parsedUser.imageUrl || '';

      // 수정 취소 기능
      originalForm.name = form.name;
      originalForm.nickname = form.nickname;
    } catch (e) {
      console.error('로컬스토리지 데이터 파싱 실패:', e);
    }
  }

  // 서버에서 최신 정보 가져오기
  try {
    const { data } = await getMyProfile();
    if (data && data.success) {
      const serverData = data.data;
      form.email = serverData.email;
      form.name = serverData.name;
      form.nickname = serverData.nickname;
      form.imageUrl = serverData.imageUrl;

      originalForm.name = form.name;
      originalForm.nickname = form.nickname;
    }
  } catch (e) {
    console.error('프로필 불러오기 실패 (로컬스토리지 데이터 유지):', e);
  }
});

// 파일 선택 창 열기
const triggerFileSelect = () => {
  fileInput.value?.click();
};

// 파일 변경 시 바로 업로드
const handleFileChange = async (event) => {
  const file = event.target.files?.[0];
  if (!file) return;

  const MAX_SIZE = 1 * 1024 * 1024;
  if (file.size > MAX_SIZE) {
    toastStore.show('파일 크기가 너무 큽니다. 1MB 이하의 이미지를 선택해주세요.', 'error');
    event.target.value = '';
    return;
  }

  try {
    await uploadProfileImage(file);
    const { data } = await getMyProfile();
    if (data.success) {
      form.imageUrl = data.data.imageUrl || '';
      toastStore.show('프로필 이미지가 변경되었습니다.', 'success');
    }
  } catch (e) {
    console.error('프로필 이미지 업로드 오류:', e);

    if (e.response?.status === 413) {
      toastStore.show('이미지 용량이 너무 커서 업로드할 수 없습니다.', 'error');
    } else {
      toastStore.show('이미지 업로드 중 오류가 발생했습니다.', 'error');
    }
  } finally {
    event.target.value = '';
  }
};

// 수정 모드 시작
const startEdit = () => {
  // 현재 값을 백업 (수정 중 취소할 경우를 대비)
  originalForm.name = form.name;
  originalForm.nickname = form.nickname;
  isEditing.value = true;
};

// 수정 취소
// 취소 버튼 클릭 → 모달 표시
const cancelEdit = () => {
  showCancelModal.value = true;
};

// 모달에서 "확인" → 실제 취소 실행
const confirmCancel = () => {
  form.name = originalForm.name;
  form.nickname = originalForm.nickname;
  isEditing.value = false;
  showCancelModal.value = false;
};

// 모달에서 "취소" → 모달만 닫기
const closeCancelModal = () => {
  showCancelModal.value = false;
};

// 수정 사항 저장
const handleSubmit = async () => {
  try {
    const payload = {
      name: form.name,
      nickname: form.nickname,
    };

    const response = await updateProfile(payload);

    // 성공 여부 체크 (API 응답 구조에 따라 수정 필요)
    if (response) {
      originalForm.name = form.name;
      originalForm.nickname = form.nickname;
      isEditing.value = false;
      alert('프로필이 수정되었습니다.');
    }
  } catch (e) {
    console.error(e);
    alert('프로필 수정 중 오류가 발생했습니다.');
  }
};
</script>
<style scoped>
/* 버튼 공통 스타일 */
.btn {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
  outline: none;
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.btn:focus {
  ring: 2px;
  ring-offset: 2px;
}

/* Primary 버튼 (저장) */
.btn-primary {
  background: linear-gradient(135deg, #e53935 0%, #ef5350 100%);
  color: white;
  border: none;
  box-shadow: 0 2px 8px rgba(229, 57, 53, 0.3);
}

.btn-primary:hover {
  background: linear-gradient(135deg, #c62828 0%, #e53935 100%);
  box-shadow: 0 4px 12px rgba(229, 57, 53, 0.4);
}

.btn-primary:focus {
  box-shadow: 0 0 0 3px rgba(229, 57, 53, 0.4);
}

.btn-primary:disabled {
  background: #e0e0e0;
  color: #999;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* Secondary 버튼 (취소) */
.btn-secondary {
  background: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.btn-secondary:focus {
  box-shadow: 0 0 0 3px rgba(156, 163, 175, 0.4);
}

.btn-secondary:disabled {
  background: #f9fafb;
  color: #9ca3af;
  cursor: not-allowed;
  transform: none;
}
</style>
