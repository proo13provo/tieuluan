package com.example.short_video.util;

import java.util.Random;

public class generateOtp {
    public static String RandomOtp(){
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
