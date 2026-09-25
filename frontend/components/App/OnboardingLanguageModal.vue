<template>
  <Dialog :open="isOpen" @update:open="onOpenChange">
    <DialogContent class="sm:max-w-[620px] max-h-[90vh] overflow-y-auto p-6">
      <DialogHeader class="text-left space-y-2">
        <div class="flex items-center gap-3">
          <div class="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <MdiTranslate class="size-6" />
          </div>
          <div>
            <DialogTitle class="text-xl font-bold tracking-tight">
              {{ isChineseSelected ? "欢迎使用 HomeBox 资产管理系统" : "Welcome to HomeBox" }}
            </DialogTitle>
            <DialogDescription class="text-sm text-muted-foreground mt-0.5">
              {{ isChineseSelected 
                ? "首次使用向导 · 请选择您的首选系统语言与初始数据方案" 
                : "First-time setup · Select your language and initial configuration" }}
            </DialogDescription>
          </div>
        </div>
      </DialogHeader>

      <div class="space-y-5 my-2">
        <!-- 语言选择卡片 -->
        <div>
          <label class="block text-sm font-semibold mb-2.5">
            {{ isChineseSelected ? "1. 选择系统界面语言" : "1. Select System Language" }}
          </label>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <!-- 简体中文卡片 -->
            <div
              class="relative flex flex-col p-4 rounded-xl border-2 cursor-pointer transition-all duration-200"
              :class="selectedLang === 'zh-CN'
                ? 'border-primary bg-primary/5 shadow-sm'
                : 'border-border hover:border-primary/40 bg-card'"
              @click="selectLanguage('zh-CN')"
            >
              <div class="flex items-center justify-between mb-1.5">
                <span class="flex items-center gap-2 text-base font-semibold">
                  <span class="text-xl">🇨🇳</span>
                  <span>简体中文</span>
                </span>
                <span class="text-xs px-2 py-0.5 rounded-full font-medium bg-primary/20 text-primary">
                  推荐 / Recommended
                </span>
              </div>
              <p class="text-xs text-muted-foreground leading-relaxed">
                全中文操作界面，支持中国家庭收纳场景与常用分类预设。
              </p>
              <div
                v-if="selectedLang === 'zh-CN'"
                class="absolute -top-2 -right-2 size-5 rounded-full bg-primary text-primary-foreground flex items-center justify-center shadow"
              >
                <MdiCheck class="size-3.5" />
              </div>
            </div>

            <!-- English Card -->
            <div
              class="relative flex flex-col p-4 rounded-xl border-2 cursor-pointer transition-all duration-200"
              :class="selectedLang === 'en'
                ? 'border-primary bg-primary/5 shadow-sm'
                : 'border-border hover:border-primary/40 bg-card'"
              @click="selectLanguage('en')"
            >
              <div class="flex items-center justify-between mb-1.5">
                <span class="flex items-center gap-2 text-base font-semibold">
                  <span class="text-xl">🇺🇸</span>
                  <span>English</span>
                </span>
              </div>
              <p class="text-xs text-muted-foreground leading-relaxed">
                Standard English interface with default configuration.
              </p>
              <div
                v-if="selectedLang === 'en'"
                class="absolute -top-2 -right-2 size-5 rounded-full bg-primary text-primary-foreground flex items-center justify-center shadow"
              >
                <MdiCheck class="size-3.5" />
              </div>
            </div>
          </div>
        </div>

        <!-- 中文本土化预设配置区块 -->
        <div v-if="isChineseSelected" class="space-y-3 rounded-xl border border-primary/20 bg-primary/[0.02] p-4">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h4 class="text-sm font-semibold flex items-center gap-2 text-foreground">
                <MdiHomeCityOutline class="size-4 text-primary" />
                <span>2. 中国家庭本土化数据方案</span>
              </h4>
              <p class="text-xs text-muted-foreground mt-0.5">
                针对国内住宅格局深度定制，免去从零配置的繁琐。
              </p>
            </div>
            <label class="flex items-center gap-2 cursor-pointer select-none">
              <input
                v-model="createChinesePresets"
                type="checkbox"
                class="size-4 rounded border-primary text-primary focus:ring-primary"
              />
              <span class="text-xs font-medium text-foreground">生成预设</span>
            </label>
          </div>

          <div v-if="createChinesePresets" class="space-y-3 pt-1 text-xs">
            <!-- 存放位置预览 -->
            <div class="p-2.5 rounded-lg bg-background/80 border">
              <div class="font-medium text-foreground mb-1 flex items-center gap-1.5">
                <MdiMapMarkerOutline class="size-3.5 text-primary" />
                <span>推荐常用存放位置 (10项)：</span>
              </div>
              <div class="flex flex-wrap gap-1.5 mt-1">
                <span
                  v-for="loc in presets.CHINESE_LOCATION_PRESETS"
                  :key="loc.name"
                  class="px-2 py-0.5 rounded bg-muted text-muted-foreground font-medium"
                >
                  {{ loc.name }}
                </span>
              </div>
            </div>

            <!-- 分类标签预览 -->
            <div class="p-2.5 rounded-lg bg-background/80 border">
              <div class="font-medium text-foreground mb-1 flex items-center gap-1.5">
                <MdiTagOutline class="size-3.5 text-primary" />
                <span>推荐精细分类标签 (10项)：</span>
              </div>
              <div class="flex flex-wrap gap-1.5 mt-1">
                <span
                  v-for="tg in presets.CHINESE_TAG_PRESETS"
                  :key="tg.name"
                  class="px-2 py-0.5 rounded text-white font-medium shadow-xs"
                  :style="{ backgroundColor: tg.color }"
                >
                  {{ tg.name }}
                </span>
              </div>
            </div>

            <!-- 数据安全提示 (严格保留英文) -->
            <div class="flex items-start gap-2.5 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-800 dark:text-emerald-300">
              <MdiShieldCheckOutline class="size-5 shrink-0 mt-0.5 text-emerald-600 dark:text-emerald-400" />
              <div class="space-y-0.5">
                <p class="font-semibold text-xs">数据安全保护原则 (保持原样)</p>
                <p class="text-[11px] leading-relaxed opacity-90">
                  若您的系统中已存在系统默认或原有的英文存放位置与标签（如 Living Room、Garage、Kitchen 等），系统将<strong>严格保持原样保留</strong>，绝不会做破坏性翻译或覆盖，确保您的资产数据完全安全！
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <DialogFooter class="flex flex-col-reverse sm:flex-row sm:items-center sm:justify-between gap-2 mt-4 pt-3 border-t">
        <button
          type="button"
          class="text-xs text-muted-foreground hover:text-foreground underline underline-offset-4 self-center sm:self-auto cursor-pointer"
          :disabled="presets.isInitializing.value"
          @click="skipOnboarding"
        >
          {{ isChineseSelected ? "稍后在设置中配置" : "Configure later in settings" }}
        </button>

        <Button
          type="button"
          class="gap-2 px-6"
          :disabled="presets.isInitializing.value"
          @click="confirmAndApply"
        >
          <MdiLoading v-if="presets.isInitializing.value" class="size-4 animate-spin" />
          <span>{{ isChineseSelected ? "确认并开始使用" : "Confirm and Get Started" }}</span>
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from "vue";
  import { useI18n } from "vue-i18n";
  import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
  import { Button } from "@/components/ui/button";
  import { toast } from "@/components/ui/sonner";
  import MdiTranslate from "~icons/mdi/translate";
  import MdiCheck from "~icons/mdi/check";
  import MdiShieldCheckOutline from "~icons/mdi/shield-check-outline";
  import MdiHomeCityOutline from "~icons/mdi/home-city-outline";
  import MdiMapMarkerOutline from "~icons/mdi/map-marker-outline";
  import MdiTagOutline from "~icons/mdi/tag-outline";
  import MdiLoading from "~icons/mdi/loading";
  import { useViewPreferences } from "~~/composables/use-preferences";
  import { useAuthContext } from "~~/composables/use-auth-context";
  import { useLocalizedPresets } from "~~/composables/use-localized-presets";

  const { locale } = useI18n();
  const preferences = useViewPreferences();
  const auth = useAuthContext();
  const presets = useLocalizedPresets();

  const isOpen = ref(false);
  const selectedLang = ref("zh-CN");
  const createChinesePresets = ref(true);

  const isChineseSelected = computed(() => selectedLang.value.startsWith("zh"));

  onMounted(() => {
    // 检查是否已完成初始化向导
    const isCompleted = presets.isOnboardingCompleted();
    
    // 如果用户已登录，且未完成初始化向导，则延迟弹出
    if (auth.token && !isCompleted) {
      // 预先依据系统当前语言或浏览器偏好推测
      const current = preferences.value.language || locale.value || navigator.language || "zh-CN";
      selectedLang.value = current.toLowerCase().startsWith("zh") ? "zh-CN" : "en";

      setTimeout(() => {
        // 二次核验是否已完成
        if (!presets.isOnboardingCompleted()) {
          isOpen.value = true;
        }
      }, 500);
    }
  });

  function selectLanguage(lang: string) {
    selectedLang.value = lang;
    if (lang === "zh-CN") {
      createChinesePresets.value = true;
    }
  }

  function onOpenChange(open: boolean) {
    if (!open && !presets.isOnboardingCompleted()) {
      skipOnboarding();
    }
    isOpen.value = open;
  }

  function skipOnboarding() {
    presets.markOnboardingCompleted();
    isOpen.value = false;
  }

  async function confirmAndApply() {
    try {
      // 1. 设置语言偏好
      preferences.value.language = selectedLang.value;
      locale.value = selectedLang.value;

      // 2. 如果选择了中文且勾选了预设，执行安全创建
      if (selectedLang.value === "zh-CN" && createChinesePresets.value) {
        await presets.initializeChinesePresets({ showToasts: false });
      }

      // 3. 标记完成并关闭
      presets.markOnboardingCompleted();
      isOpen.value = false;

      toast.success(
        selectedLang.value === "zh-CN"
          ? "系统语言与预设配置完成，欢迎使用 HomeBox！"
          : "System setup completed. Welcome to HomeBox!"
      );
    } catch (err) {
      console.error("Error during onboarding apply:", err);
      toast.error("配置过程中出现问题，请在个人设置中重新尝试。");
      isOpen.value = false;
    }
  }

  // 暴露打开方法，供个人设置或按钮调用
  defineExpose({
    open: () => {
      isOpen.value = true;
    },
  });
</script>
