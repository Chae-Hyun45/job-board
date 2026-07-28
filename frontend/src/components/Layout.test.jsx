import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { AuthContext } from '../context/AuthContextInstance'
import { Layout } from './Layout'

function renderLayout(user) {
  const logout = vi.fn().mockResolvedValue(undefined)
  render(
    <AuthContext.Provider value={{ user, loading: false, logout }}>
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/login" element={<div>로그인 페이지</div>} />
          <Route element={<Layout />}>
            <Route path="/" element={<div>메인 콘텐츠</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  )
  return { logout }
}

describe('Layout', () => {
  it('일반회원에게는 관리자 메뉴가 보이지 않는다', () => {
    renderLayout({ id: 1, name: '홍길동', role: 'USER' })

    expect(screen.getByText('메인 콘텐츠')).toBeInTheDocument()
    expect(screen.queryByText('공고 관리')).not.toBeInTheDocument()
    expect(screen.queryByText('회원 관리')).not.toBeInTheDocument()
  })

  it('관리자에게는 관리자 메뉴가 보인다', () => {
    renderLayout({ id: 1, name: '관리자', role: 'ADMIN' })

    expect(screen.getByText('PDF 업로드')).toBeInTheDocument()
    expect(screen.getByText('공고 관리')).toBeInTheDocument()
    expect(screen.getByText('회원 관리')).toBeInTheDocument()
  })

  it('로그아웃 버튼을 누르면 로그아웃하고 로그인 페이지로 이동한다', async () => {
    const { logout } = renderLayout({ id: 1, name: '홍길동', role: 'USER' })

    fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))

    await waitFor(() => expect(logout).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(screen.getByText('로그인 페이지')).toBeInTheDocument())
  })
})
