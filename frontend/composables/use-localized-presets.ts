import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { toast } from "@/components/ui/sonner";
import { useUserApi } from "~/composables/use-api";
import { useLocationStore } from "~/stores/locations";
import { useTagStore } from "~/stores/tags";
import { useEntityTypeStore } from "~/stores/entityTypes";
import { useViewPreferences } from "~/composables/use-preferences";
import { useAuthContext } from "~/composables/use-auth-context";

export interface LocalizedLocationPreset {
  name: string;
  description: string;
}

export interface LocalizedTagPreset {
  name: string;
  color: string;
  icon: string;
  description: string;
}

/**
 * 适配中国家庭收纳场景的常用存放位置预设 (10项)
 * 区别于欧美住宅的Garage/Attic/Basement，更契合国内主流住宅户型布局
 */
export const CHINESE_LOCATION_PRESETS: LocalizedLocationPreset[] = [
  { name: "客厅", description: "沙发区、电视柜、茶几及客厅公共收纳空间" },
  { name: "主卧", description: "主人房衣柜、床头柜、主卧独立储物区" },
  { name: "次卧", description: "客卧/儿童房、衣柜收纳及床上备品" },
  { name: "书房", description: "书桌、书柜、办公电脑与资料设备" },
  { name: "厨房", description: "橱柜、餐具、调料区与常用厨电设备" },
  { name: "餐厅", description: "餐边柜、饮水机、餐桌周边与茶水酒水区" },
  { name: "卫生间", description: "洗漱台、镜柜、卫浴备品及清洁日化" },
  { name: "阳台", description: "洗衣机柜、晾晒区、家政工具与绿植养护" },
  { name: "玄关", description: "鞋柜、门厅挂衣区、随手钥匙及出行备品" },
  { name: "储物间", description: "大件行李箱、换季杂物与家庭备用物资" },
];

/**
 * 适配中国家庭常用分类标签预设 (10项)
 * 附带与系统内置图标库严格匹配的图标及高辨识度色彩方案
 */
export const CHINESE_TAG_PRESETS: LocalizedTagPreset[] = [
  { name: "家用电器", color: "#2563eb", icon: "power-plug-outline", description: "电视、冰箱、洗衣机、空气净化器等大小家电" },
  { name: "数码科技", color: "#0891b2", icon: "laptop", description: "手机、电脑、充电配件、相机与网络智能设备" },
  { name: "重要证件", color: "#d97706", icon: "file-cabinet-outline", description: "身份证、户口本、房产证、合同证书等关键凭证" },
  { name: "医疗健康", color: "#dc2626", icon: "dumbbell", description: "常备急救药品、体温计、医疗器械与家庭保健品" },
  { name: "工具五金", color: "#475569", icon: "toolbox-outline", description: "螺丝刀、电钻、卷尺、五金配件与维修工具" },
  { name: "居家日用", color: "#059669", icon: "sofa-outline", description: "生活纸品、日化洗护、清洁耗材等日用品" },
  { name: "季节收纳", color: "#7c3aed", icon: "dresser-outline", description: "厚被羽绒、换季衣物、电风扇/电暖器等季节物品" },
  { name: "贵重物品", color: "#ca8a04", icon: "tag-outline", description: "贵重首饰、纪念藏品、重要票据与保值资产" },
  { name: "食品饮品", color: "#ea580c", icon: "kitchen-counter-outline", description: "茶叶咖啡、干货调料、零食备用与酒水饮料" },
  { name: "书籍办公", color: "#4f46e5", icon: "book-open-variant-outline", description: "书籍读物、办公文具、打印耗材及学习资料" },
];

const isInitializing = ref(false);

