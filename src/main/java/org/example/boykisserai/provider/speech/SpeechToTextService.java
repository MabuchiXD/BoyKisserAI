package org.example.boykisserai.provider.speech;

public interface SpeechToTextService {
    String recognizeSpeech(byte[] pcmAudioBytes);
}