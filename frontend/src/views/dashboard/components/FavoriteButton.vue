<template>
  <button
    @click="$emit('click')"
    class="shrink-0 flex items-center gap-1.5 px-4 py-2.5 bg-red-50 text-red-600 rounded-lg text-sm font-bold hover:bg-red-500 hover:text-white transition-all shadow-sm active:scale-95"
  >
    + 관심품목 등록
  </button>
</template>

<script setup>
defineEmits(['click']);
</script>
