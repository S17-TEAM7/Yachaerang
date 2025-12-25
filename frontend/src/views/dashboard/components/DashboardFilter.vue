<template>
  <div class="bg-white p-6 md:p-7 rounded-xl shadow-sm border border-gray-200">
    <div class="flex justify-between items-center mb-5">
      <h2 class="text-lg md:text-xl font-black text-slate-800 flex items-center gap-2">시세 검색</h2>
      <FavoriteButton @click="$emit('add-favorite')" />
    </div>
    <div class="space-y-4">
      <div>
        <label class="text-base font-bold text-gray-400 block mb-2 uppercase tracking-tighter">품목 선택</label>
        <button
          type="button"
          @click="isModalOpen = true"
          class="w-full border border-gray-200 rounded-xl px-4 py-3 text-base font-semibold text-gray-800 flex items-center justify-between bg-gradient-to-r from-gray-50 to-gray-100 hover:from-red-50 hover:to-white hover:border-red-300 transition-all shadow-[0_1px_3px_rgba(15,23,42,0.08)]"
        >
          <div class="flex flex-col items-start gap-0.5 overflow-hidden">
            <span class="text-base font-bold text-gray-800 truncate">
              {{ selectedItemName || "품목을 선택하세요" }}
            </span>
          </div>
        </button>
      </div>

      <div>
        <label class="text-base font-bold text-gray-400 block mb-2 uppercase tracking-tighter">품종 선택</label>
        <select v-model="modelVariety" class="w-full bg-gray-50 border border-gray-200 rounded-lg p-3 text-base font-semibold text-gray-700 focus:border-red-300 outline-none transition-all">
          <option value="" disabled>품종을 선택하세요</option>
          <option v-for="opt in varietyOptions" :key="opt.value" :value="opt.value">
            {{ opt.label.replace(/-/g, ", ") }}
          </option>
        </select>
      </div>

      <div>
        <label class="text-base font-bold text-gray-400 block mb-2 uppercase tracking-tighter">기간 기준 선택</label>
        <div class="grid grid-cols-4 gap-1 bg-gray-100 p-1 rounded-lg font-bold">
          <button
            v-for="tab in periodTabs"
            :key="tab.value"
            type="button"
            @click="$emit('updatePeriod', tab.value)"
            :class="currentPeriod === tab.value ? 'bg-white text-red-500 shadow-sm' : 'text-slate-400'"
            class="py-2 text-base font-bold rounded-md transition-all"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div class="space-y-1.5">
        <label class="text-base font-bold text-gray-400 block uppercase tracking-tighter">날짜 선택</label>

        <div v-if="currentPeriod === 'day'" class="space-y-1.5">
          <input type="date" v-model="modelDayStart" :max="maxDate" class="date-input" />
          <input type="date" v-model="modelDayEnd" :max="maxDate" class="date-input" />
        </div>

        <div v-else-if="currentPeriod === 'week'" class="space-y-1.5">
          <div class="relative">
            <input type="date" v-model="modelWeekStart" :max="maxWeekDate" @change="handleWeekStartChange" class="date-input" />
          </div>
          <div class="relative">
            <input type="date" v-model="modelWeekEnd" :max="maxWeekDate" @change="handleWeekEndChange" class="date-input" />
          </div>
          <p v-if="modelWeekStart && modelWeekEnd" class="text-xs text-gray-500 font-medium">선택된 주: {{ modelWeekStart }} ~ {{ modelWeekEnd }}</p>
        </div>

        <div v-else-if="currentPeriod === 'month'" class="space-y-1.5">
          <input type="month" v-model="modelMonthStart" :max="maxMonth" class="date-input" />
          <input type="month" v-model="modelMonthEnd" :max="maxMonth" class="date-input" />
        </div>

        <div v-else-if="currentPeriod === 'year'" class="grid grid-cols-2 gap-2">
          <select v-model="modelYearStart" class="date-input">
            <option value="">시작</option>
            <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
          </select>
          <select v-model="modelYearEnd" class="date-input">
            <option value="">종료</option>
            <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
          </select>
        </div>
      </div>

      <button @click="$emit('search')" class="w-full bg-slate-900 text-white py-3.5 rounded-lg text-base font-bold hover:bg-red-500 transition-all active:scale-95 shadow-md">조회하기</button>
    </div>

    <div v-if="isModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
      <div class="bg-white w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden animate-in">
        <div class="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
          <h3 class="text-lg font-bold">품목 선택</h3>
          <button @click="isModalOpen = false" class="text-slate-300 hover:text-red-500 text-2xl font-light">×</button>
        </div>
        <div class="p-5 grid grid-cols-3 md:grid-cols-4 gap-2 max-h-[50vh] overflow-y-auto">
          <button
            v-for="item in itemOptions"
            :key="item.value"
            @click="selectItem(item.value)"
            :class="modelItem === item.value ? 'border-red-500 bg-red-50 text-red-600 shadow-sm ring-1 ring-red-100' : 'border-gray-100 bg-gray-50/60 text-gray-600 hover:bg-gray-100'"
            class="py-3 px-2.5 border rounded-xl text-base font-semibold truncate transition-all flex items-center justify-center text-center"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import FavoriteButton from "./FavoriteButton.vue";

