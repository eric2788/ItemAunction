package com.ericlam.plugin.protect.gate.cipher;

import com.ericlam.plugin.protect.gate.cipher.sercet.plain.spec.KBind;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Text extends KBind {
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String database;

    private Cipher cipher;

    public Text(){
        super();
        username = "eKLkHrZpbDsVouersa4cMw==";
        password = "4Epj7qIFJQ39CY9zDhDCOmaHCl+dBk1PZgfzlj8YWgw=";
        host = "pHnbqyvZBfoyi0ZTIf9oQltmX8PO/hpRSxk6E+Q4uFc=";
        port = 3306;
        database = "eKLkHrZpbDsVouersa4cMw==";
        try {
            SecretKeySpec key = super.getK();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,key);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException ignored) {
        }
    }

    private String decrypte(String code) {
        try {
            byte[] bytes = Base64.getDecoder().decode(code);
            byte[] result = cipher.doFinal(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte b : result) {
                builder.append((char)b);
            }
            return builder.toString();
        } catch (IllegalBlockSizeException | BadPaddingException ignored) {
        }
        return "";
    }

    public final String getUsername() {
        return decrypte(username);
    }

    public final String getPassword() {
        return decrypte(password);
    }

    public final String getHost() {
        return decrypte(host);
    }

    public final int getPort() {
        return port;
    }

    public final String getDatabase() {
        return decrypte(database);
    }
}
