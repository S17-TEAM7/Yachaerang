<template>
  <div class="max-w-md bg-white p-6 rounded-2xl border border-gray-100 shadow-sm space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-lg md:text-xl font-black text-slate-800 flex items-center gap-2">
        <span class="w-1 h-3 bg-slate-400 rounded-full"></span>
        최근 조회 기록
      </h3>

      <button v-if="items.length" @click="$emit('clear')" class="flex items-center gap-1 text-s font-bold text-slate-400 hover:text-rose-500 transition-colors">
        <IconRefresh class="w-3.5 h-3.5" />
        초기화
      </button>
    </div>

    <ul v-if="items.length" class="space-y-2.5">
      <li
        v-for="item in items"
        :key="item.productCode"
        @click="$emit('select', item)"
        class="group flex flex-col p-4 border border-slate-50 bg-slate-50/50 rounded-xl cursor-pointer hover:bg-white hover:border-slate-300 hover:shadow-md transition-all active:scale-[0.98]"
      >
        <div class="flex justify-between items-start mb-1.5">
          <span class="text-lg font-black text-slate-800 group-hover:text-slate-900">
            {{ item.itemLabel }}
            <span class="text-slate-500 font-bold ml-1 text-sm">{{ item.varietyLabel }}</span>
          </span>
          <span class="px-2 py-1 bg-white border border-slate-200 text-sm font-black text-slate-500 rounded text-center">
            {{ periodMap[item.periodType] }}
          </span>
        </div>

        <div class="text-sm font-bold text-slate-400">
          {{ item.dateRangeLabel || "기간 정보 없음" }}
        </div>
      </li>
    </ul>

    <div v-else class="py-10 text-center space-y-2">
      <p class="text-base font-bold text-slate-300">최근 조회한 기록이 없습니다.</p>
    </div>
  </div>
</template>

<script>
import IconRefresh from "@/components/icons/IconRefresh.vue";

export default {
  name: "RecentViewedItems",
  components: { IconRefresh },
  emits: ["select", "clear"],
  props: {
    items: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {
      periodMap: {
        day: "일간",
        week: "주간",
        month: "월간",
        year: "연간",
      },
    };
  },
};
</script>

<style scoped>
li {
  will-change: transform, box-shadow;
}
</style>
