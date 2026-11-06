// @types/node is not installed; these are the only Node APIs the E2E helpers use.
declare module 'node:fs' {
  export function readFileSync(path: string, encoding: 'utf8'): string
  export function existsSync(path: string): boolean
}
declare module 'node:path' {
  export function resolve(...parts: string[]): string
}
