// Crop anh ve hinh vuong o giua (center-square) truoc upload avatar.
// File-service nhan anh goc nhung avatar hien tron (UserAvatar) nen anh chu
// nhat bi keo meo — crop truoc dam bao ti le 1:1. Dung canvas co san,
// khong them dependency. Loi/to chuc khong ho tro → tra ve file goc.
export async function cropToSquare(file: File): Promise<File> {
  try {
    const bitmap = await createImageBitmap(file);
    try {
      const side = Math.min(bitmap.width, bitmap.height);
      if (side <= 0) return file;
      const sx = Math.floor((bitmap.width - side) / 2);
      const sy = Math.floor((bitmap.height - side) / 2);
      const canvas = document.createElement('canvas');
      canvas.width = side;
      canvas.height = side;
      const ctx = canvas.getContext('2d');
      if (!ctx) return file;
      ctx.drawImage(bitmap, sx, sy, side, side, 0, 0, side, side);
      const blob = await new Promise<Blob | null>((resolve) =>
        canvas.toBlob(resolve, file.type || 'image/png'),
      );
      if (!blob) return file;
      return new File([blob], file.name, { type: blob.type || file.type });
    } finally {
      bitmap.close();
    }
  } catch {
    return file;
  }
}
