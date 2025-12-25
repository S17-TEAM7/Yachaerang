<template>
  <div
    @click="$emit('click')"
    class="item-card p-6 rounded-2xl flex flex-col justify-between h-[220px] cursor-pointer bg-white border hover:translate-y-[-4px] hover:shadow-lg transition-all animate-fadeIn"
    :class="cardRankClass"
  >
    <div class="flex justify-between items-start">
      <span class="text-xs font-bold px-2 py-1 rounded w-fit border" :class="rankBadgeClass"> {{ rank }}위 </span>
      <span class="text-sm font-bold" :class="priceColorClass"> {{ arrow }} {{ formattedRate }} </span>
    </div>

    <div class="flex-1 flex flex-col justify-center min-w-0 overflow-hidden">
      <h3 class="text-xl font-extrabold text-gray-900 leading-tight mb-1 break-keep">
        {{ itemCategoryName }}
      </h3>
      <h3 class="text-sm font-bold text-gray-500 leading-tight mb-1 break-keep">
        {{ displayName }}
      </h3>
      <p class="text-xs text-gray-400 font-medium">{{ item.unit || '단위 정보 없음' }}</p>
    </div>

    <div class="text-right border-t border-gray-50 pt-4">
      <span class="text-2xl font-black text-gray-900">{{ formattedPrice }}</span>
      <span class="text-sm font-bold text-gray-400 ml-0.5">원</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  item: Object,
  rank: Number,
});

defineEmits(['click']);

const itemCategoryName = computed(() => {
  return props.item?.itemName || '품목명 없음';
});

const displayName = computed(() => {
  return props.item?.productName || '상품명 없음';
});

const rankBadgeClass = computed(() => {
  switch (props.rank) {
    case 1:
      return 'bg-[#FFD700] text-[#8B4513] border-[#DAA520] shadow-[0_0_8px_rgba(255,215,0,0.3)]';
    case 2:
      return 'bg-[#E2E8F0] text-[#475569] border-[#CBD5E1]';
    case 3:
      return 'bg-[#CD7F32] text-white border-[#A0522D]';
    default:
      return 'bg-gray-100 text-gray-500 border-gray-200';
  }
});

const cardRankClass = computed(() => {
  switch (props.rank) {
    case 1:
      return 'border-[#FFD700] ring-1 ring-[#FFD700]/20 shadow-[0_4px_20px_rgba(255,215,0,0.1)]';
    case 2:
      return 'border-[#CBD5E1]';
    case 3:
      return 'border-[#CD7F32]/40';
    default:
      return 'border-gray-100 hover:border-gray-300';
  }
});

const formattedPrice = computed(() =>
  props.item?.price || props.item?.dpr1 ? Number(props.item.price || props.item.dpr1).toLocaleString() : '0'
);

const formattedRate = computed(() => {
  const rate = props.item?.priceChangeRate;
  return rate === undefined || rate === null ? '0%' : `${Math.abs(rate)}%`;
});

const arrow = computed(() => {
  const rate = props.item?.priceChangeRate;
  if (!rate || rate === 0) return '-';
  return rate > 0 ? '▲' : '▼';
});

const priceColorClass = computed(() => {
  const rate = props.item?.priceChangeRate;
  if (!rate || rate === 0) return 'text-gray-400';
  return rate > 0 ? 'text-red-500' : 'text-blue-500';
});
</script>

<style scoped>
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.animate-fadeIn {
  animation: fadeIn 0.3s ease-out forwards;
}
</style>
