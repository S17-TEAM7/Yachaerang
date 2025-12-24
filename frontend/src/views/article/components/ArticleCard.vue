<template>
  <div
    class="group relative overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm transition-all duration-300 hover:shadow-2xl hover:-translate-y-2 cursor-pointer"
    @click="emit('open', article.id)"
  >
    <div class="flex flex-col md:flex-row items-stretch">
      <div class="relative overflow-hidden md:w-72 h-52 md:h-auto">
        <img
          :src="article.thumbnail"
          :alt="article.title"
          class="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
        />
        <div class="absolute inset-0 bg-black/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
      </div>

      <div class="flex-1 p-6 flex flex-col justify-between relative">
        <div>
          <div class="flex gap-2 mb-3">
            <span
              v-for="tag in article.tags"
              :key="tag"
              class="px-3 py-1 bg-gray-100 text-gray-600 text-xs font-semibold rounded-lg group-hover:bg-[#FECC21]/20 group-hover:text-gray-900 transition-colors"
            >
              #{{ tag }}
            </span>
          </div>

          <h3 class="text-xl font-bold text-gray-900 leading-tight line-clamp-1 pr-12">
            {{ article.title }}
          </h3>

          <p class="mt-2 text-gray-400 text-sm font-medium">
            {{ formattedDate }}
          </p>
        </div>

        <div class="mt-6 flex items-center text-gray-900 font-bold text-sm tracking-tight">
          <span class="mr-2">자세히 보기</span>
          <div
            class="flex items-center justify-center w-8 h-8 rounded-full bg-gray-50 group-hover:bg-[#FECC21] transition-colors"
          >
            <IconArrowRight class="w-4 h-4 transition-transform group-hover:translate-x-1" />
          </div>
        </div>

        <button
          type="button"
          @click.stop="handleToggleBookmark"
          :disabled="isLoading"
          class="absolute top-6 right-6 p-3 rounded-2xl bg-white border border-gray-50 shadow-sm transition-all hover:scale-110 active:scale-95 z-10 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <IconBookmark
            :active="isBookmarked"
            :class="[isBookmarked ? 'text-[#F44323]' : 'text-gray-300 group-hover:text-gray-500']"
            class="w-5 h-5 transition-colors"
          />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { saveBookmark, removeBookmark } from '@/api/article';
import { useToastStore } from '@/stores/toast';
import IconArrowRight from '@/components/icons/IconArrowRight.vue';
import IconBookmark from '@/components/icons/IconBookmark.vue';

const props = defineProps({
  article: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['open', 'bookmark-updated']);

const formattedDate = computed(() => {
  if (!props.article?.date) return '';
  return new Date(props.article.date).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
});

const toastStore = useToastStore();
const isLoading = ref(false);
const isBookmarked = ref(props.article.isBookmarked ?? false);

// propts 변경 동기화
watch(() => props.article.isBookmarked, (newVal) => {
  isBookmarked.value = newVal ?? false;
});

const handleToggleBookmark = async () => {
  if (isLoading.value) return;
  
  isLoading.value = true;
  const wasBookmarked = isBookmarked.value;
  

  isBookmarked.value = !wasBookmarked;
  
  try {
    if (wasBookmarked) {
      await removeBookmark(props.article.id);
      toastStore.show('북마크가 해제되었습니다.', 'success');
    } else {
      await saveBookmark(props.article.id);
      toastStore.show('북마크에 저장되었습니다.', 'success');
    }
    // 상태 변경
    emit('bookmark-updated', { 
      articleId: props.article.id, 
      bookmarked: isBookmarked.value 
    });
  } catch (error) {
    isBookmarked.value = wasBookmarked;
    console.error('북마크 처리 실패:', error);
    toastStore.show('북마크 처리 중 오류가 발생했습니다.', 'error');
  } finally {
    isLoading.value = false;
  }
};
</script>
