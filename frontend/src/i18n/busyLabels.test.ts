import { expect, test } from 'vitest'
import { leaves } from './bundleTools'
import en from './en.json'
import ru from './ru.json'

const BUSY_KEYS = [
  'login.submitting',
  'setPassword.submitting',
  'profile.saving',
  'profile.changingPassword',
  'adminTeachers.create.submitting',
  'adminTeachers.deactivate.confirming',
  'adminTeachers.busy.inviteLink',
  'adminTeachers.busy.resetLink',
  'adminTeachers.busy.reactivate',
]

const enLeaves = new Map(leaves(en))
const ruLeaves = new Map(leaves(ru))

test('[web-client/R29-S5] The nine busy labels exist in both languages, end with the single ellipsis and differ between languages', () => {
  for (const key of BUSY_KEYS) {
    const english = enLeaves.get(key)
    const russian = ruLeaves.get(key)
    for (const [language, text] of [
      ['en', english],
      ['ru', russian],
    ] as const) {
      expect(text, `${language} ${key}`).toBeTruthy()
      expect(text!.endsWith('…'), `${language} ${key}`).toBe(true)
      expect(text!.endsWith('...'), `${language} ${key}`).toBe(false)
      expect(text!.indexOf('…'), `${language} ${key}`).toBe(text!.length - 1)
    }
    expect(russian, key).not.toBe(english)
  }
})

test('[web-client/R29-S5] A busy label mirrors the name of the idle action it replaces', () => {
  expect(en.adminTeachers.busy.inviteLink).toBe('Creating invite link…')
  expect(en.adminTeachers.busy.resetLink).toBe('Creating reset link…')
  expect(ru.adminTeachers.busy.resetLink).toBe('Создание ссылки для сброса…')
  expect(ru.adminTeachers.busy.reactivate).toBe('Повторная активация…')
  expect(ru.setPassword.submitting).toBe('Сохранение пароля…')
})
