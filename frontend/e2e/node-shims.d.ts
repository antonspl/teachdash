// Only what the E2E files use from Node; the project has no @types/node.
declare module 'node:fs' {
  export function readFileSync(path: URL, encoding: 'utf8'): string
}
