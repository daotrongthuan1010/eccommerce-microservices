import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Space, message } from 'antd';
import { Link } from 'react-router-dom';
import {
  useCreateProductDraftMutation,
  useGetProductQuery,
  type Product,
} from '../features/product/productApi';
import {
  PageHeader,
  FilterBar,
  DataTable,
  ApiErrorAlert,
  StatusTag,
} from '../components/ui';

// Wave 2 task 2.2 — ProductsPage. Backend chưa có GET list product
// (contract §5 chỉ có draft-create + detail) nên page tra cứu detail theo
// ID, tạo draft (→ 201 CREATED) và giữ session list hiển thị qua DataTable.
const PAGE_SIZE = 20;

export function ProductsPage() {
  const [form] = Form.useForm();
  const [lookupId, setLookupId] = useState('');
  const [submittedId, setSubmittedId] = useState('');
  const [items, setItems] = useState<Product[]>([]);
  const [page, setPage] = useState(0);
  const [createDraft, createState] = useCreateProductDraftMutation();
  const fetchState = useGetProductQuery(submittedId, { skip: !submittedId });

  const err =
    (createState.error as { message?: string } | undefined)?.message ??
    (fetchState.error as { message?: string } | undefined)?.message;

  const upsert = (p: Product) =>
    setItems((prev) => (prev.some((x) => x.id === p.id) ? prev.map((x) => (x.id === p.id ? p : x)) : [p, ...prev]));

  const onLookup = () => {
    if (lookupId.trim()) setSubmittedId(lookupId.trim());
  };

  useEffect(() => {
    if (fetchState.data?.data) upsert(fetchState.data.data);
  }, [fetchState.data]);

  const onCreateDraft = async (values: { name: string }) => {
    try {
      const res = await createDraft({ name: values.name, status: 'DRAFT' }).unwrap();
      upsert(res.data);
      form.resetFields();
      message.success(`Da tao draft (201 ${res.code ?? 'CREATED'})`);
    } catch {
      message.error('Tao draft that bai');
    }
  };

  const visible = items.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  return (
    <div>
      <PageHeader title="San pham" subTitle="Tra cuu theo ID + tao draft (chua co API list)" />
      <ApiErrorAlert message={err ?? null} />
      <FilterBar>
        <Input
          placeholder="Nhap productId de tra cuu"
          value={lookupId}
          onChange={(e) => setLookupId(e.target.value)}
          onPressEnter={onLookup}
          style={{ width: 320 }}
        />
        <Button type="primary" onClick={onLookup} loading={fetchState.isFetching}>
          Tra cuu detail
        </Button>
      </FilterBar>
      <Card size="small" title="Tao draft (POST /api/product/products → 201 CREATED)" style={{ marginBottom: 16 }}>
        <Form form={form} layout="inline" onFinish={onCreateDraft}>
          <Form.Item name="name" rules={[{ required: true, message: 'Nhap ten san pham' }]}>
            <Input placeholder="Ten san pham" style={{ width: 280 }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={createState.isLoading}>
              Tao draft
            </Button>
          </Form.Item>
        </Form>
      </Card>
      <DataTable
        columns={[
          { title: 'ID', dataIndex: 'id', key: 'id', width: 160 },
          {
            title: 'Ten',
            dataIndex: 'name',
            key: 'name',
            render: (v: unknown, r: Product) => <Link to={`/products/${String(r.id)}`}>{String(v ?? r.id)}</Link>,
          },
          {
            title: 'Trang thai',
            dataIndex: 'status',
            key: 'status',
            width: 140,
            render: (v: string) => (v ? <StatusTag status={v} /> : '-'),
          },
          {
            title: 'Thao tac',
            key: 'action',
            width: 140,
            render: (_: unknown, r: Product) => (
              <Space>
                <Link to={`/products/${String(r.id)}`}>Chi tiet</Link>
              </Space>
            ),
          },
        ]}
        data={visible}
        loading={fetchState.isFetching}
        total={items.length}
        page={page}
        onPageChange={setPage}
        rowKey="id"
      />
    </div>
  );
}

export default ProductsPage;
