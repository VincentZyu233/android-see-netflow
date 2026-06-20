from __future__ import annotations

import asyncio
import json
import time
from collections import deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml
from textual.app import App, ComposeResult
from textual.containers import Container, Horizontal
from textual.reactive import reactive
from textual.widgets import DataTable, Footer, Header, Static
import websockets


CONFIG_PATH = Path("config.yaml")
CONFIG_EXAMPLE_PATH = Path("config.example.yaml")
STALE_AFTER_SECS = 10.0


def load_config() -> dict[str, Any]:
    if not CONFIG_PATH.exists():
        raise SystemExit(
            "Missing config.yaml. Copy config.example.yaml to config.yaml and edit it before starting the server."
        )

    with CONFIG_PATH.open("r", encoding="utf-8") as handle:
        return yaml.safe_load(handle) or {}


def format_rate(value: int | float) -> str:
    units = ["B/s", "KB/s", "MB/s", "GB/s"]
    size = float(value)
    for unit in units:
        if size < 1024 or unit == units[-1]:
            return f"{size:,.1f} {unit}"
        size /= 1024
    return f"{size:,.1f} GB/s"


def format_bytes(value: int | float) -> str:
    units = ["B", "KB", "MB", "GB", "TB"]
    size = float(value)
    for unit in units:
        if size < 1024 or unit == units[-1]:
            return f"{size:,.1f} {unit}"
        size /= 1024
    return f"{size:,.1f} TB"


@dataclass
class InterfaceStats:
    name: str
    rx_bytes: int
    tx_bytes: int
    rx_rate: int
    tx_rate: int


@dataclass
class DeviceSnapshot:
    device_id: str
    device_name: str
    network_type: str
    timestamp: int
    updated_at: float
    interfaces: list[InterfaceStats] = field(default_factory=list)
    history: deque[dict[str, Any]] = field(default_factory=lambda: deque(maxlen=240))

    @property
    def total_rx_rate(self) -> int:
        return sum(item.rx_rate for item in self.interfaces)

    @property
    def total_tx_rate(self) -> int:
        return sum(item.tx_rate for item in self.interfaces)

    @property
    def total_rx_bytes(self) -> int:
        return sum(item.rx_bytes for item in self.interfaces)

    @property
    def total_tx_bytes(self) -> int:
        return sum(item.tx_bytes for item in self.interfaces)

    @property
    def primary_interface(self) -> str:
        return self.interfaces[0].name if self.interfaces else "unknown"


class DeviceStore:
    def __init__(self, history_limit: int) -> None:
        self.history_limit = history_limit
        self._devices: dict[str, DeviceSnapshot] = {}
        self._lock = asyncio.Lock()

    async def upsert(self, payload: dict[str, Any]) -> None:
        validated = validate_payload(payload)
        interfaces = [
            InterfaceStats(
                name=str(item.get("name", "")),
                rx_bytes=int(item.get("rx_bytes", 0)),
                tx_bytes=int(item.get("tx_bytes", 0)),
                rx_rate=int(item.get("rx_rate", 0)),
                tx_rate=int(item.get("tx_rate", 0)),
            )
            for item in validated["interfaces"]
        ]
        device_id = str(validated["device_id"])
        now = time.time()
        async with self._lock:
            snapshot = self._devices.get(device_id)
            if snapshot is None:
                snapshot = DeviceSnapshot(
                    device_id=device_id,
                    device_name=str(validated["device_name"]),
                    network_type=str(validated["network_type"]),
                    timestamp=int(validated["timestamp"]),
                    updated_at=now,
                    interfaces=interfaces,
                    history=deque(maxlen=self.history_limit),
                )
                self._devices[device_id] = snapshot
            else:
                snapshot.device_name = str(validated["device_name"])
                snapshot.network_type = str(validated["network_type"])
                snapshot.timestamp = int(validated["timestamp"])
                snapshot.updated_at = now
                snapshot.interfaces = interfaces

            snapshot.history.append(
                {
                    "timestamp": snapshot.timestamp,
                    "rx_rate": snapshot.total_rx_rate,
                    "tx_rate": snapshot.total_tx_rate,
                }
            )

    async def snapshot(self) -> list[DeviceSnapshot]:
        async with self._lock:
            devices = list(self._devices.values())
        return sorted(devices, key=lambda item: item.device_name.lower())


