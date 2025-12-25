import { ref, computed, onMounted, watch } from 'vue';
import {
  fetchItemsApi,
  fetchSubItemsApi,
  fetchDailyPricesApi,
  fetchWeeklyPricesApi,
  fetchMonthlyPricesApi,
  fetchYearlyPricesApi,
  fetchYearlyPriceDetailApi,
} from '@/api/price';
import { addFavorite } from '@/api/favorite';
import { useToastStore } from '@/stores/toast';
import { getLastYearDateRange } from '@/views/dashboard/composables/useLastYearRange';
const RECENT_KEY = 'recent-price-items';
const SEARCH_STATE_KEY = 'price-search-state';

export function usePriceSearch() {
  const selectedItem = ref('');
  const selectedVariety = ref('');
  const itemOptions = ref([]);
  const varietyOptions = ref([]);
  const priceResult = ref([]);
  const lastYearPrices = ref([]);
  const hasSearched = ref(false);
  const suppressVarietyReset = ref(false);
  const isLoading = ref(false);

  const periodType = ref('year');
  const periodTabs = [
    { value: 'year', label: '연간' },
    { value: 'month', label: '월간' },
    { value: 'week', label: '주간' },
    { value: 'day', label: '일간' },
  ];

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  const maxYear = Math.max(2000, yesterday.getFullYear() - 1);
  const minYear = 2000;

  const yearOptions = computed(() => {
    const years = [];
    for (let y = maxYear; y >= minYear; y -= 1) {
      years.push(String(y));
    }
    return years;
  });

  /* ================= 일별 ================= */

  const dayStartDate = ref(null);
  const dayEndDate = ref(null);

  /* ================= 주별 ================= */

  const weekStartDate = ref(null);
  const weekEndDate = ref(null);

  // "완전히 끝난 주"까지만 선택 가능하게 하는 기준 일요일
  const weekday = today.getDay();
  const daysSinceMonday = (weekday + 6) % 7;
  const lastWeekSunday = new Date(today);
  lastWeekSunday.setDate(today.getDate() - daysSinceMonday - 1);

  /* ================= 월별 ================= */

  const monthStartDate = ref(null);
  const monthEndDate = ref(null);

  /* ================= 연도별 ================= */

  const isYearDetail = ref(false);
  const yearStart = ref('');
  const yearEnd = ref('');
  const yearDetail = ref('');

  /* ================= 공통 포맷터 / 변환 ================= */

  // 날짜 → 'YYYY-MM-DD'
  const formatDateToString = (date) => {
    if (!date) return '';
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // Date 기준 주(월~일) → 문자열 범위
  const getWeekRangeFromDate = (date) => {
    if (!date) return null;
    const d = new Date(date);
    const dayOfWeek = d.getDay();
    const diff = d.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
    const monday = new Date(d.setDate(diff));
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return {
      start: formatDateToString(monday),
      end: formatDateToString(sunday),
    };
  };

  /* ================= API: 품목 / 품종 ================= */

  const fetchItems = async () => {
    try {
      const res = await fetchItemsApi();
      console.log('품목 응답:', res.data);
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

  const fetchSubItems = async (itemCode) => {
    if (!itemCode) {
      varietyOptions.value = [];
      if (!suppressVarietyReset.value) {
        selectedVariety.value = '';
      }
      return;
    }
    try {
      const res = await fetchSubItemsApi(itemCode);
      const body = res.data;
      const list = Array.isArray(body) ? body : Array.isArray(body?.data) ? body.data : [];
      varietyOptions.value = list.map((sub) => ({
        value: String(sub.productCode ?? sub.code ?? sub.id),
        label: sub.subItemName ?? sub.name ?? sub.productName ?? '',
      }));
      if (!suppressVarietyReset.value) {
        const current = selectedVariety.value ? String(selectedVariety.value) : '';
        const existsInNewOptions = varietyOptions.value.some((v) => String(v.value) === current);

        if (!existsInNewOptions) {
          selectedVariety.value = '';
        }
      }
    } catch (error) {
      console.error('하위품목 목록 조회 실패:', error);
      varietyOptions.value = [];
      if (!suppressVarietyReset.value) {
        selectedVariety.value = '';
      }
    }
  };

  watch(selectedItem, (newItem) => {
    if (suppressVarietyReset.value) return;
    fetchSubItems(newItem);
  });

  watch(selectedVariety, () => {
    if (suppressVarietyReset.value) return;
  });

  onMounted(() => {
    fetchItems();
  });

  /* ================= 필터 / 초기화 ================= */

  const handlePeriodClick = (type) => {
    periodType.value = type;
    // 기간 변경 시 날짜는 localStorage에 저장된 값을 유지하도록 변경
    // (새로고침 시에도 날짜가 유지되도록 하기 위함)
    // 날짜 초기화는 하지 않음
  };

  /* ================= 즐겨찾기 등록 ================= */

  const periodTypeMap = {
    day: 'DAILY',
    week: 'WEEKLY',
    month: 'MONTHLY',
    year: 'YEARLY',
  };

  const mapPeriodTypeToFavorite = (type) => periodTypeMap[type] ?? null;

  const handleAddFavorite = async () => {
    const accessToken = localStorage.getItem('accessToken');
    const hasValidToken =
      accessToken && accessToken.trim() !== '' && accessToken !== 'null' && accessToken !== 'undefined';

    const toastStore = useToastStore();

    if (!hasValidToken) {
      toastStore.show('관심 품목 등록은 로그인 후 이용할 수 있어요', 'info');
      return;
    }
    const productCode = selectedVariety.value;
    const mappedPeriod = mapPeriodTypeToFavorite(periodType.value);

    if (!productCode) {
      toastStore.show('올바른 품종을 선택해 주세요', 'info');
      return;
    }

    if (!mappedPeriod) {
      toastStore.show('등록할 기간 유형이 올바르지 않습니다', 'error');
      return;
    }

    try {
      await addFavorite({
        productCode,
        periodType: mappedPeriod,
      });
      toastStore.show('관심 품목으로 등록되었습니다', 'success');
    } catch (error) {
      const errorData = error.response?.data;
      const errorCode = errorData?.code;

      if (errorCode === 'FAVORITE_003') {
        toastStore.show('이미 관심 품목으로 등록된 품종입니다', 'info');
        return;
      }
      toastStore.show('관심 품목 등록 중 오류가 발생했습니다', 'error');
    }
  };

  /* ================= 즐겨찾기에서 필터 세팅 ================= */

  const mapFavoriteToPeriodType = (favoritePeriodType) => {
    switch (favoritePeriodType) {
      case 'DAILY':
        return 'day';
      case 'WEEKLY':
        return 'week';
      case 'MONTHLY':
        return 'month';
      case 'YEARLY':
        return 'year';
      default:
        return null;
    }
  };

  const findItemCodeByProductCode = async (targetProductCode) => {
    if (!targetProductCode) return null;

    const normalizedTarget = String(targetProductCode);

    if (!itemOptions.value.length) {
      await fetchItems();
    }

    for (const opt of itemOptions.value) {
      await fetchSubItems(opt.value);
      const found = varietyOptions.value.some((v) => String(v.value) === normalizedTarget);
      if (found) {
        return String(opt.value);
      }
    }

    return null;
  };

  const initializeFromFavorite = async (productCode, favoritePeriodType) => {
    if (!productCode || !favoritePeriodType) return;

    const normalizedProductCode = String(productCode);

    const mappedPeriod = mapFavoriteToPeriodType(favoritePeriodType);
    if (!mappedPeriod) return;

    // 기간 타입 설정
    periodType.value = mappedPeriod;

    // 기간별 기본 날짜/연도 범위 세팅
    if (mappedPeriod === 'day') {
      // 일별: 어제 기준 1주일(7일)
      const end = new Date(yesterday);
      const start = new Date(end);
      start.setDate(end.getDate() - 6);
      dayStartDate.value = formatDateToString(start);
      dayEndDate.value = formatDateToString(end);
    } else if (mappedPeriod === 'week') {
      // 주간: 마지막 완전한 주 기준 2주
      const end = new Date(lastWeekSunday);
      const start = new Date(end);
      start.setDate(end.getDate() - 14);
      weekStartDate.value = formatDateToString(start);
      weekEndDate.value = formatDateToString(end);
    } else if (mappedPeriod === 'month') {
      // 월간: 어제가 속한 월 기준 최근 3개월
      const end = new Date(yesterday);
      const endYear = end.getFullYear();
      const endMonthIndex = end.getMonth();
      const start = new Date(endYear, endMonthIndex - 2, 1);
      const endMonth = new Date(endYear, endMonthIndex, 1);
      const startMonthLabel = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}`;
      const endMonthLabel = `${endMonth.getFullYear()}-${String(endMonth.getMonth() + 1).padStart(2, '0')}`;
      monthStartDate.value = startMonthLabel;
      monthEndDate.value = endMonthLabel;
    } else if (mappedPeriod === 'year') {
      isYearDetail.value = false;
      const latestYear = maxYear;
      yearStart.value = String(latestYear);
      yearEnd.value = String(latestYear);
    }

    // 품목/품종 선택 세팅
    suppressVarietyReset.value = true;
    try {
      const itemCode = await findItemCodeByProductCode(normalizedProductCode);
      if (itemCode) {
        selectedItem.value = String(itemCode);
        await fetchSubItems(itemCode);

        const exactMatch = varietyOptions.value.find((v) => String(v.value) === normalizedProductCode);
        if (exactMatch) {
          selectedVariety.value = String(exactMatch.value);
        } else if (varietyOptions.value.length > 0) {
          selectedVariety.value = varietyOptions.value[0].value;
        } else {
          selectedVariety.value = '';
        }
      }
    } finally {
      suppressVarietyReset.value = false;
    }
  };

  const initializeFromRank = async (productCode) => {
    if (!productCode) return;

    const normalizedProductCode = String(productCode);

    periodType.value = 'day';

    const end = new Date(yesterday);
    const start = new Date(yesterday);
    start.setMonth(start.getMonth() - 3);

    dayStartDate.value = formatDateToString(start);
    dayEndDate.value = formatDateToString(end);

    suppressVarietyReset.value = true;
    try {
      const itemCode = await findItemCodeByProductCode(normalizedProductCode);
      if (itemCode) {
        selectedItem.value = String(itemCode);
        await fetchSubItems(itemCode);

        const exactMatch = varietyOptions.value.find((v) => String(v.value) === normalizedProductCode);
        if (exactMatch) {
          selectedVariety.value = String(exactMatch.value);
        } else if (varietyOptions.value.length > 0) {
          selectedVariety.value = String(varietyOptions.value[0].value);
        } else {
          selectedVariety.value = '';
        }
      }
    } finally {
      suppressVarietyReset.value = false;
    }
  };

  const resetFilters = () => {
    selectedItem.value = '';
    selectedVariety.value = '';
    periodType.value = 'year';
    hasSearched.value = false;

    dayStartDate.value = null;
    dayEndDate.value = null;
    weekStartDate.value = null;
    weekEndDate.value = null;
    monthStartDate.value = null;
    monthEndDate.value = null;
    yearStart.value = '';
    yearEnd.value = '';
    yearDetail.value = '';
    isYearDetail.value = false;
    varietyOptions.value = [];
    priceResult.value = [];
    hasSearched.value = false;
  };
  /* ================ 최근 조회 ================= */

  const saveRecentItem = ({ productCode, itemCode, itemLabel, varietyLabel, periodType, dateRangeLabel, range }) => {
    if (!productCode) return;

    const recent = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]');

    const newItem = {
      productCode,
      itemCode,
      itemLabel,
      varietyLabel: varietyLabel || '전체',
      periodType,
      savedAt: Date.now(),
      dateRangeLabel: dateRangeLabel || '',
      range: range || null,
    };

    const filtered = recent.filter((r) => r.productCode !== newItem.productCode);

    const updated = [newItem, ...filtered].slice(0, 5);
    localStorage.setItem(RECENT_KEY, JSON.stringify(updated));
    recentItems.value = updated;
  };

  const loadRecentItems = () => {
    return JSON.parse(localStorage.getItem(RECENT_KEY) || '[]');
  };
  const clearRecentSearches = () => {
    recentItems.value = [];
    localStorage.removeItem(RECENT_KEY);
  };

  const recentItems = ref(loadRecentItems());

  const applyRecentItem = async (recentItem) => {
    if (!recentItem || !recentItem.productCode || !recentItem.periodType) return;

    const normalizedProductCode = String(recentItem.productCode);
    const recentPeriod = recentItem.periodType;
    const savedRange = recentItem.range;

    periodType.value = recentPeriod;

    let appliedFromSavedRange = false;

    if (savedRange) {
      if (recentPeriod === 'day' && typeof savedRange.start === 'string' && typeof savedRange.end === 'string') {
        dayStartDate.value = savedRange.start;
        dayEndDate.value = savedRange.end;
        appliedFromSavedRange = true;
      } else if (
        recentPeriod === 'week' &&
        typeof savedRange.start === 'string' &&
        typeof savedRange.end === 'string'
      ) {
        weekStartDate.value = savedRange.start;
        weekEndDate.value = savedRange.end;
        appliedFromSavedRange = true;
      } else if (
        recentPeriod === 'month' &&
        typeof savedRange.start === 'string' &&
        typeof savedRange.end === 'string'
      ) {
        monthStartDate.value = savedRange.start;
        monthEndDate.value = savedRange.end;
        appliedFromSavedRange = true;
      } else if (recentPeriod === 'year') {
        if (savedRange.isDetail) {
          isYearDetail.value = true;
          if (typeof savedRange.year === 'string') {
            yearDetail.value = savedRange.year;
          }
          appliedFromSavedRange = true;
        } else {
          isYearDetail.value = false;

          const startY = savedRange.startYear ?? savedRange.start;
          const endY = savedRange.endYear ?? savedRange.end;

          if (startY != null && endY != null) {
            yearStart.value = String(startY);
            yearEnd.value = String(endY);
            appliedFromSavedRange = true;
          }
        }
      }
    }

    if (!appliedFromSavedRange) {
      if (recentPeriod === 'day') {
        const end = new Date(yesterday);
        const start = new Date(end);
        start.setDate(end.getDate() - 6);
        dayStartDate.value = formatDateToString(start);
        dayEndDate.value = formatDateToString(end);
      } else if (recentPeriod === 'week') {
        const end = new Date(lastWeekSunday);
        const start = new Date(end);
        start.setDate(end.getDate() - 14);
        weekStartDate.value = formatDateToString(start);
        weekEndDate.value = formatDateToString(end);
      } else if (recentPeriod === 'month') {
        const end = new Date(yesterday);
        const endYear = end.getFullYear();
        const endMonthIndex = end.getMonth();
        const start = new Date(endYear, endMonthIndex - 2, 1);
        const startMonthLabel = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}`;
        const endMonthLabel = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}`;
        monthStartDate.value = startMonthLabel;
        monthEndDate.value = endMonthLabel;
      } else if (recentPeriod === 'year') {
        isYearDetail.value = false;
        const latestYear = maxYear;
        yearStart.value = String(latestYear);
        yearEnd.value = String(latestYear);
      }
    }

    suppressVarietyReset.value = true;
    try {
      const itemCode = await findItemCodeByProductCode(normalizedProductCode);
      if (itemCode) {
        selectedItem.value = String(itemCode);
        await fetchSubItems(itemCode);

        const exactMatch = varietyOptions.value.find((v) => String(v.value) === normalizedProductCode);

        if (exactMatch) {
          selectedVariety.value = String(exactMatch.value);
        } else if (varietyOptions.value.length > 0) {
          selectedVariety.value = String(varietyOptions.value[0].value);
        } else {
          selectedVariety.value = '';
        }
      }
    } finally {
      suppressVarietyReset.value = false;
    }

    await handleSearch();
  };

  /* ================= 응답 파싱 ================= */

  const extractPriceList = (raw) => {
    if (!raw) return [];
    if (raw && typeof raw === 'object' && Array.isArray(raw.data)) {
      return raw.data;
    }
    if (Array.isArray(raw)) return raw;
    return [];
  };
  /* ================= 일자 변환 함수 ================= */
  const normalizeResult = (rawList, type) => {
    return rawList.map((item) => {
      const zeroToNull = (val) => (val === 0 || val === '0' ? null : val);

      if (type === 'day') {
        return {
          dateLabel: item.priceDate || item.date || '',
          priceLabel: zeroToNull(item.price ?? item.avgPrice ?? null),
          priceChange: item.priceChange ?? null,
          priceChangeRate: item.priceChangeRate ?? null,
        };
      }

      if (type === 'week' || type === 'month') {
        return {
          dateLabel:
            type === 'week'
              ? `${item.startDate} ~ ${item.endDate}`
              : `${item.priceYear}-${String(item.priceMonth).padStart(2, '0')}`,
          priceLabel: zeroToNull(item.avgPrice ?? null),
          minPrice: zeroToNull(item.minPrice ?? null),
          maxPrice: zeroToNull(item.maxPrice ?? null),
          priceChange: item.priceChange ?? null,
          priceChangeRate: item.priceChangeRate ?? null,
        };
      }

      // year 처리
      return {
        dateLabel: `${item.priceYear}`,
        priceLabel: zeroToNull(item.avgPrice ?? null),
        minPrice: zeroToNull(item.minPrice ?? null),
        maxPrice: zeroToNull(item.maxPrice ?? null),
        priceChange: item.priceChange ?? null,
        priceChangeRate: item.priceChangeRate ?? null,
      };
    });
  };

  /* ================= 검색 ================= */

  const handleSearch = async () => {
    const productCode = selectedVariety.value;
    const toastStore = useToastStore();

    if (!productCode) {
      toastStore.show('품종을 선택해 주세요', 'info');
      return;
    }

    lastYearPrices.value = [];
    let currentRange;

    try {
      if (periodType.value === 'day') {
        if (!dayStartDate.value || !dayEndDate.value) {
          toastStore.show('시작일과 종료일을 모두 선택해 주세요', 'info');
          return;
        }
        const startStr = formatDateToString(dayStartDate.value);
        const endStr = formatDateToString(dayEndDate.value);
        if (startStr > endStr) {
          toastStore.show('시작일이 종료일보다 늦을 수 없습니다', 'info');
          return;
        }

        const { data } = await fetchDailyPricesApi(productCode, { startDate: startStr, endDate: endStr });
        priceResult.value = normalizeResult(extractPriceList(data), 'day');

        const lastYearRange = getLastYearDateRange(startStr, endStr);
        if (lastYearRange) {
          try {
            const { data: lastData } = await fetchDailyPricesApi(productCode, {
              startDate: lastYearRange.startDate,
              endDate: lastYearRange.endDate,
            });
            const normalizedLast = normalizeResult(extractPriceList(lastData), 'day');

            const lastYearMap = new Map();
            normalizedLast.forEach((item) => {
              if (item.dateLabel) {
                const dateParts = item.dateLabel.split('-');
                if (dateParts.length === 3) {
                  const nextYear = String(parseInt(dateParts[0]) + 1);
                  const matchedKey = `${nextYear}-${dateParts[1]}-${dateParts[2]}`;
                  lastYearMap.set(matchedKey, item.priceLabel);
                }
              }
            });

            lastYearPrices.value = priceResult.value.map((item) => {
              const matched = lastYearMap.get(item.dateLabel);
              return matched !== undefined ? matched : null;
            });
          } catch (e) {
            lastYearPrices.value = priceResult.value.map(() => null);
          }
        } else {
          lastYearPrices.value = priceResult.value.map(() => null);
        }

        currentRange = { start: startStr, end: endStr };
      } else if (periodType.value === 'week') {
        if (!weekStartDate.value || !weekEndDate.value) {
          toastStore.show('시작 주와 종료 주를 모두 선택해 주세요', 'info');
          return;
        }
        const startRange = getWeekRangeFromDate(weekStartDate.value);
        const endRange = getWeekRangeFromDate(weekEndDate.value);
        if (startRange.start > endRange.start) {
          toastStore.show('시작 주가 종료 주보다 늦을 수 없습니다', 'info');
          return;
        }

        const { data } = await fetchWeeklyPricesApi(productCode, {
          startDate: startRange.start,
          endDate: endRange.end,
        });
        priceResult.value = normalizeResult(extractPriceList(data), 'week');

        const lastYearRange = getLastYearDateRange(startRange.start, endRange.end);
        if (lastYearRange) {
          try {
            const { data: lastData } = await fetchWeeklyPricesApi(productCode, {
              startDate: lastYearRange.startDate,
              endDate: lastYearRange.endDate,
            });
            const normalizedLast = normalizeResult(extractPriceList(lastData), 'week');
            lastYearPrices.value = priceResult.value.map((_, idx) => normalizedLast[idx]?.priceLabel ?? null);
          } catch (e) {
            lastYearPrices.value = priceResult.value.map(() => null);
          }
        }

        currentRange = { start: startRange.start, end: endRange.end };
      } else if (periodType.value === 'month') {
        if (!monthStartDate.value || !monthEndDate.value) {
          toastStore.show('시작 월과 종료 월을 모두 선택해 주세요', 'info');
          return;
        }
        const startD = new Date(monthStartDate.value);
        const endD = new Date(monthEndDate.value);
        if (startD > endD) {
          toastStore.show('시작 월이 종료 월보다 늦을 수 없습니다', 'info');
          return;
        }

        const startYear = startD.getFullYear();
        const startMonth = startD.getMonth() + 1;
        const endYear = endD.getFullYear();
        const endMonth = endD.getMonth() + 1;

        const { data } = await fetchMonthlyPricesApi(productCode, { startYear, startMonth, endYear, endMonth });
        priceResult.value = normalizeResult(extractPriceList(data), 'month');

        currentRange = {
          start: `${startYear}-${String(startMonth).padStart(2, '0')}`,
          end: `${endYear}-${String(endMonth).padStart(2, '0')}`,
        };
      } else if (periodType.value === 'year') {
        if (isYearDetail.value) {
          if (!yearDetail.value) {
            toastStore.show('조회할 연도를 선택해 주세요', 'info');
            return;
          }
          const { data } = await fetchYearlyPriceDetailApi(productCode, { year: Number(yearDetail.value) });
          priceResult.value = extractPriceList(data);
          currentRange = { isDetail: true, year: String(yearDetail.value) };
        } else {
          if (!yearStart.value || !yearEnd.value) {
            toastStore.show('시작 연도와 종료 연도를 모두 선택해 주세요', 'info');
            return;
          }
          const ys = Number(yearStart.value);
          const ye = Number(yearEnd.value);
          if (ys > ye) {
            toastStore.show('시작 연도가 종료 연도보다 클 수 없습니다', 'info');
            return;
          }
          const { data } = await fetchYearlyPricesApi(productCode, { startYear: ys, endYear: ye });
          priceResult.value = normalizeResult(extractPriceList(data), 'year');

          lastYearPrices.value = priceResult.value.map((row) => {
            if (row.priceLabel == null || row.priceChange == null) return null;
            return row.priceLabel - row.priceChange;
          });

          currentRange = { startYear: String(ys), endYear: String(ye) };
        }
      }

      const generateDateRangeLabel = () => {
        if (periodType.value === 'day') return `${currentRange.start} ~ ${currentRange.end}`;
        if (periodType.value === 'week') return `${currentRange.start} ~ ${currentRange.end}`;
        if (periodType.value === 'month') return `${currentRange.start} ~ ${currentRange.end}`;
        if (periodType.value === 'year') {
          return isYearDetail.value
            ? `${currentRange.year}년 상세`
            : `${currentRange.startYear} ~ ${currentRange.endYear}`;
        }
        return '';
      };

      saveRecentItem({
        productCode,
        itemCode: selectedItem.value,
        itemLabel: itemOptions.value.find((i) => i.value === selectedItem.value)?.label || '',
        varietyLabel: varietyOptions.value.find((v) => v.value === selectedVariety.value)?.label || '',
        periodType: periodType.value,
        dateRangeLabel: generateDateRangeLabel(),
        range: currentRange,
      });

      hasSearched.value = true;
    } catch (error) {
      console.error('가격 조회 실패', error);

      if (error.response?.status === 401) {
        toastStore.show('로그인이 필요합니다', 'info');
        return;
      }

      toastStore.show('가격 정보를 가져오는 중 오류가 발생했습니다', 'error');
    } finally {
      isLoading.value = false;
    }

    console.log('최종 priceResult', priceResult.value);
  };

  return {
    selectedItem,
    selectedVariety,
    itemOptions,
    varietyOptions,
    priceResult,
    lastYearPrices,
    periodType,
    periodTabs,
    yesterday,
    lastWeekSunday,
    yearOptions,
    dayStartDate,
    dayEndDate,
    weekStartDate,
    weekEndDate,
    monthStartDate,
    monthEndDate,
    isYearDetail,
    yearStart,
    yearEnd,
    yearDetail,
    hasSearched,
    recentItems,
    isLoading,

    handlePeriodClick,
    resetFilters,
    handleSearch,
    handleAddFavorite,
    initializeFromFavorite,
    initializeFromRank,
    loadRecentItems,
    applyRecentItem,
    clearRecentSearches,
  };
}
