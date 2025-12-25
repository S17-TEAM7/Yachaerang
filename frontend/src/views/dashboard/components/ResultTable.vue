<template>
  <div
    class="p-4 sm:p-6 bg-gray-50/50 rounded-2xl bg-white rounded-xl shadow-md border border-gray-100 font-sans text-base"
  >
    <div class="overflow-x-auto -mx-4 sm:mx-0">
      <div class="inline-block min-w-full align-middle px-4 sm:px-0">
        <table v-if="isDailyOrYearlyFormat" class="w-full text-left table-fixed min-w-[800px]">
          <thead class="bg-slate-50 font-bold text-slate-500 uppercase border-b border-gray-100 sticky top-0 z-10">
            <tr>
              <th class="w-[14%] px-4 py-5 tracking-tight text-sm">거래 일자</th>
              <th class="w-[15%] px-4 py-5 text-right text-slate-900 font-black text-sm">금년 시세</th>
              <th class="w-[14%] px-4 py-5 text-right text-sm">전일 차이</th>
              <th class="w-[14%] px-4 py-5 text-right text-sm">전일 변화율</th>
              <th class="w-[15%] px-4 py-5 text-right border-l border-slate-100 text-sm">작년 시세</th>
              <th class="w-[14%] px-4 py-5 text-right text-sm">전년 변화량</th>
              <th class="w-[14%] px-4 py-5 text-right text-sm">전년 변화율</th>
            </tr>
          </thead>

          <tbody class="divide-y divide-slate-50 font-mono text-slate-700">
            <tr v-if="paginatedData.length === 0">
              <td colspan="7" class="px-6 py-12 text-center text-slate-400 font-sans italic text-lg">
                데이터가 없습니다.
              </td>
            </tr>

            <tr v-for="row in paginatedData" :key="row.date" class="hover:bg-slate-50/80 transition-colors group">
              <td class="px-4 py-4 font-bold text-slate-500 font-sans">
                <div class="flex items-center gap-1.5 flex-wrap">
                  <span class="text-base whitespace-nowrap">{{ row.date }}</span>
                </div>
              </td>

              <td class="px-4 py-4 text-right font-black text-slate-900 text-lg tracking-tighter">
                <span v-if="row.thisPrice !== null && row.thisPrice !== undefined"
                  >{{ row.thisPrice.toLocaleString() }}원</span
                >
                <span v-else class="text-slate-300">-</span>
              </td>

              <td
                class="px-4 py-4 text-right font-bold text-base"
                :class="
                  row.prevDiff !== null && row.prevDiff > 0
                    ? 'text-rose-500'
                    : row.prevDiff !== null && row.prevDiff < 0
                    ? 'text-blue-500'
                    : 'text-slate-400'
                "
              >
                <span v-if="row.prevDiff === null || row.prevDiff === undefined" class="text-slate-300">-</span>
                <template v-else>
                  <span class="hidden sm:inline text-sm mr-0.5">{{
                    row.prevDiff > 0 ? '▲' : row.prevDiff < 0 ? '▼' : '-'
                  }}</span>
                  {{ row.prevDiff === 0 ? '0' : Math.abs(row.prevDiff).toLocaleString() }}
                </template>
              </td>

              <td
                class="px-4 py-4 text-right font-bold text-base"
                :class="
                  row.prevRate !== null && row.prevRate > 0
                    ? 'text-rose-500'
                    : row.prevRate !== null && row.prevRate < 0
                    ? 'text-blue-500'
                    : 'text-slate-400'
                "
              >
                <span v-if="row.prevRate === null || row.prevRate === undefined" class="text-slate-300">-</span>
                <template v-else>
                  <span class="hidden sm:inline text-sm mr-0.5">{{
                    row.prevRate > 0 ? '▲' : row.prevRate < 0 ? '▼' : '-'
                  }}</span>
                  <span>{{ row.prevRate === 0 ? '0.0' : Math.abs(row.prevRate).toFixed(1) }}%</span>
                </template>
              </td>

              <td
                class="px-4 py-4 text-right font-bold text-slate-700 text-base tracking-tighter border-l border-slate-50"
              >
                <span v-if="row.lastPrice !== null && row.lastPrice !== undefined">
                  {{ row.lastPrice.toLocaleString() }}원
                </span>
                <span v-else class="text-slate-300">-</span>
              </td>

              <td
                class="px-4 py-4 text-right font-bold text-base"
                :class="
                  row.yoyDiff !== null && row.yoyDiff > 0
                    ? 'text-rose-600'
                    : row.yoyDiff !== null && row.yoyDiff < 0
                    ? 'text-blue-600'
                    : 'text-slate-400'
                "
              >
                <span v-if="row.yoyDiff === null || row.yoyDiff === undefined" class="text-slate-300">-</span>
                <template v-else>
                  <span class="hidden sm:inline text-sm mr-0.5">{{
                    row.yoyDiff > 0 ? '▲' : row.yoyDiff < 0 ? '▼' : '-'
                  }}</span>
                  {{ row.yoyDiff === 0 ? '0' : Math.abs(row.yoyDiff).toLocaleString() }}
                </template>
              </td>

              <td
                class="px-4 py-4 text-right font-bold text-base"
                :class="
                  row.yoyRate !== null && row.yoyRate > 0
                    ? 'text-rose-600'
                    : row.yoyRate !== null && row.yoyRate < 0
                    ? 'text-blue-600'
                    : 'text-slate-400'
                "
              >
                <span v-if="row.yoyRate === null || row.yoyRate === undefined" class="text-slate-300">-</span>
                <template v-else>
                  <span class="hidden sm:inline text-sm mr-0.5">{{
                    row.yoyRate > 0 ? '▲' : row.yoyRate < 0 ? '▼' : '-'
                  }}</span>
                  <span>{{ row.yoyRate === 0 ? '0.0' : Math.abs(row.yoyRate).toFixed(1) }}%</span>
                </template>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Weekly/Monthly aggregate format table -->
        <table v-else class="w-full text-left table-fixed min-w-[700px]">
          <thead class="bg-slate-50 font-bold text-slate-500 uppercase border-b border-gray-100 sticky top-0 z-10">
            <tr>
              <th class="w-[20%] px-4 py-5 tracking-tight text-sm">기간</th>
              <th class="w-[15%] px-4 py-5 text-right text-slate-900 font-black text-sm">평균가</th>
              <th class="w-[13%] px-4 py-5 text-right text-sm">최고가</th>
              <th class="w-[13%] px-4 py-5 text-right text-sm">최저가</th>
              <th class="w-[13%] px-4 py-5 text-right text-sm">변화량</th>
              <th class="w-[13%] px-4 py-5 text-right text-sm">변화율</th>
              <th class="w-[13%] px-4 py-5 text-right border-l border-slate-100 text-sm">작년 시세</th>
            </tr>
          </thead>

          <tbody class="divide-y divide-slate-50 font-mono text-slate-700">
            <tr v-if="paginatedData.length === 0">
              <td colspan="7" class="px-6 py-12 text-center text-slate-400 font-sans italic text-lg">
                데이터가 없습니다.
              </td>
            </tr>

            <tr v-for="row in paginatedData" :key="row.date" class="hover:bg-slate-50/80 transition-colors">
              <td class="px-4 py-4 font-bold text-slate-500 font-sans">
                <div class="flex flex-col min-w-[120px]">
                  <span class="text-sm leading-tight whitespace-pre-line break-words">{{
                    formatPeriodLabel(row.date)
                  }}</span>
                </div>
              </td>

              <td class="px-4 py-4 text-right font-black text-slate-900 text-lg tracking-tighter">
                <span v-if="row.thisPrice !== null && row.thisPrice !== undefined"
                  >{{ row.thisPrice.toLocaleString() }}원</span
                >
                <span v-else class="text-slate-300">-</span>
              </td>

              <td class="px-4 py-4 text-right font-bold text-slate-700 text-base">
                <span v-if="row.maxPrice !== null && row.maxPrice !== undefined"
                  >{{ row.maxPrice.toLocaleString() }}원</span
                >
                <span v-else class="text-slate-300">-</span>
              </td>

              <td class="px-4 py-4 text-right font-bold text-slate-700 text-base">
                <span v-if="row.minPrice !== null && row.minPrice !== undefined"
                  >{{ row.minPrice.toLocaleString() }}원</span
                >
                <span v-else class="text-slate-300">-</span>
              </td>

              <td
                class="px-4 py-4 text-right font-bold text-base"
                :class="
                  row.prevDiff !== null && row.prevDiff > 0
                    ? 'text-rose-500'
                    : row.prevDiff !== null && row.prevDiff < 0
                    ? 'text-blue-500'
                    : 'text-slate-400'
                "
              >
                <span v-if="row.prevDiff === null || row.prevDiff === undefined" class="text-slate-300">-</span>
                <template v-else>
                  <span class="hidden sm:inline text-sm mr-0.5">{{
                    row.prevDiff > 0 ? '▲' : row.prevDiff < 0 ? '▼' : '-'
                  }}</span>
                  {{ row.prevDiff === 0 ? '0' : Math.abs(row.prevDiff).toLocaleString() }}
                </template>
              </td>

              <td
                class="px-4 py-4 text-right font-bold text-base"
                :class="
                  row.prevRate !== null && row.prevRate > 0
                    ? 'text-rose-500'
                    : row.prevRate !== null && row.prevRate < 0
                    ? 'text-blue-500'
                    : 'text-slate-400'
                "
              >
                <span v-if="row.prevRate === null || row.prevRate === undefined" class="text-slate-300">-</span>
                <template v-else>
                  <span class="hidden sm:inline text-sm mr-0.5">{{
                    row.prevRate > 0 ? '▲' : row.prevRate < 0 ? '▼' : '-'
                  }}</span>
                  <span>{{ row.prevRate === 0 ? '0.0' : Math.abs(row.prevRate).toFixed(1) }}%</span>
                </template>
              </td>

              <td
                class="px-4 py-4 text-right font-bold text-slate-700 text-base tracking-tighter border-l border-slate-50"
              >
                <span v-if="row.lastPrice !== null && row.lastPrice !== undefined">
                  {{ row.lastPrice.toLocaleString() }}원
                </span>
                <span v-else class="text-slate-300">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div
      v-if="totalPages > 1"
      class="p-5 flex justify-center items-center gap-2 bg-slate-50/50 font-sans border-t border-slate-100"
    >
      <button
        @click="$emit('updatePage', Math.max(1, currentPage - 1))"
        :disabled="currentPage === 1"
        class="page-nav-btn"
      >
        이전
      </button>

      <button
        v-for="p in visiblePages"
        :key="p"
        @click="$emit('updatePage', p)"
        :class="
          currentPage === p
            ? 'bg-slate-800 text-white shadow-md border-slate-800'
            : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
        "
        class="w-9 h-9 rounded-lg text-base font-bold border transition-all hover:scale-105 active:scale-95 flex items-center justify-center"
      >
        {{ p }}
      </button>

      <button
        @click="$emit('updatePage', Math.min(totalPages, currentPage + 1))"
        :disabled="currentPage === totalPages"
        class="page-nav-btn"
      >
        다음
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  paginatedData: Array,
  totalPages: Number,
  currentPage: Number,
  periodType: {
    type: String,
    default: 'day',
  },
});

