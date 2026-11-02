// Fails when the look is defined outside src/styles/tokens.css:
// - raw colours, font families and px or rem lengths in CSS (a bare 0 is fine, and
//   @media conditions are not checked because var() does not work there);
// - inline style attributes in TSX;
// - CSS imports and className in src/pages/ (pages use src/ui/ components).
// Other units (em, %, vw, ...) are not checked: they are relative, so they do not
// fix a design value. Test files in src/pages/ are exempt from the two page rules,
// because they do not ship and only render the pages.
import { readFileSync, readdirSync } from 'node:fs'
import { join, relative, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))
const srcDir = join(root, 'src')
const tokensFile = join('src', 'styles', 'tokens.css')

const NAMED_COLORS =
  `aliceblue antiquewhite aqua aquamarine azure beige bisque black
blanchedalmond blue blueviolet brown burlywood cadetblue chartreuse chocolate coral
cornflowerblue cornsilk crimson cyan darkblue darkcyan darkgoldenrod darkgray darkgreen
darkgrey darkkhaki darkmagenta darkolivegreen darkorange darkorchid darkred darksalmon
darkseagreen darkslateblue darkslategray darkslategrey darkturquoise darkviolet deeppink
deepskyblue dimgray dimgrey dodgerblue firebrick floralwhite forestgreen fuchsia gainsboro
ghostwhite gold goldenrod gray green greenyellow grey honeydew hotpink indianred indigo
ivory khaki lavender lavenderblush lawngreen lemonchiffon lightblue lightcoral lightcyan
lightgoldenrodyellow lightgray lightgreen lightgrey lightpink lightsalmon lightseagreen
lightskyblue lightslategray lightslategrey lightsteelblue lightyellow lime limegreen linen
magenta maroon mediumaquamarine mediumblue mediumorchid mediumpurple mediumseagreen
mediumslateblue mediumspringgreen mediumturquoise mediumvioletred midnightblue mintcream
mistyrose moccasin navajowhite navy oldlace olive olivedrab orange orangered orchid
palegoldenrod palegreen paleturquoise palevioletred papayawhip peachpuff peru pink plum
powderblue purple rebeccapurple red rosybrown royalblue saddlebrown salmon sandybrown
seagreen seashell sienna silver skyblue slateblue slategray slategrey snow springgreen
steelblue tan teal thistle tomato turquoise violet wheat white whitesmoke yellow
yellowgreen`.split(/\s+/)

const HEX = /#[0-9a-fA-F]{3,8}(?![\w-])/
const COLOR_FUNCTION =
  /(?<![\w-])(rgb|rgba|hsl|hsla|hwb|lab|lch|oklab|oklch|color|color-mix)\(/i
const NAMED = new RegExp(
  `(?<![\\w-])(${NAMED_COLORS.join('|')})(?![\\w-])`,
  'i',
)
const RAW_LENGTH = /(?<![\w.-])[+-]?(?:\d+\.?\d*|\.\d+)(?:px|rem)(?![\w-])/i
const CSS_IMPORT =
  /\bimport\s*(?:[^'";]*?\bfrom\s*)?\(?\s*['"][^'"]+\.css(?:\?[^'"]*)?['"]/
const CLASS_NAME = /(?<![\w-])className(?![\w-])/g
const DECLARATION = /[;{}]\s*([\w-]+)\s*:\s*([^;{}]+)/dg

function filesUnder(dir) {
  return readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => join(entry.parentPath, entry.name))
}

function lineOf(text, offset) {
  return text.slice(0, offset).split('\n').length
}

function checkCss(file, problems) {
  const original = readFileSync(file, 'utf8')
  const text = original.replace(/\/\*[\s\S]*?\*\//g, (comment) =>
    comment.replace(/[^\n]/g, ' '),
  )
  for (const match of text.matchAll(DECLARATION)) {
    const property = match[1].toLowerCase()
    const value = match[2]
    const at = `${relative(root, file)}:${lineOf(text, match.indices[1][0])}`
    if (HEX.test(value) || COLOR_FUNCTION.test(value) || NAMED.test(value)) {
      problems.push(`${at}: raw colour in "${property}"; use a token`)
    }
    if (RAW_LENGTH.test(value)) {
      problems.push(`${at}: raw px or rem length in "${property}"; use a token`)
    }
    if (
      (property === 'font-family' || property === 'font') &&
      !value.trim().startsWith('var(')
    ) {
      problems.push(`${at}: raw font in "${property}"; use a token`)
    }
  }
}

function checkTsx(file, path, problems) {
  const text = readFileSync(file, 'utf8')
  for (const match of text.matchAll(/(?<![\w-])style\s*=/g)) {
    const at = `${relative(root, file)}:${lineOf(text, match.index)}`
    problems.push(`${at}: inline style attribute; use a CSS Module`)
  }
  if (!path.startsWith('src/pages/') || /\.test\.tsx?$/.test(path)) {
    return
  }
  const cssImport = CSS_IMPORT.exec(text)
  if (cssImport) {
    const at = `${relative(root, file)}:${lineOf(text, cssImport.index)}`
    problems.push(`${at}: CSS import in a page; use a src/ui/ component`)
  }
  for (const match of text.matchAll(CLASS_NAME)) {
    const at = `${relative(root, file)}:${lineOf(text, match.index)}`
    problems.push(`${at}: className in a page; use a src/ui/ component`)
  }
}

const problems = []
for (const file of filesUnder(srcDir)) {
  const path = relative(root, file).split(sep).join('/')
  if (file.endsWith('.css') && path !== tokensFile.split(sep).join('/')) {
    checkCss(file, problems)
  } else if (file.endsWith('.tsx') || file.endsWith('.ts')) {
    checkTsx(file, path, problems)
  }
}

if (problems.length > 0) {
  console.error(problems.join('\n'))
  process.exit(1)
}
