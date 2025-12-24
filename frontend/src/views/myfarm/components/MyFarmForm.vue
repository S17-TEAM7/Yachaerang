<template>
  <main class="flex-1 overflow-y-auto p-8 bg-gray-50">
    <div class="mx-auto max-w-4xl space-y-6">
      <!-- 헤더 -->
      <div>
        <h1 class="text-3xl font-bold tracking-tight text-gray-900">
          {{ isEdit ? '농장 수정' : '농장 등록' }}
        </h1>
        <p class="text-gray-600">
          {{ isEdit ? '농장 정보를 수정하세요' : '새로운 농장을 등록하세요' }}
        </p>
      </div>

      <!-- 폼 카드 -->
      <div class="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-gray-900">농장 정보</h2>
          <p class="text-sm text-gray-600">농장의 상세 정보를 입력해주세요</p>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-4">
          <div class="grid gap-4 md:grid-cols-2">
            <!-- 인력 -->
            <div class="space-y-2">
              <label for="manpower" class="text-sm font-medium text-gray-900">인력</label>
              <input
                id="manpower"
                v-model.number="formData.manpower"
                type="number"
                min="0"
                placeholder="예: 3"
                class="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20"
              />
            </div>

            <!-- 농장위치 -->
            <div class="space-y-2">
              <label for="location" class="text-sm font-medium text-gray-900">농장 위치</label>
              <input
                id="location"
                v-model="formData.location"
                type="text"
                placeholder="예: 충남 논산의 평지"
                class="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20"
              />
            </div>

            <!-- 주품목 -->
            <div class="space-y-2">
              <label for="mainCrop" class="text-sm font-medium text-gray-900">주품목</label>
              <input
                id="mainCrop"
                v-model="formData.mainCrop"
                type="text"
                placeholder="예: 딸기, 토마토"
                class="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20"
              />
            </div>

            <!-- 평지면적 -->
            <div class="space-y-2">
              <label for="flatArea" class="text-sm font-medium text-gray-900">평지면적 (㎡)</label>
              <input
                id="flatArea"
                v-model.number="formData.flatArea"
                type="number"
                min="0"
                placeholder="예: 1000"
                class="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20"
              />
            </div>
          </div>

          <!-- 경작면적 (전체 너비) -->
          <div class="space-y-2">
            <label for="cultivatedArea" class="text-sm font-medium text-gray-900">경작면적 (㎡)</label>
            <input
              id="cultivatedArea"
              v-model.number="formData.cultivatedArea"
              type="number"
              min="0"
              placeholder="예: 800"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20"
            />
          </div>

          <!-- 버튼 그룹 -->
          <div class="flex flex-col sm:flex-row gap-3 pt-2">
            <button
              type="submit"
              class="flex-1 rounded-lg bg-[#F44323] px-4 py-2 font-medium text-white transition-colors hover:bg-[#d63a1f]"
            >
              {{ isEdit ? '수정 완료' : '등록하기' }}
            </button>
            <button
              type="button"
              @click="showCancelModal = true"
              class="flex-1 rounded-lg border border-gray-300 bg-white px-4 py-2 font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              취소
            </button>
          </div>
        </form>
      </div>

      <!-- 취소 확인 모달 -->
      <ConfirmModal
        :show="showCancelModal"
        title="정말 취소하시겠습니까?"
        message="취소하시면 작성 내용이 날아갑니다."
        @confirm="handleCancelConfirm"
        @cancel="showCancelModal = false"
      />
    </div>
  </main>
</template>

<script setup>
import { ref, reactive } from 'vue';
import ConfirmModal from '@/components/modal/ConfirmModal.vue';

const props = defineProps({
  initialFarm: {
    type: Object,
    default: null,
  },
  isEdit: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['submitted', 'cancel']);

const formData = reactive({
  manpower: props.initialFarm?.manpower ?? null,
  location: props.initialFarm?.location ?? '',
  mainCrop: props.initialFarm?.mainCrop ?? '',
  flatArea: props.initialFarm?.flatArea ?? null,
  cultivatedArea: props.initialFarm?.cultivatedArea ?? null,
});

const showCancelModal = ref(false);

const handleSubmit = () => {
  const payload = {
    manpower: formData.manpower === '' ? 0 : Number(formData.manpower),
    location: !formData.location || formData.location.trim() === '' ? '없음' : formData.location,
    mainCrop: !formData.mainCrop || formData.mainCrop.trim() === '' ? '없음' : formData.mainCrop,
    flatArea: formData.flatArea === '' ? 0 : Number(formData.flatArea),
    cultivatedArea: formData.cultivatedArea === '' ? 0 : Number(formData.cultivatedArea),
  };
  emit('submitted', payload);
};

const handleCancelConfirm = () => {
  showCancelModal.value = false;
  emit('cancel');
};
</script>