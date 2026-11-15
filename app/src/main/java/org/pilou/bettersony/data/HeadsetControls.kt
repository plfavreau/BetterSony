package org.pilou.bettersony.data

/* Settings writes: optimistic local update, then the MDR command. */

fun HeadsetRepository.setSound(sound: SoundControl) = write({ it.copy(sound = sound) }) { it.write(Command.sound(sound)) }

/** [remember] = a choice made in the app, kept and restored on every connection. */
fun HeadsetRepository.setTalk(on: Boolean, remember: Boolean = true) {
    if (remember) prefs.talkChoice = on
    write({ it.copy(speakToChat = on) }) { it.write(Command.talk(on)) }
}

fun HeadsetRepository.setTalkConfig(c: TalkConfig) = write({ it.copy(talk = c) }) { it.write(Command.talkConfig(c)) }

fun HeadsetRepository.setEqPreset(code: Int) = write({ h -> h.copy(eq = (h.eq ?: Equalizer()).copy(preset = code)) }) {
    it.write(Command.eqPreset(code))
}

fun HeadsetRepository.setEqBand(band: Int, value: Int) {
    val bands = (snapshot()?.eq ?: Equalizer()).with(band, value).bands
    write({ h -> h.copy(eq = (h.eq ?: Equalizer()).copy(bands = bands)) }) { it.write(Command.eqBands(bands)) }
}

fun HeadsetRepository.resetEq() {
    val flat = Equalizer().bands
    write({ h -> h.copy(eq = (h.eq ?: Equalizer()).copy(bands = flat)) }) { it.write(Command.eqBands(flat)) }
}

fun HeadsetRepository.setDsee(on: Boolean) = write({ it.copy(dsee = on) }) { it.write(Command.dsee(on)) }

fun HeadsetRepository.setPreferSound(on: Boolean) = write({ it.copy(preferSound = on) }) { it.write(Command.preferSound(on)) }

fun HeadsetRepository.setPauseWhenOff(on: Boolean) = write({ it.copy(pauseWhenOff = on) }) { it.write(Command.pause(on)) }

fun HeadsetRepository.setAutoOff(on: Boolean) = write({ it.copy(autoOff = on) }) { it.write(Command.autoOff(on)) }

fun HeadsetRepository.setTouch(on: Boolean) = write({ it.copy(touch = on) }) { it.write(Command.touch(on)) }

fun HeadsetRepository.setVoiceGuide(on: Boolean) = write({ it.copy(voiceGuide = on) }) { it.writeVoiceGuide(on) }
