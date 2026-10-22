import { useState } from 'react';
import { Button, Card, Space, Tree, message } from 'antd';
import type { DataNode } from 'antd/es/tree';
import {
  useGetCategoryTreeQuery,
  useListBrandsQuery,
  useDeleteBrandMutation,
  useDeleteCategoryMutation,
  type CategoryTreeNode,
} from '../features/catalog/catalogApi';
import { useAppSelector } from '../app/hooks';
import {
  PageHeader,
  DataTable,
  ApiErrorAlert,
  ListSkeleton,
  StatusTag,
} from '../components/ui';

// Wave 2 task 2.1 — CatalogPage: cây danh mục + bảng brands.
// Nút mutation admin-only ẩn khi thiếu role ADMIN.
function toTreeNodes(nodes: CategoryTreeNode[]): DataNode[] {
  return nodes.map((n) => ({
    key: String(n.id),
    title: `${String(n.name)}${n.status ? ` [${String(n.status)}]` : ''}`,
    children: n.children?.length ? toTreeNodes(n.children) : undefined,
  }));
}

export function CatalogPage() {
  const roles = useAppSelector((s) => s.auth.roles);
  const isAdmin = roles.includes('ADMIN');
  const [brandPage, setBrandPage] = useState(0);

  const treeQ = useGetCategoryTreeQuery();
  const brandsQ = useListBrandsQuery({ page: brandPage, size: 20 });
  const [deleteCategory, delCatState] = useDeleteCategoryMutation();
  const [deleteBrand, delBrandState] = useDeleteBrandMutation();
  const [selectedCat, setSelectedCat] = useState<string | null>(null);

  const treeData = treeQ.data?.data ? toTreeNodes(treeQ.data.data) : [];
  const brands = brandsQ.data?.data?.content ?? [];
  const brandTotal = brandsQ.data?.data?.totalElements;

  const err =
    (treeQ.error as { message?: string } | undefined)?.message ??
    (brandsQ.error as { message?: string } | undefined)?.message ??
    (delCatState.error as { message?: string } | undefined)?.message ??
    (delBrandState.error as { message?: string } | undefined)?.message;

  return (
    <div>
      <PageHeader
        title="Danh muc & Thuong hieu"
        subTitle="Cay danh muc + brands (server pagination)"
        extra={
          isAdmin ? (
            <Space>
              <Button type="primary" href="#">
                Tao danh muc
              </Button>
              {selectedCat && (
                <Button
                  danger
                  loading={delCatState.isLoading}
                  onClick={async () => {
                    try {
                      await deleteCategory(selectedCat).unwrap();
                      message.success('Da xoa danh muc');
                      setSelectedCat(null);
                    } catch {
                      message.error('Xoa danh muc that bai');
                    }
                  }}
                >
                  Xoa danh muc da chon
                </Button>
              )}
            </Space>
          ) : undefined
        }
      />
      <ApiErrorAlert message={err ?? null} />
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Card size="small" title="Cay danh muc (GET /api/catalog/categories/tree)">
          {treeQ.isLoading ? (
            <ListSkeleton />
          ) : (
            <Tree
              treeData={treeData}
              selectedKeys={selectedCat ? [selectedCat] : []}
              onSelect={(keys) => setSelectedCat(keys[0] ? String(keys[0]) : null)}
            />
          )}
        </Card>
        <Card size="small" title="Thuong hieu (GET /api/catalog/brands)">
          <DataTable
            columns={[
              { title: 'ID', dataIndex: 'id', key: 'id', width: 120 },
              { title: 'Ten', dataIndex: 'name', key: 'name' },
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
                width: 120,
                render: (_: unknown, r: { id: string }) =>
                  isAdmin ? (
                    <Button
                      danger
                      size="small"
                      loading={delBrandState.isLoading}
                      onClick={async () => {
                        try {
                          await deleteBrand(r.id).unwrap();
                          message.success('Da xoa brand');
                        } catch {
                          message.error('Xoa brand that bai');
                        }
                      }}
                    >
                      Xoa
                    </Button>
                  ) : null,
              },
            ]}
            data={brands}
            loading={brandsQ.isLoading}
            total={brandTotal}
            page={brandPage}
            onPageChange={setBrandPage}
            rowKey="id"
          />
        </Card>
      </Space>
    </div>
  );
}

export default CatalogPage;
