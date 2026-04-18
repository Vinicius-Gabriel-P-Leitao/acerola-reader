pub mod p2p {
    pub mod handlers {
        pub mod blobs;
        pub mod graphql;
        pub mod rpc;
    }

    pub mod state {
        pub mod network_state;
    }

    pub mod network_manager;
}
