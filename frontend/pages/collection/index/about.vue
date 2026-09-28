<script setup lang="ts">
  import { useI18n } from "vue-i18n";
  import { lt } from "semver";
  import { Badge } from "@/components/ui/badge";
  import { Button } from "@/components/ui/button";
  import { toast } from "@/components/ui/sonner";
  import AppLogo from "~/components/App/Logo.vue";
  import type { APISummary } from "~/lib/api/types/data-contracts";

  import MdiInformationOutline from "~icons/mdi/information-outline";
  import MdiCheckCircle from "~icons/mdi/check-circle";
  import MdiAlertCircleOutline from "~icons/mdi/alert-circle-outline";
  import MdiServer from "~icons/mdi/server";
  import MdiDocker from "~icons/mdi/docker";
  import MdiContentCopy from "~icons/mdi/content-copy";
  import MdiRefresh from "~icons/mdi/refresh";
  import MdiGithub from "~icons/mdi/github";
  import MdiOpenInNew from "~icons/mdi/open-in-new";
  import MdiAndroid from "~icons/mdi/android";
  import MdiShieldCheck from "~icons/mdi/shield-check";
  import MdiTagOutline from "~icons/mdi/tag-outline";

  definePageMeta({
    middleware: ["auth"],
  });

  const { t } = useI18n();

  useHead({ title: `HomeBox | ${t("collection.tabs.about")}` });

  const pubApi = usePublicApi();
  const loading = ref(false);
  const status = ref<APISummary | null>(null);

  const loadStatus = async () => {
    loading.value = true;
    try {
      const res = await pubApi.status();
      if (res.data) {
        status.value = res.data;
      }
    } catch (e) {
      console.error(e);
      toast.error(t("about.toast.failed_load_status"));
    } finally {
      loading.value = false;
    }
  };

  onMounted(() => {
    void loadStatus();
  });

  const rawVersion = computed(() => status.value?.build?.version || "nightly");
  const formattedVersion = computed(() => {
    const v = rawVersion.value;
    if (v === "nightly" || v === "HEAD" || v.startsWith("v")) {
      return v;
    }
    return `v${v}`;
  });

  const commitSha = computed(() => status.value?.build?.commit || "");
  const shortCommit = computed(() => {
    if (!commitSha.value || commitSha.value === "HEAD") return "HEAD";
    return commitSha.value.length > 7 ? commitSha.value.slice(0, 7) : commitSha.value;
  });

  const commitUrl = computed(() => {
    if (!commitSha.value || commitSha.value === "HEAD") return "";
    return `https://github.com/IIIYAAYIII/homebox-C/commit/${commitSha.value}`;
  });

  const formattedBuildTime = computed(() => {
    const bt = status.value?.build?.buildTime;
    if (!bt || bt === "now") return t("about.unknown");
    try {
      const d = new Date(bt);
      if (isNaN(d.getTime())) return bt;
      return d.toLocaleString();
    } catch {
      return bt;
    }
  });

  const osArch = computed(() => {
    const b = status.value?.build;
    if (!b?.os && !b?.arch) return "linux / amd64 (Docker)";
    return `${b.os || "linux"} / ${b.arch || "amd64"}`;
  });

  const databaseInfo = computed(() => {
    const db = status.value?.build?.database || "sqlite3";
    if (db.toLowerCase().includes("sqlite")) {
      return "SQLite 3 (WAL mode)";
    }
    if (db.toLowerCase().includes("postgres")) {
      return "PostgreSQL";
    }
    return db;
  });

  const isUpToDate = computed(() => {
    const cur = status.value?.build?.version?.replace(/^v/, "");
    const lat = status.value?.latest?.version?.replace(/^v/, "");
    if (!cur || !lat) return false;
    try {
      return !lt(cur, lat);
    } catch {
      return cur === lat;
    }
  });

  const hasUpdate = computed(() => {
    const cur = status.value?.build?.version?.replace(/^v/, "");
    const lat = status.value?.latest?.version?.replace(/^v/, "");
    if (!cur || !lat) return false;
    try {
      return lt(cur, lat);
    } catch {
      return false;
    }
  });

  const copyDiagnostics = async () => {
    const b = status.value?.build;
    const diag = [
      `### Homebox System Diagnostics`,
      `- **Version**: ${formattedVersion.value}`,
      `- **Commit**: ${commitSha.value}`,
      `- **Build Time**: ${status.value?.build?.buildTime || "now"}`,
      `- **OS / Arch**: ${osArch.value}`,
      `- **Go Runtime**: ${b?.goVersion || "N/A"}`,
      `- **Database**: ${databaseInfo.value}`,
      `- **Storage**: ${b?.storage || "data"}`,
      `- **Mode**: ${b?.mode || (status.value?.demo ? "demo" : "production")}`,
      `- **Healthy**: ${status.value?.health ? "Yes" : "No"}`,
      `- **Registration**: ${status.value?.allowRegistration ? "Enabled" : "Disabled"}`,
      `- **Label Printing**: ${status.value?.labelPrinting ? "Enabled" : "Disabled"}`,
      `- **OIDC SSO**: ${status.value?.oidc?.enabled ? "Enabled" : "Disabled"}`,
    ].join("\n");

    try {
      await navigator.clipboard.writeText(diag);
      toast.success(t("about.diag_copied"));
    } catch {
      toast.error(t("about.toast.copy_failed"));
    }
  };
