import { useState } from 'react';
import { Button, Card, Form, Input, Space, message } from 'antd';
import {
  useSubmitForApprovalMutation,
  useApprovePublicationMutation,
  useRejectPublicationMutation,
} from '../features/product/productApi';
import { PageHeader, ApiErrorAlert, FormSection, ActionBar } from '../components/ui';

// Wave 2 task 2.3 — PublishingPage: submit-for-approval (header X-Actor-Id)
// + approve/reject (reject body {reason}) trên đường doubled
// /api/product/api/v1/... verbatim.
export function PublishingPage() {
  const [submit, submitState] = useSubmitForApprovalMutation();
  const [approve, approveState] = useApprovePublicationMutation();
  const [reject, rejectState] = useRejectPublicationMutation();
  const [approvalId, setApprovalId] = useState('');

  const err =
    (submitState.error as { message?: string } | undefined)?.message ??
    (approveState.error as { message?: string } | undefined)?.message ??
    (rejectState.error as { message?: string } | undefined)?.message;

  const busy = submitState.isLoading || approveState.isLoading || rejectState.isLoading;

  return (
    <div>
      <PageHeader title="Xuất bản sản phẩm" subTitle="Submit → approve / reject (X-Actor-Id + reason)" />
      <ApiErrorAlert message={err ?? null} />
      <Card size="small">
        <FormSection title="1. Gửi duyệt (POST .../products/{productId}/revisions/{revisionId}/submit-for-approval)">
          <Form
            layout="inline"
            onFinish={async (v: { productId: string; revisionId: string; actorId: string }) => {
              try {
                await submit(v).unwrap();
                message.success('Đã gửi duyệt');
              } catch {
                message.error('Gửi duyệt thất bại');
              }
            }}
          >
            <Form.Item name="productId" rules={[{ required: true, message: 'Nhập productId' }]}>
              <Input placeholder="productId" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item name="revisionId" rules={[{ required: true, message: 'Nhập revisionId' }]}>
              <Input placeholder="revisionId" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item name="actorId" rules={[{ required: true, message: 'Nhập X-Actor-Id' }]}>
              <Input placeholder="X-Actor-Id" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={submitState.isLoading}>
                Submit
              </Button>
            </Form.Item>
          </Form>
        </FormSection>
        <FormSection title="2. Duyệt / từ chối (POST .../publication-approvals/{approvalId}/approve|reject)">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space wrap>
              <Input
                placeholder="approvalId"
                value={approvalId}
                onChange={(e) => setApprovalId(e.target.value)}
                style={{ width: 280 }}
              />
              <Input id="publishing-actor" placeholder="X-Actor-Id" style={{ width: 200 }} />
            </Space>
            <ActionBar>
              <Button
                type="primary"
                loading={approveState.isLoading}
                disabled={busy || !approvalId}
                onClick={async () => {
                  const actor = (document.getElementById('publishing-actor') as HTMLInputElement | null)?.value ?? '';
                  if (!actor) {
                    message.warning('Nhập X-Actor-Id');
                    return;
                  }
                  try {
                    await approve({ approvalId, actorId: actor }).unwrap();
                    message.success('Đã duyệt xuất bản');
                  } catch {
                    message.error('Duyệt thất bại');
                  }
                }}
              >
                Approve
              </Button>
              <Button
                danger
                loading={rejectState.isLoading}
                disabled={busy || !approvalId}
                onClick={async () => {
                  const actor = (document.getElementById('publishing-actor') as HTMLInputElement | null)?.value ?? '';
                  const reason = (document.getElementById('publishing-reason') as HTMLInputElement | null)?.value ?? '';
                  if (!actor || !reason) {
                    message.warning('Nhập X-Actor-Id và lý do reject');
                    return;
                  }
                  try {
                    await reject({ approvalId, actorId: actor, reason }).unwrap();
                    message.success('Đã từ chối xuất bản');
                  } catch {
                    message.error('Từ chối thất bại');
                  }
                }}
              >
                Reject
              </Button>
              <Input
                id="publishing-reason"
                placeholder="Lý do reject (body {reason})"
                style={{ width: 320 }}
              />
            </ActionBar>
          </Space>
        </FormSection>
      </Card>
    </div>
  );
}

export default PublishingPage;
