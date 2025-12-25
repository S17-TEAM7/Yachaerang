<template>
  <div
    class="flex flex-col items-center justify-center py-20 px-4 bg-white rounded-2xl border border-gray-100 shadow-sm text-center"
  >
    <div class="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mb-4">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        class="h-8 w-8 text-gray-300"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
        />
      </svg>
    </div>

    <h3 class="text-xl md:text-2xl font-bold text-gray-900 mb-3">조회된 가격 정보가 없습니다.</h3>
    <p class="text-base text-gray-500 max-w-[280px] leading-relaxed mb-6">
      선택하신 기간 내에 해당 품목의 시세 데이터가 존재하지 않습니다. 기간을 조정하거나 다른 품종을 선택해 보세요.
    </p>
  </div>
</template>

<script setup>
defineEmits(['reset']);
</script>
