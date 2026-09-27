export interface PhotoPreview {
  photoName: string;
  file: File;
  fileBase64: string;
  primary: boolean;
}

export function dataURLtoFile(dataURL: string, fileName: string) {
  const arr = dataURL.split(",");
  const mimeMatch = arr[0]!.match(/:(.*?);/);
  if (!mimeMatch || !mimeMatch[1]) {
    throw new Error("Invalid data URL format");
  }

  const mime = mimeMatch[1];
  if (!mime.startsWith("image/")) {
    throw new Error("Invalid mime type, expected image");
  }

  let safeName = fileName || `photo_${Date.now()}.jpg`;
  if (mime === "image/jpeg" && !safeName.toLowerCase().endsWith(".jpg") && !safeName.toLowerCase().endsWith(".jpeg")) {
    const dotIdx = safeName.lastIndexOf(".");
    if (dotIdx > 0) {
      safeName = safeName.substring(0, dotIdx) + ".jpg";
    } else {
      safeName = `${safeName}.jpg`;
    }
  }

  const bstr = atob(arr[arr.length - 1]!);
  let n = bstr.length;
  const u8arr = new Uint8Array(n);
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n);
  }

  return new File([u8arr], safeName, { type: mime });
}

async function compressImageFile(
  file: File,
  maxDimension = 2048,
  quality = 0.85
): Promise<{ compressedFile: File; dataUrl: string }> {
  if (!file.type.startsWith("image/") && !file.type.includes("force-camera")) {
    const dataUrl = await readFileAsDataUrl(file);
    return { compressedFile: file, dataUrl };
  }

  const objectUrl = URL.createObjectURL(file);
  try {
    const img = new Image();
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve();
      img.onerror = () => reject(new Error("Failed to load image for compression"));
      img.src = objectUrl;
    });

    let width = img.naturalWidth || img.width;
    let height = img.naturalHeight || img.height;

    if (width <= 0 || height <= 0) {
      const dataUrl = await readFileAsDataUrl(file);
      return { compressedFile: file, dataUrl };
    }

    if (width > maxDimension || height > maxDimension) {
      if (width > height) {
        height = Math.round((height * maxDimension) / width);
        width = maxDimension;
      } else {
        width = Math.round((width * maxDimension) / height);
        height = maxDimension;
      }
    }

    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext("2d");
    if (!ctx) {
      throw new Error("Canvas context unavailable");
    }

    ctx.drawImage(img, 0, 0, width, height);

    const outputType = file.type === "image/png" ? "image/png" : "image/jpeg";
    const dataUrl = canvas.toDataURL(outputType, quality);

    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        b => (b ? resolve(b) : reject(new Error("Canvas toBlob failed"))),
        outputType,
        quality
      );
    });

    let safeName = file.name || `photo_${Date.now()}.jpg`;
    if (
      outputType === "image/jpeg" &&
      !safeName.toLowerCase().endsWith(".jpg") &&
      !safeName.toLowerCase().endsWith(".jpeg")
    ) {
      const dotIdx = safeName.lastIndexOf(".");
      if (dotIdx > 0) {
        safeName = safeName.substring(0, dotIdx) + ".jpg";
      } else {
        safeName = `${safeName}.jpg`;
      }
    }

    const compressedFile = new File([blob], safeName, { type: outputType });
    canvas.width = 0;
    canvas.height = 0;

    return { compressedFile, dataUrl };
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

export async function fileToPhotoPreview(file: File, primary = false): Promise<PhotoPreview> {
  try {
    const { compressedFile, dataUrl } = await compressImageFile(file);
    return {
      photoName: compressedFile.name,
      file: compressedFile,
      fileBase64: dataUrl,
      primary,
    };
  } catch (err) {
    console.warn("Failed to compress image, using original:", err);
    const fileBase64 = await readFileAsDataUrl(file);
    const safeName = file.name || `photo_${Date.now()}.jpg`;
    return {
      photoName: safeName,
      file,
      fileBase64,
      primary,
    };
  }
}