def validate_payload(payload: dict[str, Any]) -> dict[str, Any]:
    interfaces = payload.get("interfaces")
    if not isinstance(interfaces, list) or not interfaces:
        raise ValueError("payload.interfaces must be a non-empty list")

    device_id = str(payload.get("device_id", "")).strip()
    device_name = str(payload.get("device_name", "")).strip()
    network_type = str(payload.get("network_type", "unknown")).strip() or "unknown"
    timestamp = int(payload.get("timestamp", 0))

    if not device_id:
        raise ValueError("payload.device_id is required")
    if not device_name:
        device_name = device_id

    normalized_interfaces: list[dict[str, Any]] = []
    for item in interfaces:
        if not isinstance(item, dict):
            raise ValueError("payload.interfaces entries must be objects")
        name = str(item.get("name", "")).strip() or "unknown"
        normalized_interfaces.append(
            {
                "name": name,
                "rx_bytes": int(item.get("rx_bytes", 0)),
                "tx_bytes": int(item.get("tx_bytes", 0)),
                "rx_rate": int(item.get("rx_rate", 0)),
                "tx_rate": int(item.get("tx_rate", 0)),
            }
        )

    return {
        "device_id": device_id,
        "device_name": device_name,
        "network_type": network_type,
        "timestamp": timestamp,
        "interfaces": normalized_interfaces,
    }


class SummaryPanel(Static):
    text = reactive("Waiting for devices...")

    def render(self) -> str:
        return self.text


class NetflowApp(App[None]):
    CSS = """
    Screen {
        layout: vertical;
    }

    #body {
        height: 1fr;
    }

    #summary {
        width: 36;
        padding: 1 2;
        border: solid $accent;
    }

    #devices {
        width: 1fr;
    }
    """

    def __init__(self, store: DeviceStore, title: str, refresh_interval_ms: int) -> None:
        super().__init__()
        self.store = store
        self.title = title
        self.refresh_interval = refresh_interval_ms / 1000

    def compose(self) -> ComposeResult:
        yield Header(show_clock=True)
        with Horizontal(id="body"):
            yield SummaryPanel(id="summary")
            yield DataTable(id="devices")
        yield Footer()

    def on_mount(self) -> None:
        table = self.query_one(DataTable)
        table.add_columns(
            "Device",
            "Network",
            "Interface",
            "RX Rate",
            "TX Rate",
            "RX Total",
            "TX Total",
            "State",
        )
        self.set_interval(self.refresh_interval, self.refresh_data)

    async def refresh_data(self) -> None:
        devices = await self.store.snapshot()
        table = self.query_one(DataTable)
        table.clear()

        total_rx = 0
        total_tx = 0
        for device in devices:
            total_rx += device.total_rx_rate
            total_tx += device.total_tx_rate
            age = max(0.0, time.time() - device.updated_at)
            state = "stale" if age > STALE_AFTER_SECS else f"{age:,.1f}s ago"
            table.add_row(
                device.device_name,
                device.network_type,
                device.primary_interface,
                format_rate(device.total_rx_rate),
                format_rate(device.total_tx_rate),
                format_bytes(device.total_rx_bytes),
                format_bytes(device.total_tx_bytes),
                state,
            )

        summary = self.query_one(SummaryPanel)
        summary.text = (
            f"{self.title}\n\n"
            f"Connected devices: {len(devices)}\n"
            f"Aggregate RX: {format_rate(total_rx)}\n"
            f"Aggregate TX: {format_rate(total_tx)}\n"
            f"Stale threshold: {STALE_AFTER_SECS:.0f}s\n"
            f"Config: {CONFIG_PATH.resolve()}"
        )


async def websocket_handler(store: DeviceStore, websocket: websockets.WebSocketServerProtocol) -> None:
    async for raw_message in websocket:
        try:
            payload = json.loads(raw_message)
        except json.JSONDecodeError:
            continue
        if not isinstance(payload, dict):
            continue
        try:
            await store.upsert(payload)
        except ValueError:
            continue


async def start_websocket_server(store: DeviceStore, host: str, port: int) -> websockets.server.Serve:
    return await websockets.serve(lambda ws: websocket_handler(store, ws), host, port)


async def main() -> None:
    config = load_config()
    server_cfg = config.get("server", {})
    ui_cfg = config.get("ui", {})
    store = DeviceStore(history_limit=int(server_cfg.get("history_limit", 240)))

    ws_server = await start_websocket_server(
        store=store,
        host=str(server_cfg.get("host", "0.0.0.0")),
        port=int(server_cfg.get("port", 8765)),
    )
    app = NetflowApp(
        store=store,
        title=str(ui_cfg.get("title", "Android See Netflow")),
        refresh_interval_ms=int(ui_cfg.get("refresh_interval_ms", 500)),
    )
    try:
        await app.run_async()
    finally:
        ws_server.close()
        await ws_server.wait_closed()


if __name__ == "__main__":
    asyncio.run(main())
