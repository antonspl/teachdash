// @types/node is not installed and the Docker build leaves out `e2e/`, where the E2E
// shims live; the Node APIs the unit tests use to read a file as text are declared here.
declare module 'node:fs' {
  export function readFileSync(path: string, encoding: 'utf8'): string
}
declare module 'node:path' {
  export function resolve(...parts: string[]): string
}
