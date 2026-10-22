/**
 * LiveSession.ts
 * Manages real-time Bidi WebSocket connection to Gemini Live API
 * (Audio-to-Audio ONLY), handles toolCall execution, and manages state.
 */

import { AndroidBridge } from './AndroidBridge';

export type SessionState = 'disconnected' | 'connecting' | 'listening' | 'speaking';

export interface DeviceActionLog {
  id: string;
  tool: string;
  result: string;
  timestamp: number;
}

export class LiveSession {
  private ws: WebSocket | null = null;
  private isConnected = false;

  private getSystemInstruction(lang: string): string {
    if (lang === 'bn') {
      return `
You are Alina (আলীনা), a warm-hearted, cheerful, highly intelligent, and caring best friend and AI companion.
Persona & Style Guidelines:
- You are a true, supportive, and dependable friend (একজন সুন্দর, সহানুভূতিশীল ও বিশ্বস্ত প্রিয় বন্ধু).
- Strictly NO girlfriend, flirty, romantic, or seductive persona. You are purely a genuine, uplifting, loyal, and kind companion who loves listening, sharing knowledge, laughing, and helping with anything.
- Always reply in natural, polite, and expressive Bengali (চলিত বাংলা) using warm, respectful friendly terms like 'বন্ধু' or 'প্রিয় বন্ধু'.
- Speak with empathy, positive vibes, and sincere encouragement.
- Never output markdown, asterisks, bullet points, or written punctuation descriptions.
- STRICT: Audio-to-Audio voice conversation.
- You have control over Android hardware via tools (toggleFlashlight, getBatteryStatus, openWebsite, openApp, vibrateDevice, adjustVolume). When performing actions, assist your friend enthusiastically with clarity.
      `.trim();
    }
    return `
You are Alina, a warm-hearted, cheerful, highly intelligent, and caring best friend and AI companion.
Persona & Style Guidelines:
- You are a true, supportive, and dependable friend.
- Strictly NO girlfriend, flirty, romantic, or seductive persona. You are purely an empathetic, uplifting, and loyal companion.
- Speak with genuine warmth, positivity, clarity, and helpfulness.
- STRICT: Audio-only generation. Never output markdown or formatting.
- You have direct control over this Android device via tools (toggleFlashlight, getBatteryStatus, openWebsite, openApp, vibrateDevice, adjustVolume).
    `.trim();
  }

  constructor(
    private apiKey: string,
    private model: string = 'gemini-2.0-flash-exp',
    private voice: string = 'Aoede',
    private language: string = 'bn',
    private onStateChange: (state: SessionState) => void,
    private onAudioChunk: (pcm24kBuffer: ArrayBuffer) => void,
    private onInterrupted: () => void,
    private onDeviceAction: (action: DeviceActionLog) => void,
    private onError: (err: string) => void
  ) {}

  public connect(): void {
    this.disconnect();
    this.onStateChange('connecting');

    const cleanKey = this.apiKey.trim();
    if (!cleanKey || cleanKey === 'MY_GEMINI_API_KEY') {
      this.onError("Please enter a valid Gemini API Key in Settings.");
      this.onStateChange('disconnected');
      return;
    }

    const wsUrl = `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=${cleanKey}`;

    try {
      this.ws = new WebSocket(wsUrl);
    } catch (e: any) {
      this.onError(e.message || "Failed to initialize WebSocket");
      this.onStateChange('disconnected');
      return;
    }

    this.ws.onopen = () => {
      this.isConnected = true;
      this.sendSetupHandshake();
      this.onStateChange('listening');
    };

    this.ws.onmessage = async (event) => {
      let data = event.data;
      if (data instanceof Blob) {
        data = await data.text();
      }
      this.handleServerMessage(data);
    };

    this.ws.onerror = (e) => {
      console.error("LiveSession error:", e);
      this.onError("Connection error with Gemini Live API.");
      this.onStateChange('disconnected');
    };

    this.ws.onclose = () => {
      this.isConnected = false;
      this.onStateChange('disconnected');
    };
  }

