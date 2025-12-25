<template>
  <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-base font-black text-slate-800 flex items-center gap-2">
        <IconHeart class="h-5 w-5 fill-[#F44323] text-[#F44323]" />
        등록한 관심 품목
      </h2>
    </div>

    <div v-if="loading" class="py-8">
      <LoadingSpinner />
    </div>

    <ul v-else-if="favorites.length" class="space-y-2.5 max-h-[360px] overflow-y-auto pr-1 custom-scrollbar">
      <li
        v-for="item in favorites"
        :key="item.favoriteId"
        class="group flex flex-col p-3.5 border border-slate-50 bg-slate-50/50 rounded-xl cursor-pointer hover:bg-white hover:border-slate-300 hover:shadow-md transition-all active:scale-[0.98]"
      >
        <div class="flex justify-between items-start mb-1">
          <div class="flex-1 min-w-0 mr-4" @click="goToDetail(item)">
            <div class="flex items-center">
              <span class="text-sm font-black text-slate-700 group-hover:text-slate-900 truncate">
                {{ item.displayName }}
              </span>
              <span
                class="flex-shrink-0 px-1.5 py-0.5 bg-white border border-slate-200 text-[10px] font-black text-slate-500 rounded text-center ml-2"
              >
                {{ item.periodLabel }}
              </span>
            </div>
          </div>
          <button
            @click.stop="handleRemove(item.favoriteId)"
            class="text-sm text-red-400 hover:text-red-600 flex-shrink-0 p-1 font-bold transition-colors"
          >
            ✕
          </button>
        </div>
      </li>
    </ul>

    <div v-else class="py-10 text-center space-y-2">
      <p class="text-sm font-bold text-slate-300">등록한 관심 품목이 없습니다.</p>
    </div>

    <ConfirmModal
      :show="showRemoveConfirm"
      title="관심품목 삭제"
      message="등록한 관심품목에서 삭제하시겠습니까?"
      @confirm="handleRemoveConfirm"
      @cancel="showRemoveConfirm = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchFavorites, removeFavorite } from '@/api/favorite';
import { useProductNameStore } from '@/stores/productNameStore';
import IconHeart from '@/components/icons/IconHeart.vue';
import { useToastStore } from '@/stores/toast';
import LoadingSpinner from '@/components/spinner/LoadingSpinner.vue';
import ConfirmModal from '@/components/modal/ConfirmModal.vue';

const router = useRouter();
const productNameStore = useProductNameStore();
const toastStore = useToastStore();

const favorites = ref([]);
const loading = ref(false);

const mapPeriodLabel = (periodType) => {
  const labels = {
    DAILY: '일간',
    WEEKLY: '주간',
    MONTHLY: '월간',
    YEARLY: '연간',
  };
  return labels[periodType] || periodType || '';
};

const loadFavorites = async () => {
  loading.value = true;
  try {
    const { data } = await fetchFavorites();
    const list = Array.isArray(data?.data) ? data.data : [];
    const nameMap = await productNameStore.loadProductNames(list);

    favorites.value = list.map((fav) => {
      const fromMap = nameMap[fav.productCode] || {};
      const itemName = fav.itemName || fav.item?.itemName || fromMap.itemName || '';
      const varietyName = fav.subItemName || fav.productName || fromMap.varietyName || '';

      let displayName = '';
      if (itemName && varietyName) {
        displayName = `${itemName} - ${varietyName}`;
      } else {
        displayName = varietyName || itemName || fav.productCode || '';
      }

      return {
        favoriteId: fav.favoriteId,
        productCode: fav.productCode,
        periodType: fav.periodType,
        periodLabel: mapPeriodLabel(fav.periodType),
        displayName,
      };
    });
  } catch (error) {
    console.error('등록한 관심품목 조회 실패:', error);
    favorites.value = [];
  } finally {
    loading.value = false;
  }
};

const goToDetail = (item) => {
  if (!item.productCode) {
    toastStore.show('상품 코드가 없습니다.', 'error');
    return;
  }
  router.push({
    path: '/dashboard',
    query: {
      productCode: item.productCode,
      periodType: item.periodType,
      source: 'favorite',
      t: Date.now(),
    },
  });
};

const showRemoveConfirm = ref(false);
const selectedFavoriteId = ref(null);

const handleRemove = (favoriteId) => {
  selectedFavoriteId.value = favoriteId;
  showRemoveConfirm.value = true;
};

const handleRemoveConfirm = async () => {
  if (!selectedFavoriteId.value) return;

  try {
    await removeFavorite(selectedFavoriteId.value);
    await loadFavorites();
    toastStore.show('등록한 관심품목에서 삭제되었습니다.', 'success');
  } catch (error) {
    console.error('삭제 실패:', error);
    toastStore.show('삭제 중 오류가 발생했습니다.', 'error');
  } finally {
    showRemoveConfirm.value = false;
    selectedFavoriteId.value = null;
  }
};

onMounted(loadFavorites);
</script>

<style scoped>
li {
  will-change: transform, box-shadow;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #cbd5e1;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
