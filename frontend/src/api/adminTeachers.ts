import { apiRequest } from './client'

export type TeacherStatus = 'ACTIVE' | 'WAITING' | 'DEACTIVATED'

export type Teacher = {
  id: string
  name: string
  email: string
  role: string
  timeZone: string
  active: boolean
  // Missing until the backend sends it (#70); a missing value is read as true.
  hasPassword: boolean
}

export type NewTeacher = { name: string; email: string; timeZone: string }

async function readJson(response: Response): Promise<unknown> {
  return (await response.json()) as unknown
}

function stringField(body: unknown, key: string): string | undefined {
  if (typeof body === 'object' && body !== null && key in body) {
    const value = (body as Record<string, unknown>)[key]
    return typeof value === 'string' ? value : undefined
  }
  return undefined
}

function requireLink(body: unknown, key: string): string {
  const link = stringField(body, key)
  if (link === undefined) {
    throw new Error(`Response has no ${key}`)
  }
  return link
}

function toTeacher(raw: unknown): Teacher {
  const item = raw as Record<string, unknown>
  return {
    id: String(item.id),
    name: String(item.name),
    email: String(item.email),
    role: String(item.role),
    timeZone: String(item.timeZone),
    active: item.active === true,
    hasPassword: item.hasPassword !== false,
  }
}

export function teacherStatus(teacher: Teacher): TeacherStatus {
  if (!teacher.active) {
    return 'DEACTIVATED'
  }
  return teacher.hasPassword ? 'ACTIVE' : 'WAITING'
}

export async function listTeachers(signal?: AbortSignal): Promise<Teacher[]> {
  const response = await apiRequest('admin/teachers', { signal })
  const body = await readJson(response)
  return Array.isArray(body) ? body.map(toTeacher) : []
}

// Returns the set-password link.
export async function createTeacher(teacher: NewTeacher): Promise<string> {
  const response = await apiRequest('admin/teachers', {
    method: 'POST',
    body: teacher,
  })
  return requireLink(await readJson(response), 'setPasswordLink')
}

export async function deactivateTeacher(id: string): Promise<void> {
  await apiRequest(`admin/teachers/${encodeURIComponent(id)}/deactivate`, {
    method: 'POST',
  })
}

// Returns a link only when the backend answers 200; 204 has no body.
export async function reactivateTeacher(id: string): Promise<string | null> {
  const response = await apiRequest(
    `admin/teachers/${encodeURIComponent(id)}/reactivate`,
    { method: 'POST' },
  )
  if (response.status === 204) {
    return null
  }
  return requireLink(await readJson(response), 'setPasswordLink')
}

export async function issueResetLink(id: string): Promise<string> {
  const response = await apiRequest(
    `admin/users/${encodeURIComponent(id)}/reset-link`,
    { method: 'POST' },
  )
  return requireLink(await readJson(response), 'link')
}
