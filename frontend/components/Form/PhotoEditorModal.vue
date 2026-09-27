<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-2 sm:p-4"
    role="dialog"
    aria-modal="true"
  >
    <div
      class="relative flex h-[95vh] w-full max-w-5xl flex-col rounded-xl border border-border bg-card text-card-foreground shadow-2xl overflow-hidden"
    >
      <!-- Header -->
      <header class="flex items-center justify-between border-b px-4 py-3 bg-muted/30">
        <div class="flex items-center gap-2">
          <MdiCrop class="size-5 text-primary" />
          <h2 class="text-base font-semibold">{{ $t("components.photo_editor.title") }}</h2>
          <!-- File info & savings badge -->
          <div class="hidden sm:flex items-center gap-2 ml-3 text-xs text-muted-foreground">
            <span class="rounded bg-muted px-2 py-0.5 font-mono">{{ currentDimensions.width }} × {{ currentDimensions.height }} px</span>
            <span class="rounded bg-primary/10 text-primary font-medium px-2 py-0.5 font-mono">{{ estimatedSizeText }}</span>
            <span v-if="spaceSavedText" class="rounded bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-medium px-2 py-0.5">
              {{ spaceSavedText }}
            </span>
          </div>
        </div>

        <div class="flex items-center gap-1 sm:gap-2">
          <!-- Undo / Redo / Reset buttons -->
          <TooltipProvider :delay-duration="0">
            <Tooltip>
              <TooltipTrigger as-child>
                <Button
                  size="icon"
                  variant="ghost"
                  class="size-8"
                  :disabled="historyIndex <= 0"
                  @click="undo"
                >
                  <MdiUndo class="size-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{{ $t("components.photo_editor.undo") }}</TooltipContent>
            </Tooltip>

            <Tooltip>
              <TooltipTrigger as-child>
                <Button
                  size="icon"
                  variant="ghost"
                  class="size-8"
                  :disabled="historyIndex >= history.length - 1"
                  @click="redo"
                >
                  <MdiRedo class="size-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{{ $t("components.photo_editor.redo") }}</TooltipContent>
            </Tooltip>

            <Tooltip>
              <TooltipTrigger as-child>
                <Button
                  size="icon"
                  variant="ghost"
                  class="size-8 text-destructive"
                  :disabled="history.length <= 1"
                  @click="resetToOriginal"
                >
                  <MdiRestore class="size-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{{ $t("components.photo_editor.reset_to_original") }}</TooltipContent>
            </Tooltip>
          </TooltipProvider>

          <Button size="icon" variant="ghost" class="size-8 ml-1" @click="cancel">
            <MdiClose class="size-5" />
          </Button>
        </div>
      </header>

      <!-- Center Image Preview & Crop Canvas Area -->
      <main
        ref="previewAreaRef"
        class="relative flex flex-1 items-center justify-center overflow-hidden bg-neutral-900/90 p-4 select-none touch-none"
      >
        <div class="relative flex items-center justify-center max-h-full max-w-full">
          <!-- Base canvas showing current edited image -->
          <canvas
            ref="baseCanvasRef"
            class="max-h-[60vh] max-w-full object-contain rounded shadow-lg border border-neutral-700/50"
          ></canvas>

          <!-- Interactive Crop Overlay when in Crop Tab -->
          <div
            v-if="activeTab === 'crop'"
            ref="cropOverlayRef"
            class="absolute inset-0 cursor-crosshair"
            @mousedown="onCropMouseDown"
            @touchstart.passive="onCropTouchStart"
          >
            <!-- Dimmed background outside crop box -->
            <svg class="absolute inset-0 size-full pointer-events-none">
              <defs>
                <mask id="crop-mask">
                  <rect width="100%" height="100%" fill="white" />
                  <rect
                    :x="cropRect.x"
                    :y="cropRect.y"
                    :width="cropRect.width"
                    :height="cropRect.height"
                    fill="black"
                  />
                </mask>
              </defs>
              <rect width="100%" height="100%" fill="rgba(0,0,0,0.65)" mask="url(#crop-mask)" />
            </svg>

            <!-- Active Crop Box with 3x3 Grid & Border -->
            <div
              class="absolute border-2 border-primary shadow-xl pointer-events-auto cursor-move"
              :style="{
                left: `${cropRect.x}px`,
                top: `${cropRect.y}px`,
                width: `${cropRect.width}px`,
                height: `${cropRect.height}px`,
              }"
              @mousedown.stop="onDragBoxStart"
              @touchstart.stop.passive="onDragBoxTouchStart"
            >
              <!-- 3x3 Rule of thirds grid -->
              <div class="absolute inset-0 grid grid-cols-3 grid-rows-3 pointer-events-none">
                <div class="border-b border-r border-white/20"></div>
                <div class="border-b border-r border-white/20"></div>
                <div class="border-b border-white/20"></div>
                <div class="border-b border-r border-white/20"></div>
                <div class="border-b border-r border-white/20"></div>
                <div class="border-b border-white/20"></div>
                <div class="border-r border-white/20"></div>
                <div class="border-r border-white/20"></div>
                <div></div>
              </div>

              <!-- Corner handles -->
              <div
                class="absolute -left-2 -top-2 size-4 rounded-full bg-primary border-2 border-white shadow cursor-nwse-resize"
                @mousedown.stop="onHandleStart('tl', $event)"
                @touchstart.stop.passive="onHandleTouchStart('tl', $event)"
              ></div>
              <div
                class="absolute -right-2 -top-2 size-4 rounded-full bg-primary border-2 border-white shadow cursor-nesw-resize"
                @mousedown.stop="onHandleStart('tr', $event)"
                @touchstart.stop.passive="onHandleTouchStart('tr', $event)"
              ></div>
              <div
                class="absolute -left-2 -bottom-2 size-4 rounded-full bg-primary border-2 border-white shadow cursor-nesw-resize"
                @mousedown.stop="onHandleStart('bl', $event)"
                @touchstart.stop.passive="onHandleTouchStart('bl', $event)"
              ></div>
              <div
                class="absolute -right-2 -bottom-2 size-4 rounded-full bg-primary border-2 border-white shadow cursor-nwse-resize"
                @mousedown.stop="onHandleStart('br', $event)"
                @touchstart.stop.passive="onHandleTouchStart('br', $event)"
              ></div>
            </div>
          </div>
        </div>

        <!-- History step toast indicator in center bottom of preview -->
        <div
          v-if="history.length > 1"
          class="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full bg-black/75 px-3 py-1 text-xs text-white backdrop-blur shadow"
        >
          {{ $t("components.photo_editor.step") }} {{ historyIndex + 1 }} / {{ history.length }}: {{ history[historyIndex]?.description }}
        </div>
      </main>

      <!-- Tool Tabs Navigation -->
      <nav class="flex border-t bg-muted/20 px-2 py-1.5 overflow-x-auto gap-1">
        <Button
          size="sm"
          :variant="activeTab === 'crop' ? 'default' : 'ghost'"
          class="flex items-center gap-1.5 text-xs h-8 shrink-0"
          @click="setTab('crop')"
        >
          <MdiCrop class="size-3.5" />
          <span>{{ $t("components.photo_editor.tab_crop") }}</span>
        </Button>
        <Button
          size="sm"
          :variant="activeTab === 'rotate' ? 'default' : 'ghost'"
          class="flex items-center gap-1.5 text-xs h-8 shrink-0"
          @click="setTab('rotate')"
        >
          <MdiRotateRight class="size-3.5" />
          <span>{{ $t("components.photo_editor.tab_rotate") }}</span>
        </Button>
        <Button
          size="sm"
          :variant="activeTab === 'filter' ? 'default' : 'ghost'"
          class="flex items-center gap-1.5 text-xs h-8 shrink-0"
          @click="setTab('filter')"
        >
          <MdiPaletteOutline class="size-3.5" />
          <span>{{ $t("components.photo_editor.tab_filter") }}</span>
        </Button>
        <Button
          size="sm"
          :variant="activeTab === 'resolution' ? 'default' : 'ghost'"
          class="flex items-center gap-1.5 text-xs h-8 shrink-0"
          @click="setTab('resolution')"
        >
          <MdiResize class="size-3.5" />
          <span>{{ $t("components.photo_editor.tab_resolution") }}</span>
        </Button>
        <Button
          size="sm"
          :variant="activeTab === 'adjust' ? 'default' : 'ghost'"
          class="flex items-center gap-1.5 text-xs h-8 shrink-0"
          @click="setTab('adjust')"
        >
          <MdiBrightness6 class="size-3.5" />
          <span>{{ $t("components.photo_editor.tab_adjust") }}</span>
        </Button>
      </nav>

      <!-- Tool Controls Panel -->
      <div class="border-t bg-card px-4 py-3 min-h-[90px] flex items-center justify-between">
        <!-- Sub-panel: Crop -->
        <div v-if="activeTab === 'crop'" class="flex flex-wrap items-center gap-2 w-full justify-between">
          <div class="flex items-center gap-1.5">
            <span class="text-xs font-medium text-muted-foreground mr-1">{{ $t("components.photo_editor.aspect_ratio") }}:</span>
            <Button
              size="sm"
              :variant="cropRatio === 'free' ? 'secondary' : 'outline'"
              class="h-7 text-xs px-2"
              @click="setCropRatio('free')"
            >
              {{ $t("components.photo_editor.ratio_free") }}
            </Button>
            <Button
              size="sm"
              :variant="cropRatio === '1:1' ? 'secondary' : 'outline'"
              class="h-7 text-xs px-2"
              @click="setCropRatio('1:1')"
            >
              1:1
            </Button>
            <Button
              size="sm"
              :variant="cropRatio === '4:3' ? 'secondary' : 'outline'"
              class="h-7 text-xs px-2"
              @click="setCropRatio('4:3')"
            >
              4:3
            </Button>
            <Button
              size="sm"
              :variant="cropRatio === '16:9' ? 'secondary' : 'outline'"
              class="h-7 text-xs px-2"
              @click="setCropRatio('16:9')"
            >
              16:9
            </Button>
          </div>
          <div class="flex items-center gap-2">
            <Button size="sm" variant="default" class="h-8 gap-1" @click="applyCrop">
              <MdiCheck class="size-4" />
              <span>{{ $t("components.photo_editor.apply_crop") }}</span>
            </Button>
          </div>
        </div>

        <!-- Sub-panel: Rotate & Flip -->
        <div v-else-if="activeTab === 'rotate'" class="flex flex-wrap items-center gap-2 w-full justify-center sm:justify-start">
          <Button size="sm" variant="outline" class="h-8 gap-1.5" @click="rotateClockwise(90)">
            <MdiRotateRight class="size-4" />
            <span>{{ $t("components.photo_editor.rotate_90_cw") }}</span>
          </Button>
          <Button size="sm" variant="outline" class="h-8 gap-1.5" @click="rotateClockwise(-90)">
            <MdiRotateLeft class="size-4" />
            <span>{{ $t("components.photo_editor.rotate_90_ccw") }}</span>
          </Button>
          <Button size="sm" variant="outline" class="h-8 gap-1.5" @click="flipHorizontal">
            <MdiFlipHorizontal class="size-4" />
            <span>{{ $t("components.photo_editor.flip_horizontal") }}</span>
          </Button>
          <Button size="sm" variant="outline" class="h-8 gap-1.5" @click="flipVertical">
            <MdiFlipVertical class="size-4" />
            <span>{{ $t("components.photo_editor.flip_vertical") }}</span>
          </Button>
        </div>

        <!-- Sub-panel: Filter (Color / B&W / Document Scan) -->
        <div v-else-if="activeTab === 'filter'" class="flex flex-wrap items-center gap-2 w-full justify-center sm:justify-start">
          <Button
            size="sm"
            :variant="currentFilter === 'original' ? 'default' : 'outline'"
            class="h-8 gap-1.5"
            @click="applyColorMode('original')"
          >
            <MdiPaletteOutline class="size-4" />
            <span>{{ $t("components.photo_editor.filter_color") }}</span>
          </Button>
          <Button
            size="sm"
            :variant="currentFilter === 'grayscale' ? 'default' : 'outline'"
            class="h-8 gap-1.5"
            @click="applyColorMode('grayscale')"
          >
            <MdiFormatColorFill class="size-4" />
            <span>{{ $t("components.photo_editor.filter_bw") }}</span>
          </Button>
          <Button
            size="sm"
            :variant="currentFilter === 'document' ? 'default' : 'outline'"
            class="h-8 gap-1.5"
            @click="applyColorMode('document')"
          >
            <MdiFileDocumentOutline class="size-4" />
            <span>{{ $t("components.photo_editor.filter_document") }}</span>
          </Button>
        </div>

        <!-- Sub-panel: Resolution Presets -->
        <div v-else-if="activeTab === 'resolution'" class="flex flex-wrap items-center gap-2 w-full justify-between">
          <div class="flex flex-wrap items-center gap-1.5">
            <span class="text-xs font-medium text-muted-foreground mr-1">{{ $t("components.photo_editor.preset_resolution") }}:</span>
            <Button
              size="sm"
              variant="outline"
              class="h-7 text-xs px-2"
              @click="applyResolution(720, $t('components.photo_editor.res_720'))"
            >
              720p ({{ $t("components.photo_editor.res_compact") }})
            </Button>
            <Button
              size="sm"
              variant="outline"
              class="h-7 text-xs px-2 border-primary/50 text-primary"
              @click="applyResolution(1080, $t('components.photo_editor.res_1080'))"
            >
              1080p ({{ $t("components.photo_editor.res_standard") }})
            </Button>
            <Button
              size="sm"
              variant="outline"
              class="h-7 text-xs px-2"
              @click="applyResolution(1600, $t('components.photo_editor.res_1600'))"
            >
              1600p ({{ $t("components.photo_editor.res_hd") }})
            </Button>
            <Button
              size="sm"
              variant="outline"
              class="h-7 text-xs px-2"
              @click="applyResolution(2048, $t('components.photo_editor.res_2048'))"
            >
              2K ({{ $t("components.photo_editor.res_full") }})
            </Button>
          </div>
          <span class="text-xs text-muted-foreground">
            {{ $t("components.photo_editor.current_res") }}: {{ currentDimensions.width }} × {{ currentDimensions.height }} px
          </span>
        </div>

        <!-- Sub-panel: Brightness & Contrast Adjustments -->
        <div v-else-if="activeTab === 'adjust'" class="flex flex-col sm:flex-row items-center gap-4 w-full justify-between">
          <div class="flex flex-1 items-center gap-4 w-full sm:w-auto">
            <div class="flex items-center gap-2 flex-1">
              <span class="text-xs text-muted-foreground shrink-0 w-12">{{ $t("components.photo_editor.brightness") }}:</span>
              <input
                v-model.number="adjustBrightness"
                type="range"
                min="-50"
                max="50"
                step="2"
                class="w-full accent-primary"
              />
              <span class="text-xs font-mono w-8 text-right">{{ adjustBrightness }}</span>
            </div>
            <div class="flex items-center gap-2 flex-1">
              <span class="text-xs text-muted-foreground shrink-0 w-12">{{ $t("components.photo_editor.contrast") }}:</span>
              <input
                v-model.number="adjustContrast"
                type="range"
                min="-50"
                max="50"
                step="2"
                class="w-full accent-primary"
              />
              <span class="text-xs font-mono w-8 text-right">{{ adjustContrast }}</span>
            </div>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <Button size="sm" variant="secondary" class="h-8 gap-1" @click="autoEnhance">
              <MdiAutoFix class="size-3.5" />
              <span>{{ $t("components.photo_editor.auto_enhance") }}</span>
            </Button>
            <Button size="sm" variant="default" class="h-8" @click="commitAdjustments">
              <span>{{ $t("components.photo_editor.apply") }}</span>
            </Button>
          </div>
        </div>
      </div>

      <!-- Footer Action Buttons -->
      <footer class="flex items-center justify-between border-t px-4 py-3 bg-muted/40">
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <span class="sm:hidden font-mono">{{ currentDimensions.width }}×{{ currentDimensions.height }}</span>
          <span class="sm:hidden font-mono text-primary">{{ estimatedSizeText }}</span>
        </div>

        <div class="flex items-center gap-2 ml-auto">
          <Button variant="outline" size="sm" @click="cancel">
            {{ $t("global.cancel") }}
          </Button>
          <Button variant="default" size="sm" class="gap-1.5" @click="saveAndDone">
            <MdiCheck class="size-4" />
            <span>{{ $t("global.save") }}</span>
          </Button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from "vue";
  import { useI18n } from "vue-i18n";
  import { Button } from "~/components/ui/button";
  import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "~/components/ui/tooltip";
  import { toast } from "@/components/ui/sonner";
  import MdiCrop from "~icons/mdi/crop";
  import MdiRotateRight from "~icons/mdi/rotate-right";
  import MdiRotateLeft from "~icons/mdi/rotate-left";
  import MdiFlipHorizontal from "~icons/mdi/flip-horizontal";
  import MdiFlipVertical from "~icons/mdi/flip-vertical";
  import MdiPaletteOutline from "~icons/mdi/palette-outline";
  import MdiResize from "~icons/mdi/resize";
  import MdiBrightness6 from "~icons/mdi/brightness-6";
  import MdiUndo from "~icons/mdi/undo";
  import MdiRedo from "~icons/mdi/redo";
  import MdiRestore from "~icons/mdi/restore";
  import MdiClose from "~icons/mdi/close";
  import MdiCheck from "~icons/mdi/check";
  import MdiFormatColorFill from "~icons/mdi/format-color-fill";
  import MdiFileDocumentOutline from "~icons/mdi/file-document-outline";
  import MdiAutoFix from "~icons/mdi/auto-fix";
  import type { PhotoPreview } from "./photo-uploader";

  const props = defineProps<{
    modelValue: boolean;
    photo: PhotoPreview | null;
  }>();

  const emit = defineEmits<{
    (e: "update:modelValue", value: boolean): void;
    (e: "saved", updated: PhotoPreview): void;
  }>();

  const { t } = useI18n();

  const isOpen = computed({
    get: () => props.modelValue,
    set: val => emit("update:modelValue", val),
  });

  const activeTab = ref<"crop" | "rotate" | "filter" | "resolution" | "adjust">("crop");
  const baseCanvasRef = ref<HTMLCanvasElement | null>(null);
  const previewAreaRef = ref<HTMLElement | null>(null);
  const cropOverlayRef = ref<HTMLElement | null>(null);

  interface HistorySnapshot {
    dataUrl: string;
    width: number;
    height: number;
    description: string;
    filter: string;
  }

  const history = ref<HistorySnapshot[]>([]);
  const historyIndex = ref(0);
  const currentDimensions = reactive({ width: 0, height: 0 });
  const estimatedSizeBytes = ref(0);
  const originalSizeBytes = ref(0);
  const currentFilter = ref("original");

  // Crop states
  const cropRatio = ref<"free" | "1:1" | "4:3" | "16:9">("free");
  const cropRect = reactive({ x: 0, y: 0, width: 0, height: 0 });
  let isDraggingCrop = false;
  let activeHandle: string | null = null;
  let dragStartPos = { x: 0, y: 0 };
  let cropStartRect = { x: 0, y: 0, width: 0, height: 0 };

  // Adjustments states
  const adjustBrightness = ref(0);
  const adjustContrast = ref(0);

  const estimatedSizeText = computed(() => {
    if (estimatedSizeBytes.value <= 0) return "~250 KB";
    if (estimatedSizeBytes.value > 1024 * 1024) {
      return `${(estimatedSizeBytes.value / (1024 * 1024)).toFixed(1)} MB`;
    }
    return `${Math.round(estimatedSizeBytes.value / 1024)} KB`;
  });

  const spaceSavedText = computed(() => {
    if (originalSizeBytes.value <= 0 || estimatedSizeBytes.value <= 0) return "";
    if (estimatedSizeBytes.value < originalSizeBytes.value) {
      const pct = Math.round((1 - estimatedSizeBytes.value / originalSizeBytes.value) * 100);
      if (pct > 5) return t("components.photo_editor.space_saved", { pct });
    }
    return "";
  });

  watch(
    () => props.modelValue,
    async open => {
      if (open && props.photo) {
        await initEditor(props.photo);
      }
    }
  );

  async function initEditor(photo: PhotoPreview) {
    history.value = [];
    historyIndex.value = 0;
    activeTab.value = "crop";
    currentFilter.value = "original";
    adjustBrightness.value = 0;
    adjustContrast.value = 0;
    originalSizeBytes.value = photo.file.size;

    const img = new Image();
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve();
      img.onerror = () => reject(new Error("Failed to load initial image"));
      img.src = photo.fileBase64;
    });

    const canvas = document.createElement("canvas");
    canvas.width = img.width;
    canvas.height = img.height;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.drawImage(img, 0, 0);

    const initialDataUrl = canvas.toDataURL("image/jpeg", 0.85);

    history.value.push({
      dataUrl: initialDataUrl,
      width: img.width,
      height: img.height,
      description: t("components.photo_editor.action_original"),
      filter: "original",
    });

    historyIndex.value = 0;
    await renderState(history.value[0]!);
    initCropBox();
  }

  async function renderState(snapshot: HistorySnapshot) {
    if (!baseCanvasRef.value) return;
    const canvas = baseCanvasRef.value;
    canvas.width = snapshot.width;
    canvas.height = snapshot.height;
    currentDimensions.width = snapshot.width;
    currentDimensions.height = snapshot.height;
    currentFilter.value = snapshot.filter;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const img = new Image();
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve();
      img.onerror = () => reject(new Error("Failed to load snapshot"));
      img.src = snapshot.dataUrl;
    });

    ctx.drawImage(img, 0, 0);

    // Calculate estimated size
    canvas.toBlob(
      b => {
        if (b) estimatedSizeBytes.value = b.size;
      },
      "image/jpeg",
      0.85
    );

    await nextTick();
    if (activeTab.value === "crop") {
      initCropBox();
    }
  }

  function pushState(dataUrl: string, width: number, height: number, description: string, filter = currentFilter.value) {
    // Truncate redo stack if in middle
    if (historyIndex.value < history.value.length - 1) {
      history.value = history.value.slice(0, historyIndex.value + 1);
    }
    history.value.push({ dataUrl, width, height, description, filter });
    // Limit to 50 steps
    if (history.value.length > 50) {
      history.value.shift();
    }
    historyIndex.value = history.value.length - 1;
    renderState(history.value[historyIndex.value]!);
  }

  async function undo() {
    if (historyIndex.value > 0) {
      historyIndex.value--;
      await renderState(history.value[historyIndex.value]!);
    }
  }

  async function redo() {
    if (historyIndex.value < history.value.length - 1) {
      historyIndex.value++;
      await renderState(history.value[historyIndex.value]!);
    }
  }

  async function resetToOriginal() {
    if (history.value.length > 0) {
      const original = history.value[0]!;
      pushState(original.dataUrl, original.width, original.height, t("components.photo_editor.action_reset"), "original");
    }
  }

  function setTab(tab: "crop" | "rotate" | "filter" | "resolution" | "adjust") {
    activeTab.value = tab;
    if (tab === "crop") {
      nextTick(() => initCropBox());
    }
  }

  // --- CROP LOGIC ---
  function initCropBox() {
    if (!baseCanvasRef.value) return;
    const canvas = baseCanvasRef.value;
    const rect = canvas.getBoundingClientRect();
    const w = rect.width;
    const h = rect.height;

    // Default to inset 10%
    const insetX = w * 0.05;
    const insetY = h * 0.05;
    cropRect.x = insetX;
    cropRect.y = insetY;
    cropRect.width = w - insetX * 2;
    cropRect.height = h - insetY * 2;
    applyAspectRatio();
  }

  function setCropRatio(ratio: "free" | "1:1" | "4:3" | "16:9") {
    cropRatio.value = ratio;
    applyAspectRatio();
  }

  function applyAspectRatio() {
    if (cropRatio.value === "free" || !baseCanvasRef.value) return;
    let targetRatio = 1;
    if (cropRatio.value === "1:1") targetRatio = 1;
    else if (cropRatio.value === "4:3") targetRatio = 4 / 3;
    else if (cropRatio.value === "16:9") targetRatio = 16 / 9;

    const currentW = cropRect.width;
    const newH = currentW / targetRatio;
    const canvas = baseCanvasRef.value;
    const maxH = canvas.getBoundingClientRect().height;

    if (cropRect.y + newH > maxH) {
      cropRect.height = maxH - cropRect.y;
      cropRect.width = cropRect.height * targetRatio;
    } else {
      cropRect.height = newH;
    }
  }

  function onCropMouseDown(e: MouseEvent) {
    if (e.target !== cropOverlayRef.value) return;
    isDraggingCrop = true;
    dragStartPos = { x: e.clientX, y: e.clientY };
    const overlay = cropOverlayRef.value!.getBoundingClientRect();
    cropRect.x = e.clientX - overlay.left;
    cropRect.y = e.clientY - overlay.top;
    cropRect.width = 10;
    cropRect.height = 10;
    window.addEventListener("mousemove", onCropMouseMove);
    window.addEventListener("mouseup", onCropMouseUp);
  }

  function onCropMouseMove(e: MouseEvent) {
    if (!isDraggingCrop || !cropOverlayRef.value) return;
    const overlay = cropOverlayRef.value.getBoundingClientRect();
    const currentX = Math.min(Math.max(e.clientX - overlay.left, 0), overlay.width);
    const currentY = Math.min(Math.max(e.clientY - overlay.top, 0), overlay.height);

    const x = Math.min(dragStartPos.x - overlay.left, currentX);
    const y = Math.min(dragStartPos.y - overlay.top, currentY);
    let w = Math.abs(currentX - (dragStartPos.x - overlay.left));
    let h = Math.abs(currentY - (dragStartPos.y - overlay.top));

    if (cropRatio.value === "1:1") {
      w = Math.min(w, h);
      h = w;
    } else if (cropRatio.value === "4:3") {
      h = w / (4 / 3);
    } else if (cropRatio.value === "16:9") {
      h = w / (16 / 9);
    }

    cropRect.x = x;
    cropRect.y = y;
    cropRect.width = Math.max(w, 20);
    cropRect.height = Math.max(h, 20);
  }

  function onCropMouseUp() {
    isDraggingCrop = false;
    window.removeEventListener("mousemove", onCropMouseMove);
    window.removeEventListener("mouseup", onCropMouseUp);
  }

  function onDragBoxStart(e: MouseEvent) {
    isDraggingCrop = true;
    dragStartPos = { x: e.clientX, y: e.clientY };
    cropStartRect = { ...cropRect };
    window.addEventListener("mousemove", onDragBoxMove);
    window.addEventListener("mouseup", onDragBoxEnd);
  }

  function onDragBoxMove(e: MouseEvent) {
    if (!isDraggingCrop || !cropOverlayRef.value) return;
    const dx = e.clientX - dragStartPos.x;
    const dy = e.clientY - dragStartPos.y;
    const overlay = cropOverlayRef.value.getBoundingClientRect();

    cropRect.x = Math.max(0, Math.min(cropStartRect.x + dx, overlay.width - cropRect.width));
    cropRect.y = Math.max(0, Math.min(cropStartRect.y + dy, overlay.height - cropRect.height));
  }

  function onDragBoxEnd() {
    isDraggingCrop = false;
    window.removeEventListener("mousemove", onDragBoxMove);
    window.removeEventListener("mouseup", onDragBoxEnd);
  }

  function onHandleStart(handle: string, e: MouseEvent) {
    activeHandle = handle;
    dragStartPos = { x: e.clientX, y: e.clientY };
    cropStartRect = { ...cropRect };
    window.addEventListener("mousemove", onHandleMove);
    window.addEventListener("mouseup", onHandleEnd);
  }

  function onHandleMove(e: MouseEvent) {
    if (!activeHandle || !cropOverlayRef.value) return;
    const dx = e.clientX - dragStartPos.x;
    const dy = e.clientY - dragStartPos.y;
    const overlay = cropOverlayRef.value.getBoundingClientRect();

    if (activeHandle === "br") {
      let newW = Math.max(20, Math.min(cropStartRect.width + dx, overlay.width - cropStartRect.x));
      let newH = Math.max(20, Math.min(cropStartRect.height + dy, overlay.height - cropStartRect.y));
      if (cropRatio.value === "1:1") {
        newW = Math.min(newW, newH);
        newH = newW;
      }
      cropRect.width = newW;
      cropRect.height = newH;
    } else if (activeHandle === "tl") {
      const newX = Math.min(cropStartRect.x + cropStartRect.width - 20, Math.max(0, cropStartRect.x + dx));
      const newY = Math.min(cropStartRect.y + cropStartRect.height - 20, Math.max(0, cropStartRect.y + dy));
      cropRect.width = cropStartRect.width + (cropStartRect.x - newX);
      cropRect.height = cropStartRect.height + (cropStartRect.y - newY);
      cropRect.x = newX;
      cropRect.y = newY;
    } else if (activeHandle === "tr") {
      const newY = Math.min(cropStartRect.y + cropStartRect.height - 20, Math.max(0, cropStartRect.y + dy));
      cropRect.width = Math.max(20, Math.min(cropStartRect.width + dx, overlay.width - cropStartRect.x));
      cropRect.height = cropStartRect.height + (cropStartRect.y - newY);
      cropRect.y = newY;
    } else if (activeHandle === "bl") {
      const newX = Math.min(cropStartRect.x + cropStartRect.width - 20, Math.max(0, cropStartRect.x + dx));
      cropRect.width = cropStartRect.width + (cropStartRect.x - newX);
      cropRect.height = Math.max(20, Math.min(cropStartRect.height + dy, overlay.height - cropStartRect.y));
      cropRect.x = newX;
    }
  }

  function onHandleEnd() {
    activeHandle = null;
    window.removeEventListener("mousemove", onHandleMove);
    window.removeEventListener("mouseup", onHandleEnd);
  }

  // Touch event handlers for mobile
  function onCropTouchStart(e: TouchEvent) {
    if (e.touches.length === 1) {
      const t = e.touches[0]!;
      onCropMouseDown({ clientX: t.clientX, clientY: t.clientY, target: e.target } as any);
    }
  }

  function onDragBoxTouchStart(e: TouchEvent) {
    if (e.touches.length === 1) {
      const t = e.touches[0]!;
      isDraggingCrop = true;
      dragStartPos = { x: t.clientX, y: t.clientY };
      cropStartRect = { ...cropRect };
      const onTouchMove = (ev: TouchEvent) => {
        if (ev.touches.length === 1) {
          onDragBoxMove({ clientX: ev.touches[0]!.clientX, clientY: ev.touches[0]!.clientY } as any);
        }
      };
      const onTouchEnd = () => {
        window.removeEventListener("touchmove", onTouchMove);
        window.removeEventListener("touchend", onTouchEnd);
        isDraggingCrop = false;
      };
      window.addEventListener("touchmove", onTouchMove);
      window.addEventListener("touchend", onTouchEnd);
    }
  }

  function onHandleTouchStart(handle: string, e: TouchEvent) {
    if (e.touches.length === 1) {
      const t = e.touches[0]!;
      activeHandle = handle;
      dragStartPos = { x: t.clientX, y: t.clientY };
      cropStartRect = { ...cropRect };
      const onTouchMove = (ev: TouchEvent) => {
        if (ev.touches.length === 1) {
          onHandleMove({ clientX: ev.touches[0]!.clientX, clientY: ev.touches[0]!.clientY } as any);
        }
      };
      const onTouchEnd = () => {
        window.removeEventListener("touchmove", onTouchMove);
        window.removeEventListener("touchend", onTouchEnd);
        activeHandle = null;
      };
      window.addEventListener("touchmove", onTouchMove);
      window.addEventListener("touchend", onTouchEnd);
    }
  }

  function applyCrop() {
    if (!baseCanvasRef.value) return;
    const baseCanvas = baseCanvasRef.value;
    const rect = baseCanvas.getBoundingClientRect();
    const scaleX = baseCanvas.width / rect.width;
    const scaleY = baseCanvas.height / rect.height;

    const sourceX = Math.max(0, Math.round(cropRect.x * scaleX));
    const sourceY = Math.max(0, Math.round(cropRect.y * scaleY));
    const sourceW = Math.min(baseCanvas.width - sourceX, Math.round(cropRect.width * scaleX));
    const sourceH = Math.min(baseCanvas.height - sourceY, Math.round(cropRect.height * scaleY));

    if (sourceW < 10 || sourceH < 10) return;

    const croppedCanvas = document.createElement("canvas");
    croppedCanvas.width = sourceW;
    croppedCanvas.height = sourceH;
    const ctx = croppedCanvas.getContext("2d");
    if (!ctx) return;

    ctx.drawImage(baseCanvas, sourceX, sourceY, sourceW, sourceH, 0, 0, sourceW, sourceH);
    const dataUrl = croppedCanvas.toDataURL("image/jpeg", 0.85);

    pushState(dataUrl, sourceW, sourceH, t("components.photo_editor.action_crop"));
    toast.success(t("components.photo_editor.crop_applied"));
  }

  // --- ROTATION & FLIP ---
  function rotateClockwise(deg: number) {
    if (!baseCanvasRef.value) return;
    const src = baseCanvasRef.value;
    const dst = document.createElement("canvas");

    if (Math.abs(deg) === 90 || Math.abs(deg) === 270) {
      dst.width = src.height;
      dst.height = src.width;
    } else {
      dst.width = src.width;
      dst.height = src.height;
    }

    const ctx = dst.getContext("2d");
    if (!ctx) return;

    ctx.translate(dst.width / 2, dst.height / 2);
    ctx.rotate((deg * Math.PI) / 180);
    ctx.drawImage(src, -src.width / 2, -src.height / 2);

    const desc = deg > 0 ? t("components.photo_editor.action_rotate_right") : t("components.photo_editor.action_rotate_left");
    pushState(dst.toDataURL("image/jpeg", 0.85), dst.width, dst.height, desc);
  }

  function flipHorizontal() {
    if (!baseCanvasRef.value) return;
    const src = baseCanvasRef.value;
    const dst = document.createElement("canvas");
    dst.width = src.width;
    dst.height = src.height;
    const ctx = dst.getContext("2d");
    if (!ctx) return;

    ctx.translate(dst.width, 0);
    ctx.scale(-1, 1);
    ctx.drawImage(src, 0, 0);

    pushState(dst.toDataURL("image/jpeg", 0.85), dst.width, dst.height, t("components.photo_editor.action_flip_h"));
  }

  function flipVertical() {
    if (!baseCanvasRef.value) return;
    const src = baseCanvasRef.value;
    const dst = document.createElement("canvas");
    dst.width = src.width;
    dst.height = src.height;
    const ctx = dst.getContext("2d");
    if (!ctx) return;

    ctx.translate(0, dst.height);
    ctx.scale(1, -1);
    ctx.drawImage(src, 0, 0);

    pushState(dst.toDataURL("image/jpeg", 0.85), dst.width, dst.height, t("components.photo_editor.action_flip_v"));
  }

  // --- FILTERS (B&W / Document Scan) ---
  function applyColorMode(mode: "original" | "grayscale" | "document") {
    if (!baseCanvasRef.value) return;
    const src = baseCanvasRef.value;
    const dst = document.createElement("canvas");
    dst.width = src.width;
    dst.height = src.height;
    const ctx = dst.getContext("2d");
    if (!ctx) return;

    ctx.drawImage(src, 0, 0);
    const imgData = ctx.getImageData(0, 0, dst.width, dst.height);
    const d = imgData.data;

    if (mode === "grayscale") {
      for (let i = 0; i < d.length; i += 4) {
        const gray = 0.299 * d[i]! + 0.587 * d[i + 1]! + 0.114 * d[i + 2]!;
        d[i] = gray;
        d[i + 1] = gray;
        d[i + 2] = gray;
      }
      ctx.putImageData(imgData, 0, 0);
      pushState(dst.toDataURL("image/jpeg", 0.85), dst.width, dst.height, t("components.photo_editor.action_bw"), "grayscale");
    } else if (mode === "document") {
      // Document scan mode: high-contrast black & white enhancement
      for (let i = 0; i < d.length; i += 4) {
        let gray = 0.299 * d[i]! + 0.587 * d[i + 1]! + 0.114 * d[i + 2]!;
        // Contrast curve + threshold
        if (gray > 165) {
          gray = 255; // paper background to pure white
        } else if (gray < 85) {
          gray = Math.max(0, gray * 0.5); // dark ink / serial text deep black
        } else {
          gray = (gray - 85) * (255 / 80);
        }
        d[i] = gray;
        d[i + 1] = gray;
        d[i + 2] = gray;
      }
      ctx.putImageData(imgData, 0, 0);
      pushState(dst.toDataURL("image/jpeg", 0.85), dst.width, dst.height, t("components.photo_editor.action_document"), "document");
    }
  }

  // --- RESOLUTION DOWNSCALING ---
  function applyResolution(maxEdge: number, label: string) {
    if (!baseCanvasRef.value) return;
    const src = baseCanvasRef.value;
    let w = src.width;
    let h = src.height;

    if (w <= maxEdge && h <= maxEdge) {
      toast.info(t("components.photo_editor.already_below_resolution"));
      return;
    }

    if (w > h) {
      h = Math.round((h * maxEdge) / w);
      w = maxEdge;
    } else {
      w = Math.round((w * maxEdge) / h);
      h = maxEdge;
    }

    const dst = document.createElement("canvas");
    dst.width = w;
    dst.height = h;
    const ctx = dst.getContext("2d");
    if (!ctx) return;

    ctx.drawImage(src, 0, 0, w, h);
    const dataUrl = dst.toDataURL("image/jpeg", 0.85);

    pushState(dataUrl, w, h, `${t("components.photo_editor.action_resolution")}: ${label}`);
    toast.success(t("components.photo_editor.resolution_applied", { w, h }));
  }

  // --- ADJUSTMENTS (Brightness / Contrast) ---
  function autoEnhance() {
    adjustBrightness.value = 15;
    adjustContrast.value = 20;
    commitAdjustments();
  }

  function commitAdjustments() {
    if (!baseCanvasRef.value) return;
    if (adjustBrightness.value === 0 && adjustContrast.value === 0) return;

    const src = baseCanvasRef.value;
    const dst = document.createElement("canvas");
    dst.width = src.width;
    dst.height = src.height;
    const ctx = dst.getContext("2d");
    if (!ctx) return;

    ctx.drawImage(src, 0, 0);
    const imgData = ctx.getImageData(0, 0, dst.width, dst.height);
    const d = imgData.data;

    const b = adjustBrightness.value;
    const c = adjustContrast.value;
    const factor = (259 * (c + 255)) / (255 * (259 - c));

    for (let i = 0; i < d.length; i += 4) {
      // Apply contrast then brightness
      d[i] = Math.min(255, Math.max(0, factor * (d[i]! - 128) + 128 + b));
      d[i + 1] = Math.min(255, Math.max(0, factor * (d[i + 1]! - 128) + 128 + b));
      d[i + 2] = Math.min(255, Math.max(0, factor * (d[i + 2]! - 128) + 128 + b));
    }

    ctx.putImageData(imgData, 0, 0);
    const dataUrl = dst.toDataURL("image/jpeg", 0.85);

    pushState(dataUrl, dst.width, dst.height, t("components.photo_editor.action_adjust"));
    adjustBrightness.value = 0;
    adjustContrast.value = 0;
    toast.success(t("components.photo_editor.adjust_applied"));
  }

  // --- SAVE & CANCEL ---
  async function saveAndDone() {
    if (!props.photo || history.value.length === 0) {
      cancel();
      return;
    }

    const currentSnapshot = history.value[historyIndex.value]!;
    const arr = currentSnapshot.dataUrl.split(",");
    const bstr = atob(arr[1]!);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }

    let safeName = props.photo.photoName || `photo_${Date.now()}.jpg`;
    if (!safeName.toLowerCase().endsWith(".jpg") && !safeName.toLowerCase().endsWith(".jpeg")) {
      const dotIdx = safeName.lastIndexOf(".");
      if (dotIdx > 0) {
        safeName = safeName.substring(0, dotIdx) + ".jpg";
      } else {
        safeName = `${safeName}.jpg`;
      }
    }

    const updatedFile = new File([u8arr], safeName, { type: "image/jpeg" });

    const updatedPreview: PhotoPreview = {
      ...props.photo,
      photoName: safeName,
      fileBase64: currentSnapshot.dataUrl,
      file: updatedFile,
    };

    emit("saved", updatedPreview);
    toast.success(t("components.photo_editor.saved_successfully"));
    cancel();
  }

  function cancel() {
    isOpen.value = false;
  }
</script>
