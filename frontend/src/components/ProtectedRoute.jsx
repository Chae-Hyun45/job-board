import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute() {
  const { user, loading } = useAuth()
  if (loading) return <p>로딩 중...</p>
  if (!user) return <Navigate to="/login" replace />
  return <Outlet />
}

export function AdminRoute() {
  const { user, loading } = useAuth()
  if (loading) return <p>로딩 중...</p>
  if (!user || user.role !== 'ADMIN') return <Navigate to="/" replace />
  return <Outlet />
}
