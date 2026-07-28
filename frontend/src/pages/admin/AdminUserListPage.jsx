import { useEffect, useState } from 'react'
import { adminApi } from '../../api/adminApi'

export function AdminUserListPage() {
  const [users, setUsers] = useState([])
  const [error, setError] = useState(null)

  function reload() {
    adminApi.listUsers().then(setUsers).catch((err) => setError(err.message))
  }

  useEffect(() => {
    reload()
  }, [])

  async function handleToggleRole(user) {
    const nextRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
    await adminApi.updateUserRole(user.id, nextRole)
    reload()
  }

  return (
    <div>
      <h1>회원 관리</h1>
      {error && <p role="alert">{error}</p>}
      <ul>
        {users.map((user) => (
          <li key={user.id}>
            <span>{user.email}</span> ({user.name}) - {user.role}
            <button type="button" onClick={() => handleToggleRole(user)}>
              {user.role === 'ADMIN' ? '일반회원으로 변경' : '관리자로 변경'}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
