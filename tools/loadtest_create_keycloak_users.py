#!/usr/bin/env python3
"""
Tao N user demo trong Keycloak de load test notification-service co mail that.
Dung Keycloak Admin API (service account ecommerce-admin, giong auth-service).

Cach dung:
  python tools/loadtest_create_keycloak_users.py --count 1000 \
      --server http://162.4.177.91:7080 --secret <KEYCLOAK_ADMIN_CLIENT_SECRET>

User tao dang: loadtest-user-{i} / loadtest-{i}@example.com, enabled, khong can email verify.
Chi dung cho VPS dev/UAT cua minh.
"""
import argparse
import json
import sys
import urllib.request

def get_token(server: str, client_id: str, secret: str, realm: str = "master") -> str:
    data = f"grant_type=client_credentials&client_id={client_id}&client_secret={secret}".encode()
    req = urllib.request.Request(
        f"{server}/realms/{realm}/protocol/openid-connect/token",
        data=data,
        headers={"Content-Type": "application/x-www-form-urlencoded"})
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.load(resp)["access_token"]

def create_user(server: str, token: str, i: int) -> str:
    """Tao user, tra userId (UUID) de load test event dung dung id."""
    body = json.dumps({
        "username": f"loadtest-user-{i}",
        "email": f"loadtest-{i}@example.com",
        "enabled": True,
        "emailVerified": True,
    }).encode()
    req = urllib.request.Request(
        f"{server}/admin/realms/ecommerce/users",
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        })
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            location = resp.headers.get("Location", "")
            # Location: .../users/{uuid}
            return location.rstrip("/").split("/")[-1] if location else ""
    except urllib.error.HTTPError as e:
        if e.code == 409:
            # da ton tai — tra lai id hien co
            req2 = urllib.request.Request(
                f"{server}/admin/realms/ecommerce/users?username=loadtest-user-{i}",
                headers={"Authorization": f"Bearer {token}"})
            with urllib.request.urlopen(req2, timeout=15) as resp2:
                users = json.load(resp2)
                return users[0]["id"] if users else ""
        raise

def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("-n", "--count", type=int, default=1000)
    p.add_argument("--server", default="http://162.4.177.91:7080")
    p.add_argument("--client-id", default="ecommerce-admin")
    p.add_argument("--secret", required=True)
    p.add_argument("--out", default="tools/loadtest_user_ids.json",
                   help="Ghi danh sach userId de load test dung")
    args = p.parse_args()

    token = get_token(args.server, args.client_id, args.secret)
    ids = []
    for i in range(args.count):
        uid = create_user(args.server, token, i)
        if uid:
            ids.append(uid)
        if (i + 1) % 100 == 0:
            print(f"[keycloak] {i + 1}/{args.count} users", file=sys.stderr)
    with open(args.out, "w") as f:
        json.dump(ids, f)
    print(f"XONG: {len(ids)}/{args.count} users, userIds ghi vao {args.out}")

if __name__ == "__main__":
    main()