const emit = defineEmits(['updatePage']);

const isDailyOrYearlyFormat = computed(() => {
  if (!props.paginatedData || props.paginatedData.length === 0) return true;

  const firstRow = props.paginatedData[0];
  return firstRow.hasOwnProperty('yoyDiff') || firstRow.hasOwnProperty('yoyRate');
});

const formatPeriodLabel = (label) => {
  if (!label) return '';

  if (label.includes('~')) {
    const parts = label.split('~');
    if (parts.length === 2) {
      return `${parts[0].trim()}\n~ ${parts[1].trim()}`;
    }
  }

  return label;
};

const visiblePages = computed(() => {
  const range = 10;
  const currentGroup = Math.ceil(props.currentPage / range);
  const startPage = (currentGroup - 1) * range + 1;
  const endPage = Math.min(startPage + range - 1, props.totalPages);

  const pages = [];
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i);
  }
  return pages;
});
</script>

<style scoped>
@import '../../../assets/main.css';
.page-nav-btn {
  @apply w-12 h-9 flex items-center justify-center rounded-md border border-slate-200 bg-white text-slate-600 text-sm font-bold transition-all hover:bg-slate-50 disabled:opacity-30 disabled:hover:bg-white;
}
div {
  scrollbar-width: thin;
  scrollbar-color: #e2e8f0 transparent;
}

/* Mobile: allow horizontal scroll */
@media (max-width: 640px) {
  .overflow-x-auto {
    -webkit-overflow-scrolling: touch;
  }
}
</style>
