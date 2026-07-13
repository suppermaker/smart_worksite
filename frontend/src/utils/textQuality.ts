const mojibakeMarkers = [
  '\uFFFD',
  '\u6d93',
  '\u93b6',
  '\u7487',
  '\u935b',
  '\u942d',
  '\u8930',
  '\u7459',
  '\u6fb6'
];

export function hasSuspiciousText(value: unknown) {
  const text = String(value ?? '');
  return /\?{2,}/.test(text) || mojibakeMarkers.some((marker) => text.includes(marker));
}
