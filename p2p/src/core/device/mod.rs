//! Módulos de identificação de dispositivo para cada sistema operacional.

/// Implementação para a plataforma Android.
#[cfg(any(target_os = "android", test))]
pub mod android;
/// Implementação para a plataforma Linux.
#[cfg(any(target_os = "linux", test))]
pub mod linux;
/// Implementação para a plataforma Windows.
#[cfg(any(target_os = "windows", test))]
pub mod windows;
