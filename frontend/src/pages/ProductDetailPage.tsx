import { useParams } from 'react-router-dom';
import { Card, Descriptions, Space } from 'antd';
import {
  useGetProductQuery,
  useListVariantsQuery,
  useGetPublishingHistoryQuery,
} from '../features/product/productApi';
import {
  PageHeader,
  DataTable,
  ApiErrorAlert,
  ListSkeleton,
  StatusTag,
} from '../components/ui';

// Wave 2 tasks 2.2/2.3 — ProductDetailPage: detail + variants + media +
// publishing history (group A + B).
export function ProductDetailPage() {
  const { productId = '' } = useParams<{ productId: string }>();
  const detailQ = useGetProductQuery(productId, { skip: !productId });
  const variantsQ = useListVariantsQuery(productId, { skip: !productId });
  const historyQ = useGetPublishingHistoryQuery(productId, { skip: !productId });

  const err =
    (detailQ.error as { message?: string } | undefined)?.message ??
    (variantsQ.error as { message?: string } | undefined)?.message ??
    (historyQ.error as { message?: string } | undefined)?.message;

  const product = detailQ.data?.data;
  const variants = variantsQ.data?.data ?? [];
  const history = historyQ.data?.data ?? [];

  return (
    <div>
      <PageHeader title="Chi tiet san pham" subTitle={productId} />
      <ApiErrorAlert message={err ?? null} />
      {detailQ.isLoading ? (
        <ListSkeleton />
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Card size="small" title="Thong tin (GET /api/product/products/{productId})">
            <Descriptions size="small" column={2}>
              <Descriptions.Item label="ID">{String(product?.id ?? productId)}</Descriptions.Item>
              <Descriptions.Item label="Ten">{String(product?.name ?? '-')}</Descriptions.Item>
              <Descriptions.Item label="Trang thai">
                {product?.status ? <StatusTag status={String(product.status)} /> : '-'}
              </Descriptions.Item>
            </Descriptions>
          </Card>
          <Card size="small" title="Bien the (GET .../products-variant/products/{productId}/variants)">
            <DataTable
              columns={[
                { title: 'ID', dataIndex: 'id', key: 'id', width: 160 },
                {
                  title: 'SKU lien ket',
                  dataIndex: 'skuId',
                  key: 'skuId',
                  render: (v: unknown) => String(v ?? '-'),
                },
              ]}
              data={variants}
              loading={variantsQ.isLoading}
              rowKey="id"
            />
          </Card>
          <Card size="small" title="Lich su xuat ban (GET .../api/v1/products/{productId}/publishing-history)">
            <DataTable
              columns={[
                { title: 'ID', dataIndex: 'id', key: 'id', width: 160 },
                {
                  title: 'Trang thai',
                  dataIndex: 'status',
                  key: 'status',
                  width: 160,
                  render: (v: string) => (v ? <StatusTag status={v} /> : '-'),
                },
              ]}
              data={history}
              loading={historyQ.isLoading}
              rowKey="id"
            />
          </Card>
        </Space>
      )}
    </div>
  );
}

export default ProductDetailPage;
