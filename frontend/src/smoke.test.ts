import { expect, test } from 'vitest'

test('test runner works in jsdom', () => {
  document.body.innerHTML = '<p>ok</p>'
  expect(document.querySelector('p')?.textContent).toBe('ok')
})
