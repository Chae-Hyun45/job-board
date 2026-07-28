import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { AuthContext } from '../context/AuthContextInstance'
import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
  it('이메일과 비밀번호를 입력하고 제출하면 login이 호출된다', async () => {
    const login = vi.fn().mockResolvedValue({ id: 1, role: 'USER' })

    render(
      <AuthContext.Provider value={{ user: null, loading: false, login }}>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.change(screen.getByLabelText('이메일'), { target: { value: 'user@jobboard.com' } })
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))

    await waitFor(() => expect(login).toHaveBeenCalledWith({ email: 'user@jobboard.com', password: 'password123' }))
  })
})
