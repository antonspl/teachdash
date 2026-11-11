// Zones offered on forms: the browser's list plus UTC, and the browser's own zone.

export function browserTimeZone(): string | undefined {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || undefined
  } catch {
    return undefined
  }
}

// An extra zone (e.g. the one already stored for a user) is offered even if the browser lacks it.
export function timeZoneOptions(extra?: string): string[] {
  const zones = new Set<string>(['UTC'])
  if (extra) {
    zones.add(extra)
  }
  const own = browserTimeZone()
  if (own) {
    zones.add(own)
  }
  if (typeof Intl.supportedValuesOf === 'function') {
    try {
      for (const zone of Intl.supportedValuesOf('timeZone')) {
        zones.add(zone)
      }
    } catch {
      // Keep the short list.
    }
  }
  return [...zones].sort()
}

// The browser's zone when known, otherwise UTC.
export function defaultTimeZone(): string {
  return browserTimeZone() ?? 'UTC'
}
