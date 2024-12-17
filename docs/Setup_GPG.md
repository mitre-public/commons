# Setting up GPG

---

- **Ran:** `jiparker@MM292015-PC ~ % gpg --version`
- **Got:** A new file in `/Users/jiparker/.gnupg`, and the text output:
    ```
    gpg (GnuPG/MacGPG2) 2.2.41
    libgcrypt 1.8.10
    Copyright (C) 2022 g10 Code GmbH
    License GNU GPL-3.0-or-later <https://gnu.org/licenses/gpl.html>
    This is free software: you are free to change and redistribute it.
    There is NO WARRANTY, to the extent permitted by law.
    
    Home: /Users/jiparker/.gnupg
    Supported algorithms:
    Pubkey: RSA, ELG, DSA, ECDH, ECDSA, EDDSA
    Cipher: IDEA, 3DES, CAST5, BLOWFISH, AES, AES192, AES256, TWOFISH,
            CAMELLIA128, CAMELLIA192, CAMELLIA256
    Hash: SHA1, RIPEMD160, SHA256, SHA384, SHA512, SHA224
    Compression: Uncompressed, ZIP, ZLIB, BZIP2
    ```

---

- **Ran:** `gpg --gen-key`
- **Got:** New files in `/Users/jiparker/.gnupg`, and the text output:
    ```
    gpg: directory '/Users/jiparker/.gnupg/openpgp-revocs.d' created
    gpg: revocation certificate stored as '/Users/jiparker/.gnupg/openpgp-revocs.d/D142A8AD32C8CECDAE307B9CFDBC69BA9CD6E1CE.rev'
    public and secret key created and signed.
  
    pub   rsa3072 2023-10-12 [SC] [expires: 2025-10-11]
          D142A8AD32C8CECDAE307B9CFDBC69BA9CD6E1CE
    uid                      Jon Parker <jiparker@mitre.org>
    sub   rsa3072 2023-10-12 [E] [expires: 2025-10-11]
    ```

---

- **Ran:** `gpg -ab targetFile.txt`
- **Got:** A new file named `targetFile.txt.asc`

---

- **Ran:** `gpg --verify targetFile.txt.asc`
- **Got:** The terminal output
    ``` 
    jiparker@MM292015-PC ~ % gpg --verify targetFile.txt.asc
    gpg: assuming signed data in 'targetFile.txt'
    gpg: Signature made Thu Oct 12 12:47:39 2023 EDT
    gpg:                using RSA key D142A8AD32C8CECDAE307B9CFDBC69BA9CD6E1CE
    gpg: Good signature from "Jon Parker <jiparker@mitre.org>" [ultimate]
    ```

## Distributed Key using:

**Command and Output:**

```
jiparker@MM292015-PC ~ % gpg --keyserver keyserver.ubuntu.com --send-keys D142A8AD32C8CECDAE307B9CFDBC69BA9CD6E1CE
gpg: sending key FDBC69BA9CD6E1CE to hkp://keyserver.ubuntu.com
```

```
jiparker@MM292015-PC ~ % gpg --keyserver pgp.mit.edu --send-keys D142A8AD32C8CECDAE307B9CFDBC69BA9CD6E1CE
gpg: sending key FDBC69BA9CD6E1CE to hkp://pgp.mit.edu
```

Verify publication:
`gpg --keyserver keyserver.ubuntu.com --recv-keys D142A8AD32C8CECDAE307B9CFDBC69BA9CD6E1CE`

`gpg --keyserver pgp.mit.edu --recv-keys D142A8AD32C8CECDAE307B9CFDBC69BA9CD6E1CE`
