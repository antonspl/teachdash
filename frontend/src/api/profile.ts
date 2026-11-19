import { apiRequest } from './client'
import type { User } from '../auth/AuthProvider'

export type ProfileUpdate = { name: string; timeZone: string }
export type PasswordChange = { currentPassword: string; newPassword: string }

export async function updateProfile(update: ProfileUpdate): Promise<User> {
  const response = await apiRequest('me', { method: 'PATCH', body: update })
  return (await response.json()) as User
}

// The backend answers 204 with no body, so none is parsed.
export async function changePassword(change: PasswordChange): Promise<void> {
  await apiRequest('me/password', { method: 'POST', body: change })
}
