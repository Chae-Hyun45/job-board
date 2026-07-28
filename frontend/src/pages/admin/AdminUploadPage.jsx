import { useState } from 'react'
import { useNavigate } from 'react-router'
import { adminApi } from '../../api/adminApi'

const EMPTY_FORM = {
  pdfFileName: '', companyName: '', location: '', careerLevel: 'NEW', education: 'BACHELOR',
  employmentType: 'FULL_TIME', conditionNote: '', applyStartDate: '', applyEndDate: '',
  applyMethod: '', salaryMin: '', salaryMax: '', salaryNote: '',
}

export function AdminUploadPage() {
  const navigate = useNavigate()
  const [file, setFile] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState(null)

  async function handleExtract() {
    setError(null)
    try {
      const result = await adminApi.extractPdf(file)
      setForm({
        pdfFileName: result.pdfFileName ?? '',
        companyName: result.companyName ?? '',
        location: result.location ?? '',
        careerLevel: result.careerLevel || 'NEW',
        education: result.education || 'BACHELOR',
        employmentType: result.employmentType || 'FULL_TIME',
        conditionNote: result.conditionNote ?? '',
        applyStartDate: result.applyStartDate ?? '',
        applyEndDate: result.applyEndDate ?? '',
        applyMethod: result.applyMethod ?? '',
        salaryMin: result.salaryMin ?? '',
        salaryMax: result.salaryMax ?? '',
        salaryNote: result.salaryNote ?? '',
      })
    } catch (err) {
      setError(err.message)
    }
  }

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      await adminApi.createJobPosting({
        ...form,
        salaryMin: Number(form.salaryMin) || 0,
        salaryMax: Number(form.salaryMax) || 0,
      })
      navigate('/admin/jobs')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h1>채용공고 PDF 업로드</h1>
      {error && <p role="alert">{error}</p>}
      <label htmlFor="pdf-file">PDF 파일</label>
      <input id="pdf-file" aria-label="PDF 파일" type="file" accept="application/pdf"
             onChange={(e) => setFile(e.target.files[0])} />
      <button type="button" onClick={handleExtract} disabled={!file}>PDF에서 추출</button>

      <form onSubmit={handleSubmit}>
        <label htmlFor="companyName">회사명</label>
        <input id="companyName" name="companyName" value={form.companyName} onChange={handleChange} required />
        <label htmlFor="location">위치</label>
        <input id="location" name="location" value={form.location} onChange={handleChange} required />
        <label htmlFor="careerLevel">경력</label>
        <select id="careerLevel" name="careerLevel" value={form.careerLevel} onChange={handleChange}>
          <option value="NEW">신입</option>
          <option value="EXPERIENCED">경력</option>
          <option value="ANY">무관</option>
        </select>
        <label htmlFor="education">학력</label>
        <select id="education" name="education" value={form.education} onChange={handleChange}>
          <option value="NONE">무관</option>
          <option value="HIGH_SCHOOL">고졸</option>
          <option value="ASSOCIATE">전문학사</option>
          <option value="BACHELOR">학사</option>
          <option value="MASTER">석사</option>
        </select>
        <label htmlFor="employmentType">고용형태</label>
        <select id="employmentType" name="employmentType" value={form.employmentType} onChange={handleChange}>
          <option value="FULL_TIME">정규직</option>
          <option value="CONTRACT">계약직</option>
          <option value="INTERN">인턴</option>
          <option value="PART_TIME">파트타임</option>
        </select>
        <label htmlFor="conditionNote">채용조건 비고</label>
        <textarea id="conditionNote" name="conditionNote" value={form.conditionNote} onChange={handleChange} />
        <label htmlFor="applyStartDate">지원 시작일</label>
        <input id="applyStartDate" name="applyStartDate" type="date" value={form.applyStartDate} onChange={handleChange} required />
        <label htmlFor="applyEndDate">지원 종료일</label>
        <input id="applyEndDate" name="applyEndDate" type="date" value={form.applyEndDate} onChange={handleChange} required />
        <label htmlFor="applyMethod">지원방법</label>
        <textarea id="applyMethod" name="applyMethod" value={form.applyMethod} onChange={handleChange} />
        <label htmlFor="salaryMin">예상급여 최소(만원)</label>
        <input id="salaryMin" name="salaryMin" type="number" value={form.salaryMin} onChange={handleChange} />
        <label htmlFor="salaryMax">예상급여 최대(만원)</label>
        <input id="salaryMax" name="salaryMax" type="number" value={form.salaryMax} onChange={handleChange} />
        <label htmlFor="salaryNote">급여 비고</label>
        <input id="salaryNote" name="salaryNote" value={form.salaryNote} onChange={handleChange} />
        <button type="submit">등록</button>
      </form>
    </div>
  )
}
