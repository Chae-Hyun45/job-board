import { Link, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../context/AuthContext'

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <>
      <nav className="nav">
        <Link to="/" className="nav-brand">JobBoard</Link>
        <div className="nav-links">
          <Link to="/">채용공고</Link>
          {user?.role === 'ADMIN' && (
            <>
              <Link to="/admin/upload">PDF 업로드</Link>
              <Link to="/admin/jobs">공고 관리</Link>
              <Link to="/admin/users">회원 관리</Link>
            </>
          )}
        </div>
        <div className="nav-user">
          {user?.name && <span>{user.name}님</span>}
          <button type="button" className="btn btn-secondary btn-sm" onClick={handleLogout}>로그아웃</button>
        </div>
      </nav>
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
