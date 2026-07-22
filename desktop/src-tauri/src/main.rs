// Prevents additional console window on Windows in release, DO NOT REMOVE!!
// #![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

#[cfg(target_os = "windows")]
fn configure_windows_webview_bridge() {
    use std::env;

    let command_line_args: Vec<String> = env::args().collect();
    let mut additional_browser_args: Vec<String> = Vec::new();
    let mut args_iterator = command_line_args.iter();

    while let Some(current_arg) = args_iterator.next() {
        match current_arg.as_str() {
            arg if arg.starts_with("--remote-debugging-port=") => {
                additional_browser_args.push(arg.to_string());
            }
            "--remote-debugging-port" => {
                if let Some(port_value) = args_iterator.next() {
                    additional_browser_args.push(format!("--remote-debugging-port={port_value}"));
                }
            }
            arg if arg.starts_with("--user-data-dir=") => {
                let folder_path = arg.trim_start_matches("--user-data-dir=");
                env::set_var("WEBVIEW2_USER_DATA_FOLDER", folder_path);
            }
            "--user-data-dir" => {
                if let Some(folder_path) = args_iterator.next() {
                    env::set_var("WEBVIEW2_USER_DATA_FOLDER", folder_path);
                }
            }
            _ => {}
        }
    }

    if !additional_browser_args.is_empty() {
        additional_browser_args.push("--remote-allow-origins=*".to_string());
        additional_browser_args.push("--disable-gpu".to_string());

        let existing_env_args = env::var("WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS").unwrap_or_default();
        let updated_browser_args = match existing_env_args.is_empty() {
            true => additional_browser_args.join(" "),
            false => format!("{existing_env_args} {}", additional_browser_args.join(" ")),
        };

        env::set_var("WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS", updated_browser_args);
    }
}

fn main() {
    #[cfg(target_os = "windows")]
    configure_windows_webview_bridge();

    acerola_lib::run()
}
