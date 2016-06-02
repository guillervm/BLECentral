package com.test.guillervm.blecentral.core;

import java.util.Random;

/**
 * Created by guillervm on 20/4/15.
 */
public class HexStringGenerator {
    private static final char[] characters = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static HexStringGenerator instance;

    // Private constructor.
    private HexStringGenerator() {
        super();
    }

    /**
     * Instance provider
     * @return An instance of <code>HexStringGenerator</code> object.
     */
    public static HexStringGenerator getInstance() {
        if (instance == null) {
            instance = new HexStringGenerator();
        }

        return instance;
    }

    /**
     * Hexadecimal string generator method.
     * @param bytes <code>Integer</code> value representing the wanted length.
     * @return A random hexadecimal string.
     */
    public String generateString(int bytes) {
        Random random = new Random();
        String ret = new String();

        for (int i = 0; i < bytes; i++) {
            ret += characters[random.nextInt(characters.length)];
        }

        return ret;
    }
}
