import { App, Button, Card, Col, InputNumber, Rate, Row, Space, Typography } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useGetProductQuery, useListVariantsQuery } from '../features/product/productApi';
import {
  useGetProductRatingQuery,
  useListProductReviewsQuery,
} from '../features/marketplace/browseApi';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { addItem } from '../features/marketplace/guestCartSlice';
import { useAddToCartMutation } from '../features/order/orderApi';
import { ApiErrorAlert, ListSkeleton, StatusTag } from '../components/ui';
import { formatVnd } from '../components/marketplace/ProductCardGrid';

// Marketplace PDP (2026-11-05 fe-marketplace D2, spec customer/04): guest xem
// duoc day du; "Them vao gio" di vao guest cart (chua login) hoac cart server
// (da login — guest cart tu merge o MarketplaceLayout). Gia la gia TAM TINH
// hien tai; tong that do BE tinh khi checkout (AGENTS.md luat 3).
export function ProductDetailPage() {
  const { id: productId = '' } = useParams<{ id: string }>();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const username = useAppSelector((s) => s.auth.username);
  const [quantity, setQuantity] = useState(1);

  const detailQ = useGetProductQuery(productId, { skip: !productId });
  const variantsQ = useListVariantsQuery(productId, { skip: !productId });
  const ratingQ = useGetProductRatingQuery(productId, { skip: !productId });
  const reviewsQ = useListProductReviewsQuery({ productId, size: 5 }, { skip: !productId });

  const product = detailQ.data?.data;
  const variants = variantsQ.data?.data ?? [];
  const rating = ratingQ.data?.data as
    | { totalReviews?: number; averageRating?: number }
    | undefined;
  // Envelope: PageResponse { content } (ReviewQueryController tra PageResponse).
  const reviews = ((reviewsQ.data?.data as { content?: unknown[] } | undefined)?.content
    ?? []) as {
    displayUserName?: string;
    rating?: number;
    comment?: string;
    createdAt?: string;
  }[];

  const revision = (product as { currentRevision?: { name?: string; shortDescription?: string; description?: string } } | undefined)?.currentRevision;
  const name = revision?.name ?? String(product?.productCode ?? productId);

  const err =
    (detailQ.error as { message?: string } | undefined)?.message ??
    (variantsQ.error as { message?: string } | undefined)?.message;

  // Gia tam tinh: lay tu price-schedules cua SKU dau tien (P2 se lam chon
  // variant). PDP nay hien thi gia tham chieu tu browse card khi co.
  const priceFrom = useMemo(() => {
    const rev = product as unknown as { priceFrom?: number } | undefined;
    return rev?.priceFrom ?? 0;
  }, [product]);

  const [addToCartServer] = useAddToCartMutation();

  const onAddToCart = async () => {
    // Da login: them thang vao cart server (nguon su that). Guest: guest cart
    // localStorage (merge len server sau login o MarketplaceLayout).
    if (username) {
      try {
        await addToCartServer({
          skuId: productId,
          productName: name,
          unitPrice: priceFrom,
          quantity,
        }).unwrap();
        message.success(`Đã thêm "${name}" vào giỏ hàng`);
      } catch (e) {
        message.error((e as { message?: string }).message ?? 'Thêm vào giỏ hàng thất bại');
      }
      return;
    }
    dispatch(
      addItem({
        // P1 retail: cart item = product id (SKU don chua co variant selector).
        skuId: productId,
        productName: name,
        unitPrice: priceFrom,
        quantity,
      }),
    );
    message.success(`Đã thêm "${name}" vào giỏ hàng`);
  };

  const onBuyNow = async () => {
    await onAddToCart();
    navigate(username ? '/checkout' : '/login');
  };

  return (
    <div>
      <ApiErrorAlert message={err ?? null} />
      {detailQ.isLoading ? (
        <ListSkeleton />
      ) : (
        <Row gutter={[24, 24]}>
          <Col xs={24} md={10}>
            <Card
              cover={
                <div
                  style={{
                    height: 360,
                    background: '#f5f5f5',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 72,
                  }}
                >
                  🛍️
                </div>
              }
              styles={{ body: { padding: 12 } }}
            >
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Typography.Text type="secondary">{String(product?.productCode ?? '')}</Typography.Text>
                {product?.status ? <StatusTag status={String(product.status)} /> : null}
                {variants.length > 0 && (
                  <Typography.Text type="secondary">
                    {variants.length} biến thể — chọn phiên bản sẽ có ở P2
                  </Typography.Text>
                )}
              </Space>
            </Card>
          </Col>
          <Col xs={24} md={14}>
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Typography.Title level={3} style={{ marginTop: 0 }}>
                {name}
              </Typography.Title>
              <Space size={16} align="center">
                <Typography.Text strong style={{ color: '#e8543f', fontSize: 24 }}>
                  {priceFrom > 0 ? formatVnd(priceFrom) : 'Liên hệ'}
                </Typography.Text>
                {rating && (rating.totalReviews ?? 0) > 0 ? (
                  <Space size={8}>
                    <Rate disabled value={Math.round(rating.averageRating ?? 0)} />
                    <Typography.Text type="secondary">
                      {rating.averageRating?.toFixed(1)} ({rating.totalReviews} đánh giá)
                    </Typography.Text>
                  </Space>
                ) : (
                  <Typography.Text type="secondary">Chưa có đánh giá</Typography.Text>
                )}
              </Space>
              {revision?.shortDescription && (
                <Typography.Paragraph type="secondary">
                  {revision.shortDescription}
                </Typography.Paragraph>
              )}
              <Space size={12} wrap>
                <InputNumber
                  min={1}
                  max={999}
                  value={quantity}
                  onChange={(v) => setQuantity(v ?? 1)}
                />
                <Button
                  type="primary"
                  size="large"
                  icon={<ShoppingCartOutlined />}
                  onClick={onAddToCart}
                >
                  Thêm vào giỏ
                </Button>
                <Button size="large" danger onClick={onBuyNow}>
                  Mua ngay
                </Button>
              </Space>
              <Card size="small" title="Mô tả sản phẩm" style={{ marginTop: 8 }}>
                <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                  {revision?.description ?? revision?.shortDescription ?? 'Chưa có mô tả.'}
                </Typography.Paragraph>
              </Card>
            </Space>
          </Col>
          <Col span={24}>
            <Card size="small" title={`Đánh giá sản phẩm (${reviews.length})`}>
              {reviews.length === 0 ? (
                <Typography.Text type="secondary">Chưa có đánh giá nào.</Typography.Text>
              ) : (
                <Space direction="vertical" style={{ width: '100%' }}>
                  {reviews.map((r, i) => (
                    <Card key={i} size="small">
                      <Space direction="vertical" size={4} style={{ width: '100%' }}>
                        <Space size={12}>
                          <Typography.Text strong>
                            {r.displayUserName || 'Khách hàng'}
                          </Typography.Text>
                          <Rate disabled value={r.rating ?? 0} />
                        </Space>
                        {r.comment && (
                          <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                            {r.comment}
                          </Typography.Paragraph>
                        )}
                      </Space>
                    </Card>
                  ))}
                </Space>
              )}
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
}

export default ProductDetailPage;
