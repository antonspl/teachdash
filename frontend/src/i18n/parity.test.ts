import i18next from 'i18next'
import { expect, test } from 'vitest'
import { leaves, setPath } from './bundleTools'
import en from './en.json'
import ru from './ru.json'
import { checkParity, formatProblems } from './parity'

function bundle(entries: Record<string, string>) {
  const result: Record<string, unknown> = {}
  for (const [path, value] of Object.entries(entries)) {
    setPath(result, path, value)
  }
  return result
}

function keysOf(problems: ReturnType<typeof checkParity>) {
  return problems.map((p) => p.key).sort()
}

// TODO(task 2.1): ru.json is still incomplete; slice 2 fills it and removes `.fails`.
test.fails('[web-client/R15-S1] Every English string has a Russian one', () => {
  const problems = checkParity({ en, ru })
  expect(formatProblems(problems)).toBe('')
  expect(problems).toEqual([])
})

test('[web-client/R15-S2] The parity check names the keys that break it', () => {
  const problems = checkParity({
    en: bundle({
      'a.missing': 'One',
      'a.empty': 'Two',
      'a.dropped': 'Hello {{name}}',
      'a.renamed': 'Hello {{name}}',
      'a.fine': 'Fine {{name}}',
    }),
    ru: bundle({
      'a.empty': '',
      'a.dropped': 'Привет',
      'a.renamed': 'Привет {{title}}',
      'a.fine': 'Хорошо {{name}}',
      'a.extra': 'Лишний',
    }),
  })

  expect(keysOf(problems)).toEqual([
    'a.dropped',
    'a.empty',
    'a.extra',
    'a.missing',
    'a.renamed',
  ])
  const message = formatProblems(problems)
  for (const key of [
    'a.missing',
    'a.empty',
    'a.extra',
    'a.dropped',
    'a.renamed',
  ]) {
    expect(message).toContain(key)
  }
  expect(problems.find((p) => p.key === 'a.missing')?.problem).toBe('missing')
  expect(problems.find((p) => p.key === 'a.empty')?.problem).toBe('empty')
  expect(problems.find((p) => p.key === 'a.extra')?.problem).toContain(
    'no en counterpart',
  )
})

test('[web-client/R16-S1] The parity check accepts the Russian categories', () => {
  const problems = checkParity({
    en: bundle({ item_one: '{{count}} item', item_other: '{{count}} items' }),
    ru: bundle({
      item_one: '{{count}} штука',
      item_few: '{{count}} штуки',
      item_many: '{{count}} штук',
      item_other: '{{count}} штуки',
    }),
  })

  expect(problems).toEqual([])
})

test('[web-client/R16-S2] The parity check names a missing or extra category', () => {
  const problems = checkParity({
    en: bundle({
      item_one: 'one',
      item_other: 'other',
      thing_one: 'one',
      thing_few: 'few',
      thing_other: 'other',
    }),
    ru: bundle({
      item_one: 'раз',
      item_many: 'много',
      item_other: 'другое',
      thing_one: 'раз',
      thing_few: 'несколько',
      thing_many: 'много',
      thing_other: 'другое',
    }),
  })

  const missing = problems.find((p) => p.key === 'item_few')
  expect(missing?.lang).toBe('ru')
  expect(missing?.problem).toContain('one, few, many, other')
  const extra = problems.find((p) => p.key === 'thing_few' && p.lang === 'en')
  expect(extra?.problem).toContain('extra plural form')
  expect(extra?.problem).toContain('one, other')
  expect(formatProblems(problems)).toContain('item_few')
  expect(formatProblems(problems)).toContain('thing_few')
})

test('[web-client/R16-S3] The variables of a plural base match', () => {
  const problems = checkParity({
    en: bundle({ item_one: '{{count}} item', item_other: '{{count}} items' }),
    ru: bundle({
      item_one: 'одна штука',
      item_few: '{{count}} штуки {{x}}',
      item_many: '{{count}} штук',
      item_other: '{{total}} штуки',
    }),
  })

  expect(keysOf(problems)).toEqual(['item_few', 'item_other'])
  expect(problems.map((p) => p.key)).not.toContain('item_one')
})

test('[web-client/R16-S4] A count picks the right Russian form', async () => {
  const instance = i18next.createInstance()
  await instance.init({
    lng: 'ru',
    resources: {
      ru: {
        translation: {
          item_one: 'ONE',
          item_few: 'FEW',
          item_many: 'MANY',
          item_other: 'OTHER',
        },
      },
    },
  })
  const form = (count: number) => instance.t('item', { count })

  expect([1, 21].map(form)).toEqual(['ONE', 'ONE'])
  expect([2, 22].map(form)).toEqual(['FEW', 'FEW'])
  expect([0, 5, 11].map(form)).toEqual(['MANY', 'MANY', 'MANY'])
  expect(form(1.5)).toBe('OTHER')
  expect(new Set(['ONE', 'FEW', 'MANY', 'OTHER']).size).toBe(4)
})

test('leaves lists every string leaf with its path', () => {
  expect(leaves(bundle({ 'a.b': 'x', c: 'y' }))).toEqual([
    ['a.b', 'x'],
    ['c', 'y'],
  ])
})
