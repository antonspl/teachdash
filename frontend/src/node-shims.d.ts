// @types/node is not installed; these are the only Node APIs the unit tests and the E2E
// helpers use. The Docker build leaves out `e2e/`, so the shim lives in `src/`; the
// tsconfig includes both folders.
declare module 'node:fs' {
  export function readFileSync(path: string, encoding: 'utf8'): string
  export function existsSync(path: string): boolean
}
declare module 'node:path' {
  export function resolve(...parts: string[]): string
}
