<template>
  <div class="min-h-screen bg-gray-50 text-[#1f2937] font-sans">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 pt-4 md:pt-6 w-full">
      <PageHeader title="랭킹" description="어제 시세 기준 상위/하위 품목을 확인하세요." />
    </div>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 w-full">
      <div class="max-w-[100rem] flex flex-wrap lg:flex-nowrap gap-10 items-start">
        <div class="flex-1 min-w-0 w-full">
          <div class="flex justify-between items-end mb-8 border-b border-gray-200">
            <nav class="flex gap-4 sm:gap-8">
              <button
                v-for="tab in tabs"
                :key="tab.id"
                @click="activeTab = tab.id"
                :class="[
                  'tab-btn pb-4 text-base sm:text-lg transition-all relative flex-shrink',
                  activeTab === tab.id ? 'active-tab hover:text-red-600' : 'text-gray-400 hover:text-gray-700',
                ]"
              >
                <span class="truncate">{{ tab.label }}</span>
              </button>
            </nav>

            <div class="pb-4 text-[13px] text-gray-400 font-medium">{{ yesterday }} 기준</div>
          </div>

          <div :key="activeTab" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
            <RankItem
              v-for="(item, idx) in popularItems"
              :key="idx"
              :item="item"
              :rank="idx + 1"
              @click="goRankDetail(item)"
            />
          </div>
        </div>

        <WatchListAside
          :items="watchList"
          :is-authenticated="isAuthenticated"
          @edit="editWatchList"
          @select="goFavoriteDetail"
          @remove="handleRemoveFavorite"
        />
      </div>
    </div>

    <ConfirmModal
      :show="showRemoveFavoriteConfirm"
      title="관심 품목 삭제"
      message="관심 품목에서 삭제하시겠습니까?"
      @confirm="handleRemoveFavoriteConfirm"
      @cancel="showRemoveFavoriteConfirm = false"
    />
  </div>
</template>

<script setup>
import { rank } from '@/views/rank/composables/rank';
import RankItem from './components/RankItem.vue';
import WatchListAside from './components/WatchListAside.vue';
import PageHeader from '@/components/layout/PageHeader.vue';
import ConfirmModal from '@/components/modal/ConfirmModal.vue';

const {
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
} = rank();

const tabs = [
  { id: 'top', label: '시세 상위 보기' },
  { id: 'bottom', label: '시세 하위 보기' },
];
</script>

<style scoped>
.active-tab {
  color: #ef4444;
  font-weight: 700;
}
.active-tab::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 3px;
  background: #ef4444;
  border-radius: 2px;
}
</style>
