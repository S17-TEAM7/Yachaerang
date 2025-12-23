<template>
  <div class="inline-spinner-container">
    <div class="spinner">
      <div class="spinner-track"></div>
      <div class="spinner-bar"></div>
    </div>
    <p v-if="showText" class="spinner-text">{{ text }}</p>
  </div>
</template>

<script setup>
defineProps({
  showText: {
    type: Boolean,
    default: true,
  },
  text: {
    type: String,
    default: '불러오는 중...',
  },
});
</script>

<style scoped>
.inline-spinner-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 1.5rem;
}

.spinner {
  position: relative;
  width: 36px;
  height: 36px;
}

/* 전체적인 링 형태 */
.spinner-track,
.spinner-bar {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 3px solid transparent;
}

/* 고정된 배경 링: 아주 연한 회색 */
.spinner-track {
  border-color: #f1f5f9;
}

/* 실제로 돌아가는 부분: 세련된 Slate 회색 */
.spinner-bar {
  border-top-color: #94a3b8; /* Slate-400 */
  animation: spin 0.8s linear infinite;
}

.spinner-text {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 500;
  color: #64748b; /* Slate-500 */
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
