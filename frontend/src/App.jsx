import { BrowserRouter, Routes, Route } from 'react-router'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute, AdminRoute } from './components/ProtectedRoute'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { JobListPage } from './pages/JobListPage'
import { JobDetailPage } from './pages/JobDetailPage'
import { AdminUploadPage } from './pages/admin/AdminUploadPage'
import { AdminJobListPage } from './pages/admin/AdminJobListPage'
import { AdminUserListPage } from './pages/admin/AdminUserListPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<JobListPage />} />
            <Route path="/jobs/:id" element={<JobDetailPage />} />
            <Route element={<AdminRoute />}>
              <Route path="/admin/upload" element={<AdminUploadPage />} />
              <Route path="/admin/jobs" element={<AdminJobListPage />} />
              <Route path="/admin/users" element={<AdminUserListPage />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
