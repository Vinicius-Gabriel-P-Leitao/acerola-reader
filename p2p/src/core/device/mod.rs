// Módulos de identificação de dispositivo para cada sistema operacional.
// Em modo de teste (`cfg(test)`), todos os módulos são habilitados para validação de cobertura.
#[cfg(any(target_os = "android", test))]
pub mod android;
#[cfg(any(target_os = "linux", test))]
pub mod linux;
#[cfg(any(target_os = "windows", test))]
pub mod windows;
