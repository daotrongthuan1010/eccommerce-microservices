import Keycloak from 'keycloak-js';

// Keycloak SSO (FE_AUTH_GATEWAY §2): public client ecommerce-app,
// tokens chi giu trong memory (Redux authSlice), KHONG localStorage.
export const keycloakUrl: string =
  (import.meta.env.VITE_KEYCLOAK_URL as string | undefined) ?? 'http://162.4.177.91:7080';

const keycloak = new Keycloak({
  url: keycloakUrl,
  realm: 'ecommerce',
  clientId: 'ecommerce-app',
});

export default keycloak;

let initPromise: Promise<boolean> | null = null;

// Init mot lan duy nhat khi vao /login: onLoad login-required + PKCE,
// khong checkLoginIframe (tranh 3rd-party cookie issue).
// DEV note: FE chay HTTP (http://162.4.177.91:8096) nen WebCrypto `subtle`
// khong co (chi localhost/HTTPS moi la secure context) -> keycloak-js KHONG
// tao duoc PKCE S256 (can subtle.digest). Dung 'plain' cho DEV; khi FE len
// HTTPS thi doi lai 'S256'.
export function initKeycloak(): Promise<boolean> {
  initPromise ??= keycloak.init({
    onLoad: 'login-required',
    pkceMethod: 'plain',
    checkLoginIframe: false,
  });
  return initPromise;
}

// Silent refresh: true neu token con han / vua refresh xong.
export async function refreshKeycloakToken(minValidity = 30): Promise<boolean> {
  try {
    return await keycloak.updateToken(minValidity);
  } catch {
    return false;
  }
}

export function keycloakTokens(): { access_token: string; refresh_token: string } | null {
  if (!keycloak.authenticated || !keycloak.token || !keycloak.refreshToken) return null;
  return { access_token: keycloak.token, refresh_token: keycloak.refreshToken };
}
