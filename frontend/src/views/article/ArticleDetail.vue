<template>
  <div class="min-h-screen bg-gray-50 pb-20">
    <nav class="sticky top-0 z-20 bg-white/90 backdrop-blur-md border-b border-gray-100">
      <div class="max-w-4xl mx-auto px-6 h-16 flex items-center justify-between">
        <button
          type="button"
          @click="goToList"
          class="flex items-center text-gray-600 hover:text-black transition-colors font-bold group"
        >
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
              <span
                v-for="tag in article.tags"
                :key="tag"
                @click="handleTagClick(tag)"
                class="px-3 py-1 bg-[#FECC21]/10 text-gray-800 text-sm font-bold rounded-full border border-[#FECC21]/20 cursor-pointer hover:bg-[#FECC21] hover:border-[#FECC21] hover:text-gray-900 hover:shadow-md transition-all duration-300"
                >#{{ tag }}</span
              >
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
              <a
                v-if="article.sourceUrl"
                :href="article.sourceUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="flex items-center text-[#e5b800] font-bold hover:underline transition-colors"
              >
                기사 원문 보기
                <IconExternalLink class="w-4 h-4 ml-1" />
              </a>
            </div>
          </header>

          <div
            class="markdown-body text-gray-700 leading-[1.8] text-lg prose prose-yellow max-w-none"
            v-html="renderedContent"
          ></div>
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
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  fetchArticleDetail,
  saveBookmark,
  removeBookmark,
  fetchReactionStatistics,
  fetchReactionMembers,
} from '@/api/article';
import { getMyProfile } from '@/api/member';
import { useToastStore } from '@/stores/toast';
import { useArticle } from '@/views/article/composables/useArticles';
import { tokenStorage } from '@/utils/storage';
import { useMarkdown } from '@/views/ai/composables/useMarkdown';
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue';
import IconBookmark from '@/components/icons/IconBookmark.vue';
import IconCalendar from '@/components/icons/IconCalendar.vue';
import IconExternalLink from '@/components/icons/IconExternalLink.vue';
import ArticleReactions from '@/views/article/components/ArticleReactions.vue';

const toastStore = useToastStore();
const { render: renderMarkdown } = useMarkdown();

const router = useRouter();
const route = useRoute();

const isBookmarked = ref(false);
const isBookmarkLoading = ref(false);

const myReaction = ref(null);
const article = ref({
  id: null,
  title: '',
  date: '',
  sourceUrl: '',
  image: '',
  content: [],
  tags: [],
  isBookmarked: false,
});

watch(
  () => article.value.isBookmarked,
  (newVal) => {
    isBookmarked.value = newVal ?? false;
  },
  { immediate: true }
);

const reactionIcons = { like: '👍', helpful: '💡', surprise: '😲', sad: '🥺', bummer: '😥' };
const reactionLabels = { like: '좋아요', helpful: '유익해요', surprise: '놀랐어요', sad: '슬퍼요', bummer: '아쉬워요' };

// 리액션 타입 역매핑
const REACTION_TYPE_REVERSE_MAP = {
  GOOD: 'like',
  HELPFUL: 'helpful',
  SURPRISED: 'surprise',
  SAD: 'sad',
  BUMMER: 'bummer',
};

const reactions = ref({
  like: { count: 0 },
  helpful: { count: 0 },
  surprise: { count: 0 },
  sad: { count: 0 },
  bummer: { count: 0 },
});

const allReactors = ref([]);

const { toggleReactionAction } = useArticle();

const renderedContent = ref('');

const formattedDate = computed(() => {
  if (!article.value?.date) return '';
  return new Date(article.value.date).toLocaleDateString('ko-KR');
});

