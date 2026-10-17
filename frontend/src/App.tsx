import { useState } from 'react'
import './App.css'

const dashboards = {
  admin: {
    title: 'Панель администратора',
    description: 'Управление клиникой, врачами и записями на приём.',
    sections: ['Врачи', 'Пациенты', 'Записи на приём'],
  },
  doctor: {
    title: 'Панель врача',
    description: 'Расписание приёмов, медицинские карты и рецепты.',
    sections: ['Мои приёмы', 'Медицинские карты', 'Рецепты'],
  },
}

type Role = keyof typeof dashboards

export default function App() {
  const [role, setRole] = useState<Role>('admin')
  const [section, setSection] = useState(dashboards.admin.sections[0])
  const dashboard = dashboards[role]

  function changeRole(nextRole: Role) {
    setRole(nextRole)
    setSection(dashboards[nextRole].sections[0])
  }

  return (
    <div className="layout">
      <aside>
        <a className="brand" href="/">✚ Smart Clinic</a>
        <p className="caption">ПАНЕЛЬ УПРАВЛЕНИЯ</p>
        <nav aria-label="Разделы панели">
          {dashboard.sections.map((name) => (
            <button key={name} aria-current={section === name ? 'page' : undefined}
              onClick={() => setSection(name)}>{name}</button>
          ))}
        </nav>
        <p className="sidebar-note">Всё для работы вашей клиники</p>
      </aside>
      <main>
        <header>
          <div><p className="eyebrow">SMART CLINIC / РАБОЧЕЕ ПРОСТРАНСТВО</p>
            <h1>{dashboard.title}</h1><p>{dashboard.description}</p></div>
          <label>Предпросмотр роли
            <select value={role} onChange={(event) => changeRole(event.target.value as Role)}>
              <option value="admin">Администратор</option><option value="doctor">Врач</option>
            </select>
          </label>
        </header>
        <p className="notice">Стартовый интерфейс · API ещё не подключён. Переключение роли служит только для предпросмотра.</p>
        <section className="cards" aria-label="Обзор разделов">
          {dashboard.sections.map((name, index) => (
            <button className="card" key={name} onClick={() => setSection(name)}>
              <span className="number">0{index + 1}</span><strong>{name}</strong><span>Открыть раздел →</span>
            </button>
          ))}
        </section>
        <section className="content" aria-live="polite">
          <h2>{section}</h2>
          <div className="empty"><span className="empty-icon" aria-hidden="true">＋</span>
            <h3>Здесь появятся данные клиники</h3>
            <p>Раздел «{section}» подготовлен для подключения к серверу.</p>
          </div>
        </section>
      </main>
    </div>
  )
}
