/**
 * AndroidBridge.ts
 * Communicates with the native Android layer via Capacitor / JavascriptInterface
 * with seamless fallbacks for browser testing.
 */

declare global {
  interface Window {
    AndroidBridge?: {
      toggleFlashlight: (enable: boolean) => string;
      getBatteryStatus: () => string;
      openWebsite: (url: string) => string;
      openApp: (appName: string) => string;
      vibrateDevice: (durationMs: number) => string;
      adjustVolume: (percent: number) => string;
    };
  }
}

export class AndroidBridge {
  public static async toggleFlashlight(enabled: boolean): Promise<string> {
    if (window.AndroidBridge?.toggleFlashlight) {
      return window.AndroidBridge.toggleFlashlight(enabled);
    }
    // Web fallback simulation
    return enabled ? "Flashlight turned ON ✨ (Simulated)" : "Flashlight turned OFF 🌑 (Simulated)";
  }

  public static async getBatteryStatus(): Promise<string> {
    if (window.AndroidBridge?.getBatteryStatus) {
      return window.AndroidBridge.getBatteryStatus();
    }
    if ('getBattery' in navigator) {
      try {
        const battery: any = await (navigator as any).getBattery();
        const pct = Math.round(battery.level * 100);
        return `Battery is at ${pct}%, ${battery.charging ? 'charging ⚡' : 'discharging'}`;
      } catch (_) {}
    }
    return "Battery is at 88%, discharging.";
  }

  public static async openWebsite(url: string): Promise<string> {
    if (window.AndroidBridge?.openWebsite) {
      return window.AndroidBridge.openWebsite(url);
    }
    window.open(url, '_blank');
    return `Opened ${url} in browser 🌐`;
  }

  public static async openApp(appName: string): Promise<string> {
    if (window.AndroidBridge?.openApp) {
      return window.AndroidBridge.openApp(appName);
    }
    return `Opening ${appName}... 📱`;
  }

  public static async vibrate(durationMs: number = 300): Promise<string> {
    if (window.AndroidBridge?.vibrateDevice) {
      return window.AndroidBridge.vibrateDevice(durationMs);
    }
    if (navigator.vibrate) {
      navigator.vibrate(durationMs);
    }
    return `Vibrated device for ${durationMs}ms 📳`;
  }

  public static async adjustVolume(percent: number): Promise<string> {
    if (window.AndroidBridge?.adjustVolume) {
      return window.AndroidBridge.adjustVolume(percent);
    }
    return `Volume set to ${percent}% 🔊`;
  }
}
