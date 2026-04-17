pub mod error;

pub mod db {
    pub mod migrations;
}

pub mod filesystem {
    pub mod files_guard;
    pub mod path_guard;
    pub mod scanner_engine;
}

pub mod pattern {
    pub mod archive_format;
    pub mod chapter_template;
    pub mod template_validator;
}

pub mod remote {
    pub mod peer_guard;
    pub mod types;
}
