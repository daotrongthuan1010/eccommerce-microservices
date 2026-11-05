import type { ThemeConfig } from 'antd';

// DESIGN_GUIDE §4 — nguon duy nhat cua token. Doi brand = sua file nay.
// 2026-11-05 fe-marketplace D7: shopper marketplace dung tông do cam
// (#e8543f — cam hoi Shopee nhung nhan dien rieng, khong copy y hech).
// 2026-11-05 brand-logo: backoffice chuan hoa theo logo du an (logo.png):
// navy dam #00214f + accent vang #f89818 — thay cho #0F4C81 cu.
export const marketplaceToken = {
  colorPrimary: '#e8543f',
  colorSuccess: '#1A7F37',
  colorWarning: '#9A6700',
  colorError: '#C13515',
  colorBgLayout: '#F5F5F5',
  colorBgContainer: '#FFFFFF',
  borderRadius: 8,
  borderRadiusLG: 10,
  fontFamily:
    "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
} as const;

export const themeToken = {
  // Navy dam trich tu logo HTAD (512x512: #00214f chu dao + #f89818 accent).
  colorPrimary: '#00214f',
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

/** Accent vang cam tu logo — dung highlight nho (badge, tag khuyen mai). */
export const brandAccent = '#f89818' as const;

const baseTokens = {
  colorSuccess: themeToken.colorSuccess,
  colorWarning: themeToken.colorWarning,
  colorError: themeToken.colorError,
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
};

// Shopper (MarketplaceLayout): do cam.
export const marketplaceThemeConfig: ThemeConfig = {
  token: {
    ...baseTokens,
    colorPrimary: marketplaceToken.colorPrimary,
    colorBgLayout: marketplaceToken.colorBgLayout,
    colorBgContainer: marketplaceToken.colorBgContainer,
  },
};

// Backoffice (AppLayout): xanh navy goc — giu nguyen DESIGN_GUIDE.
export const themeConfig: ThemeConfig = {
  token: {
    ...baseTokens,
    colorPrimary: themeToken.colorPrimary,
    colorBgLayout: themeToken.colorBgLayout,
    colorBgContainer: themeToken.colorBgContainer,
  },
};
