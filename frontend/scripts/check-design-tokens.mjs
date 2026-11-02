// Fails when the look is defined outside src/styles/tokens.css:
// - in CSS: raw colours, px or rem lengths (a bare 0 is fine, and @media conditions are
//   not checked because var() does not work there), a font that is not exactly one
//   var(...), and any custom property definition (it would hide raw values);
// - in .ts and .tsx, read through the TypeScript syntax tree: an inline style (a JSX
//   style attribute, or a style property in the props object of createElement);
// - in src/pages/: CSS imports and className (pages use src/ui/ components).
// Other units (em, %, vw, ...) are not checked: they are relative, so they do not
// fix a design value. Test files in src/pages/ are exempt from the two page rules,
// because they do not ship and only render the pages.
import { readFileSync, readdirSync } from 'node:fs'
import { join, relative, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'

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
const SINGLE_VAR = /^var\((?:[^()]|\([^()]*\))*\)$/
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
    if (property.startsWith('--')) {
      problems.push(
        `${at}: custom property "${property}" outside tokens.css; define it there`,
      )
    }
    if (
      (property === 'font-family' || property === 'font') &&
      !SINGLE_VAR.test(value.trim())
    ) {
      problems.push(
        `${at}: raw font in "${property}"; use exactly one var(...)`,
      )
    }
  }
}

function isNamed(name, wanted) {
  return (
    name !== undefined &&
    (ts.isIdentifier(name) || ts.isStringLiteral(name)) &&
    name.text === wanted
  )
}

function isCreateElementCall(node) {
  if (!ts.isCallExpression(node)) {
    return false
  }
  const callee = node.expression
  return (
    (ts.isIdentifier(callee) && callee.text === 'createElement') ||
    (ts.isPropertyAccessExpression(callee) &&
      callee.name.text === 'createElement')
  )
}

function checkScript(file, path, problems) {
  const text = readFileSync(file, 'utf8')
  const source = ts.createSourceFile(
    file,
    text,
    ts.ScriptTarget.Latest,
    true,
    file.endsWith('.tsx') ? ts.ScriptKind.TSX : ts.ScriptKind.TS,
  )
  const isPage = path.startsWith('src/pages/') && !/\.test\.tsx?$/.test(path)
  const report = (node, message) => {
    const line = source.getLineAndCharacterOfPosition(node.getStart()).line + 1
    problems.push(`${relative(root, file)}:${line}: ${message}`)
  }
  const reportCssImport = (specifier) => {
    if (
      isPage &&
      specifier &&
      ts.isStringLiteralLike(specifier) &&
      /\.css(?:\?.*)?$/.test(specifier.text)
    ) {
      report(specifier, 'CSS import in a page; use a src/ui/ component')
    }
  }

  function visit(node) {
    if (ts.isJsxAttribute(node)) {
      if (isNamed(node.name, 'style')) {
        report(node, 'inline style attribute; use a CSS Module')
      }
      if (isPage && isNamed(node.name, 'className')) {
        report(node, 'className in a page; use a src/ui/ component')
      }
    }
    if (ts.isObjectLiteralExpression(node)) {
      const inCreateElement =
        isCreateElementCall(node.parent) && node.parent.arguments[1] === node
      for (const property of node.properties) {
        if (
          inCreateElement &&
          (ts.isPropertyAssignment(property) ||
            ts.isShorthandPropertyAssignment(property)) &&
          isNamed(property.name, 'style')
        ) {
          report(property, 'inline style property; use a CSS Module')
        }
        if (
          isPage &&
          (ts.isPropertyAssignment(property) ||
            ts.isShorthandPropertyAssignment(property)) &&
          isNamed(property.name, 'className')
        ) {
          report(property, 'className in a page; use a src/ui/ component')
        }
      }
    }
    if (ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) {
      reportCssImport(node.moduleSpecifier)
    }
    if (
      ts.isCallExpression(node) &&
      node.expression.kind === ts.SyntaxKind.ImportKeyword
    ) {
      reportCssImport(node.arguments[0])
    }
    ts.forEachChild(node, visit)
  }
  visit(source)
}

const problems = []
for (const file of filesUnder(srcDir)) {
  const path = relative(root, file).split(sep).join('/')
  if (file.endsWith('.css') && path !== tokensFile.split(sep).join('/')) {
    checkCss(file, problems)
  } else if (file.endsWith('.tsx') || file.endsWith('.ts')) {
    checkScript(file, path, problems)
  }
}

if (problems.length > 0) {
  console.error(problems.join('\n'))
  process.exit(1)
}
