RUST_DIR := rust
API_LEVEL := 26
ABI := arm64-v8a
LIB_NAME := acerola
TARGET := aarch64-linux-android

SO_SOURCE := $(RUST_DIR)/target/$(TARGET)/release/lib$(LIB_NAME).so
SO_SOURCE_REL := target/$(TARGET)/release/lib$(LIB_NAME).so

KOTLIN_OUT := src/main/java/br/acerola/comic
JNILIBS_OUT := src/main/jniLibs/$(ABI)

ifeq ($(OS),Windows_NT)
    MKDIR := powershell -Command "New-Item -ItemType Directory -Force -Path"
    CP := powershell -Command "Copy-Item -Force -Path"
else
    MKDIR := mkdir -p
    CP := cp
endif

all: build

build: build-rust generate-bindings copy-libs
	@echo "==> [SUCESSO] Tudo pronto! Kotlin gerado e .so copiado."

build-rust:
	@echo "==> Building Rust (NDK) for $(ABI)..."
	cd $(RUST_DIR) && cargo ndk -t $(TARGET) build --release

generate-bindings:
	@echo "==> Generating UniFFI Kotlin bindings..."
	cd $(RUST_DIR) && \
	cargo run --bin uniffi-bindgen -- generate \
	--config uniffi.toml \
	--library $(SO_SOURCE_REL) \
	--language kotlin \
	--out-dir ../$(KOTLIN_OUT)

copy-libs:
	@echo "==> Copying .so to $(JNILIBS_OUT)..."
	$(MKDIR) $(JNILIBS_OUT)
	$(CP) $(SO_SOURCE) $(JNILIBS_OUT)/

clean:
	@echo "==> Cleaning..."
	cd $(RUST_DIR) && cargo clean