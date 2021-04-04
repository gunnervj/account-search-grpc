package com.bbb.grpc.account;

import com.bbb.grpc.account.service.AccountSearchImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.logging.Logger;

public class AccountServer {
    private static final Logger logger = Logger.getLogger(AccountServer.class.getName());
    public static void main(String[] args) throws IOException, InterruptedException {
        logger.info("Initializing AccountServer");
        Server server = ServerBuilder.forPort(50051)
                .addService(new AccountSearchImpl())
                .build();
        logger.info("Starting AccountServer");
        server.start();
        logger.info("Started AccountServer");
        System.out.println("Started");
        Runtime.getRuntime().addShutdownHook(new Thread( () -> {
            logger.info("Received shutdown request");
            server.shutdown();
            logger.info("Shutdown successfully");
        }));
        server.awaitTermination();
    }
}
