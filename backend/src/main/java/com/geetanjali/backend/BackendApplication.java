package com.geetanjali.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class BackendApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@GetMapping("/")
	public String Hello(){
		return "Hello from a Pro dev shubham";
	}
	private RazorpayService srvc = new RazorpayService();


	@Override
	public void run(String... args) throws Exception {
		String payment = srvc.pay();
		System.out.println("payment done :" + payment);
	}

}
