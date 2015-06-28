package com.nestos.accountservice.main;

import com.nestos.accountservice.javaconfig.ServiceConfig;
import com.nestos.accountservice.processor.PartitionProcessorPool;
import java.util.Scanner;
import org.apache.log4j.Logger;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Main class.
 *
 * @author Roman Osipov
 */
public class Main {
    //-------------------Logger---------------------------------------------------
    private final static Logger logger = Logger.getLogger(Main.class.getName());

    //-------------------Constants------------------------------------------------
    //-------------------Fields---------------------------------------------------
    //-------------------Constructors---------------------------------------------
    //-------------------Getters and setters--------------------------------------
    //-------------------Methods--------------------------------------------------
    public static void main(String args[]) throws InterruptedException {
        AnnotationConfigApplicationContext springContext = null;
        PartitionProcessorPool partitionProcessorPool = null;
        try {
            logger.info("Starting AccountService...");
            System.setProperty("spring.profiles.active", "prod");
            springContext = new AnnotationConfigApplicationContext();
            springContext.register(ServiceConfig.class);
            springContext.refresh();
            partitionProcessorPool =
                    springContext.getBean("partitionProcessorPool", PartitionProcessorPool.class);
            partitionProcessorPool.start();
            logger.info("AccountService started.");
            System.out.println("AccountService working now. Type 'stop' to shutdown service.");
            Scanner scanner = new Scanner(System.in);
            while (!scanner.nextLine().equals("stop")) {
            };
            System.out.println("Service is shutdowning now... Please wait.");
        } finally {
            if (partitionProcessorPool != null) {
                partitionProcessorPool.awaitIdle();
            }
            if (springContext != null) {
                springContext.close();
            }
            System.out.println("AccountService stoped.");
            logger.info("AccountService stoped.");
        }
    }
}
