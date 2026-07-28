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
      {error && <p className="alert" role="alert">{error}</p>}
      {users.length === 0 ? (
        <p className="empty-state">등록된 회원이 없습니다.</p>
      ) : (
        <ul className="admin-list">
          {users.map((user) => (
            <li key={user.id}>
              <span className="item-info">
                <span>{user.email}</span>
                <span className="muted">({user.name})</span>
                <span className="tag">{user.role}</span>
              </span>
              <span className="item-actions">
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => handleToggleRole(user)}>
                  {user.role === 'ADMIN' ? '일반회원으로 변경' : '관리자로 변경'}
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
