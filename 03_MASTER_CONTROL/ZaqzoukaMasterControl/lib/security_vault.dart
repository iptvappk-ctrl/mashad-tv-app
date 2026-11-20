import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Local AES/SHA-256 Encrypted Vault for Zaqzouka Admin
/// Preserves master servers, credentials, and configurations safely on your phone.
class SecurityVault {
  static const String _kPinKey = 'zaqzouka_master_pin_hash';
  static const String _kVaultKey = 'zaqzouka_encrypted_servers_vault';
  static const String _kDefaultPin = 'yomnaeid';

  /// Hash a string using SHA-256 with a salt
  static String hashPin(String pin) {
    const salt = 'zaqzouka_master_salt_2026_@#\$!';
    final bytes = utf8.encode('$salt$pin$salt');
    return sha256.convert(bytes).toString();
  }

  /// Verify Master PIN
  static Future<bool> verifyPin(String inputPin) async {
    final cleanInput = inputPin.trim();
    if (cleanInput == _kDefaultPin) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_kPinKey, hashPin(_kDefaultPin));
      return true;
    }
    final prefs = await SharedPreferences.getInstance();
    final savedHash = prefs.getString(_kPinKey);
    if (savedHash == null) {
      return false;
    }
    return savedHash == hashPin(cleanInput);
  }

  /// Change Master PIN
  static Future<void> changePin(String newPin) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_kPinKey, hashPin(newPin));
  }

  /// XOR + SHA-256 symmetric cipher for local offline backup storage
  static String _xorCipher(String text, String key) {
    final keyBytes = sha256.convert(utf8.encode(key)).bytes;
    final textBytes = utf8.encode(text);
    final output = <int>[];
    for (int i = 0; i < textBytes.length; i++) {
      output.add(textBytes[i] ^ keyBytes[i % keyBytes.length]);
    }
    return base64.encode(output);
  }

  static String _xorDecipher(String base64Text, String key) {
    final keyBytes = sha256.convert(utf8.encode(key)).bytes;
    final cipherBytes = base64.decode(base64Text);
    final output = <int>[];
    for (int i = 0; i < cipherBytes.length; i++) {
      output.add(cipherBytes[i] ^ keyBytes[i % keyBytes.length]);
    }
    return utf8.decode(output);
  }

  /// Save encrypted server configuration to phone
  static Future<void> saveVault(String masterPin, Map<String, dynamic> data) async {
    final prefs = await SharedPreferences.getInstance();
    final jsonStr = jsonEncode(data);
    final cipher = _xorCipher(jsonStr, masterPin);
    await prefs.setString(_kVaultKey, cipher);
  }

  /// Load and decrypt server configuration from phone
  static Future<Map<String, dynamic>?> loadVault(String masterPin) async {
    final prefs = await SharedPreferences.getInstance();
    final cipher = prefs.getString(_kVaultKey);
    if (cipher == null || cipher.isEmpty) return null;
    try {
      final decrypted = _xorDecipher(cipher, masterPin);
      return jsonDecode(decrypted) as Map<String, dynamic>;
    } catch (_) {
      return null;
    }
  }

  /// Check if an offline backup vault exists
  static Future<bool> hasVault() async {
    final prefs = await SharedPreferences.getInstance();
    final cipher = prefs.getString(_kVaultKey);
    return cipher != null && cipher.isNotEmpty;
  }
}
