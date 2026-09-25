<script setup lang="ts">
  import { computed } from "vue";
  import { useI18n } from "vue-i18n";
  import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
  } from "@/components/ui/dropdown-menu";
  import { Button } from "@/components/ui/button";
  import MdiTranslate from "~icons/mdi/translate";
  import MdiCheck from "~icons/mdi/check";
  import { useViewPreferences } from "~~/composables/use-preferences";

  import { toast } from "@/components/ui/sonner";
  import { useLocalizedPresets } from "~~/composables/use-localized-presets";

  const { locale, availableLocales, t } = useI18n();
  const preferences = useViewPreferences();
  const presets = useLocalizedPresets();

  const currentLocale = computed(() => locale.value);

  // Key languages prioritized for easy access
  const primaryLocales = [
    { code: "zh-CN", label: "简体中文 (Simplified Chinese)", flag: "🇨🇳" },
    { code: "en", label: "English", flag: "🇺🇸" },
  ];

  const otherLocales = computed(() => {
    return availableLocales.filter(code => code !== "zh-CN" && code !== "en");
  });

  async function selectLanguage(langCode: string) {
    const prevLang = locale.value;
    preferences.value.language = langCode;
    locale.value = langCode;

    // 如果切换为简体中文，且尚未初始化中文位置与标签，弹出提示引导一键生成
    if (langCode === "zh-CN" && prevLang !== "zh-CN") {
      preferences.value.overrideFormatLocale = "zh-CN";
      const hasChinese = await presets.hasChinesePresets();
      if (!hasChinese) {
        toast("已切换为简体中文", {
          description: "是否同步将货币设为人民币(CNY - ¥)并生成常用中文存放位置与标签？（已有英文数据将严格保持原样）",
          action: {
            label: "立即同步",
            onClick: async () => {
              await presets.initializeChinesePresets({ updateCurrency: true, updateLocaleFormat: true });
            },
          },
          duration: 8000,
        });
      }
    }
  }
</script>

<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button
        variant="ghost"
        size="icon"
        class="h-9 w-9 text-muted-foreground transition-colors hover:text-foreground"
        :title="$t('profile.language') || 'Language'"
      >
        <MdiTranslate class="size-5" />
        <span class="sr-only">Toggle language</span>
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" class="w-56 max-h-80 overflow-y-auto z-50">
      <DropdownMenuLabel class="text-xs font-semibold text-muted-foreground">
        {{ $t("profile.language") || "Language / 语言" }}
      </DropdownMenuLabel>
      <DropdownMenuSeparator />

      <!-- Primary Locales -->
      <DropdownMenuItem
        v-for="item in primaryLocales"
        :key="item.code"
        class="flex cursor-pointer items-center justify-between font-medium"
        :class="{ 'bg-accent text-accent-foreground': currentLocale === item.code }"
        @click="selectLanguage(item.code)"
      >
        <span class="flex items-center gap-2">
          <span>{{ item.flag }}</span>
          <span>{{ item.label }}</span>
        </span>
        <MdiCheck v-if="currentLocale === item.code" class="size-4 text-primary" />
      </DropdownMenuItem>

      <DropdownMenuSeparator />
      <DropdownMenuLabel class="text-xs text-muted-foreground">
        {{ currentLocale.startsWith('zh') ? '其他语言' : 'Other Languages' }}
      </DropdownMenuLabel>

      <!-- Other Locales -->
      <DropdownMenuItem
        v-for="code in otherLocales"
        :key="code"
        class="flex cursor-pointer items-center justify-between text-sm"
        :class="{ 'bg-accent text-accent-foreground': currentLocale === code }"
        @click="selectLanguage(code)"
      >
        <span>{{ t(`languages.${code}`) }} ({{ t(`languages.${code}`, 1, { locale: code }) }})</span>
        <MdiCheck v-if="currentLocale === code" class="size-4 text-primary" />
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
