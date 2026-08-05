/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        graphite: '#14171C',
        panel: '#1B2029',
        'panel-raised': '#20262F',
        iron: '#2A303A',
        fog: '#E7EAEE',
        ash: '#8B93A1',
        flare: '#FF5C5C',
        amber: '#FFA53C',
        signal: '#5CA3FF',
      },
      fontFamily: {
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
        sans: ['"IBM Plex Sans"', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
