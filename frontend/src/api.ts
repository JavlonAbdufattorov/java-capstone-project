export type Role = 'admin' | 'doctor' | 'patient'
export type Session = { role: Role; token: string; name: string }
export type Doctor = { id: number; name: string; specialty: string; email: string; phone: string; availableTimes: string[] }
export type Patient = { id: number; name: string; email: string; phone: string; address: string }
export type Appointment = { id: number; doctorId: number; doctorName: string; patientId: number; patientName: string; patientEmail: string; patientPhone: string; patientAddress: string; appointmentTime: string; status: number }
export type Prescription = { medication: string; dosage: string; doctorNotes: string }
export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) { super(message); this.status = status }
}
export const part = (value: string | number) => encodeURIComponent(value)
export async function api<T>(path: string, method = 'GET', body?: unknown, signal?: AbortSignal): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, { method, signal, headers: body ? { 'Content-Type': 'application/json' } : {}, body: body ? JSON.stringify(body) : undefined })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new Error('Не удалось связаться с сервером. Проверьте, запущен ли backend.')
  }
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new ApiError(data.message || `Ошибка сервера (${response.status})`, response.status)
  return data as T
}
