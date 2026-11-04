#!/usr/bin/env python3
"""
Load test notification-service: gui HANG TRAM NGHIN mail demo qua Kafka -> notification-service -> Mailpit.

2 che do:
  1. kafka   : publish OrderConfirmed event vao topic `order-events` (dung flow that:
               consumer tao NotificationRequest, worker gui SMTP Mailpit).
               Day la duong duong production: test end-to-end + throughput.
  2. smtp    : ban SMTP thang vao Mailpit (do pure SMTP throughput cua Mailpit,
               khong qua notification-service).

Cach dung:
  python tools/loadtest_notification.py kafka --count 100000 --bootstrap 162.4.177.91:9092
  python tools/loadtest_notification.py smtp  --count 100000 --host 127.0.0.1 --port 1025

Chi dung cho moi truong dev/UAT cua minh (VPS 162.4.177.91) — KHONG nhac muc dich khac.
Kafka client: dung kafka-python (pip install kafka-python-ng). Neu chua co, script tu
fallback sinh JSON lines de ban bang kcat/kafka-console-producer.
"""
import argparse
import json
import random
import string
import sys
import time
import uuid
from datetime import datetime, timezone

ORDER_EVENTS_TOPIC = "order-events"


def gen_event(i: int) -> dict:
    order_code = f"ORD-LOAD-{i:07d}"
    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "OrderConfirmed",
        "orderId": f"load-order-{i}",
        "orderCode": order_code,
        "userId": f"load-user-{i % 1000}",  # 1000 user lap lai -> co trung recipient
        "status": "CONFIRMED",
        "totalAmount": random.randint(10000, 5000000),
        "currency": "VND",
        "paymentMethod": "COD",
        "occurredAt": datetime.now(timezone.utc).isoformat(),
    }


def run_kafka(args) -> None:
    total = args.count
    t0 = time.time()
    try:
        from kafka import KafkaProducer  # kafka-python-ng
    except ImportError:
        # Fallback: xuat JSON lines ra stdout de pipe vao kafka-console-producer
        for i in range(total):
            print(json.dumps(gen_event(i), ensure_ascii=False))
        sys.stderr.write(
            f"[fallback] kafka-python khong co — da xuat {total} JSON lines.\n"
            f"Dung:  python {sys.argv[0]} kafka -n {total} | "
            f"kafka-console-producer --bootstrap-server {args.bootstrap} --topic {ORDER_EVENTS_TOPIC}\n"
        )
        return

    producer = KafkaProducer(
        bootstrap_servers=args.bootstrap,
        value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
        acks=1,  # load test: all lam cham; at-least-once la du cho demo
        linger_ms=50,
        batch_size=64 * 1024,
        compression_type="gzip",  # lz4 can pip package tren Windows; gzip co san
    )
    sent = 0
    report_every = max(1, total // 10)
    for i in range(total):
        producer.send(ORDER_EVENTS_TOPIC, gen_event(i))
        sent += 1
        if sent % report_every == 0:
            elapsed = time.time() - t0
            rate = sent / elapsed
            sys.stderr.write(
                f"[kafka] {sent}/{total} events ({rate:.0f}/s, {elapsed:.1f}s)\n")
            sys.stderr.flush()
    producer.flush()
    elapsed = time.time() - t0
    sys.stderr.write(f"[kafka] XONG {total} events trong {elapsed:.1f}s "
                     f"({total / elapsed:.0f} events/s)\n")


def run_smtp(args) -> None:
    import smtplib
    from email.message import EmailMessage

    total = args.count
    t0 = time.time()
    sent = 0
    report_every = max(1, total // 10)
    # 1 connection SMTP dai de Mailpit khong phai chap nhan handshake tung mail
    with smtplib.SMTP(args.host, args.port, timeout=30) as smtp:
        smtp.ehlo()
        for i in range(total):
            msg = EmailMessage()
            msg["From"] = "loadtest@shop-hatd.online"
            msg["To"] = f"load-user-{i % 1000}@example.com"
            msg["Subject"] = f"Load test mail {i}"
            msg["X-Notification-Business-Key"] = f"load-{i}"
            msg.set_content(f"Day la mail load test so {i}.\n" + "x" * 200)
            smtp.send_message(msg)
            sent += 1
            if sent % report_every == 0:
                elapsed = time.time() - t0
                sys.stderr.write(f"[smtp] {sent}/{total} mails "
                                 f"({sent / elapsed:.0f}/s, {elapsed:.1f}s)\n")
                sys.stderr.flush()
    elapsed = time.time() - t0
    sys.stderr.write(f"[smtp] XONG {total} mails trong {elapsed:.1f}s "
                     f"({total / elapsed:.0f} mails/s)\n")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="mode", required=True)

    p_kafka = sub.add_parser("kafka", help="Publish OrderConfirmed qua Kafka")
    p_kafka.add_argument("-n", "--count", type=int, default=100_000)
    p_kafka.add_argument("--bootstrap", default="162.4.177.91:9092")

    p_smtp = sub.add_parser("smtp", help="Ban SMTP thang vao Mailpit")
    p_smtp.add_argument("-n", "--count", type=int, default=100_000)
    p_smtp.add_argument("--host", default="127.0.0.1")
    p_smtp.add_argument("--port", type=int, default=1025)

    args = parser.parse_args()
    if args.mode == "kafka":
        run_kafka(args)
    else:
        run_smtp(args)


if __name__ == "__main__":
    main()
