from __future__ import annotations

import asyncio
import json
import random
import time

import websockets


async def main() -> None:
    rx_total = 100_000_000
    tx_total = 50_000_000
    async with websockets.connect("ws://127.0.0.1:8765") as websocket:
        while True:
            rx_rate = random.randint(50_000, 900_000)
            tx_rate = random.randint(30_000, 400_000)
            rx_total += rx_rate
            tx_total += tx_rate
            payload = {
                "device_id": "mock-pixel",
                "device_name": "Mock Pixel",
                "timestamp": int(time.time() * 1000),
                "network_type": "wifi",
                "interfaces": [
                    {
                        "name": "wlan0",
                        "rx_bytes": rx_total,
                        "tx_bytes": tx_total,
                        "rx_rate": rx_rate,
                        "tx_rate": tx_rate,
                    }
                ],
            }
            await websocket.send(json.dumps(payload))
            await asyncio.sleep(1)


if __name__ == "__main__":
    asyncio.run(main())
