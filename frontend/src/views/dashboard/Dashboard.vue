<template>
  <main class="min-h-screen bg-gray-50 pb-10">
    <div class="max-w-9xl mx-auto px-4 sm:px-6 pt-4 md:pt-6 w-full">
      <div class="flex justify-between items-center gap-4">
        <div class="min-w-0 flex-1">
          <PageHeader title="대시보드" description="전국 주요 시장 상세 시세 분석 데이터" />
        </div>
      </div>
    </div>

    <div class="max-w-9xl mx-auto px-4 sm:px-6 pt-2 w-full">
      <div class="grid grid-cols-12 gap-4 md:gap-6 pt-2">
        <div class="col-span-12 lg:col-span-3 lg:col-start-10 order-1 lg:order-2 flex flex-col gap-4">
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

          <RecentViewedItems :items="recentItems" @select="handleRecentSelect" @clear="clearRecentSearches" />
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
            <p class="animate-pulse">데이터를 조회 중입니다...</p>
          </div>

          <EmptyResult v-else-if="hasSearched && (!priceResult || priceResult.length === 0)" />

          <template v-else>
            <ResultGraph :chartData="formattedChartData" />
            <ResultTable
              :paginatedData="paginatedData"
              :totalPages="totalPages"
              :currentPage="currentPage"
              @updatePage="(p) => (currentPage = p)"
            />
          </template>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { usePriceSearch } from '@/views/dashboard/composables/usePriceSearch';
import DashboardFilter from '@/views/dashboard/components/DashboardFilter.vue';
import DashboardSummary from '@/views/dashboard/components/DashboardSummary.vue';
import ResultGraph from '@/views/dashboard/components/ResultGraph.vue';
import ResultTable from '@/views/dashboard/components/ResultTable.vue';
import FavoriteButton from '@/views/dashboard/components/FavoriteButton.vue';
import PageHeader from '@/components/layout/PageHeader.vue';
import EmptyResult from '@/views/dashboard/components/EmptyResult.vue';
import RecentViewedItems from '@/views/dashboard/components/RecentViewedItems.vue';

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
  return {
    labels: priceResult.value.map((item) => item.dateLabel),
    thisPrices: priceResult.value.map((item) => item.priceLabel),
    lastPrices: normalizedLastYearPrices.value,
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
    const thisPrice = item.priceLabel || 0;

    const lastPrice = normalizedLastYearPrices.value[globalIndex] || 0;

    const yoyDiff = lastPrice > 0 ? thisPrice - lastPrice : 0;
    const yoyRate = lastPrice > 0 ? (yoyDiff / lastPrice) * 100 : 0;

    return {
      date: item.dateLabel,
      thisPrice,
      prevDiff: item.priceChange ?? 0,
      prevRate: item.priceChangeRate ?? 0,
      lastPrice,
      yoyDiff,
      yoyRate,
    };
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

    if (state.selectedItem && state.selectedVariety) {
      await handleSearch();
    }

    currentPage.value = state.currentPage || 1;
  } else {
    // 최초 접속 시 기본값 (계란)
    selectedItem.value = '9903';
    selectedVariety.value = 'KM-9903-23-71';
    periodType.value = 'day';
    dayStartDate.value = '2025-11-01';
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    dayEndDate.value = yesterday.toISOString().slice(0, 10);

    await handleSearch();
  }
});

const triggerSearch = async () => {
  currentPage.value = 1;
  await handleSearch();
};

const handleRecentSelect = async (item) => {
  currentPage.value = 1;
  await applyRecentItem(item);
};
</script>
