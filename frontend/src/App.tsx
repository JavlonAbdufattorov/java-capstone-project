import { useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { api, ApiError, part } from './api'
import type { Appointment, Doctor, Patient, Prescription, Role, Session } from './api'
import './App.css'

const roles: Record<Role, string> = { patient: 'Пациент', doctor: 'Врач', admin: 'Администратор' }
const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }
const dateLabel = (value: string) => new Date(value).toLocaleString('ru-RU', { dateStyle: 'medium', timeStyle: 'short' })
const values = (event: FormEvent<HTMLFormElement>) => { event.preventDefault(); return Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string> }
function Field({ label, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return <label>{label}<input required {...props} /></label>
}
function Panel({ title, children, close }: { title: string; children: ReactNode; close: () => void }) {
  return <section className="editor" aria-label={title}><div className="section-heading"><h2>{title}</h2><button className="secondary" onClick={close}>Закрыть</button></div>{children}</section>
}
export default function App() {
  const [session, setSession] = useState<Session | null>(null)
  const [role, setRole] = useState<Role>('patient')
  const [register, setRegister] = useState(false)
  const [section, setSection] = useState('doctors')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(false)
  const [revision, setRevision] = useState(0)
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [patient, setPatient] = useState<Patient | null>(null)
  const [name, setName] = useState('')
  const [specialty, setSpecialty] = useState('')
  const [period, setPeriod] = useState('all')
  const [date, setDate] = useState(today)
  const [doctorEditor, setDoctorEditor] = useState<Doctor | 'new' | null>(null)
  const [booking, setBooking] = useState<{ doctorId: number; id?: number } | null>(null)
  const [bookingDate, setBookingDate] = useState(today)
  const [slots, setSlots] = useState<string[]>([])
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [record, setRecord] = useState<Appointment | null>(null)
  const [prescriptions, setPrescriptions] = useState<Prescription[] | null>(null)
  const [history, setHistory] = useState<Appointment[] | null>(null)
  const token = session ? part(session.token) : ''

  function fail(e: unknown) {
    if (e instanceof ApiError && e.status === 401 && session) { setSession(null); setPatient(null); setRecord(null); setBooking(null); setDoctorEditor(null) }
    setError(e instanceof Error ? e.message : 'Не удалось выполнить действие')
  }
  async function act(action: () => Promise<void>) {
    setBusy(true); setError(''); setNotice('')
    try { await action() } catch (e) { fail(e) } finally { setBusy(false) }
  }
  useEffect(() => {
    if (!session) return
    const controller = new AbortController()
    setLoading(true); setError(''); setDoctors([]); setAppointments([])
    const run = async () => {
      if (section === 'doctors') {
        const data = await api<{ doctors: Doctor[] }>(`/doctor/filter/${part(name || 'all')}/${period}/${part(specialty || 'all')}`, 'GET', undefined, controller.signal)
        setDoctors(data.doctors)
      } else if (session.role === 'doctor') {
        const data = await api<{ appointments: Appointment[] }>(`/appointments/${date}/${part(name || 'all')}/${token}`, 'GET', undefined, controller.signal)
        setAppointments(data.appointments)
      } else {
        const data = await api<{ appointments: Appointment[] }>(`/patient/filter/${period}/${part(name || 'all')}/${token}`, 'GET', undefined, controller.signal)
        setAppointments(data.appointments)
      }
    }
    const timer = setTimeout(() => { run().catch(e => { if (!controller.signal.aborted) fail(e) }).finally(() => { if (!controller.signal.aborted) setLoading(false) }) }, 250)
    return () => { clearTimeout(timer); controller.abort() }
  // Session and query changes cancel outdated requests.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session, section, name, specialty, period, date, revision, token])

  useEffect(() => {
    if (!booking || !session) return
    const controller = new AbortController()
    setSlots([]); setSlotsLoading(true)
    api<{ availableTimes: string[] }>(`/doctor/availability/patient/${booking.doctorId}/${bookingDate}/${token}`, 'GET', undefined, controller.signal)
      .then(data => setSlots(data.availableTimes)).catch(e => { if (!controller.signal.aborted) fail(e) })
      .finally(() => { if (!controller.signal.aborted) setSlotsLoading(false) })
    return () => controller.abort()
  }, [booking, bookingDate, token, revision, session])

  function navigate(next: string) {
    setSection(next); setName(''); setPeriod('all'); setSpecialty(''); setDoctorEditor(null); setBooking(null); setRecord(null); setError(''); setNotice('')
  }
  async function authenticate(event: FormEvent<HTMLFormElement>) {
    const data = values(event)
    await act(async () => {
      if (register) { await api('/patient', 'POST', data); setRegister(false); setNotice('Аккаунт создан. Войдите с вашим email и паролем.'); return }
      const result = await api<{ token: string }>(`/${role}/login`, 'POST', data)
      const profile = role === 'patient' ? (await api<{ patient: Patient }>(`/patient/${part(result.token)}`)).patient : null
      setPatient(profile); navigate(role === 'doctor' ? 'appointments' : 'doctors')
      setSession({ role, token: result.token, name: profile?.name || data.email || data.username })
    })
  }
  function saveDoctor(event: FormEvent<HTMLFormElement>) {
    const data = values(event)
    void act(async () => {
      const availableTimes = data.availableTimes.split(',').map(s => s.trim()).filter(Boolean)
      if (!availableTimes.length || availableTimes.some(s => !/^([01]\d|2[0-3]):[0-5]\d-([01]\d|2[0-3]):[0-5]\d$/.test(s))) throw new Error('Укажите интервалы в формате 09:00-10:00 через запятую.')
      await api(`/doctor/${token}`, doctorEditor === 'new' ? 'POST' : 'PUT', { ...data, availableTimes, ...(doctorEditor !== 'new' && doctorEditor ? { id: doctorEditor.id } : {}) })
      setDoctorEditor(null); setRevision(r => r + 1); setNotice('Данные врача сохранены.')
    })
  }
  function saveBooking(event: FormEvent<HTMLFormElement>) {
    const data = values(event)
    void act(async () => {
      if (!booking || !patient) return
      await api(`/appointments/${token}`, booking.id ? 'PUT' : 'POST', { ...(booking.id ? { id: booking.id } : {}), doctor: { id: booking.doctorId }, patient: { id: patient.id }, appointmentTime: `${bookingDate}T${data.slot.split('-')[0]}:00` })
      setBooking(null); navigate('appointments'); setRevision(r => r + 1); setNotice('Запись на приём сохранена.')
    })
  }
  function openRecord(a: Appointment) {
    setRecord(a); setPrescriptions(null); setHistory(null)
    void act(async () => {
      const [rx, visits] = await Promise.all([api<{ prescription: Prescription[] }>(`/prescription/${a.id}/${token}`), api<{ appointments: Appointment[] }>(`/patient/${a.patientId}/doctor/${token}`)])
      setPrescriptions(rx.prescription); setHistory(visits.appointments)
    })
  }
  const alerts = <>{error && <div className="alert error" role="alert">{error}</div>}{notice && <div className="alert success" role="status">{notice}</div>}</>
  if (!session) return <div className="auth-layout"><section className="welcome"><a className="brand" href="/">✚ Smart Clinic</a><div><p className="eyebrow">ЗАБОТА НАЧИНАЕТСЯ ЗДЕСЬ</p><h1>Ближе к врачу.<br />Спокойнее за здоровье.</h1><p>Записывайтесь на приём и управляйте визитами в одном месте.</p></div><span>Единое пространство вашей клиники</span></section><main className="auth"><p className="eyebrow">ДОБРО ПОЖАЛОВАТЬ</p><h1>{register ? 'Регистрация пациента' : 'Вход в Smart Clinic'}</h1><p>Выберите вашу роль, чтобы продолжить.</p><div className="role-tabs">{(Object.keys(roles) as Role[]).map(r => <button disabled={busy} aria-pressed={role === r} key={r} onClick={() => { setRole(r); setRegister(false); setError('') }}>{roles[r]}</button>)}</div>{alerts}<form key={`${role}-${register}`} onSubmit={authenticate}><fieldset disabled={busy}>{register && <Field label="ФИО" name="name" minLength={3} maxLength={100} autoComplete="name" />}<Field label={role === 'admin' ? 'Имя пользователя' : 'Email'} name={role === 'admin' ? 'username' : 'email'} type={role === 'admin' ? 'text' : 'email'} autoComplete="username" /><Field label="Пароль" name="password" type="password" minLength={register ? 6 : undefined} autoComplete={register ? 'new-password' : 'current-password'} />{register && <><Field label="Телефон — 10 цифр" name="phone" pattern="[0-9]{10}" autoComplete="tel" /><Field label="Адрес" name="address" maxLength={255} autoComplete="street-address" /></>}<button type="submit">{busy ? 'Подождите…' : register ? 'Создать аккаунт' : 'Войти →'}</button></fieldset></form>{role === 'patient' && <button disabled={busy} className="text-button" onClick={() => { setRegister(!register); setError('') }}>{register ? 'Уже есть аккаунт? Войти' : 'Нет аккаунта? Зарегистрироваться'}</button>}</main></div>
  const edited = doctorEditor && doctorEditor !== 'new' ? doctorEditor : null
  return <div className="layout"><aside><a className="brand" href="/">✚ Smart Clinic</a><p className="caption">{roles[session.role]}</p><nav aria-label="Разделы">{session.role !== 'doctor' && <button disabled={busy} aria-current={section === 'doctors' ? 'page' : undefined} onClick={() => navigate('doctors')}>♧ Врачи клиники</button>}{session.role !== 'admin' && <button disabled={busy} aria-current={section === 'appointments' ? 'page' : undefined} onClick={() => navigate('appointments')}>▦ Мои приёмы</button>}</nav><div className="sidebar-note"><strong>{session.name}</strong><p>{roles[session.role]}</p><button disabled={busy} className="logout" onClick={() => { setSession(null); setPatient(null); setDoctors([]); setAppointments([]); setNotice(''); setError(''); setRecord(null); setBooking(null); setDoctorEditor(null) }}>Выйти из аккаунта</button></div></aside><main><header><div><p className="eyebrow">SMART CLINIC / РАБОЧЕЕ ПРОСТРАНСТВО</p><h1>{section === 'doctors' ? 'Врачи клиники' : 'Мои приёмы'}</h1><p>{session.role === 'admin' ? 'Команда специалистов и расписание работы.' : session.role === 'doctor' ? 'Ваше расписание, пациенты и назначения.' : 'Выберите специалиста и удобное время для визита.'}</p></div><span className="date-chip">{new Date().toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })}</span></header>{alerts}
    <section className="toolbar"><Field label={session.role === 'doctor' ? 'Поиск пациента' : 'Поиск врача'} required={false} value={name} onChange={e => setName(e.target.value)} placeholder="Введите имя…" />{section === 'doctors' ? <><Field label="Специальность" required={false} value={specialty} onChange={e => setSpecialty(e.target.value)} placeholder="Все специальности" /><label>Время<select value={period} onChange={e => setPeriod(e.target.value)}><option value="all">Любое</option><option value="AM">До 12:00</option><option value="PM">После 12:00</option></select></label></> : session.role === 'doctor' ? <Field label="Дата приёма" type="date" value={date} onChange={e => { if (e.target.value) setDate(e.target.value) }} /> : <label>Статус<select value={period} onChange={e => setPeriod(e.target.value)}><option value="all">Все приёмы</option><option value="future">Запланированные</option><option value="past">Завершённые</option></select></label>}<button className="secondary" disabled={busy || loading} onClick={() => setRevision(r => r + 1)}>Обновить</button>{session.role === 'admin' && <button disabled={busy} onClick={() => setDoctorEditor('new')}>＋ Добавить врача</button>}</section>
    {doctorEditor && <Panel title={edited ? 'Редактирование врача' : 'Новый врач'} close={() => { if (!busy) setDoctorEditor(null) }}><form key={edited?.id || 'new'} onSubmit={saveDoctor}><fieldset className="form-grid" disabled={busy}><Field label="ФИО" name="name" defaultValue={edited?.name} minLength={3} maxLength={100} /><Field label="Специальность" name="specialty" defaultValue={edited?.specialty} minLength={3} maxLength={50} /><Field label="Email" name="email" type="email" defaultValue={edited?.email} /><Field label="Телефон — 10 цифр" name="phone" pattern="[0-9]{10}" defaultValue={edited?.phone} /><Field label={edited ? 'Новый пароль (обязателен при изменении)' : 'Пароль'} name="password" type="password" minLength={6} autoComplete="new-password" /><Field label="Часовые интервалы через запятую" name="availableTimes" placeholder="09:00-10:00, 10:00-11:00" defaultValue={edited?.availableTimes.join(', ')} /><button type="submit">{busy ? 'Сохранение…' : 'Сохранить врача'}</button></fieldset></form></Panel>}
    {booking && <Panel title={booking.id ? 'Перенести приём' : 'Запись на приём'} close={() => { if (!busy) setBooking(null) }}><form onSubmit={saveBooking}><fieldset className="form-grid" disabled={busy}><Field label="Дата визита" type="date" min={today()} value={bookingDate} onChange={e => { if (e.target.value) setBookingDate(e.target.value) }} /><label>Свободное время<select key={`${booking.doctorId}-${bookingDate}-${revision}`} name="slot" required defaultValue="" disabled={slotsLoading}><option value="">{slotsLoading ? 'Загрузка…' : slots.length ? 'Выберите время' : 'Нет свободных интервалов'}</option>{slots.map(slot => <option key={slot}>{slot}</option>)}</select></label><button disabled={slotsLoading || !slots.length} type="submit">{busy ? 'Сохранение…' : 'Подтвердить запись'}</button></fieldset></form></Panel>}
    {record && <Panel title={`Карта пациента · ${record.patientName}`} close={() => { if (!busy) setRecord(null) }}><p>{record.patientEmail} · {record.patientPhone}<br />{record.patientAddress}</p><h3>История приёмов у вас</h3>{history === null ? <p>История не загружена.</p> : <ul>{history.map(a => <li key={a.id}>{dateLabel(a.appointmentTime)} — {a.status === 1 ? 'Завершён' : 'Запланирован'}</li>)}</ul>}<h3>Рецепт к приёму {dateLabel(record.appointmentTime)}</h3>{prescriptions === null ? <button disabled={busy} onClick={() => openRecord(record)}>{busy ? 'Загрузка…' : 'Повторить загрузку'}</button> : prescriptions.length ? prescriptions.map((rx, i) => <div className="prescription" key={i}><strong>{rx.medication}</strong><p>{rx.dosage}</p><p>{rx.doctorNotes}</p></div>) : <form onSubmit={e => { const data = values(e); void act(async () => { await api(`/prescription/${token}`, 'POST', { ...data, appointmentId: record.id, patientName: record.patientName }); setRecord(null); setRevision(r => r + 1); setNotice('Рецепт сохранён, приём завершён.') }) }}><fieldset disabled={busy}><Field label="Препарат" name="medication" minLength={3} maxLength={100} /><Field label="Дозировка" name="dosage" /><label>Рекомендации<textarea name="doctorNotes" maxLength={200} /></label><p>Сохранение рецепта завершит приём.</p><button type="submit">Сохранить рецепт</button></fieldset></form>}</Panel>}
    <section className="content" aria-busy={loading}><div className="section-heading"><h2>{section === 'doctors' ? 'Специалисты' : 'Список приёмов'}</h2><span className="count">{loading ? '…' : section === 'doctors' ? doctors.length : appointments.length}</span></div>{loading ? <div className="empty" role="status">Загружаем данные…</div> : section === 'doctors' ? doctors.length ? <div className="doctor-grid">{doctors.map(d => <article className="doctor-card" key={d.id}><div className="avatar">{d.name.split(' ').map(n => n[0]).slice(0, 2).join('')}</div><span className="specialty">{d.specialty}</span><h3>{d.name}</h3><p>{d.email}<br />{d.phone}</p><div className="slots">{d.availableTimes.map(t => <span key={t}>{t}</span>)}</div><div className="actions">{session.role === 'admin' ? <><button disabled={busy} className="secondary" onClick={() => setDoctorEditor(d)}>Изменить</button><button disabled={busy} className="danger" onClick={() => { if (window.confirm(`Удалить врача ${d.name} и все его приёмы?`)) void act(async () => { await api(`/doctor/${d.id}/${token}`, 'DELETE'); setRevision(r => r + 1); setNotice('Врач удалён.') }) }}>Удалить</button></> : <button disabled={busy} onClick={() => { setBooking({ doctorId: d.id }); setBookingDate(today()); setError('') }}>Записаться →</button>}</div></article>)}</div> : <Empty text="Врачи не найдены" /> : appointments.length ? <div className="table-wrap"><table><thead><tr><th>Дата и время</th><th>{session.role === 'doctor' ? 'Пациент' : 'Врач'}</th><th>Статус</th><th>Действия</th></tr></thead><tbody>{appointments.map(a => <tr key={a.id}><td>{dateLabel(a.appointmentTime)}</td><td><strong>{session.role === 'doctor' ? a.patientName : a.doctorName}</strong>{session.role === 'doctor' && <small>{a.patientPhone}</small>}</td><td><span className={`badge ${a.status === 1 ? 'completed' : ''}`}>{a.status === 1 ? 'Завершён' : 'Запланирован'}</span></td><td><div className="actions">{session.role === 'doctor' ? <button disabled={busy} className="secondary" onClick={() => openRecord(a)}>Карта и рецепт</button> : a.status === 0 && new Date(a.appointmentTime) > new Date() ? <><button disabled={busy} className="secondary" onClick={() => { setBooking({ doctorId: a.doctorId, id: a.id }); setBookingDate(a.appointmentTime.slice(0, 10)) }}>Перенести</button><button disabled={busy} className="danger" onClick={() => { if (window.confirm('Отменить запись на приём?')) void act(async () => { await api(`/appointments/${a.id}/${token}`, 'DELETE'); setRevision(r => r + 1); setNotice('Запись отменена.') }) }}>Отменить</button></> : '—'}</div></td></tr>)}</tbody></table></div> : <Empty text="Приёмов пока нет" />}</section><p className="footnote">Smart Clinic · Всё для заботы о здоровье</p></main></div>
}
function Empty({ text }: { text: string }) { return <div className="empty"><span className="empty-icon" aria-hidden="true">＋</span><h3>{text}</h3><p>Измените фильтры или обновите список.</p></div> }
