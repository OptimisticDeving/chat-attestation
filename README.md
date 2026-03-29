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

## Issues

- Incompatible with ChatPatches ([#2](https://code.optmstc.dev/opt/chat-attestation/issues/2))
  - Seems that the contents are updated after being added to the chat component. This breaks system chat.
- Compressed normal chat does not work with ChipmunkMod ([#4](https://code.optmstc.dev/opt/chat-attestation/issues/4))
  - You cannot send compressed normal chat, but you can read it. Seems like sendChat is invoked twice.
- Occasional command sign failure ([#3](https://code.optmstc.dev/opt/chat-attestation/issues/3))
  - Looks to be a race condition. Honestly, I think I might just remove this feature.
