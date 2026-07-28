import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { adminApi } from '../../api/adminApi'
import { AdminUploadPage } from './AdminUploadPage'

vi.mock('../../api/adminApi')

describe('AdminUploadPage', () => {
  beforeEach(() => {
    adminApi.extractPdf.mockResolvedValue({
      pdfFileName: 'stored-uuid.pdf',
      companyName: '테스트회사',
      location: '서울',
      careerLevel: 'NEW',
      education: 'BACHELOR',
      employmentType: 'FULL_TIME',
      conditionNote: '비고',
      applyStartDate: '2026-08-01',
      applyEndDate: '2026-08-31',
      applyMethod: '이메일 접수',
      salaryMin: 3000,
      salaryMax: 3500,
      salaryNote: '협의가능',
    })
    adminApi.createJobPosting.mockResolvedValue({ id: 1 })
  })

  it('PDF를 업로드하면 추출결과가 폼에 채워지고 저장할 수 있다', async () => {
    render(
      <MemoryRouter>
        <AdminUploadPage />
      </MemoryRouter>
    )

    const file = new File(['dummy'], 'posting.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('PDF 파일'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'PDF에서 추출' }))

    await waitFor(() => expect(screen.getByDisplayValue('테스트회사')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '등록' }))

    await waitFor(() => expect(adminApi.createJobPosting).toHaveBeenCalled())
  })

  it('PDF 추출이 실패하면 에러 메시지가 화면에 표시된다', async () => {
    adminApi.extractPdf.mockRejectedValueOnce(new Error('추출 실패'))

    render(
      <MemoryRouter>
        <AdminUploadPage />
      </MemoryRouter>
    )

    const file = new File(['dummy'], 'posting.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('PDF 파일'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'PDF에서 추출' }))

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
  })

  it('AI 추출이 실패해도 파일명이 유지되어 수동 입력으로 등록할 수 있다', async () => {
    adminApi.extractPdf.mockResolvedValueOnce({
      pdfFileName: 'stored-uuid.pdf',
      companyName: null,
      location: null,
      careerLevel: null,
      education: null,
      employmentType: null,
      conditionNote: null,
      applyStartDate: null,
      applyEndDate: null,
      applyMethod: null,
      salaryMin: null,
      salaryMax: null,
      salaryNote: null,
    })

    render(
      <MemoryRouter>
        <AdminUploadPage />
      </MemoryRouter>
    )

    const file = new File(['dummy'], 'posting.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('PDF 파일'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'PDF에서 추출' }))

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())

    fireEvent.change(screen.getByLabelText('회사명'), { target: { value: '수동회사' } })
    fireEvent.change(screen.getByLabelText('위치'), { target: { value: '부산' } })
    fireEvent.change(screen.getByLabelText('지원 시작일'), { target: { value: '2026-08-01' } })
    fireEvent.change(screen.getByLabelText('지원 종료일'), { target: { value: '2026-08-31' } })

    fireEvent.click(screen.getByRole('button', { name: '등록' }))

    await waitFor(() => expect(adminApi.createJobPosting).toHaveBeenCalled())
    expect(adminApi.createJobPosting).toHaveBeenCalledWith(
      expect.objectContaining({ pdfFileName: 'stored-uuid.pdf', companyName: '수동회사', location: '부산' })
    )
  })

  it('등록이 실패하면 에러 메시지가 화면에 표시된다', async () => {
    adminApi.createJobPosting.mockRejectedValueOnce(new Error('등록 실패'))

    render(
      <MemoryRouter>
        <AdminUploadPage />
      </MemoryRouter>
    )

    const file = new File(['dummy'], 'posting.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('PDF 파일'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'PDF에서 추출' }))

    await waitFor(() => expect(screen.getByDisplayValue('테스트회사')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '등록' }))

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
  })
})
