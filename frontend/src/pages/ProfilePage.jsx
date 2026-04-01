import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { useOnboarding } from '../components/onboarding/OnboardingContext'
import {
  getProfile,
  updateProfileName,
  changePassword,
  deleteAccount,
} from '../services/api'
import './ProfilePage.css'

const PREF_AMOUNT_KEY = 'pref_amount'
const PREF_YEARS_KEY = 'pref_years'
const PREF_RISK_KEY = 'pref_risk'

function formatDate(iso) {
  if (!iso) return 'N/A'
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

export default function ProfilePage() {
  const navigate = useNavigate()
  const { user, logout, updateStoredDisplayName } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const { restart } = useOnboarding()

  const [profile, setProfile] = useState(null)
  const [profileLoading, setProfileLoading] = useState(true)

  // Account info edit
  const [editingName, setEditingName] = useState(false)
  const [nameValue, setNameValue] = useState('')
  const [nameSaving, setNameSaving] = useState(false)
  const [nameMsg, setNameMsg] = useState(null)

  // Password change
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [passwordSaving, setPasswordSaving] = useState(false)
  const [passwordMsg, setPasswordMsg] = useState(null)

  // Financial preferences (localStorage)
  const [defaultAmount, setDefaultAmount] = useState(
    () => localStorage.getItem(PREF_AMOUNT_KEY) || '10000'
  )
  const [defaultYears, setDefaultYears] = useState(
    () => localStorage.getItem(PREF_YEARS_KEY) || '10'
  )
  const [riskProfile, setRiskProfile] = useState(
    () => localStorage.getItem(PREF_RISK_KEY) || 'moderate'
  )
  const [prefSaved, setPrefSaved] = useState(false)

  // Delete account
  const [deleteConfirm, setDeleteConfirm] = useState('')
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    getProfile()
      .then((data) => {
        setProfile(data)
        setNameValue(data.displayName || '')
      })
      .catch(() => setProfile(null))
      .finally(() => setProfileLoading(false))
  }, [])

  async function handleSaveName(e) {
    e.preventDefault()
    if (!nameValue.trim()) return
    setNameSaving(true)
    setNameMsg(null)
    try {
      const updated = await updateProfileName(nameValue.trim())
      setProfile((p) => ({ ...p, displayName: updated.displayName }))
      updateStoredDisplayName(updated.displayName)
      setEditingName(false)
      setNameMsg({ type: 'success', text: 'Name updated.' })
    } catch (err) {
      setNameMsg({ type: 'error', text: err.message })
    } finally {
      setNameSaving(false)
    }
  }

  async function handleChangePassword(e) {
    e.preventDefault()
    setPasswordMsg(null)
    if (newPassword !== confirmPassword) {
      setPasswordMsg({ type: 'error', text: 'New passwords do not match.' })
      return
    }
    if (newPassword.length < 8) {
      setPasswordMsg({ type: 'error', text: 'New password must be at least 8 characters.' })
      return
    }
    setPasswordSaving(true)
    try {
      await changePassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setPasswordMsg({ type: 'success', text: 'Password changed successfully.' })
    } catch (err) {
      setPasswordMsg({ type: 'error', text: err.message })
    } finally {
      setPasswordSaving(false)
    }
  }

  function handleSavePreferences() {
    localStorage.setItem(PREF_AMOUNT_KEY, defaultAmount)
    localStorage.setItem(PREF_YEARS_KEY, defaultYears)
    localStorage.setItem(PREF_RISK_KEY, riskProfile)
    setPrefSaved(true)
    setTimeout(() => setPrefSaved(false), 2000)
  }

  async function handleDeleteAccount() {
    if (deleteConfirm !== user?.email) return
    setDeleting(true)
    try {
      await deleteAccount()
      logout()
      navigate('/')
    } catch {
      setDeleting(false)
    }
  }

  return (
    <>
      <Navbar />
      <main id="main-content" className="profile-page">
        <div className="profile-page__inner">
          <h1 className="profile-page__title">Account Settings</h1>

          {/* ── Account Info ── */}
          <section className="profile-section">
            <h2 className="profile-section__heading">Account Info</h2>
            <div className="profile-card">
              <div className="profile-field">
                <span className="profile-field__label">Email</span>
                <span className="profile-field__value">{profileLoading ? '—' : profile?.email}</span>
              </div>
              <div className="profile-field">
                <span className="profile-field__label">Display Name</span>
                {editingName ? (
                  <form className="profile-inline-form" onSubmit={handleSaveName}>
                    <input
                      id="display-name-input"
                      className="profile-input"
                      value={nameValue}
                      onChange={(e) => setNameValue(e.target.value)}
                      autoFocus
                      required
                      aria-required="true"
                      aria-label="Display name"
                    />
                    <button className="profile-btn profile-btn--primary" type="submit" disabled={nameSaving}>
                      {nameSaving ? 'Saving…' : 'Save'}
                    </button>
                    <button
                      className="profile-btn"
                      type="button"
                      onClick={() => { setEditingName(false); setNameValue(profile?.displayName || '') }}
                    >
                      Cancel
                    </button>
                  </form>
                ) : (
                  <div className="profile-field__value-row">
                    <span className="profile-field__value">{profile?.displayName || '—'}</span>
                    <button className="profile-btn profile-btn--ghost" onClick={() => setEditingName(true)}>
                      Edit
                    </button>
                  </div>
                )}
              </div>
              {nameMsg && (
                <p
                  id="name-msg"
                  className={`profile-msg profile-msg--${nameMsg.type}`}
                  role={nameMsg.type === 'error' ? 'alert' : 'status'}
                  aria-live={nameMsg.type === 'error' ? 'assertive' : 'polite'}
                >
                  {nameMsg.text}
                </p>
              )}
              <div className="profile-field">
                <span className="profile-field__label">Member Since</span>
                <span className="profile-field__value">
                  {profileLoading ? '—' : formatDate(profile?.createdAt)}
                </span>
              </div>
            </div>
          </section>

          {/* ── Change Password ── */}
          <section className="profile-section">
            <h2 className="profile-section__heading">Change Password</h2>
            <div className="profile-card">
              <form className="profile-form" onSubmit={handleChangePassword}>
                <div className="profile-form__row">
                  <label className="profile-form__label" htmlFor="current-pw">Current Password</label>
                  <input
                    id="current-pw"
                    className="profile-input"
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    required
                    aria-required="true"
                    autoComplete="current-password"
                  />
                </div>
                <div className="profile-form__row">
                  <label className="profile-form__label" htmlFor="new-pw">New Password</label>
                  <input
                    id="new-pw"
                    className="profile-input"
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                    aria-required="true"
                    minLength={8}
                    autoComplete="new-password"
                    aria-describedby={passwordMsg?.type === 'error' ? 'password-msg' : undefined}
                  />
                </div>
                <div className="profile-form__row">
                  <label className="profile-form__label" htmlFor="confirm-pw">Confirm New Password</label>
                  <input
                    id="confirm-pw"
                    className="profile-input"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    aria-required="true"
                    autoComplete="new-password"
                  />
                </div>
                {passwordMsg && (
                  <p
                    id="password-msg"
                    className={`profile-msg profile-msg--${passwordMsg.type}`}
                    role={passwordMsg.type === 'error' ? 'alert' : 'status'}
                    aria-live={passwordMsg.type === 'error' ? 'assertive' : 'polite'}
                  >
                    {passwordMsg.text}
                  </p>
                )}
                <button className="profile-btn profile-btn--primary" type="submit" disabled={passwordSaving}>
                  {passwordSaving ? 'Updating…' : 'Update Password'}
                </button>
              </form>
            </div>
          </section>

          {/* ── Financial Preferences ── */}
          <section className="profile-section">
            <h2 className="profile-section__heading">Financial Preferences</h2>
            <div className="profile-card">
              <div className="profile-form__row">
                <label className="profile-form__label" htmlFor="pref-amount">Default Investment Amount</label>
                <input
                  id="pref-amount"
                  className="profile-input"
                  type="number"
                  min="0"
                  step="100"
                  value={defaultAmount}
                  onChange={(e) => setDefaultAmount(e.target.value)}
                />
              </div>
              <div className="profile-form__row">
                <label className="profile-form__label" htmlFor="pref-years">Default Projection Horizon (years)</label>
                <input
                  id="pref-years"
                  className="profile-input"
                  type="number"
                  min="1"
                  max="50"
                  value={defaultYears}
                  onChange={(e) => setDefaultYears(e.target.value)}
                />
              </div>
              <div className="profile-form__row">
                <label className="profile-form__label" htmlFor="pref-risk">Risk Profile</label>
                <select
                  id="pref-risk"
                  className="profile-select"
                  value={riskProfile}
                  onChange={(e) => setRiskProfile(e.target.value)}
                >
                  <option value="conservative">Conservative</option>
                  <option value="moderate">Moderate</option>
                  <option value="aggressive">Aggressive</option>
                </select>
              </div>
              <button
                className={`profile-btn profile-btn--primary${prefSaved ? ' profile-btn--saved' : ''}`}
                type="button"
                onClick={handleSavePreferences}
              >
                {prefSaved ? 'Saved!' : 'Save Preferences'}
              </button>
            </div>
          </section>

          {/* ── Appearance ── */}
          <section className="profile-section">
            <h2 className="profile-section__heading">Appearance</h2>
            <div className="profile-card">
              <div className="profile-field">
                <span className="profile-field__label">Theme</span>
                <div className="profile-theme-toggle">
                  <button
                    className={`profile-theme-btn${theme === 'dark' ? ' profile-theme-btn--active' : ''}`}
                    type="button"
                    onClick={() => theme !== 'dark' && toggleTheme()}
                    aria-pressed={theme === 'dark'}
                  >
                    🌙 Dark
                  </button>
                  <button
                    className={`profile-theme-btn${theme === 'light' ? ' profile-theme-btn--active' : ''}`}
                    type="button"
                    onClick={() => theme !== 'light' && toggleTheme()}
                    aria-pressed={theme === 'light'}
                  >
                    ☀️ Light
                  </button>
                </div>
              </div>
              <div className="profile-field">
                <span className="profile-field__label">Platform Tour</span>
                <div className="profile-field__value-row">
                  <span className="profile-field__value profile-field__value--muted">Replay the guided walkthrough of every feature</span>
                  <button className="profile-btn profile-btn--ghost" type="button" onClick={restart}>
                    Start Tour
                  </button>
                </div>
              </div>
            </div>
          </section>

          {/* ── Security ── */}
          <section className="profile-section">
            <h2 className="profile-section__heading">Security</h2>
            <div className="profile-card">
              <div className="profile-field">
                <span className="profile-field__label">Account Email</span>
                <span className="profile-field__value">{profile?.email || '—'}</span>
              </div>
              <div className="profile-field">
                <span className="profile-field__label">Account Created</span>
                <span className="profile-field__value">{formatDate(profile?.createdAt)}</span>
              </div>
              <div className="profile-field">
                <span className="profile-field__label">Session</span>
                <span className="profile-field__value profile-field__value--muted">JWT — expires 24h from login</span>
              </div>
              <button className="profile-btn profile-btn--outline" type="button" onClick={logout}>
                Logout
              </button>
            </div>
          </section>

          {/* ── Danger Zone ── */}
          <section className="profile-section profile-section--danger">
            <h2 className="profile-section__heading profile-section__heading--danger">Danger Zone</h2>
            <div className="profile-card profile-card--danger">
              <p className="profile-danger__desc">
                Permanently delete your account and all associated portfolios and data. This cannot be undone.
              </p>
              <div className="profile-form__row">
                <label className="profile-form__label" htmlFor="delete-confirm">
                  Type your email to confirm: <strong>{user?.email}</strong>
                </label>
                <input
                  id="delete-confirm"
                  className="profile-input profile-input--danger"
                  type="text"
                  value={deleteConfirm}
                  onChange={(e) => setDeleteConfirm(e.target.value)}
                  placeholder={user?.email}
                  autoComplete="off"
                />
              </div>
              <button
                className="profile-btn profile-btn--danger"
                type="button"
                onClick={handleDeleteAccount}
                disabled={deleteConfirm !== user?.email || deleting}
              >
                {deleting ? 'Deleting…' : 'Delete My Account'}
              </button>
            </div>
          </section>
        </div>
      </main>
    </>
  )
}