const loadReactionStatistics = async (articleId) => {
  try {
    const response = await fetchReactionStatistics(articleId);
    const data = response.data?.data || [];

    // 리액션 통계 초기화
    reactions.value = {
      like: { count: 0 },
      helpful: { count: 0 },
      surprise: { count: 0 },
      sad: { count: 0 },
      bummer: { count: 0 },
    };

    data.forEach((item) => {
      const frontendType = REACTION_TYPE_REVERSE_MAP[item.reactionType];
      if (frontendType) {
        reactions.value[frontendType].count = item.count || 0;
      }
    });
  } catch (error) {
    console.error('리액션 통계 조회 실패:', error);
  }
};

const loadReactionMembers = async (articleId) => {
  try {
    myReaction.value = null; // 초기화
    
    let currentUser = tokenStorage.getUser();
    
    // 로컬 스토리지에 유저가 없으면 서버에서 가져오기
    if (!currentUser && tokenStorage.getAccessToken()) {
      try {
        const response = await getMyProfile();
        if (response.data?.success) {
          currentUser = response.data.data;
          tokenStorage.setUser(currentUser);
        }
      } catch (error) {
        console.error('사용자 프로필 조회 실패:', error);
      }
    }
    
    const currentUserNickname = currentUser?.nickname;
    const reactionTypes = ['HELPFUL', 'GOOD', 'SURPRISED', 'SAD', 'BUMMER'];
    
    // 모든 리액션 타입에 대한 멤버 병렬 조회
    const memberResponses = await Promise.all(
      reactionTypes.map(type => fetchReactionMembers(articleId, type))
    );

    const allMembers = [];
    
    memberResponses.forEach((response, index) => {
      const members = response.data?.data || [];
      const backendType = reactionTypes[index];
      const frontendType = REACTION_TYPE_REVERSE_MAP[backendType];

      members.forEach((member) => {
        allMembers.push({
          nickname: member.nickname,
          profile: member.imageUrl || '',
          type: frontendType,
        });

        // 현재 사용자의 리액션 타입 확인
        if (currentUserNickname && member.nickname === currentUserNickname) {
          myReaction.value = frontendType;
        }
      });
    });

    allReactors.value = allMembers;
  } catch (error) {
    console.error('리액션 멤버 조회 실패:', error);
  }
};

const loadArticleDetail = async () => {
  try {
    const articleId = route.params.id;
    const response = await fetchArticleDetail(articleId);
    const data = response.data?.data;
    if (data) {
      const contentText = (data.content || '').replace(/\\n/g, '\n').trim();

      article.value = {
        id: data.articleId,
        title: data.title,
        date: data.createdAt,
        sourceUrl: data.url,
        image: data.imageUrl,
        content: contentText.split('\n').map((p) => p.trim()).filter((p) => p.length > 0),
        tags: data.tagList || [],
        isBookmarked: data.isBookmarked,
      };

      // 마크다운 렌더링
      renderedContent.value = renderMarkdown(contentText);

      // 리액션 통계 및 멤버 조회
      await Promise.all([loadReactionStatistics(articleId), loadReactionMembers(articleId)]);
    }
  } catch (error) {
    console.error('기사 상세 조회 실패:', error);
  }
};

const handleToggleBookmark = async () => {
  if (isBookmarkLoading.value) return;

  const accessToken = tokenStorage.getAccessToken();
  const hasValidToken =
    accessToken && accessToken.trim() !== '' && accessToken !== 'null' && accessToken !== 'undefined';

  if (!hasValidToken) {
    toastStore.show('로그인이 필요한 서비스입니다. 로그인 후 이용해 주세요', 'info');
    return;
  }

  isBookmarkLoading.value = true;
  const wasBookmarked = isBookmarked.value;

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

const handleToggleReaction = async (type) => {
  if (!article.value.id) return;

  await toggleReactionAction(article.value.id, type, myReaction, reactions);

  // 리액션 변경 후 통계 및 멤버 다시 조회
  await Promise.all([loadReactionStatistics(article.value.id), loadReactionMembers(article.value.id)]);
};

const goToList = () => router.push('/articles');

const handleTagClick = (tag) => {
  router.push({ path: '/articles', query: { keyword: tag } });
};

onMounted(loadArticleDetail);
</script>
