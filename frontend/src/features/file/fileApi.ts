import { createApi } from '@reduxjs/toolkit/query/react';
import { api, axiosBaseQuery, type ApiResponse } from '../../app/api';

// FE_API_CONTRACT §7: FileController `/files` — mau envelope chuan (tru download stream).

export interface FileObjectResponse {
  key: string;
  fileName?: string;
  size?: number;
  contentType?: string;
  url?: string;
  [k: string]: unknown;
}

export interface FileFailure {
  fileName?: string;
  error?: string;
  message?: string;
  [k: string]: unknown;
}

export interface BatchUploadData {
  files: FileObjectResponse[];
  failures?: FileFailure[];
  [k: string]: unknown;
}

export interface FileInfoResponse {
  key: string;
  fileName?: string;
  size?: number;
  contentType?: string;
  [k: string]: unknown;
}

export interface UrlResponse {
  url: string;
  expiresIn?: number;
  [k: string]: unknown;
}

export interface PresignUploadRequest {
  fileName: string;
  contentType?: string;
  folder?: string;
  [k: string]: unknown;
}

export const fileApi = createApi({
  reducerPath: 'fileApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['File'],
  endpoints: (build) => ({
    // real 2026-10-22: POST /api/file/files (multipart: file, folder?)
    upload: build.mutation<ApiResponse<FileObjectResponse>, { file: File | Blob; folder?: string }>({
      query: ({ file, folder }) => {
        const form = new FormData();
        form.append('file', file);
        if (folder) form.append('folder', folder);
        return { url: '/file/files', method: 'post', data: form };
      },
      invalidatesTags: ['File'],
    }),
    // real 2026-10-22: POST /api/file/files/batch (multipart: files, folder?) — 207 neu co failures
    uploadBatch: build.mutation<ApiResponse<BatchUploadData>, { files: Array<File | Blob>; folder?: string }>({
      query: ({ files, folder }) => {
        const form = new FormData();
        files.forEach((f) => form.append('files', f));
        if (folder) form.append('folder', folder);
        return { url: '/file/files/batch', method: 'post', data: form };
      },
      invalidatesTags: ['File'],
    }),
    // real 2026-10-22: GET /api/file/files/info?key=
    fileInfo: build.query<ApiResponse<FileInfoResponse>, string>({
      query: (key) => ({ url: '/file/files/info', method: 'get', params: { key } }),
      providesTags: ['File'],
    }),
    // real 2026-10-22: GET /api/file/files/url?key=&expirySeconds=
    fileUrl: build.query<ApiResponse<UrlResponse>, { key: string; expirySeconds?: number }>({
      query: ({ key, expirySeconds }) => ({
        url: '/file/files/url',
        method: 'get',
        params: { key, expirySeconds },
      }),
    }),
    // real 2026-10-22: POST /api/file/files/presign-upload
    presignUpload: build.mutation<ApiResponse<UrlResponse>, PresignUploadRequest>({
      query: (body) => ({ url: '/file/files/presign-upload', method: 'post', data: body }),
    }),
    // real 2026-10-22: DELETE /api/file/files?key=
    deleteFile: build.mutation<ApiResponse<{ success: boolean }>, string>({
      query: (key) => ({ url: '/file/files', method: 'delete', params: { key } }),
      invalidatesTags: ['File'],
    }),
  }),
});

export const {
  useUploadMutation,
  useUploadBatchMutation,
  useLazyFileInfoQuery,
  useLazyFileUrlQuery,
  usePresignUploadMutation,
  useDeleteFileMutation,
} = fileApi;

// real 2026-10-22: GET /api/file/files/download?key= — stream nhi phan,
// KHONG parse envelope (responseType blob).
export async function downloadFile(key: string): Promise<Blob> {
  const res = await api.get('/file/files/download', {
    params: { key },
    responseType: 'blob',
  });
  return res.data as Blob;
}
