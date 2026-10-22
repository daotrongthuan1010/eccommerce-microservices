import { Card, Descriptions, Tag } from 'antd';
import { ApiErrorAlert, ListSkeleton, PageHeader } from '../components/ui';
import { useGetMeQuery } from '../features/user/userApi';

// FE_API_CONTRACT §2: GET /api/user/me — cung shape voi GET /api/auth/me.
export default function ProfilePage() {
  const { data, isLoading, error } = useGetMeQuery();
  const me = data?.data;

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <PageHeader title="Hồ sơ" subTitle="Thông tin tài khoản" />
      {isLoading ? (
        <ListSkeleton />
      ) : (
        <>
          <ApiErrorAlert
            message={error ? 'Không tải được thông tin hồ sơ' : null}
          />
          {me && (
            <Card>
              <Descriptions column={1} bordered size="small">
                <Descriptions.Item label="Tên đăng nhập">{me.username}</Descriptions.Item>
                {me.email && <Descriptions.Item label="Email">{me.email}</Descriptions.Item>}
                <Descriptions.Item label="Subject">{me.subject}</Descriptions.Item>
                <Descriptions.Item label="Quyền">
                  {(me.roles ?? []).map((r) => (
                    <Tag key={r} color={r === 'ADMIN' ? 'red' : 'blue'}>
                      {r}
                    </Tag>
                  ))}
                  {me.admin && <Tag color="red">ADMIN (admin flag)</Tag>}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
