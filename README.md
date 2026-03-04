# chat-attestation

Botless impersonation protection for Kaboom and perhaps other servers

## Usage

Download the latest compatible messaging-lib jar
from [here](https://code.chipmunk.land/kaboomstandardsorganization/-/packages/maven/land.chipmunk.code.kaboom-standards-organization:messaging-lib/1.1.0-snapshot),
or compile it from source.

You also need to get Mod Menu, Cloth Config & Fabric API from Modrinth.

Compile this mod with `./gradlew --no-daemon build` or `.\gradlew.bat --no-daemon build` if you're on Windows.

**NEVER SEND YOUR chat-attestation.priv.key FILE TO ANYONE**. Your public key is printed in logs at startup (search
for "
Encoded public key").

## Caveats

- You cannot use Extras messaging mode to verify messages sent in vanish. The fallback mode works though.
