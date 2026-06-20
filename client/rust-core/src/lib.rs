use jni::objects::JClass;
use jni::sys::jstring;
use jni::JNIEnv;
use serde::Serialize;

#[derive(Serialize)]
struct InterfaceStats<'a> {
    name: &'a str,
    rx_bytes: u64,
    tx_bytes: u64,
    rx_rate: u64,
    tx_rate: u64,
}

#[derive(Serialize)]
struct TelemetryFrame<'a> {
    device_id: &'a str,
    device_name: &'a str,
    timestamp: u64,
    network_type: &'a str,
    interfaces: Vec<InterfaceStats<'a>>,
}

pub fn sample_payload() -> String {
    let frame = TelemetryFrame {
        device_id: "android-placeholder",
        device_name: "Android Placeholder",
        timestamp: 0,
        network_type: "unknown",
        interfaces: vec![InterfaceStats {
            name: "wlan0",
            rx_bytes: 0,
            tx_bytes: 0,
            rx_rate: 0,
            tx_rate: 0,
        }],
    };
    serde_json::to_string(&frame).expect("serialize telemetry frame")
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_vincentzyu_androidseenetflow_RustBridge_stringFromRust(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let value = sample_payload();
    env.new_string(value)
        .expect("create JNI string")
        .into_raw()
}
