export const DATE_FORMAT_PRESETS = ['yyyy', 'yyyyMM', 'MMdd', 'yyyyMMdd', 'yyyyMMddHHmmss'] as const;

const EXAMPLE_DATE = { year: 2026, month: 5, day: 23, hour: 14, minute: 30, second: 59 };

function pad(value: number, width: number) {
  return String(value).padStart(width, '0');
}

/** Mirrors the common Java DateTimeFormatter tokens used by the page preview. */
export function dateExample(format?: string) {
  if (!format?.trim()) return '';
  return format
    .trim()
    .replace(/yyyy/g, String(EXAMPLE_DATE.year))
    .replace(/yy/g, String(EXAMPLE_DATE.year).slice(-2))
    .replace(/MM/g, pad(EXAMPLE_DATE.month, 2))
    .replace(/M/g, String(EXAMPLE_DATE.month))
    .replace(/dd/g, pad(EXAMPLE_DATE.day, 2))
    .replace(/d/g, String(EXAMPLE_DATE.day))
    .replace(/HH/g, pad(EXAMPLE_DATE.hour, 2))
    .replace(/H/g, String(EXAMPLE_DATE.hour))
    .replace(/mm/g, pad(EXAMPLE_DATE.minute, 2))
    .replace(/m/g, String(EXAMPLE_DATE.minute))
    .replace(/ss/g, pad(EXAMPLE_DATE.second, 2))
    .replace(/s/g, String(EXAMPLE_DATE.second));
}
