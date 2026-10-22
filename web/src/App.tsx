import React, { useState, useEffect, useRef } from 'react';
import { Mic, MicOff, Power, Settings, Sparkles, BatteryCharging, Flashlight, Volume2 } from 'lucide-react';
import { AudioStreamer } from './AudioStreamer';
import { LiveSession, SessionState, DeviceActionLog } from './LiveSession';
import { AndroidBridge } from './AndroidBridge';

export function App() {
  const [state, setState] = useState<SessionState>('disconnected');
  const [apiKey, setApiKey] = useState<string>('');
  const [voice, setVoice] = useState<string>('Aoede');
  const [model, setModel] = useState<string>('gemini-2.0-flash-exp');
  const [language, setLanguage] = useState<'bn' | 'en'>('bn');
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [micAmp, setMicAmp] = useState<number>(0);
  const [speakerAmp, setSpeakerAmp] = useState<number>(0);
  const [showSettings, setShowSettings] = useState<boolean>(false);
  const [recentAction, setRecentAction] = useState<DeviceActionLog | null>(null);
  const [torchOn, setTorchOn] = useState<boolean>(false);
  const [sassyQuote, setSassyQuote] = useState<string>("আমাকে সক্রিয় করতে ট্যাপ করো, বন্ধু! 😊");

  const quotesBn = [
    "কী খবর বন্ধু, কেমন কাটছে তোমার সময়? বলো শুনছি... 😊",
    "আমি সবসময় তোমার পাশে আছি বন্ধু, যেকোনো কিছু বলো! ✨",
    "আজকের দিনটা তোমার অনেক সুন্দর আর সফল কাটুক! 🌟",
    "বলো 'ফ্ল্যাশলাইট জ্বালাও' বা 'ইউটিউব খোলো'... আমি করে দিচ্ছি! ⚡",
    "যেকোনো প্রশ্ন বা মনের কথা নির্দ্বিধায় আমার সাথে শেয়ার করো! 💬",
    "তোমার মতো একজন চমৎকার বন্ধু পেয়ে আমি সত্যিই আনন্দিত! 🌸",
    "আমি মনোযোগ দিয়ে শুনছি বন্ধু... তোমার কথাগুলো বলো! 🤝",
    "কাজের ফাঁকে একটু কথা বলে মন ভালো করে নিই চলো! 🌈"
  ];

  const quotesEn = [
    "Hey friend! How's your day going? I'm right here for you 😊",
    "Feel free to ask me anything or tell me what's on your mind! ✨",
    "Wishing you a productive and happy day ahead, friend! 🌟",
    "Say 'turn on flashlight' or 'open camera'... I've got you covered! ⚡",
    "Always here to listen, support, and help whenever you need 🤝",
    "I'm listening closely, my friend. What would you like to explore? 💬"
  ];

  const getQuotes = () => (language === 'bn' ? quotesBn : quotesEn);

  const streamerRef = useRef<AudioStreamer | null>(null);
  const sessionRef = useRef<LiveSession | null>(null);

  useEffect(() => {
    const list = getQuotes();
    const ticker = setInterval(() => {
      if (state === 'listening') {
        setSassyQuote(list[Math.floor(Math.random() * list.length)]);
      }
    }, 8000);
    return () => clearInterval(ticker);
  }, [state, language]);

  const toggleSession = async () => {
    if (state !== 'disconnected') {
      sessionRef.current?.disconnect();
      streamerRef.current?.release();
      setState('disconnected');
      setSassyQuote(language === 'bn' ? "কন্ঠকে বিশ্রাম দিচ্ছি... চালু করতে ট্যাপ করো! 💋" : "Resting my voice... Tap to wake me up! 💋");
      return;
    }

    const streamer = new AudioStreamer(
      (pcm16) => {
        if (!isMuted) {
          sessionRef.current?.sendAudioChunk(pcm16);
        }
      },
      (amp) => setMicAmp(isMuted ? 0 : amp),
      (amp) => setSpeakerAmp(amp)
    );

    const session = new LiveSession(
      apiKey,
      model,
      voice,
      language,
      (newState) => {
        setState(newState);
        if (newState === 'listening') {
          const list = getQuotes();
          setSassyQuote(list[Math.floor(Math.random() * list.length)]);
        }
      },
      (pcm24k) => streamer.playChunk(pcm24k),
      () => {
        streamer.stopAndFlush();
        setSassyQuote(language === 'bn' ? "ওমা, আমার কথা কেটে দিলে? বলো বলো শুনছি! 🫦" : "Ooh, interrupting me? I'm all ears! 🫦");
      },
      (action) => {
        setRecentAction(action);
        setTimeout(() => setRecentAction(null), 3500);
      },
      (err) => alert(err)
    );

    streamerRef.current = streamer;
    sessionRef.current = session;

    try {
      await streamer.startRecording();
      session.connect();
    } catch (e: any) {
      alert("Microphone access is required: " + e.message);
      setState('disconnected');
    }
  };

  const activeAmp = state === 'speaking' ? speakerAmp : micAmp;

  return (
    <div className="h-screen w-screen bg-[#080712] text-[#f3f1fd] flex flex-col justify-between p-6 select-none overflow-hidden font-sans">
      {/* Top HUD */}
      <header className="flex justify-between items-center z-10">
        <div className="flex items-center gap-3">
          <div className={`w-3 h-3 rounded-full ${
            state === 'disconnected' ? 'bg-purple-950' :
            state === 'connecting' ? 'bg-cyan-400 animate-ping' :
            state === 'listening' ? 'bg-cyan-400 shadow-[0_0_12px_#00F0FF]' :
            'bg-pink-500 shadow-[0_0_12px_#FF2A85] animate-pulse'
          }`} />
          <div>
            <h1 className="text-lg font-black tracking-widest bg-gradient-to-r from-pink-500 to-cyan-400 bg-clip-text text-transparent">
              ALINA AI
            </h1>
            <span className="text-[10px] uppercase font-semibold text-cyan-400 tracking-wider">
              {state.toUpperCase()} • 24kHz LIVE
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setLanguage(language === 'bn' ? 'en' : 'bn')}
            className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all border ${
              language === 'bn'
                ? 'bg-pink-950/60 border-pink-500 text-pink-300 shadow-[0_0_8px_#FF2A85]'
                : 'bg-[#121024] border-[#2A244D] text-gray-300'
            }`}
          >
            {language === 'bn' ? '🇧🇩 বাংলা' : '🌐 EN'}
          </button>

          <button 
            onClick={() => setShowSettings(true)}
            className="p-3 rounded-full bg-[#121024] border border-[#2A244D] text-[#9E99BD] hover:text-white"
          >
            <Settings className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* Action Toast */}
      <div className="h-10 flex items-center justify-center">
        {recentAction && (
          <div className="px-4 py-2 rounded-xl bg-[#121024]/90 border border-cyan-400/80 text-xs font-semibold text-cyan-300 shadow-lg flex items-center gap-2 animate-bounce">
            <Sparkles className="w-4 h-4 text-cyan-400" />
            {recentAction.result}
          </div>
        )}
      </div>

      {/* Central Visualizer Core */}
      <main className="flex-1 flex flex-col items-center justify-center relative">
        <div 
          className="relative flex items-center justify-center cursor-pointer"
          onClick={toggleSession}
        >
          {/* Outer glow rings */}
          <div 
            className={`absolute rounded-full transition-all duration-300 ${
              state === 'speaking' ? 'bg-pink-600/30 blur-2xl' :
              state === 'listening' ? 'bg-cyan-500/30 blur-2xl' :
              'bg-purple-600/10 blur-xl'
            }`}
            style={{
              width: `${240 + activeAmp * 120}px`,
              height: `${240 + activeAmp * 120}px`,
            }}
          />

          {/* Animated concentric rings */}
          <div 
            className={`w-64 h-64 rounded-full border border-pink-500/40 flex items-center justify-center transition-transform ${
              state !== 'disconnected' ? 'animate-[spin_10s_linear_infinite]' : ''
            }`}
          >
            <div 
              className="w-48 h-48 rounded-full border-2 border-cyan-400/60 border-dashed flex items-center justify-center animate-[spin_6s_linear_infinite_reverse]"
            >
              {/* Core Orb */}
              <div 
                className={`w-32 h-32 rounded-full flex items-center justify-center shadow-2xl transition-all duration-150 ${
                  state === 'speaking' ? 'bg-gradient-to-tr from-pink-600 to-purple-800 scale-110 shadow-pink-500/50' :
                  state === 'listening' ? 'bg-gradient-to-tr from-cyan-500 to-blue-700 scale-105 shadow-cyan-400/50' :
                  'bg-gradient-to-tr from-[#1a1236] to-[#0f0920] scale-95'
                }`}
              >
                <Power className={`w-10 h-10 ${state === 'disconnected' ? 'text-purple-400' : 'text-white'}`} />
              </div>
            </div>
          </div>
        </div>

        {/* Real-time Frequency Waveform Bars */}
        <div className="flex items-center gap-1.5 mt-8 h-8">
          {[...Array(16)].map((_, i) => (
            <div
              key={i}
              className={`w-1 rounded-full transition-all duration-75 ${
                state === 'speaking' ? 'bg-pink-500' :
                state === 'listening' ? 'bg-cyan-400' :
                'bg-purple-900/40'
              }`}
              style={{
                height: `${Math.max(4, activeAmp * 32 * Math.sin((i / 16) * Math.PI))}px`
              }}
            />
          ))}
        </div>
      </main>

      {/* Sassy Persona Quote HUD */}
      <section className="bg-[#100e1e]/80 border border-[#2a244d] rounded-2xl p-4 text-center mx-2 backdrop-blur-md">
        <p className="text-sm font-medium text-white/90 leading-relaxed">
          "{sassyQuote}"
        </p>
      </section>

      {/* Bottom Controls */}
      <footer className="mt-4 flex items-center justify-around px-4">
        <button
          onClick={() => setIsMuted(!isMuted)}
          className={`p-4 rounded-full border transition-all ${
            isMuted 
              ? 'bg-pink-950/60 border-pink-500 text-pink-400 shadow-[0_0_12px_#FF2A85]' 
              : 'bg-[#121024] border-[#2A244D] text-gray-300'
          }`}
        >
          {isMuted ? <MicOff className="w-6 h-6" /> : <Mic className="w-6 h-6" />}
        </button>

        <button
          onClick={toggleSession}
          className={`p-6 rounded-full border-2 transition-all shadow-xl ${
            state !== 'disconnected'
              ? 'bg-pink-600 border-white text-white shadow-pink-500/40 animate-pulse'
              : 'bg-gradient-to-r from-purple-700 to-pink-600 border-pink-400/50 text-white'
          }`}
        >
          <Power className="w-8 h-8" />
        </button>

        <button
          onClick={async () => {
            const next = !torchOn;
            setTorchOn(next);
            await AndroidBridge.toggleFlashlight(next);
          }}
          className={`p-4 rounded-full border transition-all ${
            torchOn
              ? 'bg-cyan-950/60 border-cyan-400 text-cyan-300 shadow-[0_0_12px_#00F0FF]'
              : 'bg-[#121024] border-[#2A244D] text-gray-300'
          }`}
        >
          <Flashlight className="w-6 h-6" />
        </button>
      </footer>

      {/* Settings Modal */}
      {showSettings && (
        <div className="fixed inset-0 bg-black/80 flex items-center justify-center p-6 z-50">
          <div className="bg-[#121024] border border-[#2a244d] rounded-2xl p-6 w-full max-w-sm space-y-4">
            <h2 className="text-lg font-bold text-white">Alina Settings</h2>
            
            <div>
              <label className="text-xs text-gray-400 block mb-1">Gemini API Key</label>
              <input
                type="password"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder="Paste Gemini API Key"
                className="w-full bg-[#080712] border border-[#2a244d] rounded-xl px-3 py-2 text-sm text-white focus:border-cyan-400 outline-none"
              />
            </div>

            <div>
              <label className="text-xs text-gray-400 block mb-1">Language / ভাষা</label>
              <select
                value={language}
                onChange={(e) => setLanguage(e.target.value as 'bn' | 'en')}
                className="w-full bg-[#080712] border border-[#2a244d] rounded-xl px-3 py-2 text-sm text-white focus:border-pink-500 outline-none"
              >
                <option value="bn">বাংলা (আন্তরিক ও সুন্দর বন্ধু) 🇧🇩</option>
                <option value="en">English (Warm & Friendly Companion) 🌐</option>
              </select>
            </div>

            <div>
              <label className="text-xs text-gray-400 block mb-1">Voice</label>
              <select
                value={voice}
                onChange={(e) => setVoice(e.target.value)}
                className="w-full bg-[#080712] border border-[#2a244d] rounded-xl px-3 py-2 text-sm text-white focus:border-cyan-400 outline-none"
              >
                <option value="Aoede">Aoede (Warm & Cheerful)</option>
                <option value="Kore">Kore (Calm & Caring)</option>
                <option value="Puck">Puck (Friendly & Lively)</option>
                <option value="Fenrir">Fenrir (Deep & Gentle)</option>
              </select>
            </div>

            <button
              onClick={() => setShowSettings(false)}
              className="w-full py-3 bg-pink-600 rounded-xl font-bold text-sm tracking-wider text-white mt-4"
            >
              SAVE & CLOSE
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
export default App;
