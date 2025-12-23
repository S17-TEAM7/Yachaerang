<template>
  <header class="mb-3 md:mb-4">
    <h1 class="text-2xl md:text-3xl font-bold tracking-tight text-gray-900">{{ title }}</h1>
    <p class="text-gray-500 mt-2 text-sm md:text-base">{{ description }}</p>
  </header>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
});
</script>
