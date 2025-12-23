import { defineStore } from 'pinia';

export const useAlertStore = defineStore('alert', {
  state: () => ({
    show: false,
    title: '알림',
    message: '',
    onConfirm: null,
  }),

  actions: {
    showAlert(message, title = '알림', onConfirmCallback = null) {
      this.message = message;
      this.title = title;
      this.onConfirm = onConfirmCallback;
      this.show = true;
    },

    hide() {
      this.show = false;
      this.message = '';
      this.title = '알림';
      this.onConfirm = null;
    },

    handleConfirm() {
      if (this.onConfirm && typeof this.onConfirm === 'function') {
        this.onConfirm();
      }
      this.hide();
    },
  },
});

