<template>
  <main class="min-h-screen bg-gray-50 pb-10 text-base">
    <div class="max-w-9xl mx-auto px-4 sm:px-6 pt-4 md:pt-6 w-full">
      <div class="flex justify-between items-center gap-4">
        <div class="min-w-0 flex-1">
          <PageHeader title="대시보드" description="전국 주요 시장 상세 시세 분석 데이터" />
        </div>
      </div>
    </div>

    <div class="max-w-9xl mx-auto px-4 sm:px-6 pt-2 w-full">
      <div class="grid grid-cols-12 gap-4 md:gap-6 pt-2">
        <div
          class="w-full max-w-[320px] col-span-12 lg:col-span-3 lg:col-start-10 order-1 lg:order-2 flex flex-col gap-4"
        >
          <DashboardFilter
            v-model:selectedItem="selectedItem"
            v-model:selectedVariety="selectedVariety"
            v-model:dayStartDate="dayStartDate"
            v-model:dayEndDate="dayEndDate"
            v-model:weekStartDate="weekStartDate"
            v-model:weekEndDate="weekEndDate"
            v-model:monthStartDate="monthStartDate"
            v-model:monthEndDate="monthEndDate"
            v-model:yearStart="yearStart"
            v-model:yearEnd="yearEnd"
            :yearOptions="yearOptions"
            :itemOptions="itemOptions"
            :varietyOptions="varietyOptions"
            :periodTabs="periodTabs"
            :currentPeriod="periodType"
            :selectedItemName="selectedItemLabel"
            @updatePeriod="handlePeriodClick"
            @search="triggerSearch"
            @add-favorite="handleAddFavorite"
          />

          <RecentViewedItems :items="recentItems" @select="handleRecentSelect" @clear="handleClearRecent" />
        </div>

        <div class="col-span-12 lg:col-span-9 order-2 lg:order-1 flex flex-col gap-4">
          <DashboardSummary
            :priceResult="priceResult"
            :lastYearPrices="lastYearPrices"
            :itemName="selectedItemLabel"
            :varietyName="selectedVarietyLabel"
            :hasSearched="hasSearched"
            :periodType="periodType"
            :dateRangeLabel="currentDateRangeLabel"
          />

          <div
            v-if="!hasSearched"
            class="bg-white p-20 rounded-xl border border-gray-200 text-center text-gray-400 shadow-sm"
          >
            <p class="text-lg">조회하기 버튼을 클릭하여 데이터를 조회하세요.</p>
          </div>

          <template v-else-if="hasSearched">
            <EmptyResult v-if="!priceResult || priceResult.length === 0" />
            <template v-else>
              <ResultGraph
                :chartData="formattedChartData"
                :periodType="periodType"
                :priceResult="priceResult"
                :weekStartDate="weekStartDate"
                :weekEndDate="weekEndDate"
                :monthStartDate="monthStartDate"
                :monthEndDate="monthEndDate"
                :yearStart="yearStart"
                :yearEnd="yearEnd"
              />
              <ResultTable
                :paginatedData="paginatedData"
                :totalPages="totalPages"
                :currentPage="currentPage"
                :periodType="periodType"
                @updatePage="(p) => (currentPage = p)"
              />
            </template>
          </template>
        </div>
      </div>
    </div>

    <ConfirmModal
      :show="showClearConfirm"
      title="최근 조회 기록 삭제"
      message="최근 조회 기록을 모두 삭제하시겠습니까?"
      @confirm="handleClearConfirm"
      @cancel="showClearConfirm = false"
    />
  </main>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { usePriceSearch } from '@/views/dashboard/composables/usePriceSearch';
import { useToastStore } from '@/stores/toast';
import DashboardFilter from '@/views/dashboard/components/DashboardFilter.vue';
import DashboardSummary from '@/views/dashboard/components/DashboardSummary.vue';
import ResultGraph from '@/views/dashboard/components/ResultGraph.vue';
import ResultTable from '@/views/dashboard/components/ResultTable.vue';

