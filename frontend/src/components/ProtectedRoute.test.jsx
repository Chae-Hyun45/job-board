import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { AuthContextTestProvider } from '../test-utils/AuthContextTestProvider'
import { ProtectedRoute } from './ProtectedRoute'

describe('ProtectedRoute', () => {
  it('로그인하지 않았으면 로그인 페이지로 리다이렉트한다', () => {
    render(
      <AuthContextTestProvider value={{ user: null, loading: false }}>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route path="/login" element={<div>로그인 페이지</div>} />
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<div>보호된 페이지</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContextTestProvider>
    )

    expect(screen.getByText('로그인 페이지')).toBeInTheDocument()
  })

  it('로그인했으면 보호된 페이지를 보여준다', () => {
    render(
      <AuthContextTestProvider value={{ user: { id: 1, role: 'USER' }, loading: false }}>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route path="/login" element={<div>로그인 페이지</div>} />
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<div>보호된 페이지</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContextTestProvider>
    )

    expect(screen.getByText('보호된 페이지')).toBeInTheDocument()
  })
})
