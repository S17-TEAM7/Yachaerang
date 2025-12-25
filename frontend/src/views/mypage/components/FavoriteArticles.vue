<template>
  <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-base font-black text-slate-800 flex items-center gap-2">
        <IconNewspaper class="h-5 w-5 text-[#F44323]" />
        북마크한 기사
      </h2>
    </div>
    
    <div v-if="loading" class="py-8">
      <LoadingSpinner />
    </div>
    <ul v-else-if="bookmarks?.length" class="space-y-2.5 max-h-[360px] overflow-y-auto pr-1 custom-scrollbar">
      <li
        v-for="item in bookmarks"
        :key="item.bookmarkId"
        @click="goToDetail(item)"
        class="group flex flex-col p-3.5 border border-slate-50 bg-slate-50/50 rounded-xl cursor-pointer hover:bg-white hover:border-slate-300 hover:shadow-md transition-all active:scale-[0.98]"
      >
        <div class="flex justify-between items-start mb-1">
          <div class="flex-1 min-w-0 mr-4">
            <span class="text-sm font-black text-slate-700 group-hover:text-slate-900 line-clamp-2">
              {{ item.title }}
            </span>
          </div>
          <button
            @click.stop="handleRemove(item.articleId)"
            class="text-sm text-red-400 hover:text-red-600 flex-shrink-0 p-1 font-bold transition-colors"
          >
            ✕
          </button>
        </div>
        
        <div v-if="item.tagList?.length" class="flex flex-wrap gap-1 mt-2">
          <span
            v-for="tag in item.tagList"
            :key="tag"
            class="px-1.5 py-0.5 bg-white border border-slate-200 text-[10px] font-black text-slate-500 rounded"
          >
            {{ tag }}
          </span>
        </div>
      </li>
    </ul>
    <div v-else class="py-10 text-center space-y-2">
      <p class="text-sm font-bold text-slate-300">북마크한 기사가 없습니다.</p>
    </div>

    <ConfirmModal
      :show="showRemoveConfirm"
      title="북마크 삭제"
      message="북마크에서 삭제하시겠습니까?"
      @confirm="handleRemoveConfirm"
      @cancel="showRemoveConfirm = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchBookmarks, removeBookmark } from '@/api/article';
import { useToastStore } from '@/stores/toast';
import LoadingSpinner from '@/components/spinner/LoadingSpinner.vue';
import IconNewspaper from '@/components/icons/IconNewspaper.vue';
import ConfirmModal from '@/components/modal/ConfirmModal.vue';

const router = useRouter();
const toastStore = useToastStore();

const bookmarks = ref([]);
const loading = ref(false);
const navigating = ref(false);

const loadBookmarks = async () => {
  loading.value = true;
  try {
    const response = await fetchBookmarks();
    console.log('북마크 응답:', response); // 디버깅용
    
    const { data } = response;
    bookmarks.value = Array.isArray(data?.data) ? data.data : [];
    
    console.log('북마크 목록:', bookmarks.value); // 디버깅용
  } catch (error) {
    console.error('북마크 조회 실패:', error);
    bookmarks.value = [];
  } finally {
    loading.value = false;
  }
};

const goToDetail = (item) => {
  if (navigating.value) return;
  
  navigating.value = true;
  
  router.push({
    path: `/articles/${item.articleId}`,
  }).catch((err) => {
    console.error('라우터 이동 실패:', err);
    navigating.value = false;
  });
};


const showRemoveConfirm = ref(false);
const selectedArticleId = ref(null);

const handleRemove = (articleId) => {
  selectedArticleId.value = articleId;
  showRemoveConfirm.value = true;
};

const handleRemoveConfirm = async () => {
  if (!selectedArticleId.value) return;

  try {
    await removeBookmark(selectedArticleId.value);
    bookmarks.value = bookmarks.value.filter(item => item.articleId !== selectedArticleId.value);
    
    toastStore.show('북마크에서 삭제되었습니다.', 'success');
  } catch (error) {
    console.error('삭제 실패:', error);
    toastStore.show('삭제 중 오류가 발생했습니다.', 'error');
  } finally {
    showRemoveConfirm.value = false;
    selectedArticleId.value = null;
  }
};

onMounted(loadBookmarks);
</script>