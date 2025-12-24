<template>
  <main class="flex-1 bg-gray-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 pt-4 md:pt-6 w-full">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <PageHeader title="농촌 기사" description="야채랑 PICK이 엄선한 농촌 소식을 만나보세요." />
        </div>
        <ArticleSearchBar v-model="searchQuery" @search="handleSearch" @clear="handleClearSearch" />
      </div>
    </div>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 w-full space-y-8">
      <div v-if="articles.length" class="grid gap-6">
        <ArticleCard
          v-for="article in articles"
          :key="article.id"
          :article="article"
          @open="goToDetail"
          @toggle-bookmark="handleToggleBookmark"
        />
      </div>

      <ArticleEmptyState v-else :keyword="searchQuery" @reset="handleResetSearch" />

      <ArticlePagination :current-page="currentPage" :total-pages="totalPages" @change-page="goToPage" />
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchArticles, searchArticles } from '@/api/article';
import { useArticle } from '@/views/article/composables/useArticles';
import ArticleSearchBar from '@/views/article/components/ArticleSearchBar.vue';
import ArticleCard from '@/views/article/components/ArticleCard.vue';
import ArticlePagination from '@/views/article/components/ArticlePagination.vue';
import ArticleEmptyState from '@/views/article/components/ArticleEmptyState.vue';
import PageHeader from '@/components/layout/PageHeader.vue';

const router = useRouter();
const searchQuery = ref('');
const currentPage = ref(1);
const totalPages = ref(1);
const articles = ref([]);

const { toggleBookmarkAction } = useArticle();

const loadArticles = async (page = 1, keyword = '') => {
  try {
    const response = keyword
      ? await searchArticles({ page, size: 5, keyword })
      : await fetchArticles({ page, size: 5 });

    const data = response.data?.data;
    if (data) {
      currentPage.value = data.currentPage;
      totalPages.value = data.totalPages;
      articles.value = (data.content || []).map((item) => ({
        id: item.articleId,
        title: item.title,
        tags: item.tagList || [],
        date: item.createdAt,
        thumbnail: item.imageUrl,
        isBookmarked: item.isBookmarked,
      }));
    }
  } catch (error) {
    console.error('데이터 로드 실패:', error);
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadArticles(1, searchQuery.value);
};

const resetToAll = () => {
  searchQuery.value = '';
  currentPage.value = 1;
  loadArticles(1);
};

const handleClearSearch = () => {
  resetToAll();
};

const handleResetSearch = () => {
  resetToAll();
};

const handleToggleBookmark = (article) => {
  toggleBookmarkAction(article);
};

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    loadArticles(page, searchQuery.value);
  }
};

const goToDetail = (id) => router.push(`/articles/${id}`);

onMounted(() => loadArticles());
</script>
