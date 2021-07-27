package dev.adrwas.trafficlib.executable;

import dev.adrwas.trafficlib.server.SocketServer;

public class TrafficLibExecutable {
    public static void main(String[] args) {
        System.out.println("TrafficLib executed as a standalone JAR");

//        String input = "my plain text";
//        SecretKey key = AESUtil.generateKey(128);
//        IvParameterSpec ivParameterSpec = AESUtil.generateIv();
//
//        String algorithm = "AES/CBC/PKCS5Padding";
//        String cipherText = EncryptionManager.encrypt(algorithm, input, key, ivParameterSpec);
//
//        System.out.println("Ciphered: " + cipherText);
//
//        System.out.println("Plain Text: " + EncryptionManager.decrypt(algorithm, cipherText, key, ivParameterSpec));
//
//
//
//

        SocketServer server = new SocketServer(6000, "password");
        server.startServer();
    }
}
