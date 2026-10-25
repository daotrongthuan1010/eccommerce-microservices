import { Alert, App, Button, Card, Input, List, Space, Typography } from 'antd';
import { useState } from 'react';
import { ApiErrorAlert, ListSkeleton, PageHeader } from '../components/ui';
import {
  downloadFile,
  useDeleteFileMutation,
  useLazyFileInfoQuery,
  useLazyFileUrlQuery,
  useUploadBatchMutation,
  useUploadMutation,
  type BatchUploadData,
} from '../features/file/fileApi';

// FE_API_CONTRACT §7: upload/batch multipart, 207 failures hien thi per-file,
// download stream blob (KHONG parse envelope), delete by key.
export default function FilesPage() {
  const { message } = App.useApp();
  const [folder, setFolder] = useState('');
  const [key, setKey] = useState('');
  const [batchResult, setBatchResult] = useState<BatchUploadData | null>(null);

  const [upload, { isLoading: uploading }] = useUploadMutation();
  const [uploadBatch, { isLoading: batching }] = useUploadBatchMutation();
  const [triggerInfo, infoState] = useLazyFileInfoQuery();
  const [triggerUrl] = useLazyFileUrlQuery();
  const [deleteFile, { isLoading: deleting }] = useDeleteFileMutation();

  const onSingle = async (file: File | false) => {
    if (!file) return;
    try {
      await upload({ file, folder: folder || undefined }).unwrap();
      message.success('Tải lên thành công');
    } catch {
      message.error('Tải lên thất bại');
    }
  };

  const onBatch = async (fileList: File[]) => {
    if (fileList.length === 0) return;
    try {
      const res = await uploadBatch({ files: fileList, folder: folder || undefined }).unwrap();
      setBatchResult(res.data);
      const failures = res.data.failures ?? [];
      if (failures.length > 0) {
        message.warning(`${failures.length}/${fileList.length} file thất bại (207)`);
      } else {
        message.success(`Đã tải lên ${res.data.files.length} file`);
      }
    } catch {
      message.error('Tải batch thất bại');
    }
  };

  const onDownload = async () => {
    if (!key) return;
    try {
      const blob = await downloadFile(key);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = key.split('/').pop() ?? key;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Tải xuống thất bại');
    }
  };

  const onDelete = async () => {
    if (!key) return;
    try {
      await deleteFile(key).unwrap();
      message.success('Đã xóa file');
    } catch {
      message.error('Xóa file thất bại');
    }
  };

  const onOpenUrl = async (expirySeconds?: number) => {
    if (!key) return;
    try {
      const res = await triggerUrl({ key, expirySeconds }).unwrap();
      window.open(res.data.url, '_blank', 'noopener');
    } catch {
      message.error('Không lấy được URL');
    }
  };

  return (
    <div style={{ maxWidth: 860, margin: '0 auto' }}>
      <PageHeader title="Files" subTitle="Upload / tra cứu / tải xuống / xóa" />
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Card title="Tải lên" size="small">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Input
              placeholder="Thư mục (tùy chọn)"
              value={folder}
              onChange={(e) => setFolder(e.target.value)}
            />
            <Space wrap>
              <label htmlFor="file-single">
                <Button type="primary" loading={uploading} onClick={() => document.getElementById('file-single')?.click()}>
                  Chọn 1 file
                </Button>
              </label>
              <input
                id="file-single"
                type="file"
                hidden
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) void onSingle(f);
                  e.target.value = '';
                }}
              />
              <label htmlFor="file-batch">
                <Button loading={batching} onClick={() => document.getElementById('file-batch')?.click()}>
                  Chọn nhiều file (batch)
                </Button>
              </label>
              <input
                id="file-batch"
                type="file"
                multiple
                hidden
                onChange={(e) => {
                  const fs = Array.from(e.target.files ?? []);
                  if (fs.length > 0) void onBatch(fs);
                  e.target.value = '';
                }}
              />
            </Space>
            {batchResult?.failures && batchResult.failures.length > 0 && (
              <Alert
                type="warning"
                showIcon
                message={`${batchResult.failures.length} file thất bại (207)`}
                description={
                  <List
                    size="small"
                    dataSource={batchResult.failures}
                    renderItem={(f, i) => (
                      <List.Item key={i}>
                        <Typography.Text>
                          {f.fileName ?? `#${i + 1}`}: {f.message ?? f.error ?? 'Thất bại'}
                        </Typography.Text>
                      </List.Item>
                    )}
                  />
                }
              />
            )}
          </Space>
        </Card>

        <Card title="Tra cứu theo key" size="small">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space wrap>
              <Input
                placeholder="File key"
                value={key}
                onChange={(e) => setKey(e.target.value)}
                style={{ width: 320 }}
              />
              <Button onClick={() => key && void triggerInfo(key)}>Xem info</Button>
              <Button onClick={() => void onOpenUrl()}>Mở URL</Button>
              <Button onClick={onDownload}>Tải xuống (blob)</Button>
              <Button danger loading={deleting} onClick={() => void onDelete()}>
                Xóa
              </Button>
            </Space>
            {infoState.isFetching ? (
              <ListSkeleton />
            ) : (
              <>
                <ApiErrorAlert
                  message={infoState.isError ? 'Không tải được metadata file' : null}
                />
                {infoState.data?.data && (
                  <Typography.Text code>{JSON.stringify(infoState.data.data)}</Typography.Text>
                )}
              </>
            )}
          </Space>
        </Card>
      </Space>
    </div>
  );
}
