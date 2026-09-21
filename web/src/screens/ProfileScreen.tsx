import React, { useState, useRef } from 'react';
import {
  ArrowLeft,
  Clock,
  Download,
  Upload,
  RotateCcw,
  CheckCircle2,
  AlertTriangle
} from 'lucide-react';
import { UserProfile } from '../types';
import { storage } from '../services/storage';
import { sound } from '../services/audio';
import { TimePickerModal } from '../components/TimePickerModal';
import { StudyPuppetAvatar } from '../components/StudyPuppetAvatar';

interface ProfileScreenProps {
  profile: UserProfile;
  onBack: () => void;
  onNavigateToFullRoutine: (type: string) => void;
  onNavigateToFullExams: (type: string) => void;
}

export const ProfileScreen: React.FC<ProfileScreenProps> = ({
  profile,
  onBack,
  onNavigateToFullRoutine,
  onNavigateToFullExams
}) => {
  const [name, setName] = useState(profile.name);
  const [targetInstitution, setTargetInstitution] = useState(profile.targetInstitution);
  const [collegeName, setCollegeName] = useState(profile.collegeName);
  const [hscBatch, setHscBatch] = useState(profile.hscBatch);
  const [dailyGoalHours, setDailyGoalHours] = useState(String(profile.dailyGoalHours));

  // Time Pickers
  const [activeTimePicker, setActiveTimePicker] = useState<'morning' | 'evening' | null>(null);

  // Toast
  const [toastText, setToastText] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const showToast = (msg: string) => {
    setToastText(msg);
    setTimeout(() => setToastText(null), 3500);
  };

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    sound.playSuccess();
    storage.saveProfile({
      name,
      targetInstitution,
      collegeName,
      hscBatch,
      dailyGoalHours: parseFloat(dailyGoalHours) || 8
    });
    showToast('Profile updated successfully');
  };

  const handleDownloadBackup = () => {
    sound.playSuccess();
    storage.downloadBackupFile();
    showToast('Backup JSON downloaded');
  };

  const handleRestoreBackup = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      if (content) {
        const ok = storage.importBackupJson(content);
        if (ok) {
          sound.playSuccess();
          showToast('Data restored successfully');
        } else {
          showToast('Invalid backup file');
        }
      }
    };
    reader.readAsText(file);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleResetSyllabus = () => {
    sound.playTick();
    if (window.confirm('Reset all syllabus progress?')) {
      storage.resetSyllabus();
      showToast('Syllabus reset');
    }
  };

  const handleResetExams = () => {
    sound.playTick();
    if (window.confirm('Clear all exam records?')) {
      storage.resetExams();
      showToast('Exam records cleared');
    }
  };

  const handleFactoryReset = () => {
    sound.playTick();
    if (window.confirm('Erase all data and reset app?')) {
      storage.clearAllData();
      window.location.reload();
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, width: '100%', paddingBottom: 60 }}>
      {/* Toast Alert */}
      {toastText && (
        <div
          style={{
            position: 'fixed',
            top: 20,
            left: '50%',
            transform: 'translateX(-50%)',
            zIndex: 200,
            background: 'rgba(29, 27, 32, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid #d0bcff',
            color: '#fff',
            padding: '10px 22px',
            borderRadius: 'var(--radius-full)',
            boxShadow: '0 8px 30px rgba(0,0,0,0.6)',
            fontSize: 14,
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            gap: 8
          }}
        >
          <CheckCircle2 size={18} color="#81c784" />
          <span>{toastText}</span>
        </div>
      )}

      {/* Top Header with Back Arrow */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '4px 0' }}>
        <button
          onClick={() => {
            sound.playTick();
            onBack();
          }}
          style={{
            width: 42,
            height: 42,
            borderRadius: '50%',
            border: '1px solid rgba(255, 255, 255, 0.15)',
            background: 'rgba(255, 255, 255, 0.06)',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer'
          }}
        >
          <ArrowLeft size={20} />
        </button>

        <h2 style={{ margin: 0, fontSize: 22, fontWeight: 900, color: '#fff', letterSpacing: '1px' }}>
          PROFILE & SETTINGS
        </h2>
      </div>

      {/* 2-Column Responsive Layout on Desktop */}
      <div className="responsive-two-col">
        {/* Column 1: Aspirant Identity Card */}
        <div className="haze-card" style={{ padding: '24px 22px', borderRadius: '26px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 20 }}>
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: '50%',
                background: 'rgba(208, 188, 255, 0.18)',
                border: '1px solid rgba(208, 188, 255, 0.35)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <StudyPuppetAvatar studyHours={storage.getTodayStudyMinutes() / 60} size={50} />
            </div>

            <div>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 800, color: '#fff' }}>
                {profile.name}
              </h3>
              <div style={{ fontSize: 13, color: '#d0bcff', marginTop: 2 }}>
                {profile.targetInstitution} • {profile.hscBatch}
              </div>
            </div>
          </div>

          <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div>
              <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.6)', fontWeight: 700, marginBottom: 4 }}>
                YOUR NAME
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.6)', fontWeight: 700, marginBottom: 4 }}>
                  TARGET INSTITUTION
                </label>
                <input
                  type="text"
                  value={targetInstitution}
                  onChange={(e) => setTargetInstitution(e.target.value)}
                  style={{ width: '100%' }}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.6)', fontWeight: 700, marginBottom: 4 }}>
                  COLLEGE
                </label>
                <input
                  type="text"
                  value={collegeName}
                  onChange={(e) => setCollegeName(e.target.value)}
                  style={{ width: '100%' }}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.6)', fontWeight: 700, marginBottom: 4 }}>
                  HSC BATCH
                </label>
                <select
                  value={hscBatch}
                  onChange={(e) => setHscBatch(e.target.value)}
                  style={{ width: '100%' }}
                >
                  <option value="HSC 2025">HSC 2025</option>
                  <option value="HSC 2026">HSC 2026</option>
                  <option value="HSC 2027">HSC 2027</option>
                  <option value="2nd Timer">2nd Timer</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.6)', fontWeight: 700, marginBottom: 4 }}>
                  DAILY GOAL (HOURS)
                </label>
                <input
                  type="number"
                  step="0.5"
                  min="1"
                  max="24"
                  value={dailyGoalHours}
                  onChange={(e) => setDailyGoalHours(e.target.value)}
                  style={{ width: '100%' }}
                />
              </div>
            </div>

            <button
              type="submit"
              style={{
                marginTop: 6,
                padding: '11px 22px',
                borderRadius: '12px',
                border: 'none',
                background: '#d0bcff',
                color: '#1d1b20',
                fontWeight: 800,
                fontSize: 13.5,
                cursor: 'pointer',
                alignSelf: 'flex-start'
              }}
            >
              Save Profile
            </button>
          </form>
        </div>

        {/* Column 2: Reminders & Backup */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Routine & Reminder Alerts Card with Xiaomi Picker */}
          <div className="haze-card" style={{ padding: '22px 20px', borderRadius: '26px' }}>
            <h3 style={{ margin: '0 0 4px 0', fontSize: 16, fontWeight: 800, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Clock size={18} color="#d0bcff" />
              <span>Routine & Study Alerts</span>
            </h3>
            <div style={{ fontSize: 12.5, color: 'rgba(255,255,255,0.6)', marginBottom: 14 }}>
              Xiaomi / HyperOS style 3-column scroll wheel clock picker
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {/* Morning Routine Alert */}
              <div
                style={{
                  padding: '12px 14px',
                  borderRadius: '14px',
                  background: 'rgba(255, 255, 255, 0.04)',
                  border: '1px solid rgba(255, 255, 255, 0.08)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}
              >
                <div>
                  <div style={{ fontSize: 13.5, fontWeight: 700, color: '#fff' }}>Daily Morning Schedule</div>
                  <div style={{ fontSize: 12, color: '#d0bcff', marginTop: 2 }}>
                    Time: {profile.dailyRoutineAlertTime || '08:00'}
                  </div>
                </div>

                <button
                  onClick={() => {
                    sound.playTick();
                    setActiveTimePicker('morning');
                  }}
                  style={{
                    padding: '6px 14px',
                    borderRadius: '10px',
                    border: '1px solid rgba(208, 188, 255, 0.4)',
                    background: 'rgba(208, 188, 255, 0.15)',
                    color: '#d0bcff',
                    fontSize: 12,
                    fontWeight: 700,
                    cursor: 'pointer'
                  }}
                >
                  Change
                </button>
              </div>

              {/* Evening Study Log Reminder */}
              <div
                style={{
                  padding: '12px 14px',
                  borderRadius: '14px',
                  background: 'rgba(255, 255, 255, 0.04)',
                  border: '1px solid rgba(255, 255, 255, 0.08)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}
              >
                <div>
                  <div style={{ fontSize: 13.5, fontWeight: 700, color: '#fff' }}>Evening Study Log Reminder</div>
                  <div style={{ fontSize: 12, color: '#d0bcff', marginTop: 2 }}>
                    Time: {profile.eveningReminderTime || '21:00'}
                  </div>
                </div>

                <button
                  onClick={() => {
                    sound.playTick();
                    setActiveTimePicker('evening');
                  }}
                  style={{
                    padding: '6px 14px',
                    borderRadius: '10px',
                    border: '1px solid rgba(208, 188, 255, 0.4)',
                    background: 'rgba(208, 188, 255, 0.15)',
                    color: '#d0bcff',
                    fontSize: 12,
                    fontWeight: 700,
                    cursor: 'pointer'
                  }}
                >
                  Change
                </button>
              </div>
            </div>
          </div>

          {/* Google Drive & Local JSON Backup Card */}
          <div className="haze-card" style={{ padding: '22px 20px', borderRadius: '26px' }}>
            <h3 style={{ margin: '0 0 4px 0', fontSize: 16, fontWeight: 800, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Download size={18} color="#d0bcff" />
              <span>Cross-Platform Backup & Restore</span>
            </h3>
            <div style={{ fontSize: 12.5, color: 'rgba(255,255,255,0.6)', marginBottom: 14 }}>
              100% compatible with Android App backups (`EAPTracker_Backup_*.json`).
            </div>

            <div style={{ display: 'flex', gap: 10 }}>
              <button
                onClick={handleDownloadBackup}
                style={{
                  flex: 1,
                  padding: '12px 14px',
                  borderRadius: '14px',
                  border: 'none',
                  background: '#d0bcff',
                  color: '#1d1b20',
                  fontSize: 13,
                  fontWeight: 800,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 6
                }}
              >
                <Download size={15} />
                <span>Download Backup</span>
              </button>

              <input
                type="file"
                accept=".json,application/json"
                ref={fileInputRef}
                onChange={handleRestoreBackup}
                style={{ display: 'none' }}
              />

              <button
                onClick={() => {
                  sound.playTick();
                  fileInputRef.current?.click();
                }}
                style={{
                  flex: 1,
                  padding: '12px 14px',
                  borderRadius: '14px',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  background: 'rgba(255, 255, 255, 0.06)',
                  color: '#fff',
                  fontSize: 13,
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 6
                }}
              >
                <Upload size={15} />
                <span>Restore Backup</span>
              </button>
            </div>
          </div>

          {/* Danger Zone */}
          <div
            className="haze-card"
            style={{
              padding: '20px',
              borderRadius: '26px',
              borderColor: 'rgba(244, 63, 94, 0.3)',
              background: 'rgba(244, 63, 94, 0.05)'
            }}
          >
            <h3 style={{ margin: '0 0 4px 0', fontSize: 15, fontWeight: 800, color: '#f43f5e', display: 'flex', alignItems: 'center', gap: 8 }}>
              <AlertTriangle size={16} />
              <span>Data Reset Zone</span>
            </h3>
            <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)', marginBottom: 12 }}>
              Clear specific stored data or reset the entire app.
            </div>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              <button
                onClick={handleResetSyllabus}
                style={{
                  padding: '8px 12px',
                  borderRadius: '10px',
                  border: '1px solid rgba(244, 63, 94, 0.4)',
                  background: 'rgba(244, 63, 94, 0.1)',
                  color: '#f43f5e',
                  fontSize: 12,
                  fontWeight: 700,
                  cursor: 'pointer'
                }}
              >
                Reset Syllabus
              </button>

              <button
                onClick={handleResetExams}
                style={{
                  padding: '8px 12px',
                  borderRadius: '10px',
                  border: '1px solid rgba(244, 63, 94, 0.4)',
                  background: 'rgba(244, 63, 94, 0.1)',
                  color: '#f43f5e',
                  fontSize: 12,
                  fontWeight: 700,
                  cursor: 'pointer'
                }}
              >
                Reset Exams
              </button>

              <button
                onClick={handleFactoryReset}
                style={{
                  padding: '8px 12px',
                  borderRadius: '10px',
                  border: '1px solid rgba(244, 63, 94, 0.6)',
                  background: 'rgba(244, 63, 94, 0.25)',
                  color: '#fff',
                  fontSize: 12,
                  fontWeight: 700,
                  cursor: 'pointer'
                }}
              >
                Factory Reset App
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Xiaomi Time Picker Modal */}
      {activeTimePicker && (
        <TimePickerModal
          title={activeTimePicker === 'morning' ? 'Morning Routine Alert Time' : 'Evening Study Reminder Time'}
          initialTime={
            activeTimePicker === 'morning'
              ? profile.dailyRoutineAlertTime || '08:00'
              : profile.eveningReminderTime || '21:00'
          }
          onSave={(timeStr) => {
            if (activeTimePicker === 'morning') {
              storage.saveProfile({ dailyRoutineAlertTime: timeStr });
            } else {
              storage.saveProfile({ eveningReminderTime: timeStr });
            }
            setActiveTimePicker(null);
            showToast('Reminder time updated');
          }}
          onClose={() => setActiveTimePicker(null)}
        />
      )}
    </div>
  );
};
