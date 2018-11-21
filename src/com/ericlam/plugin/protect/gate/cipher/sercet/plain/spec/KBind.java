package com.ericlam.plugin.protect.gate.cipher.sercet.plain.spec;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class KBind {
    private byte[] K;

    protected KBind(){
        byte[] k = new StringBuilder(KBind.class.toGenericString().intern()).reverse().toString().getBytes(StandardCharsets.UTF_8);
        K = Arrays.copyOf(k,16);
    }

    protected final SecretKeySpec getK(){
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : K) {
            stringBuilder.append((char)b);
        }
        String[] strings = stringBuilder.toString().split("\\.");
        String[] str = new String[3];
        int j = 0;
        for (int i = strings.length - 1; i > -1; i--) {
            str[j] = strings[i];
            j++;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length; i++) {
            builder.append(str[i]).append(i == str.length -1 ? "" : "?");
        }
        byte[] get = builder.toString().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(get,"AES");
    }

}
