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
              <button
                type="button"
                @click="isCropModalOpen = true"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-left text-sm font-semibold text-gray-800 flex items-center justify-between bg-white hover:border-[#F44323] transition-all focus:border-[#F44323] focus:outline-none focus:ring-2 focus:ring-[#F44323]/20"
              >
                <span class="truncate">{{ selectedCropLabel || '품목을 선택하세요' }}</span>
                <i class="fa-solid fa-chevron-down text-gray-400 ml-2"></i>
              </button>
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

      <!-- 작물 선택 모달 -->
      <div
        v-if="isCropModalOpen"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm"
      >
        <div class="bg-white w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden animate-in">
          <div class="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
            <h3 class="text-lg font-bold">주품목 선택 (중복 가능)</h3>
            <button @click="isCropModalOpen = false" class="text-slate-300 hover:text-red-500 text-2xl font-light">
              ×
            </button>
          </div>

          <div class="p-5 grid grid-cols-3 md:grid-cols-4 gap-2 max-h-[50vh] overflow-y-auto">
            <button
              v-for="item in itemOptions"
              :key="item.value"
              @click="toggleCrop(item.label)"
              :class="
                isSelected(item.label)
                  ? 'border-red-500 bg-red-50 text-red-600 shadow-sm ring-1 ring-red-100'
                  : 'border-gray-100 bg-gray-50/60 text-gray-600 hover:bg-gray-100'
              "
              class="py-3 px-2.5 border rounded-xl text-sm font-semibold truncate transition-all flex items-center justify-center text-center"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="p-4 bg-gray-50 border-t flex justify-end">
            <button
              @click="isCropModalOpen = false"
              class="bg-red-500 text-white px-6 py-2 rounded-lg font-bold hover:bg-red-600 transition-colors"
            >
              선택 완료
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import ConfirmModal from '@/components/modal/ConfirmModal.vue';
import { fetchItemsApi } from '@/api/price';

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
const isCropModalOpen = ref(false);
const itemOptions = ref([]);

const selectedCropLabel = computed(() => {
  if (!formData.mainCrop) return '';
  return formData.mainCrop;
});

const fetchItems = async () => {
  try {
    const res = await fetchItemsApi();
    const body = res.data;
    const list = Array.isArray(body) ? body : Array.isArray(body?.data) ? body.data : [];
    itemOptions.value = list.map((item) => ({
      value: String(item.itemCode ?? item.code ?? item.id),
      label: item.itemName ?? item.name ?? '',
    }));
  } catch (error) {
    console.error('품목 목록 조회 실패:', error);
    itemOptions.value = [];
  }
};

const toggleCrop = (itemLabel) => {
  let selectedArray = formData.mainCrop ? formData.mainCrop.split(',').filter((item) => item.trim() !== '') : [];

  const index = selectedArray.indexOf(itemLabel);

  if (index > -1) {
    selectedArray.splice(index, 1);
  } else {
    selectedArray.push(itemLabel);
  }

  formData.mainCrop = selectedArray.join(',');
};

const isSelected = (itemLabel) => {
  if (!formData.mainCrop) return false;
  return formData.mainCrop.split(',').includes(itemLabel);
};

onMounted(() => {
  fetchItems();
});

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
