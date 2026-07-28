import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { adminApi } from '../../api/adminApi'
import { AdminUserListPage } from './AdminUserListPage'

vi.mock('../../api/adminApi')

describe('AdminUserListPage', () => {
  beforeEach(() => {
    adminApi.listUsers.mockResolvedValue([
      { id: 2, email: 'member@jobboard.com', name: '회원1', role: 'USER' },
    ])
    adminApi.updateUserRole.mockResolvedValue({ id: 2, role: 'ADMIN' })
  })

  it('회원 목록을 보여주고 권한을 변경할 수 있다', async () => {
    render(
      <MemoryRouter>
        <AdminUserListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(screen.getByText('member@jobboard.com')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '관리자로 변경' }))

    await waitFor(() => expect(adminApi.updateUserRole).toHaveBeenCalledWith(2, 'ADMIN'))
  })
})
