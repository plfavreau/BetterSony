# Sony WH-1000XM4 control protocol (MDR v1)

Verified against a WH-1000XM4 on firmware 3.0.1 (protocol info `70 00`), paired to a Pixel 10 Pro XL on Android 17, November 2026. Every command below was sent to the real headset and its reply captured. Anything not listed here is not used by the app.

Thanks to [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge) and [SonyHeadphonesClient](https://github.com/mos9527/SonyHeadphonesClient) (and the original by [Plutoberth](https://github.com/Plutoberth/SonyHeadphonesClient)), which mapped this protocol first.

## Transport

RFCOMM to the SDP service `96CC203E-5068-46AD-B32D-E316F5E069BA`, secure socket. The first `connect()` right after the link comes up often fails with `read failed`; the second one works. The official app can stay connected at the same time.

## Framing

```
3e | escape( type | seq | len (4 bytes, big endian) | payload | checksum ) | 3c
```

- `type`: `01` ACK, `0c` table 1 (almost everything), `0e` table 2 (voice guidance, paired devices).
- `checksum`: sum of `type`, `seq`, `len` and `payload` bytes, mod 256, before escaping.
- Escaping: `3c`, `3d`, `3e` inside the frame become `3d` followed by the byte `& 0xef` (`2c`, `2d`, `2e`).
- `seq` alternates 0/1. The headset ACKs every command with `seq = 1 - ours`, and our next command uses the ACK's seq.
- Every frame the headset sends (replies and notifications) must be ACKed with `seq = 1 - theirs`.
- One command in flight at a time: wait for the ACK before sending the next.

Example, init request with seq 0: `3e 0c 00 00 00 00 02 00 00 0e 3c`.

## Commands

Payloads only. The first byte is the command, the second the "inquired type". Replies use the GET byte + 1, notifications the SET byte + 1, and both share the SET layout.

### Connect

| | Request | Reply (this headset) |
|---|---|---|
| Init | `00 00` | `01 00 70 00` |
| Model | `04 01` | `05 01 0a "WH-1000XM4"` |
| Firmware | `04 02` | `05 02 05 "3.0.1"` |
| Supported functions | `06 00` | `07 00 17` + 23 function codes |

Function codes on the XM4: `11` battery, `12` DSEE indicator, `13` codec, `14` BLE setup, `21` power off, `22` concierge, `30` firmware update, `38` paired devices, `39` voice guidance, `51` EQ, `62` noise cancelling + ambient, `71` adaptive sound control, `81` NC optimizer, `a1` playback, `c1` action log, `d1` touch panel, `d2` multipoint, `e1` connection mode, `e2` DSEE, `f3` pause when taken off, `f4` auto power off, `f5` speak-to-chat, `f6` custom button.

### Status

| | GET | Reply / notify |
|---|---|---|
| Battery | `10 00` | `11 00 <percent> <charging 00/01>`, notify `13 00 …` |
| Codec | `18 00` | `19 00 <codec>`, notify `1b 00 …`. `01` SBC, `02` AAC, `10` LDAC |
| DSEE active | `14 00` | `15 00 02 <01 active>` (type `02` = DSEE Extreme) |
| Volume | `a6 01 20` | `a7 01 20 <0..30>` |

### Noise cancelling and ambient sound

GET `66 02`, SET `68 02 <effect> 02 <nc> 01 <voice> <level>`, notify `69 02 …`.

| Field | Values |
|---|---|
| effect | send `11` for on, `00` for off; the headset reports `01` for on |
| nc | `02` noise cancelling, `01` wind reduction, `00` ambient sound |
| voice | `01` focus on voice |
| level | ambient level `01..14` (1 to 20), `00` in noise cancelling |

Captured:

| Setting | Payload | Notify |
|---|---|---|
| Noise cancelling | `68 02 11 02 02 01 00 00` | `69 02 01 02 02 01 00 00` |
| Wind reduction | `68 02 11 02 01 01 00 00` | `69 02 01 02 01 01 00 00` |
| Ambient 20 | `68 02 11 02 00 01 00 14` | `69 02 01 02 00 01 00 14` |
| Ambient 10, voice | `68 02 11 02 00 01 01 0a` | `69 02 01 02 00 01 01 0a` |
| Off | `68 02 00 02 00 01 00 14` | `69 02 00 02 00 01 00 14` |

### Speak-to-chat

| | Payload |
|---|---|
| GET | `f6 05` → `f7 05 00 <00 off / 01 on>` |
| SET | `f8 05 01 <00 off / 01 on>` |
| Notify | `f9 05 01 <v>`, then `f9 05 02 00` (preview) and, when turning on, `f5 05 00 00` |
| GET config | `fa 05` → `fb 05 00 <sensitivity> <voice focus> <timeout>` |
| SET config | `fc 05 00 <sensitivity> <voice focus> <timeout>` |

Sensitivity `00` auto, `01` high, `02` low. Timeout `00` 15 s, `01` 30 s, `02` 1 min, `03` never.

The setting is stored in the headset and survived turning the phone's Bluetooth off and on.

### Equalizer

| | Payload |
|---|---|
| GET | `56 01` → `57 01 <preset> 06 <clear bass> <400> <1k> <2.5k> <6.3k> <16k>` |
| Set preset | `58 01 <preset> 00` |
| Set bands | `58 01 ff 06 <six values>` |

Values are −10..+10 sent as `00..14`. Presets: `00` off, `10` bright, `11` excited, `12` mellow, `13` relaxed, `14` vocal, `15` treble boost, `16` bass boost, `17` speech, `a0` manual, `a1` custom 1, `a2` custom 2.

Band writes apply to the current preset (only Manual, Custom 1 and Custom 2 are editable). The headset ACKs them but sends no `59` notification; read `56 01` to confirm.

### Other settings

Each SET below was sent to the headset, which ACKed it and echoed the notification shown.

| Setting | GET → reply | SET | Notify |
|---|---|---|---|
| DSEE Extreme | `e6 02` → `e7 02 00 <v>` | `e8 02 00 <v>` | `e9 02 00 <v>`, then `17 00 02 <v>` |
| Sound quality mode | `e6 01` → `e7 01 00 <00 sound / 01 connection>` | `e8 01 00 <v>` | `e9 01 00 <v>`, codec drops to `1b 00 00` and comes back about a second later. The link stays up. |
| Pause when taken off | `f6 03` → `f7 03 00 <v>` | `f8 03 00 <v>` | `f9 03 00 <v>` |
| Auto power off | `f6 04` → `f7 04 01 <10 when taken off / 11 never> 00` | `f8 04 01 <v> 00` | `f9 04 01 <v> 00` |
| Touch panel | `d6 d1` → `d7 d1 01 <v>` | `d8 d1 01 <v>` | `d9 d1 01 <v>` |
| Voice guidance | `0e: 46 01 01` → `47 01 01 <v>` | `0e: 48 01 01 <v>` | `0e: 49 01 01 <v>` |
| NC optimizer state | `86 01` → `87 01 01 01 01 <pressure × 10>` | not used | |

Speak-to-chat config was also set and restored: `fc 05 00 00 01 01` → `fd 05 00 00 01 01`.

### Power off

`22 00 01`. Verified: the ACK comes back within 50 ms, the RFCOMM link drops right after and the headset is off a second later. Press its power button to turn it back on.

### Settings that restart the headset

Multipoint and the custom button make the headset restart. It asks first and only applies the change once the app says yes.

| Setting | GET → reply | SET | Question |
|---|---|---|---|
| Multipoint (GS2, `MULTIPOINT_SETTING`) | `d6 d2` → `d7 d2 01 <v>` | `d8 d2 01 <v>` | `99 01 06 01` (turning on), `99 01 07 01` (turning off) |
| Custom button | `f6 06` → `f7 06 01 <00 ambient / 31 Google / 32 Alexa>` | `f8 06 01 <preset>` | `99 01 02 01` |

Flow, verified both ways for each setting:

1. `94 01 00` turns on these questions (alert status, fixed messages, `00` = enable).
2. Send the SET. The headset ACKs it and asks within about 20 ms: `99 01 <message> 01` (`01` = yes/no question).
3. Answer `98 01 <message> 01` to go ahead, or `98 01 <message> 00` to cancel. On cancel it notifies the unchanged value (`d9 d2 01 01`).
4. On yes, the control channel drops right away and the headset restarts. It came back on its own after about 7 s when turning multipoint off, and about 40 s whenever multipoint ends up on.

With multipoint off, the Pixel switched from AAC to LDAC.

Button capability, `f0 06` → `f1 06 01 02 01 00 03 | 00 02 00 01 21 02 | 31 03 00 31 01 33 22 32 | 32 01 00 34`: one CUSTOM button (`02`, a physical button `01`, default `00`) with three presets. Ambient sound: press cycles noise cancelling and ambient sound, long press runs the NC optimizer. Google Assistant: press for notifications, double press stops it, hold to talk. Alexa: press to talk.

## One control channel

The headset serves a single MDR session per phone. Opening a second RFCOMM socket to the service closes the first one (seen with two clients in the same phone). Sound Connect opens a session in the background right after the headset connects, even with its screen closed. That is the moment it can push its own idea of the settings, so BetterSony connects at the same time (which closes Sound Connect's session), checks speak-to-chat, and checks again 15 s later.

Speak-to-chat survived both a phone Bluetooth off/on and a full headset power cycle, so the headset itself never resets it.

## Not used

Firmware update (`34`–`37`), the `ff` test commands, `98` answers to anything but the questions above, and paired device management (`0e: 3x`) are never sent.
