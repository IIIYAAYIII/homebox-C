<template>
  <Dialog :open="isOpen" @update:open="onOpenChange">
    <DialogContent class="sm:max-w-[640px] max-h-[90vh] overflow-y-auto p-6">
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
        <!-- 1. 语言选择卡片 -->
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

        <!-- 2. 中文联动配置选项 (货币、格式、存放位置与标签) -->
        <div v-if="isChineseSelected" class="space-y-3 rounded-xl border border-primary/20 bg-primary/[0.02] p-4">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h4 class="text-sm font-semibold flex items-center gap-2 text-foreground">
                <MdiHomeCityOutline class="size-4 text-primary" />
                <span>2. 本土化信息联动配置 (随中文语言自动生效)</span>
              </h4>
              <p class="text-xs text-muted-foreground mt-0.5">
                货币单位、日期显示及家庭收纳格局全面适配国内使用习惯。
              </p>
            </div>
          </div>

          <div class="space-y-2.5 pt-1 text-xs">
            <!-- 货币联动选项 -->
            <label class="flex items-center justify-between p-2.5 rounded-lg bg-background/90 border cursor-pointer hover:border-primary/30 transition-colors">
              <div class="flex items-center gap-2.5">
                <span class="flex size-7 items-center justify-center rounded-md bg-amber-500/10 text-amber-600 font-bold text-sm">
                  ¥
                </span>
                <div>
                  <div class="font-medium text-foreground text-xs">
                    结算货币同步设为 人民币 (CNY - ¥)
                  </div>
                  <div class="text-[11px] text-muted-foreground">
                    物品价格、存放位置价值与总资产以 ¥ 货币符号计价显示
                  </div>
                </div>
              </div>
              <input
                v-model="setCurrencyCNY"
                type="checkbox"
                class="size-4 rounded border-primary text-primary focus:ring-primary cursor-pointer"
              />
            </label>

            <!-- 日期区域格式联动选项 -->
            <label class="flex items-center justify-between p-2.5 rounded-lg bg-background/90 border cursor-pointer hover:border-primary/30 transition-colors">
              <div class="flex items-center gap-2.5">
                <span class="flex size-7 items-center justify-center rounded-md bg-blue-500/10 text-blue-600">
                  <MdiCalendarClock class="size-4" />
                </span>
                <div>
                  <div class="font-medium text-foreground text-xs">
                    日期与数字格式设为 中文标准 (zh-CN)
                  </div>
                  <div class="text-[11px] text-muted-foreground">
                    日期显示为“2026年9月26日”，相对时间显示为“刚刚”、“3分钟前”等
                  </div>
                </div>
              </div>
              <input
                v-model="setChineseFormat"
                type="checkbox"
                class="size-4 rounded border-primary text-primary focus:ring-primary cursor-pointer"
              />
            </label>

            <!-- 存放位置与标签联动选项 -->
            <div class="p-2.5 rounded-lg bg-background/90 border space-y-2">
              <label class="flex items-center justify-between cursor-pointer">
                <div class="flex items-center gap-2.5">
                  <span class="flex size-7 items-center justify-center rounded-md bg-emerald-500/10 text-emerald-600">
                    <MdiHomeOutline class="size-4" />
                  </span>
                  <div>
                    <div class="font-medium text-foreground text-xs">
                      生成中国家庭常用存放位置与分类标签
                    </div>
                    <div class="text-[11px] text-muted-foreground">
                      提供客厅、主卧、书房、阳台、数码科技、重要证件等10位置+10标签
                    </div>
                  </div>
                </div>
                <input
                  v-model="createChinesePresets"
                  type="checkbox"
                  class="size-4 rounded border-primary text-primary focus:ring-primary cursor-pointer"
                />
              </label>

              <!-- 展开位置与标签预览 -->
              <div v-if="createChinesePresets" class="pt-2 border-t space-y-2">
                <div>
                  <div class="font-medium text-foreground text-[11px] mb-1 flex items-center gap-1">
                    <MdiMapMarkerOutline class="size-3 text-primary" />
                    <span>常用存放位置预览：</span>
                  </div>
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="loc in presets.CHINESE_LOCATION_PRESETS"
                      :key="loc.name"
                      class="px-1.5 py-0.5 rounded bg-muted text-muted-foreground text-[11px]"
                    >
                      {{ loc.name }}
                    </span>
                  </div>
                </div>

                <div>
                  <div class="font-medium text-foreground text-[11px] mb-1 flex items-center gap-1">
                    <MdiTagOutline class="size-3 text-primary" />
                    <span>分类标签预览：</span>
                  </div>
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="tg in presets.CHINESE_TAG_PRESETS"
                      :key="tg.name"
                      class="px-1.5 py-0.5 rounded text-white text-[11px] font-medium"
                      :style="{ backgroundColor: tg.color }"
                    >
                      {{ tg.name }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 数据安全提示 (严格保留英文原有数据) -->
            <div class="flex items-start gap-2.5 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-800 dark:text-emerald-300">
              <MdiShieldCheckOutline class="size-5 shrink-0 mt-0.5 text-emerald-600 dark:text-emerald-400" />
              <div class="space-y-0.5">
                <p class="font-semibold text-xs">数据安全保护原则 (原有数据保持原样)</p>
                <p class="text-[11px] leading-relaxed opacity-90">
                  若系统中已存在默认或原有的英文存放位置与标签（如 Living Room、Garage、Kitchen 等），系统将<strong>严格保持原样保留</strong>，绝不会做破坏性翻译或覆盖，确保您的资产数据完全安全！
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
  import MdiHomeOutline from "~icons/mdi/home-outline";
  import MdiMapMarkerOutline from "~icons/mdi/map-marker-outline";
  import MdiTagOutline from "~icons/mdi/tag-outline";
  import MdiCalendarClock from "~icons/mdi/calendar-clock";
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
  const setCurrencyCNY = ref(true);
  const setChineseFormat = ref(true);
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
        if (!presets.isOnboardingCompleted()) {
          isOpen.value = true;
        }
      }, 500);
    }
  });

  function selectLanguage(lang: string) {
    selectedLang.value = lang;
    if (lang === "zh-CN") {
      setCurrencyCNY.value = true;
      setChineseFormat.value = true;
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
      // 1. 设置系统语言
      preferences.value.language = selectedLang.value;
      locale.value = selectedLang.value;

      // 2. 如果选择了中文，根据勾选项执行联动配置
      if (selectedLang.value === "zh-CN") {
        if (setChineseFormat.value) {
          preferences.value.overrideFormatLocale = "zh-CN";
        }
        await presets.initializeChinesePresets({
          showToasts: false,
          updateCurrency: setCurrencyCNY.value,
          updateLocaleFormat: setChineseFormat.value,
        });
      } else {
        preferences.value.overrideFormatLocale = "en-US";
      }

      // 3. 标记完成并关闭
      presets.markOnboardingCompleted();
      isOpen.value = false;

      toast.success(
        selectedLang.value === "zh-CN"
          ? "系统设置已完成，货币已设为人民币 (CNY - ¥)，欢迎使用 HomeBox！"
          : "System setup completed. Welcome to HomeBox!"
      );
    } catch (err) {
      console.error("Error during onboarding apply:", err);
      toast.error("配置过程中出现问题，请在个人设置中重新尝试。");
      isOpen.value = false;
    }
  }

  defineExpose({
    open: () => {
      isOpen.value = true;
    },
  });
</script>
