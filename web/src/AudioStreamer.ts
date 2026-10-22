/**
 * AudioStreamer.ts
 * Manages 16kHz PCM linear mic capture and 24kHz PCM linear audio playback
 * with seamless interruption flushing and RMS amplitude visualization.
 */

export class AudioStreamer {
  private audioContext: AudioContext | null = null;
  private mediaStream: MediaStream | null = null;
  private scriptProcessor: ScriptProcessorNode | null = null;
  private gainNode: GainNode | null = null;

  private isRecording = false;
  private nextPlayTime = 0;
  private activeSources: AudioBufferSourceNode[] = [];

  constructor(
    private onMicChunk: (pcm16Data: ArrayBuffer) => void,
    private onMicAmp: (amp: number) => void,
    private onSpeakerAmp: (amp: number) => void
  ) {}

  public async startRecording(): Promise<void> {
    this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)({
      sampleRate: 16000
    });

    this.mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        sampleRate: 16000,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    });

    const source = this.audioContext.createMediaStreamSource(this.mediaStream);
    // Buffer size of 2048 at 16kHz gives ~128ms packets
    this.scriptProcessor = this.audioContext.createScriptProcessor(2048, 1, 1);

    this.scriptProcessor.onaudioprocess = (e) => {
      if (!this.isRecording) return;
      const inputFloat32 = e.inputBuffer.getChannelData(0);

      // 1. Calculate RMS amplitude
      let sum = 0;
      for (let i = 0; i < inputFloat32.length; i++) {
        sum += inputFloat32[i] * inputFloat32[i];
      }
      const rms = Math.sqrt(sum / inputFloat32.length);
      this.onMicAmp(Math.min(rms * 4.0, 1.0));

      // 2. Convert Float32 to Int16 PCM
      const pcm16 = new Int16Array(inputFloat32.length);
      for (let i = 0; i < inputFloat32.length; i++) {
        const s = Math.max(-1, Math.min(1, inputFloat32[i]));
        pcm16[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
      }

      this.onMicChunk(pcm16.buffer);
    };

    source.connect(this.scriptProcessor);
    this.scriptProcessor.connect(this.audioContext.destination);
    this.isRecording = true;
    this.nextPlayTime = this.audioContext.currentTime;
  }

  public stopRecording(): void {
    this.isRecording = false;
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
    }
    if (this.scriptProcessor) {
      this.scriptProcessor.disconnect();
      this.scriptProcessor = null;
    }
  }

  /**
   * Schedules a 24kHz PCM linear audio chunk from the model for low-latency playback.
   */
  public playChunk(pcm24kBuffer: ArrayBuffer): void {
    if (!this.audioContext) {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
    if (this.audioContext.state === 'suspended') {
      this.audioContext.resume();
    }

    const int16Array = new Int16Array(pcm24kBuffer);
    const float32Array = new Float32Array(int16Array.length);

    let sum = 0;
    for (let i = 0; i < int16Array.length; i++) {
      const f = int16Array[i] / 32768.0;
      float32Array[i] = f;
      sum += f * f;
    }

    const rms = Math.sqrt(sum / float32Array.length);
    this.onSpeakerAmp(Math.min(rms * 3.5, 1.0));

    // Create 24kHz AudioBuffer
    const audioBuffer = this.audioContext.createBuffer(1, float32Array.length, 24000);
    audioBuffer.copyToChannel(float32Array, 0);

    const source = this.audioContext.createBufferSource();
    source.buffer = audioBuffer;
    source.connect(this.audioContext.destination);

    const now = this.audioContext.currentTime;
    const playAt = Math.max(now, this.nextPlayTime);
    source.start(playAt);

    this.nextPlayTime = playAt + audioBuffer.duration;
    this.activeSources.push(source);

    source.onended = () => {
      const idx = this.activeSources.indexOf(source);
      if (idx !== -1) this.activeSources.splice(idx, 1);
      if (this.activeSources.length === 0) {
        this.onSpeakerAmp(0);
      }
    };
  }

  /**
   * Instantly stops playback and flushes queued audio buffers on user interruption.
   */
  public stopAndFlush(): void {
    this.activeSources.forEach(s => {
      try { s.stop(); s.disconnect(); } catch (_) {}
    });
    this.activeSources = [];
    if (this.audioContext) {
      this.nextPlayTime = this.audioContext.currentTime;
    }
    this.onSpeakerAmp(0);
  }

  public release(): void {
    this.stopRecording();
    this.stopAndFlush();
    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
  }
}
