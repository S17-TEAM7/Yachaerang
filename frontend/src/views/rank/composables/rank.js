import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchFavorites, removeFavorite } from '@/api/favorite';
import { fetchItemsApi, fetchSubItemsApi, fetchHighPriceRank, fetchLowPriceRank } from '@/api/price';
import { tokenStorage } from '@/utils/storage';
import { useToastStore } from '@/stores/toast';

export function rank() {
  const router = useRouter();
  const toastStore = useToastStore();
  const activeTab = ref('top');

  const topItems = ref([]);
  const bottomItems = ref([]);

  const watchList = ref([]);
  const isAuthenticated = computed(() => tokenStorage.hasTokens());
  const popularItems = computed(() => (activeTab.value === 'top' ? topItems.value : bottomItems.value));

  const yesterday = computed(() => {
    const date = new Date();
    date.setDate(date.getDate() - 1);

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  });

  async function loadRanks() {
    try {
      const { data: highData } = await fetchHighPriceRank();
      const { data: lowData } = await fetchLowPriceRank();

      topItems.value = highData.data;
      bottomItems.value = lowData.data;
    } catch (error) {
      console.error('랭킹 데이터 로딩 실패:', error);
      topItems.value = [];
      bottomItems.value = [];
    }
  }

  async function loadFavorites() {
    if (!tokenStorage.hasTokens()) {
      watchList.value = [];
      return;
    }

    try {
      const { data } = await fetchFavorites();
      const list = Array.isArray(data?.data) ? data.data : [];

      const buildProductNameMap = async (favoritesList) => {
        const productCodes = new Set(
          favoritesList.map((f) => f.productCode).filter((code) => typeof code === 'string' && code.trim() !== '')
        );
        const nameMap = {};

        if (productCodes.size === 0) return nameMap;

        try {
          const itemsRes = await fetchItemsApi();
          const body = itemsRes.data;
          const items = Array.isArray(body) ? body : Array.isArray(body?.data) ? body.data : [];

          await Promise.all(
            items.map(async (item) => {
              const itemCode = item.itemCode ?? item.code ?? item.id;
              const itemName = item.itemName ?? item.name ?? '';
              if (!itemCode) return;

              try {
                const subRes = await fetchSubItemsApi(itemCode);
                const subBody = subRes.data;
                const subs = Array.isArray(subBody) ? subBody : Array.isArray(subBody?.data) ? subBody.data : [];

                subs.forEach((sub) => {
                  const productCode = sub.productCode ?? sub.code ?? sub.id;
                  if (!productCodes.has(productCode)) return;

                  const varietyName = sub.subItemName ?? sub.name ?? sub.productName ?? '';
                  nameMap[productCode] = { itemName, varietyName };
                });
              } catch (e) {
                console.error('하위 품목 조회 실패:', e);
              }
            })
          );
        } catch (e) {
          console.error('품목/품종 전체 조회 실패:', e);
        }

        return nameMap;
      };

      const nameMap = await buildProductNameMap(list);

      const mapPeriodLabel = (periodType) => {
        switch (periodType) {
          case 'DAILY':
            return '일간';
          case 'WEEKLY':
            return '주간';
          case 'MONTHLY':
            return '월간';
          case 'YEARLY':
            return '연간';
          default:
            return periodType || '';
        }
      };

      watchList.value = list.map((fav) => {
        const fromMap = nameMap[fav.productCode] || {};

        const itemName = fav.itemName || fav.item?.itemName || fav.item?.name || fromMap.itemName || '';

        const varietyName =
          fav.subItemName ||
          fav.productName ||
          fav.varietyName ||
          fav.product?.productName ||
          fromMap.varietyName ||
          '';

        const codeFallback = fav.productCode ?? '';
        const period = mapPeriodLabel(fav.periodType);

        let mainLabel = '';
        if (itemName && varietyName) {
          mainLabel = `${itemName} - ${varietyName}`;
        } else if (varietyName) {
          mainLabel = varietyName;
        } else if (itemName) {
          mainLabel = itemName;
        } else {
          mainLabel = codeFallback;
        }

        return {
          favoriteId: fav.favoriteId,
          productCode: fav.productCode,
          periodType: fav.periodType,
          displayName: mainLabel,
          periodLabel: period,
        };
      });
    } catch (error) {
      watchList.value = [];
    }
  }

  const showRemoveFavoriteConfirm = ref(false);
  const selectedFavoriteId = ref(null);

  function handleRemoveFavorite(favoriteId) {
    selectedFavoriteId.value = favoriteId;
    showRemoveFavoriteConfirm.value = true;
  }

  async function handleRemoveFavoriteConfirm() {
    if (!selectedFavoriteId.value) return;

    try {
      await removeFavorite(selectedFavoriteId.value);
      await loadFavorites();
    } catch (error) {
      toastStore.show('삭제 중 오류가 발생했습니다.', 'error');
      console.error('삭제 중 오류가 발생했습니다:', error);
    } finally {
      showRemoveFavoriteConfirm.value = false;
      selectedFavoriteId.value = null;
    }
  }

  onMounted(() => {
    loadRanks();
    loadFavorites();
  });

  function editWatchList() {
    router.push('/mypage/items');
  }

  function goFavoriteDetail(fav) {
    if (!fav || !fav.productCode) return;

    router.push({
      path: '/dashboard',
      query: {
        productCode: fav.productCode,
        source: 'favorite',
      },
    });
  }

  const extractProductCodeFromRank = (item) => {
    return item.productCode ?? item.itemCode ?? item.code ?? item.id ?? item.productId ?? null;
  };

  function goRankDetail(rankItem) {
    const productCode = extractProductCodeFromRank(rankItem);
    if (!productCode) return;

    router.push({
      path: '/dashboard',
      query: {
        productCode,
        source: 'rank',
      },
    });
  }

  return {
    activeTab,
    popularItems,
    watchList,
    isAuthenticated,
    yesterday,
    editWatchList,
    goFavoriteDetail,
    goRankDetail,
    handleRemoveFavorite,
    showRemoveFavoriteConfirm,
    handleRemoveFavoriteConfirm,
  };
}