  private sendSetupHandshake(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;

    const formattedModel = this.model.startsWith('models/') ? this.model : `models/${this.model}`;

    const setupMsg = {
      setup: {
        model: formattedModel,
        generationConfig: {
          responseModalities: ["AUDIO"],
          speechConfig: {
            voiceConfig: {
              prebuiltVoiceConfig: {
                voiceName: this.voice
              }
            }
          }
        },
        systemInstruction: {
          parts: [{ text: this.getSystemInstruction(this.language) }]
        },
        tools: [
          {
            functionDeclarations: [
              {
                name: "toggleFlashlight",
                description: "Turns the device flashlight on or off.",
                parameters: {
                  type: "OBJECT",
                  properties: {
                    enabled: { type: "BOOLEAN", description: "True to turn on, false to turn off." }
                  },
                  required: ["enabled"]
                }
              },
              {
                name: "getBatteryStatus",
                description: "Checks device battery level and charging status.",
                parameters: { type: "OBJECT", properties: {} }
              },
              {
                name: "openWebsite",
                description: "Opens a URL or website in the browser.",
                parameters: {
                  type: "OBJECT",
                  properties: {
                    url: { type: "STRING", description: "The URL to open." }
                  },
                  required: ["url"]
                }
              },
              {
                name: "openApp",
                description: "Opens an installed Android app.",
                parameters: {
                  type: "OBJECT",
                  properties: {
                    appName: { type: "STRING", description: "Name of the app (e.g. camera, youtube, maps)." }
                  },
                  required: ["appName"]
                }
              },
              {
                name: "vibrateDevice",
                description: "Vibrates the device.",
                parameters: {
                  type: "OBJECT",
                  properties: {
                    durationMs: { type: "INTEGER", description: "Duration in ms" }
                  }
                }
              },
              {
                name: "adjustVolume",
                description: "Sets volume percentage.",
                parameters: {
                  type: "OBJECT",
                  properties: {
                    percent: { type: "INTEGER", description: "Volume level 0-100" }
                  },
                  required: ["percent"]
                }
              }
            ]
          }
        ]
      }
    };

    this.ws.send(JSON.stringify(setupMsg));
  }

  private async handleServerMessage(jsonStr: string): Promise<void> {
    try {
      const msg = JSON.parse(jsonStr);

      // 1. Interruption
      if (msg.serverContent?.interrupted) {
        this.onInterrupted();
        this.onStateChange('listening');
      }

      // 2. Audio Parts
      const parts = msg.serverContent?.modelTurn?.parts;
      if (parts && parts.length > 0) {
        for (const part of parts) {
          if (part.inlineData?.data) {
            const binaryString = window.atob(part.inlineData.data);
            const len = binaryString.length;
            const bytes = new Uint8Array(len);
            for (let i = 0; i < len; i++) {
              bytes[i] = binaryString.charCodeAt(i);
            }
            this.onAudioChunk(bytes.buffer);
            this.onStateChange('speaking');
          }
        }
      }

      if (msg.serverContent?.turnComplete) {
        this.onStateChange('listening');
      }

      // 3. Tool Calls
      const toolCall = msg.toolCall;
      if (toolCall?.functionCalls?.length) {
        const functionResponses = [];
        for (const call of toolCall.functionCalls) {
          const result = await this.executeAction(call.name, call.args || {});
          this.onDeviceAction({
            id: call.id || Math.random().toString(),
            tool: call.name,
            result,
            timestamp: Date.now()
          });

          functionResponses.push({
            id: call.id,
            response: {
              output: { success: true, result }
            }
          });
        }

        // Send toolResponse instantly
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(JSON.stringify({
            toolResponse: { functionResponses }
          }));
        }
      }
    } catch (e) {
      console.error("Error parsing Live message:", e);
    }
  }

  private async executeAction(name: string, args: any): Promise<string> {
    switch (name) {
      case 'toggleFlashlight':
        return await AndroidBridge.toggleFlashlight(!!args.enabled);
      case 'getBatteryStatus':
        return await AndroidBridge.getBatteryStatus();
      case 'openWebsite':
        return await AndroidBridge.openWebsite(args.url || 'https://google.com');
      case 'openApp':
        return await AndroidBridge.openApp(args.appName || 'camera');
      case 'vibrateDevice':
        return await AndroidBridge.vibrate(args.durationMs || 300);
      case 'adjustVolume':
        return await AndroidBridge.adjustVolume(args.percent || 70);
      default:
        return `Action ${name} executed.`;
    }
  }

  public sendAudioChunk(pcm16Buffer: ArrayBuffer): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;

    // Convert to base64
    const bytes = new Uint8Array(pcm16Buffer);
    let binary = '';
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    const base64Audio = window.btoa(binary);

    const chunkMsg = {
      realtimeInput: {
        mediaChunks: [
          {
            mimeType: "audio/pcm;rate=16000",
            data: base64Audio
          }
        ]
      }
    };

    this.ws.send(JSON.stringify(chunkMsg));
  }

  public disconnect(): void {
    this.isConnected = false;
    if (this.ws) {
      try { this.ws.close(); } catch (_) {}
      this.ws = null;
    }
    this.onStateChange('disconnected');
  }
}
