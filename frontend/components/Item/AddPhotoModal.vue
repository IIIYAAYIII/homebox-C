<template>
  <BaseModal :dialog-id="DialogID.AddPhoto" :title="$t('items.add_photo')" :hide-footer="true">
    <form class="flex min-w-0 flex-col gap-4" @submit.prevent="upload">
      <PhotoUploader
        :label="$t('items.select_or_take_photo')"
        :button-label="$t('items.take_or_upload_photo')"
        :existing-count="photos.length"
        @selected="appendPhotos"
      />

      <PhotoUploaderPreview
        :photos="photos"
        @delete="deletePhotoAt"
        @rotate="rotatePhotoAt"
        @set-primary="setPrimaryPhotoAt"
      />

      <div class="mt-4 flex flex-row-reverse gap-2">
        <Button :disabled="loading || photos.length === 0" type="submit">
          <span v-if="loading">{{ $t("items.uploading_photos") }}</span>
          <span v-else>{{ $t("global.save") }}</span>
        </Button>
        <Button variant="outline" :disabled="loading" type="button" @click="close">
          {{ $t("global.cancel") }}
        </Button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted } from "vue";
  import { useI18n } from "vue-i18n";
  import { DialogID } from "@/components/ui/dialog-provider/utils";
  import { toast } from "@/components/ui/sonner";
  import BaseModal from "@/components/App/CreateModal.vue";
  import { useDialog } from "~/components/ui/dialog-provider";
  import PhotoUploader from "~/components/Form/PhotoUploader.vue";
  import PhotoUploaderPreview from "~/components/Form/PhotoUploaderPreview.vue";
  import {
    deletePhoto,
    rotatePhotoPreview,
    setPrimaryPhoto,
    type PhotoPreview,
  } from "~/components/Form/photo-uploader";
  import { AttachmentTypes } from "~/lib/api/types/data-contracts";
  import { Button } from "~/components/ui/button";

  const props = defineProps<{
    itemId?: string;
  }>();

  const emit = defineEmits<{
    (e: "uploaded"): void;
  }>();

  const { t } = useI18n();
  const { closeDialog, registerOpenDialogCallback } = useDialog();
  const api = useUserApi();

  const photos = ref<PhotoPreview[]>([]);
  const loading = ref(false);
  const activeItemId = ref(props.itemId || "");
  let onUploadedCallback: (() => void) | undefined;

  onMounted(() => {
    const cleanup = registerOpenDialogCallback(DialogID.AddPhoto, params => {
      photos.value = [];
      loading.value = false;
      if (params?.itemId) {
        activeItemId.value = params.itemId;
      } else if (props.itemId) {
        activeItemId.value = props.itemId;
      }
      onUploadedCallback = params?.onUploaded;
    });

    onUnmounted(cleanup);
  });

  function appendPhotos(newPhotos: PhotoPreview[]) {
    photos.value.push(...newPhotos);
  }

  function deletePhotoAt(index: number) {
    photos.value = deletePhoto(photos.value, index);
  }

  function setPrimaryPhotoAt(index: number) {
    photos.value = setPrimaryPhoto(photos.value, index);
  }

  async function rotatePhotoAt(index: number) {
    const photo = photos.value[index];
    if (!photo) return;
    try {
      photos.value[index] = await rotatePhotoPreview(photo);
    } catch (error) {
      toast.error(t("components.entity.create_modal.toast.rotate_process_failed"));
      console.error(error);
    }
  }

  function close() {
    closeDialog(DialogID.AddPhoto);
    photos.value = [];
    loading.value = false;
  }

  async function upload() {
    if (!activeItemId.value) {
      toast.error(t("items.toast.failed_load_item"));
      return;
    }
    if (photos.value.length === 0) {
      return;
    }

    loading.value = true;
    toast.info(t("components.entity.create_modal.toast.uploading_photos", { count: photos.value.length }));

    let hasError = false;
    for (const photo of photos.value) {
      const { error } = await api.items.attachments.add(
        activeItemId.value,
        photo.file,
        photo.photoName,
        AttachmentTypes.Photo,
        photo.primary
      );

      if (error) {
        hasError = true;
        toast.error(t("components.entity.create_modal.toast.upload_failed", { photoName: photo.photoName }));
        console.error(error);
      }
    }

    loading.value = false;

    if (hasError) {
      toast.warning(t("components.entity.create_modal.toast.some_photos_failed", { count: photos.value.length }));
    } else {
      toast.success(t("components.entity.create_modal.toast.upload_success", { count: photos.value.length }));
    }

    emit("uploaded");
    onUploadedCallback?.();
    close();
  }
</script>
