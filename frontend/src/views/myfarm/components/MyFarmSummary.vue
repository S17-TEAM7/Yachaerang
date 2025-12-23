<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 w-full pt-4 pb-8">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- 농장 정보 카드 -->
      <div v-if="farm" class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 h-fit">
        <h2 class="text-lg font-bold text-gray-800 mb-6 flex items-center gap-2">
          <span class="w-1 h-6 bg-[#F44323] rounded-full block"></span>
          농장 정보
        </h2>

        <div class="space-y-5">
          <!-- 농장 위치 -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">농장 위치</label>
            <div class="relative">
              <i class="fa-solid fa-location-dot absolute left-3 top-3.5 text-gray-400"></i>
              <div class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-700">
                {{ farm.location || '-' }}
              </div>
            </div>
          </div>

          <!-- 주품목 -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">주품목</label>
            <div class="relative">
              <i class="fa-solid fa-apple-whole absolute left-3 top-3.5 text-gray-400"></i>
              <div class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-700">
                {{ farm.mainCrop || '-' }}
              </div>
            </div>
          </div>

          <!-- 면적 그리드 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">경작면적</label>
              <div class="relative">
                <div class="w-full pl-4 pr-12 py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-700">
                  {{ farm.cultivatedArea ?? 0 }}
                </div>
                <span class="absolute right-4 top-3.5 text-gray-500 text-sm font-medium">㎡</span>
              </div>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">평지면적</label>
              <div class="relative">
                <div class="w-full pl-4 pr-12 py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-700">
                  {{ farm.flatArea ?? 0 }}
                </div>
                <span class="absolute right-4 top-3.5 text-gray-500 text-sm font-medium">㎡</span>
              </div>
            </div>
          </div>

          <!-- 인력 -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">농장 인력</label>
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-full border border-gray-300 bg-gray-50 flex items-center justify-center text-gray-400">
                <i class="fa-solid fa-user"></i>
              </div>
              <span class="text-lg font-bold text-gray-800">{{ farm.manpower ?? 0 }}</span>
              <span class="text-gray-500">명</span>
            </div>
          </div>

          <!-- 수정 버튼 -->
          <button
            @click="$emit('edit')"
            class="w-full bg-[#F44323] text-white font-bold py-4 rounded-lg shadow-md hover:bg-[#d63a1f] transform active:scale-[0.98] transition-all flex items-center justify-center gap-2"
          >
            <i class="fa-solid fa-pen-to-square"></i>
            농장 정보 수정하기
          </button>
        </div>
      </div>

      <!-- 농장 미등록 상태 (왼쪽 카드) -->
      <div v-else class="relative h-fit">
        <!-- 블러 처리된 배경 -->
        <div class="blur-sm pointer-events-none select-none bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 class="text-lg font-bold text-gray-800 mb-6 flex items-center gap-2">
            <span class="w-1 h-6 bg-[#F44323] rounded-full block"></span>
            농장 정보
          </h2>
          <div class="space-y-5">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">농장 위치</label>
              <div class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-400">-</div>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">주품목</label>
              <div class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-400">-</div>
            </div>
            <div class="grid grid-cols-2 gap-5">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">경작면적</label>
                <div class="w-full py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-400 text-center">0 ㎡</div>
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">평지면적</label>
                <div class="w-full py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-400 text-center">0 ㎡</div>
              </div>
            </div>
          </div>
        </div>
        <!-- 오버레이 -->
        <div class="absolute inset-0 flex flex-col items-center justify-center bg-white/70 rounded-xl">
          <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
            <i class="fa-solid fa-tractor text-2xl text-gray-400"></i>
          </div>
          <p class="text-lg font-bold text-gray-700 mb-2">농장 정보가 등록되지 않았습니다!</p>
          <p class="text-sm text-gray-500 mb-4">농장을 등록하고 AI 진단을 받아보세요</p>
          <button
            @click="$emit('register')"
            class="bg-[#F44323] text-white font-bold px-6 py-3 rounded-lg shadow-md hover:bg-[#d63a1f] transition-all flex items-center gap-2"
          >
            <i class="fa-solid fa-plus"></i>
            농장 등록하기
          </button>
        </div>
      </div>

      <!-- 농장 평가 결과 (오른쪽 카드) -->
      <div v-if="farm && (farm.grade || farm.comment)" class="h-fit">
        <!-- 상단: 등급 표시 -->
        <div class="bg-white p-6 rounded-t-xl shadow-sm border border-gray-100 border-b-0 relative overflow-hidden">
          <div class="absolute top-0 left-0 w-full h-2 bg-[#F44323]"></div>
          <div class="flex flex-col items-center justify-center py-6">
            <p class="text-gray-500 font-medium mb-2 uppercase tracking-widest text-xs">농장 등급</p>
            
            <div 
              :class="gradeCircleClass(farm.grade)"
              class="relative w-32 h-32 flex items-center justify-center rounded-full shadow-lg mb-4"
            >
              <span class="text-6xl font-black text-white drop-shadow-md">{{ farm.grade ?? '?' }}</span>
              <i class="fa-solid fa-star text-white text-xl absolute -top-1 -right-1"></i>
              <i class="fa-solid fa-star text-white text-xl absolute bottom-2 left-0"></i>
            </div>
            
            <h3 class="text-2xl font-bold text-gray-800">{{ gradeTitle(farm.grade) }}</h3>
            <p class="text-sm text-gray-500">AI가 분석한 농장 평가 결과입니다</p>
          </div>
        </div>

        <!-- 하단: AI 분석 및 인사이트 -->
        <div class="bg-amber-50 p-6 rounded-b-xl border border-t-0 border-gray-200">
          <!-- AI 분석 메시지 -->
          <div class="flex gap-4 mb-6">
            <div class="flex-shrink-0">
              <div class="w-10 h-10 rounded-full bg-[#F44323] text-white flex items-center justify-center shadow-sm">
                <i class="fa-solid fa-robot"></i>
              </div>
            </div>
            <div class="bg-white p-4 rounded-2xl rounded-tl-none shadow-sm border border-gray-100 flex-1">
              <p class="text-gray-700 text-sm leading-relaxed">
                <span class="font-bold text-[#F44323]">AI 분석:</span> 
                {{ farm.comment ?? '평가 내용이 없습니다.' }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 평가 미완료 상태 (오른쪽 빈 상태) -->
      <div 
        v-else-if="farm && !farm.grade && !farm.comment"
        class="flex flex-col items-center justify-center text-center h-full min-h-[400px] border-2 border-dashed border-gray-200 rounded-xl bg-gray-50/50"
      >
        <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mb-4">
          <i class="fa-solid fa-clipboard-list text-3xl text-gray-300"></i>
        </div>
        <h3 class="text-lg font-bold text-gray-400">아직 평가가 없습니다</h3>
        <p class="text-sm text-gray-400 max-w-xs mt-2">농장 정보를 저장하면 AI가 자동으로 평가를 진행합니다.</p>
      </div>

      <!-- 농장 미등록 시 오른쪽 빈 상태 -->
      <div 
        v-if="!farm"
        class="flex flex-col items-center justify-center text-center h-full min-h-[400px] border-2 border-dashed border-gray-200 rounded-xl bg-gray-50/50"
      >
        <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mb-4">
          <i class="fa-solid fa-chart-line text-3xl text-gray-300"></i>
        </div>
        <h3 class="text-lg font-bold text-gray-400">평가 대기 중</h3>
        <p class="text-sm text-gray-400 max-w-xs mt-2">농장을 등록하면 AI 진단 결과를 확인할 수 있습니다.</p>
      </div>
      </div>
    </div>
</template>

<script setup>

defineProps({
  farm: {
    type: Object,
    default: null,
  },
});

defineEmits(['edit', 'register']);

const gradeCircleClass = (grade) => {
  switch (grade) {
    case 'A':
      return 'bg-green-500';
    case 'B':
      return 'bg-blue-500';
    case 'C':
      return 'bg-yellow-500';
    case 'D':
      return 'bg-orange-500';
    default:
      return 'bg-gray-400';
  }
};

const gradeTitle = (grade) => {
  switch (grade) {
    case 'A':
      return '우수합니다!';
    case 'B':
      return '양호합니다!';
    case 'C':
      return '보통입니다';
    case 'D':
      return '개선이 필요합니다';
    default:
      return '평가 없음';
  }
};
</script>