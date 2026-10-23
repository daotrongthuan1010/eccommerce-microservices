import { Button, Card, Descriptions, Empty, Tag } from 'antd';
import { Link } from 'react-router-dom';
import { ApiErrorAlert, ListSkeleton, PageHeader, StatusTag } from '../components/ui';
import { toProfile, useGetMeQuery } from '../features/user/userApi';

// FE_API_CONTRACT §2: GET /api/user/me — cung shape voi GET /api/auth/me.
// Server tra raw Map (khong envelope); map qua toProfile voi defensive
// defaults de khong bao gio render trang.
export default function ProfilePage() {
  const { data, isLoading, isFetching, isError, error, refetch } = useGetMeQuery();
  const profile = toProfile(data);

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <PageHeader
        title="Hồ sơ"
        subTitle="Thông tin tài khoản"
        extra={<Link to="/change-password"><Button>Đổi mật khẩu</Button></Link>}
      />
      {isLoading ? (
        <ListSkeleton />
      ) : isError ? (
        <>
          <ApiErrorAlert
            message={`Không tải được thông tin hồ sơ: ${(error as { message?: string } | undefined)?.message ?? 'lỗi không xác định'}`}
          />
          <Card>
            <Empty description="Không tải được hồ sơ">
              <Button type="primary" loading={isFetching} onClick={() => refetch()}>
                Thử lại
              </Button>
            </Empty>
          </Card>
        </>
      ) : !profile ? (
        <Card>
          <Empty description="Chưa có thông tin hồ sơ">
            <Button loading={isFetching} onClick={() => refetch()}>
              Tải lại
            </Button>
          </Empty>
        </Card>
      ) : (
        <Card loading={isFetching}>
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="Tên đăng nhập">{profile.username}</Descriptions.Item>
            <Descriptions.Item label="Họ tên">{profile.fullName}</Descriptions.Item>
            <Descriptions.Item label="Email">{profile.email}</Descriptions.Item>
            <Descriptions.Item label="Subject">{profile.subject}</Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <StatusTag status={profile.enabled ? 'ENABLED' : 'DISABLED'} />
              {profile.emailVerified !== null && (
                <Tag color={profile.emailVerified ? 'green' : 'orange'} style={{ marginLeft: 8 }}>
                  {profile.emailVerified ? 'Đã xác thực email' : 'Chưa xác thực email'}
                </Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Quyền">
              {profile.roles.length === 0 ? (
                '—'
              ) : (
                profile.roles.map((r) => (
                  <Tag key={r} color={r === 'ADMIN' ? 'red' : 'blue'}>
                    {r}
                  </Tag>
                ))
              )}
              {profile.admin && !profile.roles.includes('ADMIN') && (
                <Tag color="red">ADMIN (admin flag)</Tag>
              )}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      )}
    </div>
  );
}