</script>

<template>
  <div class="space-y-6">
    <!-- Top Hero Card -->
    <div
      class="relative overflow-hidden rounded-xl border bg-gradient-to-br from-card via-card to-muted/40 p-6 shadow-sm"
    >
      <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-4">
          <div
            class="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary shadow-inner"
          >
            <AppLogo class="size-8" />
          </div>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-xl font-bold tracking-tight sm:text-2xl">Homebox</h2>
              <Badge variant="default" class="px-2.5 py-0.5 font-mono text-xs shadow-sm">
                {{ formattedVersion }}
              </Badge>
              <Badge
                v-if="isUpToDate"
                variant="secondary"
                class="border-emerald-500/30 bg-emerald-500/15 text-xs text-emerald-600 dark:text-emerald-400"
              >
                <MdiCheckCircle class="mr-1 inline size-3.5" />
                {{ $t("about.up_to_date") }}
              </Badge>
              <Badge
                v-else-if="hasUpdate"
                variant="secondary"
                class="border-amber-500/30 bg-amber-500/15 text-xs text-amber-600 dark:text-amber-400"
              >
                <MdiAlertCircleOutline class="mr-1 inline size-3.5" />
                {{ $t("about.update_available") }}: {{ status?.latest?.version }}
              </Badge>
            </div>
            <p class="mt-1 text-sm text-muted-foreground">
              {{ $t("about.description") }}
            </p>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <Button variant="outline" size="sm" :disabled="loading" @click="loadStatus">
            <MdiRefresh class="mr-1.5 size-4" :class="{ 'animate-spin': loading }" />
            {{ $t("about.check_update") }}
          </Button>
          <Button variant="secondary" size="sm" @click="copyDiagnostics">
            <MdiContentCopy class="mr-1.5 size-4" />
            {{ $t("about.copy_diag") }}
          </Button>
        </div>
      </div>
    </div>

    <!-- Main 2-Column Grid -->
    <div class="grid gap-6 md:grid-cols-2">
      <!-- Card 1: Version & Build Info -->
      <div class="rounded-xl border bg-card p-5 shadow-sm">
        <div class="mb-4 flex items-center gap-2 border-b pb-3">
          <MdiDocker class="size-5 text-primary" />
          <h3 class="text-base font-semibold">{{ $t("about.version_and_build") }}</h3>
        </div>

        <dl class="space-y-3.5 text-sm">
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.version") }} / {{ $t("about.docker_tag") }}</dt>
            <dd class="font-mono font-semibold">{{ formattedVersion }}</dd>
          </div>
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.commit") }}</dt>
            <dd class="flex items-center gap-1.5 font-mono">
              <span v-if="commitSha">{{ shortCommit }}</span>
              <span v-else class="text-muted-foreground">N/A</span>
              <a
                v-if="commitSha && commitSha !== 'HEAD'"
                :href="commitUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center text-primary hover:underline"
                :title="$t('about.view_commit_github')"
              >
                <MdiOpenInNew class="ml-1 size-3.5" />
              </a>
            </dd>
          </div>
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.build_time") }}</dt>
            <dd class="font-mono text-xs">{{ formattedBuildTime }}</dd>
          </div>
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.latest_release") }}</dt>
            <dd class="font-mono">
              <span v-if="status?.latest?.version">{{ status.latest.version }}</span>
              <span v-else class="text-muted-foreground">-</span>
            </dd>
          </div>
        </dl>
      </div>

      <!-- Card 2: Runtime Environment -->
      <div class="rounded-xl border bg-card p-5 shadow-sm">
        <div class="mb-4 flex items-center gap-2 border-b pb-3">
          <MdiServer class="size-5 text-primary" />
          <h3 class="text-base font-semibold">{{ $t("about.system_info") }}</h3>
        </div>

        <dl class="space-y-3.5 text-sm">
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.os_arch") }}</dt>
            <dd class="font-mono">{{ osArch }}</dd>
          </div>
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.runtime") }}</dt>
            <dd class="font-mono">{{ status?.build?.goVersion || "Go" }}</dd>
          </div>
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.mode") }}</dt>
            <dd>
              <Badge variant="outline" class="capitalize">
                {{ status?.build?.mode || (status?.demo ? "demo" : "production") }}
              </Badge>
            </dd>
          </div>
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.database") }}</dt>
            <dd class="font-mono">{{ databaseInfo }}</dd>
          </div>
          <div class="flex items-center justify-between">
            <dt class="text-muted-foreground">{{ $t("about.health_status") }}</dt>
            <dd>
              <Badge
                variant="secondary"
                class="border-emerald-500/30 bg-emerald-500/15 text-emerald-600 dark:text-emerald-400"
              >
                {{ status?.health ? $t("about.healthy") : $t("about.unhealthy") }}
              </Badge>
            </dd>
          </div>
        </dl>
      </div>

      <!-- Card 3: Features & Services -->
      <div class="rounded-xl border bg-card p-5 shadow-sm">
        <div class="mb-4 flex items-center gap-2 border-b pb-3">
          <MdiShieldCheck class="size-5 text-primary" />
          <h3 class="text-base font-semibold">{{ $t("about.feature_status") }}</h3>
        </div>

        <div class="grid grid-cols-2 gap-3 text-sm">
          <div class="rounded-lg border bg-muted/30 p-3">
            <div class="text-xs text-muted-foreground">{{ $t("about.registration") }}</div>
            <div class="mt-1 flex items-center gap-1.5 font-semibold">
              <span
                class="size-2 rounded-full"
                :class="status?.allowRegistration ? 'bg-emerald-500' : 'bg-muted-foreground'"
              />
              {{ status?.allowRegistration ? $t("about.enabled") : $t("about.disabled") }}
            </div>
          </div>
          <div class="rounded-lg border bg-muted/30 p-3">
            <div class="text-xs text-muted-foreground">{{ $t("about.label_printing") }}</div>
            <div class="mt-1 flex items-center gap-1.5 font-semibold">
              <span
                class="size-2 rounded-full"
                :class="status?.labelPrinting ? 'bg-emerald-500' : 'bg-muted-foreground'"
              />
              {{ status?.labelPrinting ? $t("about.enabled") : $t("about.disabled") }}
            </div>
          </div>
          <div class="rounded-lg border bg-muted/30 p-3">
            <div class="text-xs text-muted-foreground">{{ $t("about.oidc") }}</div>
            <div class="mt-1 flex items-center gap-1.5 font-semibold">
              <span
                class="size-2 rounded-full"
                :class="status?.oidc?.enabled ? 'bg-emerald-500' : 'bg-muted-foreground'"
              />
              {{ status?.oidc?.enabled ? $t("about.enabled") : $t("about.disabled") }}
            </div>
          </div>
          <div class="rounded-lg border bg-muted/30 p-3">
            <div class="text-xs text-muted-foreground">{{ $t("about.demo_mode") }}</div>
            <div class="mt-1 flex items-center gap-1.5 font-semibold">
              <span class="size-2 rounded-full" :class="status?.demo ? 'bg-amber-500' : 'bg-muted-foreground'" />
              {{ status?.demo ? $t("about.enabled") : $t("about.disabled") }}
            </div>
          </div>
        </div>
      </div>

      <!-- Card 4: Links & Resources -->
      <div class="rounded-xl border bg-card p-5 shadow-sm">
        <div class="mb-4 flex items-center gap-2 border-b pb-3">
          <MdiGithub class="size-5 text-primary" />
          <h3 class="text-base font-semibold">{{ $t("about.links") }}</h3>
        </div>

        <div class="space-y-2.5">
          <a
            href="https://github.com/IIIYAAYIII/homebox-C"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center justify-between rounded-lg border p-3 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            <div class="flex items-center gap-2.5">
              <MdiGithub class="size-4" />
              <span>{{ $t("about.github_repo") }}</span>
            </div>
            <MdiOpenInNew class="size-4 text-muted-foreground" />
          </a>

          <a
            href="https://github.com/IIIYAAYIII/homebox-C/releases"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center justify-between rounded-lg border p-3 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            <div class="flex items-center gap-2.5">
              <MdiTagOutline class="size-4" />
              <span>{{ $t("about.releases") }}</span>
            </div>
            <MdiOpenInNew class="size-4 text-muted-foreground" />
          </a>

          <a
            href="https://github.com/IIIYAAYIII/homebox-C/actions/workflows/android-build.yml"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center justify-between rounded-lg border p-3 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            <div class="flex items-center gap-2.5">
              <MdiAndroid class="size-4" />
              <span>{{ $t("about.android_app") }}</span>
            </div>
            <MdiOpenInNew class="size-4 text-muted-foreground" />
          </a>

          <a
            href="https://homebox.software"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center justify-between rounded-lg border p-3 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            <div class="flex items-center gap-2.5">
              <MdiInformationOutline class="size-4" />
              <span>{{ $t("about.docs") }}</span>
            </div>
            <MdiOpenInNew class="size-4 text-muted-foreground" />
          </a>
        </div>
      </div>
    </div>
  </div>
</template>