export async function filesToPhotoPreviews(files: FileList | File[], existingCount = 0): Promise<PhotoPreview[]> {
  const nextPhotos: PhotoPreview[] = [];

  for (const file of Array.from(files)) {
    nextPhotos.push(await fileToPhotoPreview(file, existingCount + nextPhotos.length === 0));
  }

  return nextPhotos;
}

export async function rotatePhotoPreview(photo: PhotoPreview): Promise<PhotoPreview> {
  const offScreenCanvas = document.createElement("canvas");
  const offScreenCanvasCtx = offScreenCanvas.getContext("2d");

  if (!offScreenCanvasCtx) {
    throw new Error("Canvas not supported");
  }

  const img = new Image();
  await new Promise<void>((resolve, reject) => {
    img.onload = () => resolve();
    img.onerror = () => reject(new Error("Failed to load image"));
    img.src = photo.fileBase64;
  });

  const maxDimension = 2048;
  let targetWidth = img.height;
  let targetHeight = img.width;

  if (targetWidth > maxDimension || targetHeight > maxDimension) {
    if (targetWidth > targetHeight) {
      targetHeight = Math.round((targetHeight * maxDimension) / targetWidth);
      targetWidth = maxDimension;
    } else {
      targetWidth = Math.round((targetWidth * maxDimension) / targetHeight);
      targetHeight = maxDimension;
    }
  }

  offScreenCanvas.width = targetWidth;
  offScreenCanvas.height = targetHeight;

  offScreenCanvasCtx.translate(targetWidth / 2, targetHeight / 2);
  offScreenCanvasCtx.rotate((90 * Math.PI) / 180);
  offScreenCanvasCtx.drawImage(img, -targetHeight / 2, -targetWidth / 2, targetHeight, targetWidth);

  const imageType = photo.fileBase64.match(/^data:(.+);base64/)?.[1] || "image/jpeg";
  const outputType = imageType === "image/png" ? "image/png" : "image/jpeg";
  const fileBase64 = offScreenCanvas.toDataURL(outputType, 0.85);

  const blob = await new Promise<Blob>((resolve, reject) => {
    offScreenCanvas.toBlob(
      b => (b ? resolve(b) : reject(new Error("Canvas toBlob failed"))),
      outputType,
      0.85
    );
  });

  let safeName = photo.photoName || `photo_${Date.now()}.jpg`;
  if (
    outputType === "image/jpeg" &&
    !safeName.toLowerCase().endsWith(".jpg") &&
    !safeName.toLowerCase().endsWith(".jpeg")
  ) {
    const dotIdx = safeName.lastIndexOf(".");
    if (dotIdx > 0) {
      safeName = safeName.substring(0, dotIdx) + ".jpg";
    } else {
      safeName = `${safeName}.jpg`;
    }
  }

  const file = new File([blob], safeName, { type: outputType });

  offScreenCanvas.width = 0;
  offScreenCanvas.height = 0;

  return {
    ...photo,
    photoName: safeName,
    fileBase64,
    file,
  };
}

export function setPrimaryPhoto(photos: PhotoPreview[], index: number): PhotoPreview[] {
  const nextPhotos = photos.map(photo => ({ ...photo, primary: false }));
  if (nextPhotos[index]) {
    nextPhotos[index] = { ...nextPhotos[index]!, primary: true };
  }
  return nextPhotos;
}

export function deletePhoto(photos: PhotoPreview[], index: number): PhotoPreview[] {
  const nextPhotos = photos.filter((_, photoIndex) => photoIndex !== index);
  if (nextPhotos.length > 0 && !nextPhotos.some(photo => photo.primary)) {
    nextPhotos[0] = { ...nextPhotos[0]!, primary: true };
  }
  return nextPhotos;
}

async function readFileAsDataUrl(file: File): Promise<string> {
  return await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = event => resolve(event.target?.result as string);
    reader.onerror = () => reject(new Error(`Failed to read file: ${file.name}`));
    reader.readAsDataURL(file);
  });
}