export function useLocalizedPresets() {
  const api = useUserApi();
  const locationStore = useLocationStore();
  const tagStore = useTagStore();
  const entityTypeStore = useEntityTypeStore();
  const preferences = useViewPreferences();
  const auth = useAuthContext();
  const { t } = useI18n();

  /**
   * 检查当前系统是否已包含中文位置或标签
   */
  async function hasChinesePresets(): Promise<boolean> {
    try {
      await Promise.all([
        locationStore.ensureLocationsFetched(),
        tagStore.ensureAllTagsFetched(),
      ]);

      const chineseRegex = /[\u4e00-\u9fa5]/;
      const hasChineseLoc = (locationStore.allLocations || []).some(l => chineseRegex.test(l.name));
      const hasChineseTag = (tagStore.tags || []).some(tg => chineseRegex.test(tg.name));

      return hasChineseLoc || hasChineseTag;
    } catch {
      return false;
    }
  }

  /**
   * 统计当前已有位置和标签信息
   */
  function getPresetsStatistics() {
    const chineseRegex = /[\u4e00-\u9fa5]/;
    const allLocs = locationStore.allLocations || [];
    const allTags = tagStore.tags || [];

    const chineseLocCount = allLocs.filter(l => chineseRegex.test(l.name)).length;
    const chineseTagCount = allTags.filter(tg => chineseRegex.test(tg.name)).length;
    const englishLocCount = allLocs.length - chineseLocCount;
    const englishTagCount = allTags.length - chineseTagCount;

    return {
      totalLocations: allLocs.length,
      totalTags: allTags.length,
      chineseLocations: chineseLocCount,
      chineseTags: chineseTagCount,
      otherLocations: englishLocCount,
      otherTags: englishTagCount,
    };
  }

  /**
   * 生成中文存放位置与标签
   * 核心原则：安全无破坏性，严格保留原有英文及自定义存放位置和标签！
   */
  async function initializeChinesePresets(options?: {
    showToasts?: boolean;
  }) {
    if (isInitializing.value) return;
    isInitializing.value = true;

    const showToasts = options?.showToasts ?? true;

    try {
      // 1. 确保获取最新数据
      await Promise.all([
        entityTypeStore.ensureFetched(),
        locationStore.refreshChildren(),
        tagStore.refresh(),
      ]);

      // 2. 获取位置实体类型 ID
      const locationType =
        entityTypeStore.locationTypes[0] ||
        entityTypeStore.allTypes.find(t => t.isLocation);
      const entityTypeId = locationType?.id;

      if (!entityTypeId) {
        throw new Error("Unable to find default location entity type");
      }

      // 3. 安全获取现有名称，确保不重复创建，更不覆盖原有数据
      const existingLocNames = new Set(
        (locationStore.allLocations || []).map(l => l.name.trim().toLowerCase())
      );
      const existingTagNames = new Set(
        (tagStore.tags || []).map(tg => tg.name.trim().toLowerCase())
      );

      let locationsCreated = 0;
      let tagsCreated = 0;

      // 4. 创建中文存放位置（已存在同名则跳过）
      for (const loc of CHINESE_LOCATION_PRESETS) {
        if (!existingLocNames.has(loc.name.trim().toLowerCase())) {
          try {
            const { error } = await api.items.createLocation({
              name: loc.name,
              description: loc.description,
              entityTypeId,
              quantity: 1,
              tagIds: [],
            });
            if (!error) {
              locationsCreated++;
              existingLocNames.add(loc.name.trim().toLowerCase());
            }
          } catch (e) {
            console.warn(`Failed to create location preset: ${loc.name}`, e);
          }
        }
      }

      // 5. 创建中文分类标签（已存在同名则跳过）
      for (const tag of CHINESE_TAG_PRESETS) {
        if (!existingTagNames.has(tag.name.trim().toLowerCase())) {
          try {
            const { error } = await api.tags.create({
              name: tag.name,
              description: tag.description,
              color: tag.color,
              icon: tag.icon,
            });
            if (!error) {
              tagsCreated++;
              existingTagNames.add(tag.name.trim().toLowerCase());
            }
          } catch (e) {
            console.warn(`Failed to create tag preset: ${tag.name}`, e);
          }
        }
      }

      // 6. 刷新前端 Store，使用户立即可见
      await Promise.all([
        locationStore.refreshParents(),
        locationStore.refreshChildren(),
        tagStore.refresh(),
      ]);

      // 7. 标记配置完成
      markOnboardingCompleted();

      if (showToasts) {
        if (locationsCreated > 0 || tagsCreated > 0) {
          toast.success(
            t("onboarding.preset_success", {
              locations: locationsCreated,
              tags: tagsCreated,
            }) ||
              `成功生成 ${locationsCreated} 个常用存放位置与 ${tagsCreated} 个常用标签！原有数据保持原样安全保留。`
          );
        } else {
          toast.info(
            t("onboarding.preset_already_exists") ||
              "中文常用存放位置与标签已存在，原有数据已完好保留。"
          );
        }
      }

      return {
        locationsCreated,
        tagsCreated,
      };
    } catch (err) {
      console.error("Failed to initialize Chinese presets", err);
      if (showToasts) {
        toast.error("初始化中文预设时遇到问题，请重试。");
      }
      throw err;
    } finally {
      isInitializing.value = false;
    }
  }

  function markOnboardingCompleted() {
    preferences.value.languageOnboardingCompleted = true;
    try {
      const userId = auth.user?.id || "global";
      localStorage.setItem(`hb_onboarding_completed_${userId}`, "true");
      localStorage.setItem("hb_onboarding_completed", "true");
    } catch (e) {
      console.warn("Could not save to localStorage", e);
    }
  }

  function isOnboardingCompleted(): boolean {
    if (preferences.value.languageOnboardingCompleted) return true;
    try {
      const userId = auth.user?.id || "global";
      if (localStorage.getItem(`hb_onboarding_completed_${userId}`) === "true") {
        return true;
      }
      if (localStorage.getItem("hb_onboarding_completed") === "true") {
        return true;
      }
    } catch {
      // ignore
    }
    return false;
  }

  return {
    isInitializing,
    hasChinesePresets,
    getPresetsStatistics,
    initializeChinesePresets,
    markOnboardingCompleted,
    isOnboardingCompleted,
    CHINESE_LOCATION_PRESETS,
    CHINESE_TAG_PRESETS,
  };
}
