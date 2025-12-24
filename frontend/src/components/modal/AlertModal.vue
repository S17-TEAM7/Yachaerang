<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="show" class="modal-overlay" @click.self="onConfirm">
        <div class="modal-container">
          <div class="modal-content">
            <h3 class="modal-title">{{ title }}</h3>
            <p class="modal-message">{{ message }}</p>
          </div>
          
          <div class="modal-actions">
            <button class="modal-btn modal-btn-confirm" @click="onConfirm">
              확인
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({
  show: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '알림'
  },
  message: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['confirm']);

const onConfirm = () => {
  emit('confirm');
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-container {
  background: white;
  border-radius: 20px;
  padding: 32px;
  max-width: 400px;
  width: calc(100% - 32px);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.modal-content {
  margin-bottom: 24px;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #222;
  margin-bottom: 12px;
}

.modal-message {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  white-space: pre-line;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-btn {
  padding: 10px 24px;
  border-radius: 26px;
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s ease;
  font-size: 14px;
}

.modal-btn-confirm {
  background: #e53935;
  color: white;
}

.modal-btn-confirm:hover {
  background: #c62828;
}

/* Transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.9);
}
</style>

