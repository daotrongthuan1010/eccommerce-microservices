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

// Polyfill crypto.randomUUID cho DEV HTTP: keycloak-js 26 dung randomUUID de
// sinh state/nonce, nhung API nay chi co tren secure context (localhost/HTTPS)
// -> http://IP nem "Web Crypto API is not available". Dung getRandomValues
// (van co tren HTTP) de tao UUIDv4. Len HTTPS thi polyfill tu vo hieu.
const cryptoAny = (
  typeof crypto !== 'undefined' ? crypto : undefined
) as unknown as { randomUUID?: () => string; getRandomValues?: (a: Uint8Array) => Uint8Array } | undefined;
if (cryptoAny && typeof cryptoAny.randomUUID === 'undefined' && cryptoAny.getRandomValues) {
  const getRandomValues = cryptoAny.getRandomValues.bind(crypto);
  cryptoAny.randomUUID = () => {
    const b = getRandomValues(new Uint8Array(16));
    b[6] = (b[6] & 0x0f) | 0x40; // version 4
    b[8] = (b[8] & 0x3f) | 0x80; // variant RFC4122
    const h = Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');
    return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
  };
}
// DEV note: FE chay HTTP (http://162.4.177.91:8096) nen WebCrypto `subtle`
// khong co (chi localhost/HTTPS moi la secure context) -> keycloak-js 26
// KHONG tao duoc PKCE S256 (can subtle.digest, adapter 26 chi co S256/false).
// Tat PKCE cho DEV (pkceMethod: false); khi FE len HTTPS thi bat lai 'S256'.
export function initKeycloak(): Promise<boolean> {
  initPromise ??= keycloak.init({
    onLoad: 'login-required',
    pkceMethod: false,
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
