/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        neonMagenta: '#FF2A85',
        neonCyan: '#00F0FF',
        neonPurple: '#9D4EDD',
        obsidianBg: '#080712',
        obsidianSurface: '#100E1E'
      }
    },
  },
  plugins: [],
}
