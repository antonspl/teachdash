import { afterEach, expect, test, vi } from 'vitest'
import { defaultTimeZone, timeZoneOptions } from './timeZones'

afterEach(() => {
  vi.restoreAllMocks()
})

function browserZone(zone: string) {
  vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({
    timeZone: zone,
  } as Intl.ResolvedDateTimeFormatOptions)
}

test('UTC is always offered, once, and the list is sorted', () => {
  vi.spyOn(Intl, 'supportedValuesOf').mockReturnValue([
    'Europe/Berlin',
    'UTC',
    'Asia/Tokyo',
  ])
  const zones = timeZoneOptions()
  expect(zones.filter((z) => z === 'UTC')).toHaveLength(1)
  expect(zones).toEqual([...zones].sort())
})

test('the browser zone is added when the list lacks it and is the default', () => {
  vi.spyOn(Intl, 'supportedValuesOf').mockReturnValue(['Europe/Berlin'])
  browserZone('Europe/Kyiv')
  expect(timeZoneOptions()).toContain('Europe/Kyiv')
  expect(defaultTimeZone()).toBe('Europe/Kyiv')
})

test('without supportedValuesOf only the browser zone and UTC are offered', () => {
  vi.stubGlobal('Intl', {
    DateTimeFormat: Intl.DateTimeFormat,
  })
  browserZone('Europe/Moscow')
  expect(timeZoneOptions()).toEqual(['Europe/Moscow', 'UTC'])
  vi.unstubAllGlobals()
})
