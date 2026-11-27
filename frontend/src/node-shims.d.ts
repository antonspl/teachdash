// @types/node is not installed; these are the only Node APIs the unit tests and the E2E
// helpers use (the token and CSS rule tests add the file, process and child process calls). The Docker build leaves out `e2e/`, so the shim lives in `src/`; the
// tsconfig includes both folders.
declare module 'node:fs' {
  export function readFileSync(path: string, encoding: 'utf8'): string
  export function existsSync(path: string): boolean
  export function writeFileSync(path: string, data: string): void
  export function mkdirSync(path: string): void
  export function mkdtempSync(prefix: string): string
  export function rmSync(
    path: string,
    options: { recursive: boolean; force: boolean },
  ): void
  export function readdirSync(
    path: string,
    options: { recursive: boolean; withFileTypes: true },
  ): { name: string; parentPath: string; isFile(): boolean }[]
}
declare module 'node:os' {
  export function tmpdir(): string
}
declare module 'node:child_process' {
  export function spawnSync(
    command: string,
    args: string[],
    options: { encoding: 'utf8' },
  ): { status: number | null; stdout: string; stderr: string }
}
declare const process: { execPath: string }
declare module 'node:path' {
  export function resolve(...parts: string[]): string
  export function join(...parts: string[]): string
}
interface ImportMeta {
  /** Directory of the current file; Vitest sets it. */
  readonly dirname: string
}
