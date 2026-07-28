const BASE_URL = '/api'

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || `요청 실패 (${response.status})`)
  }
  if (response.status === 204) return null
  return response.json()
}

export function get(path) {
  return request(path)
}

export function post(path, body) {
  return request(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
}

export function put(path, body) {
  return request(path, { method: 'PUT', body: JSON.stringify(body) })
}

export function patch(path, body) {
  return request(path, { method: 'PATCH', body: JSON.stringify(body) })
}

export function del(path) {
  return request(path, { method: 'DELETE' })
}

export async function postForm(path, formData) {
  const response = await fetch(`${BASE_URL}${path}`, { method: 'POST', body: formData })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || `요청 실패 (${response.status})`)
  }
  return response.json()
}
