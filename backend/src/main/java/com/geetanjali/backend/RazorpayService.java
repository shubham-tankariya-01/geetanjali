package com.geetanjali.backend;

import org.springframework.boot.CommandLineRunner;

public class RazorpayService  {
    public String pay(){
        String payment = "razor pay";
        System.out.println("Pyemnt from :" + payment);
        return payment;
    }

}
