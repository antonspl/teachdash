import { leaves } from './bundleTools'

export type ParityProblem = { lang: string; key: string; problem: string }

const PLURAL_SUFFIX = /_(zero|one|two|few|many|other)$/

function variables(text: string): Set<string> {
  return new Set(
    Array.from(text.matchAll(/\{\{\s*([^}\s,]+)[^}]*\}\}/g), (m) => m[1]),
  )
}

function sameSet(a: Set<string>, b: Set<string>) {
  return a.size === b.size && [...a].every((item) => b.has(item))
}

function list(items: Iterable<string>) {
  return [...items].join(', ')
}

function stemOf(key: string): string | null {
  return PLURAL_SUFFIX.test(key) ? key.replace(PLURAL_SUFFIX, '') : null
}

function categoriesOf(lang: string): string[] {
  return new Intl.PluralRules(lang).resolvedOptions().pluralCategories
}

// The leaves of one language: plain keys, and plural bases with their forms.
function split(bundle: unknown) {
  const plain = new Map<string, string>()
  const plurals = new Map<string, Map<string, string>>()
  for (const [key, text] of leaves(bundle)) {
    const stem = stemOf(key)
    if (stem === null) {
      plain.set(key, text)
    } else {
      const forms = plurals.get(stem) ?? new Map<string, string>()
      forms.set(key.slice(stem.length + 1), text)
      plurals.set(stem, forms)
    }
  }
  return { plain, plurals }
}

// Compares every language with the base one: keys, plural categories and
// `{{variables}}`. Returns one problem per broken key, never throws.
export function checkParity(
  bundles: Record<string, unknown>,
  baseLang = 'en',
): ParityProblem[] {
  const problems: ParityProblem[] = []
  const add = (lang: string, key: string, problem: string) =>
    problems.push({ lang, key, problem })
  const parts = new Map(
    Object.entries(bundles).map(([lang, bundle]) => [lang, split(bundle)]),
  )
  const base = parts.get(baseLang) ?? split({})

  for (const [lang, { plain, plurals }] of parts) {
    const expected = categoriesOf(lang)
    // Plural categories, in every language.
    for (const [stem, forms] of plurals) {
      for (const category of expected) {
        if (!forms.has(category)) {
          add(
            lang,
            `${stem}_${category}`,
            `missing plural form (${lang} expects ${list(expected)})`,
          )
        }
      }
      for (const category of forms.keys()) {
        if (!expected.includes(category)) {
          add(
            lang,
            `${stem}_${category}`,
            `extra plural form (${lang} expects ${list(expected)})`,
          )
        }
      }
    }
    if (lang === baseLang) {
      continue
    }
    // Plain keys.
    for (const [key, text] of base.plain) {
      const other = plain.get(key)
      if (other === undefined) {
        add(lang, key, 'missing')
      } else if (other.trim() === '') {
        add(lang, key, 'empty')
      } else if (!sameSet(variables(text), variables(other))) {
        add(
          lang,
          key,
          `variables differ (${baseLang}: ${list(variables(text))}; ${lang}: ${list(variables(other))})`,
        )
      }
    }
    for (const key of plain.keys()) {
      if (!base.plain.has(key)) {
        add(lang, key, `no ${baseLang} counterpart`)
      }
    }
    // Plural bases: the base language's `_other` text is the variable baseline.
    for (const stem of base.plurals.keys()) {
      if (!plurals.has(stem)) {
        for (const category of expected) {
          add(lang, `${stem}_${category}`, 'missing')
        }
      }
    }
    for (const [stem, forms] of plurals) {
      const baseForms = base.plurals.get(stem)
      if (baseForms === undefined) {
        for (const category of forms.keys()) {
          add(lang, `${stem}_${category}`, `no ${baseLang} counterpart`)
        }
        continue
      }
      checkForms(lang, stem, forms, variables(baseForms.get('other') ?? ''))
    }
  }
  // The base language's own plural forms against its `_other` baseline.
  for (const [stem, forms] of base.plurals) {
    checkForms(baseLang, stem, forms, variables(forms.get('other') ?? ''))
  }

  function checkForms(
    lang: string,
    stem: string,
    forms: Map<string, string>,
    baseline: Set<string>,
  ) {
    for (const [category, text] of forms) {
      const key = `${stem}_${category}`
      if (text.trim() === '') {
        add(lang, key, 'empty')
        continue
      }
      const own = variables(text)
      const extra = [...own].filter((name) => !baseline.has(name))
      const missing = [...baseline].filter((name) => !own.has(name))
      const mayDropCount = category !== 'other'
      const droppedTooMuch = mayDropCount
        ? missing.some((name) => name !== 'count')
        : missing.length > 0
      if (extra.length > 0 || droppedTooMuch) {
        add(
          lang,
          key,
          `variables differ (baseline: ${list(baseline)}; ${lang}: ${list(own)})`,
        )
      }
    }
  }
  return problems
}

export function formatProblems(problems: ParityProblem[]): string {
  return problems.map((p) => `${p.lang}: ${p.key}: ${p.problem}`).join('\n')
}
