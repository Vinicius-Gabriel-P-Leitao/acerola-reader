#[cfg(target_os = "windows")]
pub mod windows;
#[cfg(target_os = "linux")]
pub mod linux;
#[cfg(target_os = "android")]
pub mod android;

#[cfg(target_os = "windows")]
pub use windows::DefaultDeviceInfoProvider;
#[cfg(target_os = "linux")]
pub use linux::DefaultDeviceInfoProvider;
#[cfg(target_os = "android")]
pub use android::DefaultDeviceInfoProvider;
