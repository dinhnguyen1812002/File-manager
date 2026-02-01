package com.app.file_transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class FileTransferApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileTransferApplication.class, args);
	}

}
