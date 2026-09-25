/* eslint-disable @typescript-eslint/no-explicit-any */
import type { CompileError, MessageContext } from "vue-i18n";
import { createI18n } from "vue-i18n";
import { IntlMessageFormat } from "intl-messageformat";

export default defineNuxtPlugin(({ vueApp }) => {
  function checkDefaultLanguage() {
    let matched = null;
    const languages = Object.getOwnPropertyNames(messages());
    
    // Check direct matching in navigator.languages
    if (navigator.languages && navigator.languages.length > 0) {
      for (const navLang of navigator.languages) {
        const lower = navLang.toLowerCase();
        // Exact match
        const exact = languages.find(l => l.toLowerCase() === lower);
        if (exact) {
          return exact;
        }
        // Chinese special handling
        if (lower.startsWith("zh")) {
          if (lower.includes("tw") || lower.includes("hant")) {
            return languages.find(l => l.toLowerCase() === "zh-tw") || "zh-TW";
          }
          if (lower.includes("hk")) {
            return languages.find(l => l.toLowerCase() === "zh-hk") || "zh-HK";
          }
          return languages.find(l => l.toLowerCase() === "zh-cn") || "zh-CN";
        }
        // Prefix match
        const prefix = lower.split("-")[0];
        const partial = languages.find(l => l.toLowerCase().split("-")[0] === prefix);
        if (partial) {
          return partial;
        }
      }
    }

    const currentNav = (navigator.language || "").toLowerCase();
    if (currentNav.startsWith("zh")) {
      if (currentNav.includes("tw") || currentNav.includes("hant")) {
        return languages.find(l => l.toLowerCase() === "zh-tw") || "zh-TW";
      }
      return languages.find(l => l.toLowerCase() === "zh-cn") || "zh-CN";
    }

    const currentPartial = currentNav.split("-")[0];
    matched = languages.find(l => l.toLowerCase().split("-")[0] === currentPartial);

    return matched;
  }
  const preferences = useViewPreferences();
  const i18n = createI18n({
    fallbackLocale: "en",
    globalInjection: true,
    legacy: false,
    locale: preferences.value.language || checkDefaultLanguage() || "en",
    messageCompiler,
    messages: messages(),
  });
  vueApp.use(i18n);

  watch(
    () => preferences.value.language,
    language => {
      if (!language) {
        return;
      }

      i18n.global.locale.value = language;
    }
  );

  return {
    provide: {
      i18nGlobal: i18n.global,
    },
  };
});

export const messages = () => {
  const messages: Record<string, any> = {};
  const modules = import.meta.glob("~//locales/**.json", { eager: true });
  for (const path in modules) {
    const key = path.slice(9, -5);
    messages[key] = modules[path];
  }
  return messages;
};

export const messageCompiler: (
  message: string | any,
  {
    locale,
    key,
    onError,
  }: {
    locale: any;
    key: any;
    onError: any;
  }
) => (ctx: MessageContext) => unknown = (message, { locale, key, onError }) => {
  if (typeof message === "string") {
    /**
     * You can tune your message compiler performance more with your cache strategy or also memoization at here
     */
    const formatter = new IntlMessageFormat(message, locale);
    return (ctx: MessageContext) => {
      return formatter.format(ctx.values);
    };
  } else {
    /**
     * for AST.
     * If you would like to support it,
     * You need to transform locale messages such as `json`, `yaml`, etc. with the bundle plugin.
     */
    if (onError) {
      onError(new Error("not support for AST") as CompileError);
    }
    return () => key;
  }
};
