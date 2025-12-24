<template>
  <div class="min-h-screen bg-gray-50 pb-20">
    <nav class="sticky top-0 z-20 bg-white/90 backdrop-blur-md border-b border-gray-100">
      <div class="max-w-4xl mx-auto px-6 h-16 flex items-center justify-between">
        <button type="button" @click="goToList" class="flex items-center text-gray-600 hover:text-black transition-colors font-bold group">
          <IconChevronLeft class="w-5 h-5 mr-1 group-hover:-translate-x-1 transition-transform" />
          목록으로
        </button>
        <button 
          type="button" 
          @click="handleToggleBookmark" 
          :disabled="isBookmarkLoading"
          class="p-2 hover:bg-gray-100 rounded-full transition-colors disabled:opacity-50"
        >
          <IconBookmark :active="isBookmarked" class="w-6 h-6" />
        </button>
      </div>
    </nav>

    <div class="max-w-4xl mx-auto px-6 mt-8">
      <article class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        <div v-if="article.image" class="w-full h-[400px] overflow-hidden">
          <img :src="article.image" :alt="article.title" class="w-full h-full object-cover" />
        </div>

        <div class="p-8 md:p-12">
          <header class="mb-10 text-center">
            <div class="flex justify-center gap-2 mb-4">
              <span v-for="tag in article.tags" :key="tag" class="px-3 py-1 bg-[#FECC21]/10 text-gray-800 text-sm font-bold rounded-full">#{{ tag }}</span>
            </div>
            <h1 class="text-3xl md:text-4xl font-black text-gray-900 leading-tight mb-6">
              {{ article.title }}
            </h1>
            <div class="flex items-center justify-center text-gray-400 text-sm gap-4 border-y border-gray-50 py-4">
              <div class="flex items-center">
                <IconCalendar class="w-4 h-4 mr-1" />
                {{ formattedDate }}
              </div>
              <span class="text-gray-200">|</span>
              <a v-if="article.sourceUrl" :href="article.sourceUrl" target="_blank" rel="noopener noreferrer" class="flex items-center text-[#e5b800] font-bold hover:underline transition-colors">
                기사 원문 보기
                <IconExternalLink class="w-4 h-4 ml-1" />
              </a>
            </div>
          </header>

          <div class="prose prose-yellow max-w-none">
            <p v-for="(paragraph, index) in article.content" :key="index" class="text-gray-700 leading-[1.8] text-lg mb-6 break-all">
              {{ paragraph }}
            </p>
          </div>
        </div>

        <footer class="bg-gray-50/50 p-8 md:p-12 border-t border-gray-100">
          <ArticleReactions
            :reaction-icons="reactionIcons"
            :reaction-labels="reactionLabels"
            :reactions="reactions"
            :my-reaction="myReaction"
            :all-reactors="allReactors"
            @toggle-reaction="handleToggleReaction"
          />
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { fetchArticleDetail, saveBookmark, removeBookmark } from "@/api/article";
import { useToastStore } from '@/stores/toast';
import { useArticle } from "@/views/article/composables/useArticles";
import IconChevronLeft from "@/components/icons/IconChevronLeft.vue";
import IconBookmark from "@/components/icons/IconBookmark.vue";
import IconCalendar from "@/components/icons/IconCalendar.vue";
import IconExternalLink from "@/components/icons/IconExternalLink.vue";
import ArticleReactions from "@/views/article/components/ArticleReactions.vue";

const toastStore = useToastStore();

const router = useRouter();
const route = useRoute();

const isBookmarked = ref(false);
const isBookmarkLoading = ref(false);

const myReaction = ref(null);
const article = ref({
  id: null,
  title: "",
  date: "",
  sourceUrl: "",
  image: "",
  content: [],
  tags: [],
  isBookmarked: false,  // bookmarked -> isBookmarked로 통일
});


watch(
  () => article.value.isBookmarked,
  (newVal) => {
    isBookmarked.value = newVal ?? false;
  },
  { immediate: true }
);

const reactionIcons = { like: "👍", helpful: "💡", suprise: "😲", sad: "🥺", bummer: "💪" };
const reactionLabels = { like: "좋아요", helpful: "유익해요", suprise: "놀랐어요", sad: "슬퍼요", bummer: "아쉬워요" };
const reactions = ref({
  like: { count: 24 },
  helpful: { count: 12 },
  suprise: { count: 18 },
  sad: { count: 2 },
  bummer: { count: 31 },
});

const allReactors = ref([
  { nickname: "김야채", profile: "", type: "like" },
  { nickname: "도시농부", profile: "", type: "helpful" },
  { nickname: "해피팜", profile: "", type: "suprise" },
  { nickname: "귀농꿈나무", profile: "", type: "sad" },
  { nickname: "프레쉬맨", profile: "", type: "like" },
]);

const { toggleBookmarkAction, toggleReactionAction } = useArticle();

const formattedDate = computed(() => {
  if (!article.value?.date) return "";
  return new Date(article.value.date).toLocaleDateString("ko-KR");
});

const loadArticleDetail = async () => {
  try {
    const articleId = route.params.id;
    const response = await fetchArticleDetail(articleId);
    const data = response.data?.data;
    if (data) {
      article.value = {
        id: data.articleId,
        title: data.title,
        date: data.createdAt,
        sourceUrl: data.url,
        image: data.imageUrl,
        content: (data.content || "")
          .replace(/\\n/g, "\n")
          .split("\n")
          .map((p) => p.trim())
          .filter((p) => p.length > 0),
        tags: data.tagList || [],
        isBookmarked: data.isBookmarked,
      };
    }
  } catch (error) {
    console.error("기사 상세 조회 실패:", error);
  }
};

const handleToggleBookmark = async () => {
  if (isBookmarkLoading.value) return;
  
  isBookmarkLoading.value = true;
  const wasBookmarked = isBookmarked.value;
  
  // 낙관적 업데이트
  isBookmarked.value = !wasBookmarked;
  
  try {
    if (wasBookmarked) {
      await removeBookmark(article.value.id);
      toastStore.show('북마크가 해제되었습니다.', 'success');
    } else {
      await saveBookmark(article.value.id);
      toastStore.show('북마크에 저장되었습니다.', 'success');
    }
    // 성공 시 article 객체도 동기화
    article.value.isBookmarked = isBookmarked.value;
  } catch (error) {
    isBookmarked.value = wasBookmarked;
    console.error('북마크 처리 실패:', error);
    toastStore.show('북마크 처리 중 오류가 발생했습니다.', 'error');
  } finally {
    isBookmarkLoading.value = false;
  }
};

const handleToggleReaction = (type) => {
  toggleReactionAction(type, myReaction, reactions);
};

const goToList = () => router.push("/articles");

onMounted(loadArticleDetail);
</script>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}
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
</style>