import PageHeader from '@/components/layout/PageHeader.vue';
import EmptyResult from '@/views/dashboard/components/EmptyResult.vue';
import RecentViewedItems from '@/views/dashboard/components/RecentViewedItems.vue';
import ConfirmModal from '@/components/modal/ConfirmModal.vue';

const {
  selectedItem,
  selectedVariety,
  itemOptions,
  varietyOptions,
  priceResult,
  lastYearPrices,
  periodType,
  periodTabs,
  yearOptions,
  dayStartDate,
  dayEndDate,
  weekStartDate,
  weekEndDate,
  monthStartDate,
  monthEndDate,
  yearStart,
  yearEnd,
  hasSearched,
  recentItems,
  handlePeriodClick,
  handleSearch,
  handleAddFavorite,
  applyRecentItem,
  clearRecentSearches,
  initializeFromRank,
} = usePriceSearch();

const route = useRoute();
const toast = useToastStore();

const selectedItemLabel = computed(() => {
  if (!selectedItem.value) return '품목 선택';
  const target = itemOptions.value.find((opt) => opt.value === selectedItem.value);
  return target ? target.label : '품목 선택';
});

const selectedVarietyLabel = computed(() => {
  if (!selectedVariety.value) return '품종 선택';
  const target = varietyOptions.value.find((o) => o.value === selectedVariety.value);
  const label = target ? target.label : '품종 선택';
  return label.replace(/-/g, ', ');
});

const currentDateRangeLabel = computed(() => {
  if (periodType.value === 'day') {
    return `${dayStartDate.value || ''} ~ ${dayEndDate.value || ''}`;
  } else if (periodType.value === 'week') {
    return `${weekStartDate.value || ''} ~ ${weekEndDate.value || ''}`;
  } else if (periodType.value === 'month') {
    return `${monthStartDate.value || ''} ~ ${monthEndDate.value || ''}`;
  } else if (periodType.value === 'year') {
    return `${yearStart.value || ''}년 ~ ${yearEnd.value || ''}년`;
  }
  return '';
});

// --- Computed: 차트 데이터 포맷팅 ---
const normalizedLastYearPrices = computed(() => {
  if (!Array.isArray(lastYearPrices.value) || lastYearPrices.value.length === 0) {
    return priceResult.value.map(() => null);
  }

  return lastYearPrices.value.map((val) => (typeof val === 'number' ? val : null));
});

const formattedChartData = computed(() => {
  // day 모드: 기존 방식 유지
  if (periodType.value === 'day') {
    return {
      labels: priceResult.value.map((item) => item.dateLabel),
      thisPrices: priceResult.value.map((item) => item.priceLabel),
      lastPrices: normalizedLastYearPrices.value,
    };
  }

  // week/month/year 모드: 집계 데이터 포함
  return {
    labels: priceResult.value.map((item) => item.dateLabel),
    thisPrices: priceResult.value.map((item) => item.priceLabel),
    lastPrices: normalizedLastYearPrices.value,
    minPrices: priceResult.value.map((item) => item.minPrice ?? null),
    maxPrices: priceResult.value.map((item) => item.maxPrice ?? null),
  };
});

