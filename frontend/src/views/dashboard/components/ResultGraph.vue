<template>
  <div class="bg-white p-6 md:p-8 rounded-2xl shadow-sm border border-gray-100 space-y-12 font-sans">
    <div class="space-y-5">
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-3">
        <div>
          <h3 class="text-lg md:text-xl font-black text-slate-800 flex items-center gap-2">
            <span class="w-1.5 h-4 bg-slate-900 rounded-full"></span>
            시세 변동 추이
          </h3>
          <p class="text-sm text-slate-500 mt-1 font-medium">전년 동기 대비 금년 실거래가의 흐름을 비교합니다.</p>
        </div>

        <div class="flex items-center gap-5 text-sm font-bold bg-slate-50 px-3 py-2 rounded-xl border border-slate-100">
          <div class="flex items-center gap-2 text-slate-600"><span class="w-4 h-0.5 bg-slate-500"></span> 금년</div>
          <div class="flex items-center gap-2 text-slate-400">
            <span class="w-4 h-2 bg-slate-200 rounded-sm"></span> 전년
          </div>
          <div v-if="periodType === 'day'" class="flex items-center gap-3 ml-2 border-l border-slate-200 pl-3">
            <span class="text-rose-500">● 최고</span>
            <span class="text-blue-500">● 최저</span>
          </div>
          <div v-else class="flex items-center gap-3 ml-2 border-l border-slate-200 pl-3">
            <span class="text-slate-600">━ 평균</span>
            <span class="text-slate-400">▬ 범위</span>
          </div>
        </div>
      </div>

      <PriceLineChart
        :labels="processedData.labels"
        :thisPrices="processedData.thisPrices"
        :lastPrices="processedData.lastPrices"
        :maxIdx="processedData.maxIdx"
        :minIdx="processedData.minIdx"
        :minPrices="processedData.minPrices"
        :maxPrices="processedData.maxPrices"
        :periodType="periodType"
      />
    </div>

    <div class="pt-10 border-t border-slate-50 space-y-5 relative">
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-3">
        <div>
          <h3 class="text-lg md:text-xl font-black text-slate-800 flex items-center gap-2">
            <span class="w-1.5 h-4 bg-rose-400 rounded-full"></span>
            평균 대비 등락 현황
          </h3>
          <p class="text-sm text-slate-500 mt-1 font-medium">
            조회 기간 전체 평균(<span class="font-mono font-bold text-slate-700 bg-slate-100 px-1 rounded"
              >{{ Math.floor(processedData.avgPrice).toLocaleString() }}원</span
            >) 대비 현황입니다.
          </p>
        </div>
        <div class="flex gap-4 text-sm font-black italic tracking-tight">
          <span class="text-rose-400">상승 (+)</span>
          <span class="text-blue-400">하락 (-)</span>
        </div>
      </div>

      <div
        v-if="shouldBlurChart"
        class="absolute inset-x-0 bottom-0 top-[60px] z-10 flex items-center justify-center bg-white/10 backdrop-blur-[1px]"
      >
        <div class="bg-white/90 px-5 py-3 rounded-lg shadow-md border border-slate-200 flex items-center gap-3">
          <IconInfo class="h-5 w-5 text-red-500" />
          <p class="text-slate-600 text-sm font-semibold">
            조회된 데이터가 1개이므로 평균 대비 등락 그래프가 제공되지 않습니다.
          </p>
        </div>
      </div>

      <div :class="['transition-all', { 'blur-md opacity-40 grayscale': shouldBlurChart }]">
        <PriceBarChart :labels="processedData.labels" :diffData="processedData.diffData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import PriceLineChart from './PriceLineChart.vue';
import PriceBarChart from './PriceBarChart.vue';
import IconInfo from '@/components/icons/IconInfo.vue';

const props = defineProps({
  chartData: {
    type: Object,
    required: true,
    default: () => ({ labels: [], thisPrices: [], lastPrices: [] }),
  },
  periodType: {
    type: String,
    default: 'day',
  },
  priceResult: {
    type: Array,
    default: () => [],
  },
  weekStartDate: {
    type: String,
    default: null,
  },
  weekEndDate: {
    type: String,
    default: null,
  },
  monthStartDate: {
    type: String,
    default: null,
  },
  monthEndDate: {
    type: String,
    default: null,
  },
  yearStart: {
    type: String,
    default: null,
  },
  yearEnd: {
    type: String,
    default: null,
  },
});

