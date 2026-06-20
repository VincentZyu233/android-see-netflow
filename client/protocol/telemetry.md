# Telemetry protocol

The Android client will send one JSON object per sample frame over WebSocket.

```json
{
  "device_id": "pixel-7a",
  "device_name": "Pixel 7a",
  "timestamp": 1781922334123,
  "network_type": "wifi",
  "interfaces": [
    {
      "name": "wlan0",
      "rx_bytes": 123456789,
      "tx_bytes": 45678901,
      "rx_rate": 182340,
      "tx_rate": 92341
    }
  ]
}
```

## Notes

- `timestamp` uses Unix epoch milliseconds.
- `rx_rate` and `tx_rate` are bytes per second.
- `interfaces` may later include cellular interfaces like `rmnet_data0`.