const props = defineProps({
  itemOptions: Array,
  varietyOptions: Array,
  periodTabs: Array,
  currentPeriod: String,
  selectedItemName: String,
  yearOptions: Array,
});

const modelItem = defineModel("selectedItem");
const modelVariety = defineModel("selectedVariety");
const modelDayStart = defineModel("dayStartDate");
const modelDayEnd = defineModel("dayEndDate");
const modelWeekStart = defineModel("weekStartDate");
const modelWeekEnd = defineModel("weekEndDate");
const modelMonthStart = defineModel("monthStartDate");
const modelMonthEnd = defineModel("monthEndDate");
const modelYearStart = defineModel("yearStart");
const modelYearEnd = defineModel("yearEnd");

const emit = defineEmits(["search", "updatePeriod", "add-favorite"]);
const isModalOpen = ref(false);

// 미래 일자는 선택 불가 (데이터는 어제까지)
const maxDate = computed(() => {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return d.toISOString().split("T")[0];
});

// 주간: 완전히 끝난 지난 주의 일요일까지만 선택 가능
const maxWeekDate = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const weekday = today.getDay(); // 0(일)~6(토)
  const daysSinceMonday = (weekday + 6) % 7;
  const lastWeekSunday = new Date(today);
  lastWeekSunday.setDate(today.getDate() - daysSinceMonday - 1);
  return lastWeekSunday.toISOString().split("T")[0];
});

// 월 단위는 "완전히 끝난 지난 달"까지만 허용 (이번 달은 제외)
const maxMonth = computed(() => {
  const d = new Date();
  d.setMonth(d.getMonth() - 1);
  return d.toISOString().substring(0, 7);
});

const formatDate = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
};

const handleWeekStartChange = (e) => {
  const val = e.target.value;
  if (!val) return;
  const d = new Date(val);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  modelWeekStart.value = formatDate(new Date(d.setDate(diff)));
};

const handleWeekEndChange = (e) => {
  const val = e.target.value;
  if (!val) return;
  const d = new Date(val);
  const day = d.getDay();
  const diff = d.getDate() + (day === 0 ? 0 : 7 - day);

  let sunday = new Date(d.setDate(diff));
  const max = new Date(maxWeekDate.value);
  if (sunday > max) {
    sunday = max;
  }

  modelWeekEnd.value = formatDate(sunday);
};

const selectItem = (itemCode) => {
  modelItem.value = itemCode;
  modelVariety.value = "";
  isModalOpen.value = false;
};
</script>

<style scoped>
@import "@/assets/main.css";
.date-input {
  @apply w-full bg-gray-50 border border-gray-200 rounded-lg p-3 text-base font-bold text-gray-600 focus:border-red-300 outline-none transition-all;
}
.animate-in {
  animation: modalIn 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.98) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