// --- Pagination: 테이블 데이터 처리 ---
const currentPage = ref(1);
const itemsPerPage = 5;
const totalPages = computed(() => {
  if (priceResult.value.length === 0) return 1;
  return Math.ceil(priceResult.value.length / itemsPerPage);
});

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  const end = start + itemsPerPage;

  return priceResult.value.slice(start, end).map((item, index) => {
    const globalIndex = start + index;
    const thisPrice = item.priceLabel ?? null;
    const lastPrice = normalizedLastYearPrices.value[globalIndex] ?? null;

    const hasAggregateFields = item.hasOwnProperty('minPrice') || item.hasOwnProperty('maxPrice');

    if (hasAggregateFields && (periodType.value === 'week' || periodType.value === 'month')) {
      return {
        date: item.dateLabel,
        thisPrice,
        minPrice: item.minPrice ?? null,
        maxPrice: item.maxPrice ?? null,
        prevDiff: item.priceChange ?? null,
        prevRate: item.priceChangeRate ?? null,
        lastPrice,
      };
    } else {
      // Daily/Yearly format
      const yoyDiff = thisPrice !== null && lastPrice !== null && lastPrice > 0 ? thisPrice - lastPrice : null;
      const yoyRate = thisPrice !== null && lastPrice !== null && lastPrice > 0 ? (yoyDiff / lastPrice) * 100 : null;

      const validPrices = priceResult.value
        .map((r) => r.priceLabel)
        .filter((p) => p !== null && p !== undefined && typeof p === 'number');
      const maxVal = validPrices.length ? Math.max(...validPrices) : null;
      const minVal = validPrices.length ? Math.min(...validPrices) : null;
      const isMax = thisPrice === maxVal;
      const isMin = thisPrice === minVal;

      return {
        date: item.dateLabel,
        thisPrice,
        prevDiff: item.priceChange ?? null,
        prevRate: item.priceChangeRate ?? null,
        lastPrice,
        yoyDiff,
        yoyRate,
        isMax,
        isMin,
      };
    }
  });
});

const STORAGE_KEY = 'price-search-state';

watch(
  () => ({
    selectedItem: selectedItem.value,
    selectedVariety: selectedVariety.value,
    periodType: periodType.value,
    dayStartDate: dayStartDate.value,
    dayEndDate: dayEndDate.value,
    weekStartDate: weekStartDate.value,
    weekEndDate: weekEndDate.value,
    monthStartDate: monthStartDate.value,
    monthEndDate: monthEndDate.value,
    yearStart: yearStart.value,
    yearEnd: yearEnd.value,
    priceResult: priceResult.value,
    hasSearched: hasSearched.value,
    currentPage: currentPage.value,
  }),

  (state) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  },
  { deep: true }
);

onMounted(async () => {
  const { productCode, source } = route.query || {};

  if (productCode && (source === 'rank' || source === 'favorite')) {
    await initializeFromRank(productCode);
    await handleSearch();
    return;
  }

  const saved = localStorage.getItem(STORAGE_KEY);

  if (saved) {
    const state = JSON.parse(saved);

    selectedItem.value = state.selectedItem || '';
    selectedVariety.value = state.selectedVariety || '';
    periodType.value = state.periodType || 'year';

    dayStartDate.value = state.dayStartDate ?? null;
    dayEndDate.value = state.dayEndDate ?? null;
    weekStartDate.value = state.weekStartDate ?? null;
    weekEndDate.value = state.weekEndDate ?? null;
    monthStartDate.value = state.monthStartDate ?? null;
    monthEndDate.value = state.monthEndDate ?? null;
    yearStart.value = state.yearStart ?? '';
    yearEnd.value = state.yearEnd ?? '';

    if (state.selectedItem && state.selectedVariety && state.hasSearched) {
      await handleSearch();
    }

    currentPage.value = state.currentPage || 1;
  } else {
    selectedItem.value = '9903';
    selectedVariety.value = 'KM-9903-23-71';
    periodType.value = 'day';
    dayStartDate.value = '2025-11-01';
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    dayEndDate.value = yesterday.toISOString().slice(0, 10);
  }
});

const triggerSearch = async () => {
  if (periodType.value === 'day') {
    if (periodType.value === 'day' && dayStartDate.value === dayEndDate.value) {
      toast.show('일간 조회 시 시작일과 종료일은 같을 수 없습니다.', 'error');
      return;
    }
  }
  currentPage.value = 1;
  await handleSearch();
};

const handleRecentSelect = async (item) => {
  currentPage.value = 1;
  await applyRecentItem(item);
};

const showClearConfirm = ref(false);
const handleClearRecent = () => {
  showClearConfirm.value = true;
};
const handleClearConfirm = () => {
  clearRecentSearches();
  showClearConfirm.value = false;
};
</script>