// periodType별 데이터 매핑 함수
const mapDayData = (src) => {
  let labels = src.labels ? [...src.labels] : [];
  let thisPrices = src.thisPrices ? [...src.thisPrices] : [];
  let lastPrices = src.lastPrices ? [...src.lastPrices] : [];

  // 데이터 1개일 때 처리
  if (labels.length === 1) {
    labels = ['', labels[0], ''];
    const mainThis = thisPrices[0] ?? null;
    const mainLast = lastPrices[0] ?? null;
    thisPrices = [null, mainThis, null];
    lastPrices = [null, typeof mainLast === 'number' ? mainLast : null, null];
  }

  // 평균 계산 (null 제외)
  const validThisPrices = thisPrices.filter((p) => p !== null && p !== undefined);
  const avgPrice = validThisPrices.length ? validThisPrices.reduce((a, b) => a + b, 0) / validThisPrices.length : 0;

  // 최대/최소값 인덱스 추출 (Point 점 표기용)
  const maxVal = validThisPrices.length ? Math.max(...validThisPrices) : 0;
  const minVal = validThisPrices.length ? Math.min(...validThisPrices) : 0;
  const maxIdx = thisPrices.indexOf(maxVal);
  const minIdx = thisPrices.indexOf(minVal);

  return {
    labels,
    thisPrices,
    lastPrices,
    minPrices: null,
    maxPrices: null,
    avgPrice,
    maxIdx,
    minIdx,
  };
};

const mapAggregateData = (src) => {
  let labels = src.labels ? [...src.labels] : [];
  let thisPrices = src.thisPrices ? [...src.thisPrices] : [];
  let lastPrices = src.lastPrices ? [...src.lastPrices] : [];
  let minPrices = src.minPrices ? [...src.minPrices] : [];
  let maxPrices = src.maxPrices ? [...src.maxPrices] : [];

  // 데이터 1개일 때 처리
  if (labels.length === 1) {
    const label = labels[0];
    const val = thisPrices[0];
    const min = minPrices[0];
    const max = maxPrices[0];
    const last = lastPrices[0];

    labels = ['', label, ' ']; // 뒤쪽 공백은 중복 방지용
    thisPrices = [val, val, val]; // 같은 값을 채워 수평선 형성
    minPrices = [min, min, min];
    maxPrices = [max, max, max];
    lastPrices = [last, last, last];
  }

  // 평균 계산 (null 제외)
  const validThisPrices = thisPrices.filter((p) => p !== null && p !== undefined);
  const avgPrice = validThisPrices.length ? validThisPrices.reduce((a, b) => a + b, 0) / validThisPrices.length : 0;

  return {
    labels,
    thisPrices,
    lastPrices,
    minPrices,
    maxPrices,
    avgPrice,
    maxIdx: -1,
    minIdx: -1,
  };
};

// 모든 계산 로직을 부모에서 통합 관리
const processedData = computed(() => {
  const src = props.chartData;

  // periodType에 따라 다른 매핑 함수 사용
  const baseData = props.periodType === 'day' ? mapDayData(src) : mapAggregateData(src);

  // 등락 데이터 계산 (Bar Chart 전용, null 제외)
  const validThisPrices = baseData.thisPrices.filter((p) => p !== null && p !== undefined);
  const avgPrice = validThisPrices.length ? validThisPrices.reduce((a, b) => a + b, 0) / validThisPrices.length : 0;

  const diffData = baseData.thisPrices.map((val) => {
    if (val === null || val === undefined) return { diff: null, pct: null, val: null };
    return {
      diff: val - avgPrice,
      pct: avgPrice !== 0 ? ((val - avgPrice) / avgPrice) * 100 : 0,
      val,
    };
  });

  return {
    ...baseData,
    diffData,
  };
});

// 주간/월간/연간일 때 시작일과 끝일이 같으면 블러 처리
const shouldBlurChart = computed(() => {
  if (props.periodType === 'week') {
    return props.weekStartDate && props.weekEndDate && props.weekStartDate === props.weekEndDate;
  }
  if (props.periodType === 'month') {
    return props.monthStartDate && props.monthEndDate && props.monthStartDate === props.monthEndDate;
  }
  if (props.periodType === 'year') {
    return props.yearStart && props.yearEnd && props.yearStart === props.yearEnd;
  }
  return false;
});
</script>
