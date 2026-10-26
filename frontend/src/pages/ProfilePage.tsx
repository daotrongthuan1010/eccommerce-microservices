import { useEffect, useState } from 'react';
import { App, Button, Card, Col, Descriptions, Empty, Form, Input, Modal, Row, Space, Tag, Typography, Upload } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { useAppSelector } from '../app/hooks';
import { ApiErrorAlert, ListSkeleton, RoleTags, StatusTag, UserAvatar } from '../components/ui';
import { toProfile, useGetMeQuery, useUpdateAvatarMutation, useUpdateProfileMutation } from '../features/user/userApi';
import { useLazyFileUrlQuery, useUploadMutation } from '../features/file/fileApi';
import { cropToSquare } from '../utils/cropImage';

// /account — ho so ca nhan tu GET /auth/me (enriched, map qua toProfile).
// Banner nhan dien (avatar that tu picture → file-service URL, fallback chu
// cai) + form sua ho so (PUT /auth/me/profile) + upload avatar
// (file-service POST /file/files → PUT /auth/me/avatar).
export default function ProfilePage() {
  const { message } = App.useApp();
  const { data, isLoading, isFetching, isError, error, refetch } = useGetMeQuery();
  const profile = toProfile(data);
  const roles = useAppSelector((s) => s.auth.roles);
  const isAdmin = roles.includes('ADMIN');

  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [editOpen, setEditOpen] = useState(false);
  const [form] = Form.useForm();

  const [triggerFileUrl] = useLazyFileUrlQuery();
  const [uploadFile, uploadState] = useUploadMutation();
  const [saveAvatar, avatarState] = useUpdateAvatarMutation();
  const [saveProfile, profileState] = useUpdateProfileMutation();

  // Resolve file-key picture → URL tuoi tho ngan moi lan picture doi.
  useEffect(() => {
    let cancelled = false;
    setAvatarUrl(null);
    if (!profile?.picture) return;
    triggerFileUrl({ key: profile.picture })
      .unwrap()
      .then((res) => {
        if (!cancelled) setAvatarUrl(res.data?.url ?? null);
      })
      .catch(() => {
        if (!cancelled) setAvatarUrl(null);
      });
    return () => {
      cancelled = true;
    };
  }, [profile?.picture, triggerFileUrl]);

  if (isLoading) {
    return (
      <div style={{ maxWidth: 880, margin: '0 auto' }}>
        <ListSkeleton />
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div style={{ maxWidth: 880, margin: '0 auto' }}>
        <ApiErrorAlert
          message={`Không tải được thông tin hồ sơ: ${(error as { message?: string } | undefined)?.message ?? 'lỗi không xác định'}`}
        />
        <Card>
          <Empty description={isError ? 'Không tải được hồ sơ' : 'Chưa có thông tin hồ sơ'}>
            <Button type="primary" loading={isFetching} onClick={() => refetch()}>
              Thử lại
            </Button>
          </Empty>
        </Card>
      </div>
    );
  }

  const displayName = profile.fullName !== '—' ? profile.fullName : profile.username;

  // Client validation truoc upload: chi nhan anh, toi da 2MB.
  const MAX_AVATAR_BYTES = 2 * 1024 * 1024;

  const onAvatarFile = async (file: File) => {
    try {
      const square = await cropToSquare(file);
      const up = await uploadFile({ file: square, folder: 'avatars' }).unwrap();
      const key = up.data?.key;
      if (!key) {
        message.error('Upload ảnh thất bại: không nhận được file key');
        return;
      }
      await saveAvatar({ fileKey: key }).unwrap();
      message.success('Đã cập nhật ảnh đại diện');
    } catch {
      message.error('Cập nhật ảnh đại diện thất bại');
    }
  };

  const onSaveProfile = async () => {
    try {
      const values = await form.validateFields();
      await saveProfile({
        firstName: values.firstName?.trim() || undefined,
        lastName: values.lastName?.trim() || undefined,
        email: values.email?.trim() || undefined,
      }).unwrap();
      message.success('Đã lưu hồ sơ');
      setEditOpen(false);
    } catch {
      // validateFields loi → form tu hien thi; mutation loi → bao chung
      if (profileState.error) message.error('Lưu hồ sơ thất bại');
    }
  };

  const saving = uploadState.isLoading || avatarState.isLoading;

  return (
    <div style={{ maxWidth: 880, margin: '0 auto' }}>
      <Card
        loading={isFetching}
        style={{ marginBottom: 16 }}
      >
        <Space align="start" size={20}>
          <UserAvatar name={profile.username} picture={avatarUrl} size={72} />
          <div>
            <Space align="baseline" size={8}>
              <Typography.Title level={3} style={{ margin: 0 }}>
                {displayName}
              </Typography.Title>
              <StatusTag status={profile.enabled ? 'ENABLED' : 'DISABLED'} />
            </Space>
            <div>
              <Typography.Text type="secondary">
                @{profile.username} · {profile.email}
              </Typography.Text>
            </div>
            <div style={{ marginTop: 10 }}>
              <Space wrap size={[6, 6]}>
                <RoleTags roles={profile.roles} admin={profile.admin} />
                {profile.emailVerified !== null && (
                  <Tag color={profile.emailVerified ? 'green' : 'orange'}>
                    {profile.emailVerified ? 'Đã xác thực email' : 'Chưa xác thực email'}
                  </Tag>
                )}
              </Space>
            </div>
            <div style={{ marginTop: 12 }}>
              <Upload
                accept="image/*"
                showUploadList={false}
                beforeUpload={(file) => {
                  if (!file.type.startsWith('image/')) {
                    message.error('Chỉ chấp nhận file ảnh');
                    return false;
                  }
                  if (file.size > MAX_AVATAR_BYTES) {
                    message.error('Ảnh đại diện tối đa 2MB');
                    return false;
                  }
                  void onAvatarFile(file as File);
                  return false;
                }}
              >
                <Button icon={<UploadOutlined />} loading={saving}>
                  {profile.picture ? 'Đổi ảnh đại diện' : 'Tải ảnh đại diện'}
                </Button>
              </Upload>
              {profile.picture && (
                <Button
                  type="link"
                  danger
                  loading={saving}
                  style={{ marginTop: 8, padding: 0 }}
                  onClick={async () => {
                    try {
                      await saveAvatar({}).unwrap();
                      message.success('Đã xóa ảnh đại diện');
                    } catch {
                      message.error('Xóa ảnh đại diện thất bại');
                    }
                  }}
                >
                  Xóa ảnh
                </Button>
              )}
            </div>
          </div>
        </Space>
      </Card>

      <Row gutter={16}>
        <Col xs={24} md={14}>
          <Card
            title="Thông tin tài khoản"
            extra={<Button type="link" onClick={() => {
              form.setFieldsValue({
                firstName: profile.firstName ?? '',
                lastName: profile.lastName ?? '',
                email: profile.email !== '—' ? profile.email : '',
              });
              setEditOpen(true);
            }}>Sửa</Button>}
            style={{ marginBottom: 16 }}
          >
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Tên đăng nhập">{profile.username}</Descriptions.Item>
              <Descriptions.Item label="Họ tên">{profile.fullName}</Descriptions.Item>
              <Descriptions.Item label="Email">{profile.email}</Descriptions.Item>
              <Descriptions.Item label="Subject (Keycloak)">{profile.subject}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <StatusTag status={profile.enabled ? 'ENABLED' : 'DISABLED'} />
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>
        <Col xs={24} md={10}>
          <Card title="Thao tác nhanh">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Link to="/change-password" style={{ width: '100%' }}>
                <Button type="primary" block>
                  Đổi mật khẩu
                </Button>
              </Link>
              {isAdmin && (
                <>
                  <Link to="/admin/users" style={{ width: '100%' }}>
                    <Button block>Quản trị người dùng</Button>
                  </Link>
                  <Link to="/admin/roles" style={{ width: '100%' }}>
                    <Button block>Quản trị vai trò</Button>
                  </Link>
                </>
              )}
            </Space>
          </Card>
        </Col>
      </Row>

      <Modal
        title="Sửa hồ sơ"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={onSaveProfile}
        confirmLoading={profileState.isLoading}
        okText="Lưu"
        cancelText="Hủy"
        destroyOnClose
      >
        <ApiErrorAlert message={(profileState.error as { message?: string } | undefined)?.message ?? null} />
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="firstName" label="Tên" rules={[{ max: 64, message: 'Tối đa 64 ký tự' }]}>
            <Input placeholder="Tên" />
          </Form.Item>
          <Form.Item name="lastName" label="Họ" rules={[{ max: 64, message: 'Tối đa 64 ký tự' }]}>
            <Input placeholder="Họ" />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}>
            <Input placeholder="Email" />
          </Form.Item>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Đổi email sẽ đặt lại trạng thái xác thực — cần xác thực lại sau khi lưu.
          </Typography.Text>
        </Form>
      </Modal>
    </div>
  );
}
