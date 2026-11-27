import { expect, test } from 'vitest'
import { leaves } from './bundleTools'
import en from './en.json'
import ru from './ru.json'

const NBSP = ' '
const CYRILLIC_ALLOW = new Set(['app.name', 'uiLanguage.name.en'])
const DENY = [/учетн/iu, /пожалуйста/iu, /преподавател/iu]
// `\b` is ASCII-only, so the whole word is found with Unicode lookarounds.
const WHOLE_WORD_YESHCHO = /(?<!\p{L})еще(?!\p{L})/iu

function problemsOf(bundle: Array<[string, string]>) {
  const problems: string[] = []
  for (const [key, text] of bundle) {
    if (text.includes('...')) problems.push(`${key}: three dots`)
    for (const rule of DENY) {
      if (rule.test(text)) problems.push(`${key}: deny-listed ${rule.source}`)
    }
    if (WHOLE_WORD_YESHCHO.test(text)) problems.push(`${key}: whole word еще`)
    if (!CYRILLIC_ALLOW.has(key) && !/\p{Script=Cyrillic}/u.test(text)) {
      problems.push(`${key}: no Cyrillic letter`)
    }
  }
  return problems
}

const ruLeaves = leaves(ru)
const enLeaves = leaves(en)
const ruValue = (key: string) => ruLeaves.find(([k]) => k === key)?.[1]

test('[web-client/R15-S5] The Russian copy follows the typographic rules', () => {
  // no three dots, no deny-listed form, Cyrillic everywhere but the allow-list
  expect(problemsOf(ruLeaves)).toEqual([])
  // no three dots in en either
  expect(enLeaves.filter(([, text]) => text.includes('...'))).toEqual([])

  // a number and its unit are joined by U+00A0, asserted by character code
  const unit = /(\d+)(.)(символов|байт|дней)/u
  const withUnit = ruLeaves.filter(([, text]) => unit.test(text))
  expect(withUnit.map(([key]) => key).sort()).toEqual([
    'issuedLink.note',
    'password.tooLong',
    'password.tooShort',
  ])
  for (const [key, text] of withUnit) {
    expect(text.match(unit)![2].charCodeAt(0), key).toBe(0xa0)
  }
  expect(ruValue('password.tooShort')).toContain(`8${NBSP}символов`)
  expect(ruValue('password.tooLong')).toContain(`72${NBSP}байт`)
  expect(ruValue('issuedLink.note')).toContain(`7${NBSP}дней`)

  // the same text in both languages where it is a name
  expect(ruValue('app.name')).toBe(en.app.name)
  expect(ru.uiLanguage.name.en).toBe(en.uiLanguage.name.en)
  expect(ru.uiLanguage.name.ru).toBe(en.uiLanguage.name.ru)

  // admin row buttons stay short enough to wrap, not truncate
  for (const [key, text] of ruLeaves.filter(([k]) =>
    k.startsWith('adminTeachers.actions.'),
  )) {
    expect(text.length, key).toBeLessThanOrEqual(24)
  }
})

test('[web-client/R15-S5] The whole-word check on a synthetic bundle', () => {
  const failing = problemsOf([
    ['a', 'Еще раз'],
    ['b', 'Учетная запись'],
  ])
  expect(failing.some((p) => p.startsWith('a:'))).toBe(true)
  expect(failing.some((p) => p.startsWith('b:'))).toBe(true)
  expect(
    problemsOf([
      ['c', 'ещё'],
      ['d', 'вещество'],
    ]),
  ).toEqual([])
  expect(problemsOf([['f', 'Latin only']])).toEqual(['f: no Cyrillic letter'])
})
