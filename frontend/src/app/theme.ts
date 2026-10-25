import type { ThemeConfig } from 'antd';

// DESIGN_GUIDE §4 — nguon duy nhat cua token. Doi brand = sua file nay.
// Cam mau mac dinh cua AntD trong moi file frontend/src (grep phai 0 hit).
export const themeToken = {
  colorPrimary: '#0F4C81',
  colorSuccess: '#1A7F37',
  colorWarning: '#9A6700',
  colorError: '#C13515',
  colorBgLayout: '#F0F2F5',
  colorBgContainer: '#FFFFFF',
  borderRadius: 8,
  borderRadiusLG: 10,
  fontFamily:
    "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
} as const;

export const themeConfig: ThemeConfig = {
  token: {
    colorPrimary: themeToken.colorPrimary,
    colorSuccess: themeToken.colorSuccess,
    colorWarning: themeToken.colorWarning,
    colorError: themeToken.colorError,
    colorBgLayout: themeToken.colorBgLayout,
    colorBgContainer: themeToken.colorBgContainer,
    borderRadius: themeToken.borderRadius,
    borderRadiusLG: themeToken.borderRadiusLG,
    fontFamily: themeToken.fontFamily,
    colorTextSecondary: '#667085',
    colorBorderSecondary: '#EAECF0',
    fontSizeSM: 12,
    fontSize: 14,
    fontSizeLG: 16,
    lineHeight: 1.5,
    lineHeightLG: 1.4,
    lineHeightSM: 1.6,
  },
};
