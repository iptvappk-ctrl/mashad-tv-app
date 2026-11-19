import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;
import java.util.*;

public class DecodeBadPatterns {
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
        String text = Files.readString(Paths.get("MadarTV/app/src/main/java/com/madar/tv/data/ArabseedRepository.kt"));
        Pattern p = Pattern.compile("byteArrayOf\\(([^)]+)\\)");
        Matcher m = p.matcher(text.substring(text.indexOf("_badPatterns")));
        int i = 0;
        while (m.find()) {
            String body = m.group(1);
            String[] tokens = body.split(",");
            byte[] bytes = new byte[tokens.length];
            for (int k = 0; k < tokens.length; k++) {
                String clean = tokens[k].replaceAll("[^0-9\\-]", "");
                bytes[k] = (byte) Integer.parseInt(clean);
            }
            System.out.println("Pattern " + (i++) + ": " + s(bytes));
        }
    }
}
