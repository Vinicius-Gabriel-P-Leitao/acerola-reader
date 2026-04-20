pub mod connection {
    pub mod p2p {
        pub mod network_manager;

        pub mod state {
            pub mod network_state;
        }

        pub mod handlers {
            pub mod rpc;
        }
    }
}
