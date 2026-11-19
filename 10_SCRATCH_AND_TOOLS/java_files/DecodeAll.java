import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;
import java.util.*;

public class DecodeAll {
    private static final int key1 = (0x2E << 1);
    private static final int key2 = (0x51 << 1) | 1;
    private static final int key3 = (0x3B ^ 0x04);
    private static final int rotation = (9 - 6);
    private static final int shift = (34 >>> 1);

    private static int rotateRight(int value, int bits) {
        int amount = bits & 7;
        return (((value >>> amount) | (value << (8 - amount))) & 0xFF);
    }

    private static int swapNibbles(int value) {
        return (((value >> 4) & 0x0F) | ((value & 0x0F) << 4));
    }

    public static String s(byte[] bytes) {
        byte[] decoded = new byte[bytes.length];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xFF;
            value = value ^ ((key3 ^ ((index * 7 + 13) & 0xFF)) & 0xFF);
            value = swapNibbles(value);
            value = (value - shift) & 0xFF;
            value = value ^ key2;
            value = rotateRight(value, rotation);
            value = value ^ key1;
            decoded[index] = (byte) value;
        }
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("ArabseedRepo_old.kt"), StandardCharsets.UTF_16);
        Pattern p = Pattern.compile("val\\s+(_[a-zA-Z0-9_]+)\\s*=\\s*byteArrayOf\\((.*)\\)");
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                String name = m.group(1);
                String body = m.group(2);
                String[] tokens = body.split(",");
                byte[] bytes = new byte[tokens.length];
                boolean ok = true;
                for (int i = 0; i < tokens.length; i++) {
                    String t = tokens[i].replace(".toByte()", "").replaceAll("\\s+", "").trim();
                    try {
                        bytes[i] = (byte) Integer.parseInt(t);
                    } catch (Exception e) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    String res = s(bytes);
                    sb.append(name).append(" = \"").append(res.replace("\n", "\\n")).append("\"\n");
                }
            }
        }
        Files.writeString(Paths.get("decoded_results.txt"), sb.toString(), StandardCharsets.UTF_8);
        System.out.println("Done! Count: " + sb.toString().lines().count());
    }
}
