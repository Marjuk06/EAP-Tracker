import React, { useState, useRef } from 'react';
import {
 User,
 Target,
 Download,
 Upload,
 RotateCcw,
 Clock,
 CheckCircle2,
 AlertTriangle,
 Volume2,
 Vibrate,
 Smartphone,
 Zap,
 Rocket,
 Flame,
 BookOpen,
 Lightbulb,
 GraduationCap,
 Award,
 Shield,
 Sparkles
} from 'lucide-react';
import { UserProfile } from '../types';
import { storage } from '../services/storage';
import { sound } from '../services/audio';
import { admissionExamPresets } from '../data/initialData';
import { TimePickerModal } from './TimePickerModal';

interface ProfileSettingsViewProps {
 profile: UserProfile;
}

export const ProfileSettingsView: React.FC<ProfileSettingsViewProps> = ({ profile }) => {
 const [name, setName] = useState(profile.name);
 const [targetInstitution, setTargetInstitution] = useState(profile.targetInstitution);
 const [collegeName, setCollegeName] = useState(profile.collegeName);
 const [hscBatch, setHscBatch] = useState(profile.hscBatch);
 const [rollNo, setRollNo] = useState(profile.rollNo);
 const [avatarEmoji, setAvatarEmoji] = useState(profile.avatarEmoji || 'Zap');
 const [quoteText, setQuoteText] = useState(profile.quoteText);
 const [dailyGoalHours, setDailyGoalHours] = useState(String(profile.dailyGoalHours));

 const [targetExamName, setTargetExamName] = useState(profile.targetExamName);
 const [targetExamDate, setTargetExamDate] = useState(
  new Date(profile.targetExamEpoch).toISOString().split('T')[0]
 );

 // Time picker modals
 const [activeTimePicker, setActiveTimePicker] = useState<'daily' | 'evening' | null>(null);

 // Toast / Status messages
 const [toastMessage, setToastMessage] = useState<string | null>(null);
 const fileInputRef = useRef<HTMLInputElement>(null);

 const showToast = (msg: string) => {
  setToastMessage(msg);
  setTimeout(() => setToastMessage(null), 3500);
 };

 const handleSaveProfile = (e: React.FormEvent) => {
  e.preventDefault();
  sound.playSuccess();

  const targetEpoch = new Date(targetExamDate).getTime() || profile.targetExamEpoch;

  storage.saveProfile({
   name,
   targetInstitution,
   collegeName,
   hscBatch,
   rollNo,
   avatarEmoji,
   quoteText,
   dailyGoalHours: parseFloat(dailyGoalHours) || 8,
   targetExamName,
   targetExamEpoch: targetEpoch
  });

  showToast('Profile and Target settings saved successfully');
 };

 const handlePresetSelect = (preset: typeof admissionExamPresets[0]) => {
  sound.playTick();
  setTargetExamName(preset.name);
  const newDate = new Date(Date.now() + preset.days * 24 * 60 * 60 * 1000);
  setTargetExamDate(newDate.toISOString().split('T')[0]);
 };

 // Cross-Platform JSON Backup / Restore
 const handleDownloadBackup = () => {
  sound.playSuccess();
  storage.downloadBackupFile();
  showToast('Backup JSON file downloaded');
 };

 const handleUploadFile = (e: React.ChangeEvent<HTMLInputElement>) => {
  const file = e.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (event) => {
   const content = event.target?.result as string;
   if (content) {
    const success = storage.importBackupJson(content);
    if (success) {
     sound.playSuccess();
     showToast('Backup restored successfully');
    } else {
     sound.playTick();
     showToast('Invalid backup file format');
    }
   }
  };
  reader.readAsText(file);
  if (fileInputRef.current) fileInputRef.current.value = '';
 };

 const handleResetSyllabus = () => {
  sound.playTick();
  if (window.confirm('Are you sure you want to reset all syllabus progress?')) {
   storage.resetSyllabus();
   showToast('Syllabus progress has been reset');
  }
 };

 const handleResetExams = () => {
  sound.playTick();
  if (window.confirm('Are you sure you want to delete all exam records?')) {
   storage.resetExams();
   showToast('Exam records have been cleared');
  }
 };

 const handleFactoryReset = () => {
  sound.playTick();
  if (window.confirm('WARNING: This will erase all data and reset the app. Continue?')) {
   storage.clearAllData();
   window.location.reload();
  }
 };

 const avatarOptions = [
  { id: 'Zap', icon: Zap },
  { id: 'Rocket', icon: Rocket },
  { id: 'Target', icon: Target },
  { id: 'Flame', icon: Flame },
  { id: 'BookOpen', icon: BookOpen },
  { id: 'Lightbulb', icon: Lightbulb },
  { id: 'GraduationCap', icon: GraduationCap },
  { id: 'Award', icon: Award },
  { id: 'Shield', icon: Shield },
  { id: 'Sparkles', icon: Sparkles }
 ];

 const CurrentAvatarIcon = avatarOptions.find(o => o.id === avatarEmoji)?.icon || Zap;

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
   {/* Toast Alert */}
   {toastMessage && (
    <div
          style={{
      position: 'fixed',
      top: 20,
      left: '50%',
      transform: 'translateX(-50%)',
      zIndex: 120,
      background: 'rgba(16, 22, 36, 0.95)',
      backdropFilter: 'blur(16px)',
      border: '1px solid var(--accent-cyan)',
      color: '#fff',
      padding: '10px 20px',
      borderRadius: 'var(--radius-full)',
      boxShadow: '0 8px 30px rgba(0, 0, 0, 0.6)',
      fontSize: 13.5,
      fontWeight: 600,
      display: 'flex',
      alignItems: 'center',
      gap: 8
     }}
    >
     <CheckCircle2 size={16} color="var(--accent-cyan)" />
     <span>{toastMessage}</span>
    </div>
   )}

   {/* Main Profile Editor Card */}
      <div className="glass-card" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
     <div
            style={{
       width: 42,
       height: 42,
       borderRadius: 12,
       background: 'linear-gradient(135deg, var(--accent-cyan), var(--accent-violet))',
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'center',
       color: '#06101e'
      }}
     >
      <CurrentAvatarIcon size={22} color="#06101e" />
     </div>
     <div>
            <h2 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: '#fff' }}>
       Aspirant Profile & Target
      </h2>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
       Personalize your admission identity and exam milestones
      </div>
     </div>
    </div>

        <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
     {/* Avatar Icon Selection */}
     <div>
            <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 8 }}>
       CHOOSE AVATAR ICON
      </label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
       {avatarOptions.map((opt) => {
        const IconComp = opt.icon;
        const isSelected = avatarEmoji === opt.id;
        return (
         <button
          type="button"
          key={opt.id}
          onClick={() => {
           sound.playTick();
           setAvatarEmoji(opt.id);
          }}
                    style={{
           width: 38,
           height: 38,
           borderRadius: 10,
           border: isSelected ? '2px solid var(--accent-cyan)' : '1px solid var(--border-glass)',
           background: isSelected ? 'rgba(0, 229, 255, 0.2)' : 'rgba(255, 255, 255, 0.05)',
           color: isSelected ? 'var(--accent-cyan)' : '#fff',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           cursor: 'pointer'
          }}
         >
          <IconComp size={18} />
         </button>
        );
       })}
      </div>
     </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
      <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
        FULL NAME
       </label>
       <input
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
                style={{ width: '100%' }}
        required
       />
      </div>

      <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
        TARGET INSTITUTION / DEPT
       </label>
       <input
        type="text"
        value={targetInstitution}
        onChange={(e) => setTargetInstitution(e.target.value)}
        placeholder="e.g. BUET (CSE / EEE)"
                style={{ width: '100%' }}
        required
       />
      </div>

      <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
        COLLEGE NAME
       </label>
       <input
        type="text"
        value={collegeName}
        onChange={(e) => setCollegeName(e.target.value)}
        placeholder="e.g. Notre Dame College"
                style={{ width: '100%' }}
       />
      </div>

      <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
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
        <option value="2nd Timer">2nd Timer Admission</option>
       </select>
      </div>

      <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
        DAILY STUDY GOAL (HOURS)
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

     {/* Target Countdown Settings */}
     <div
            style={{
       padding: '16px',
       borderRadius: 'var(--radius-md)',
       background: 'rgba(255, 255, 255, 0.03)',
       border: '1px solid var(--border-glass)',
       marginTop: 6
      }}
     >
            <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--accent-cyan)', marginBottom: 10 }}>
       Target Admission Exam & Countdown Date
      </div>

      {/* Presets */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 12 }}>
       {admissionExamPresets.map((p) => (
        <button
         type="button"
         key={p.name}
         onClick={() => handlePresetSelect(p)}
                  style={{
          padding: '4px 10px',
          borderRadius: 'var(--radius-sm)',
          border: targetExamName === p.name ? '1px solid var(--accent-cyan)' : '1px solid var(--border-glass)',
          background: targetExamName === p.name ? 'rgba(0, 229, 255, 0.2)' : 'transparent',
          color: '#fff',
          fontSize: 11.5,
          cursor: 'pointer'
         }}
        >
         {p.name}
        </button>
       ))}
      </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
       <div>
                <label style={{ display: 'block', fontSize: 11.5, color: 'var(--text-muted)', marginBottom: 4 }}>
         EXAM TITLE
        </label>
        <input
         type="text"
         value={targetExamName}
         onChange={(e) => setTargetExamName(e.target.value)}
                  style={{ width: '100%' }}
         required
        />
       </div>

       <div>
                <label style={{ display: 'block', fontSize: 11.5, color: 'var(--text-muted)', marginBottom: 4 }}>
         EXAM DATE
        </label>
        <input
         type="date"
         value={targetExamDate}
         onChange={(e) => setTargetExamDate(e.target.value)}
                  style={{ width: '100%' }}
         required
        />
       </div>
      </div>
     </div>

     <div>
            <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
       PERSONAL MOTIVATIONAL BIO / QUOTE
      </label>
      <textarea
       rows={2}
       value={quoteText}
       onChange={(e) => setQuoteText(e.target.value)}
              style={{ width: '100%', resize: 'none' }}
      />
     </div>

          <button type="submit" className="btn-primary" style={{ alignSelf: 'flex-start' }}>
      Save Changes
     </button>
    </form>
   </div>

   {/* Routine & Reminder Alerts Card (Xiaomi Wheel Picker) */}
      <div className="glass-card" style={{ padding: '24px' }}>
        <h3 style={{ margin: '0 0 8px 0', fontSize: 18, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
     <Clock size={18} color="var(--accent-violet)" />
     <span>Daily Routine & Study Alerts</span>
    </h3>
        <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 18 }}>
     Configure routine notification times with custom MIUI / HyperOS clock scroll picker
    </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 14 }}>
     {/* Daily Morning Routine Alert */}
     <div
            style={{
       padding: '16px',
       borderRadius: 'var(--radius-md)',
       background: 'rgba(255, 255, 255, 0.04)',
       border: '1px solid var(--border-glass)',
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'space-between'
      }}
     >
      <div>
              <div style={{ fontSize: 14, fontWeight: 700, color: '#fff' }}>Daily Morning Schedule</div>
              <div style={{ fontSize: 12, color: 'var(--accent-cyan)', marginTop: 2 }}>
        Time: {profile.dailyRoutineAlertTime || '08:00'}
       </div>
      </div>

      <button
       onClick={() => {
        sound.playTick();
        setActiveTimePicker('daily');
       }}
       className="btn-glass"
      >
       Change
      </button>
     </div>

     {/* Evening Study Log Reminder */}
     <div
            style={{
       padding: '16px',
       borderRadius: 'var(--radius-md)',
       background: 'rgba(255, 255, 255, 0.04)',
       border: '1px solid var(--border-glass)',
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'space-between'
      }}
     >
      <div>
              <div style={{ fontSize: 14, fontWeight: 700, color: '#fff' }}>Evening Study Log Reminder</div>
              <div style={{ fontSize: 12, color: 'var(--accent-violet)', marginTop: 2 }}>
        Time: {profile.eveningReminderTime || '21:00'}
       </div>
      </div>

      <button
       onClick={() => {
        sound.playTick();
        setActiveTimePicker('evening');
       }}
       className="btn-glass"
      >
       Change
      </button>
     </div>
    </div>
   </div>

   {/* Cloud & Cross-Platform JSON Backup Card */}
      <div className="glass-card" style={{ padding: '24px' }}>
        <h3 style={{ margin: '0 0 6px 0', fontSize: 18, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
     <Download size={18} color="var(--accent-cyan)" />
     <span>Cross-Platform Backup & Restore</span>
    </h3>
        <div style={{ fontSize: 12.5, color: 'var(--text-muted)', marginBottom: 18 }}>
     100% compatible with Android App! Export JSON from Web to Android or restore Android backups here.
    </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
     <button onClick={handleDownloadBackup} className="btn-primary">
      <Download size={16} />
      <span>Download Backup (.json)</span>
     </button>

     <input
      type="file"
      accept=".json,application/json"
      ref={fileInputRef}
      onChange={handleUploadFile}
            style={{ display: 'none' }}
     />

     <button
      onClick={() => {
       sound.playTick();
       fileInputRef.current?.click();
      }}
      className="btn-glass"
     >
      <Upload size={16} />
      <span>Restore from File (.json)</span>
     </button>
    </div>
   </div>

   {/* Danger Zone: Reset Data */}
   <div
    className="glass-card"
        style={{
     padding: '24px',
     borderColor: 'rgba(244, 63, 94, 0.3)',
     background: 'rgba(244, 63, 94, 0.04)'
    }}
   >
        <h3 style={{ margin: '0 0 6px 0', fontSize: 17, color: 'var(--accent-rose)', display: 'flex', alignItems: 'center', gap: 8 }}>
     <AlertTriangle size={18} />
     <span>Data Reset Zone</span>
    </h3>
        <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 16 }}>
     Clear specific datasets or reset the entire web app to initial defaults.
    </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
     <button
      onClick={handleResetSyllabus}
      className="btn-glass"
            style={{ borderColor: 'rgba(244, 63, 94, 0.4)', color: 'var(--accent-rose)' }}
     >
      Reset Syllabus Progress
     </button>

     <button
      onClick={handleResetExams}
      className="btn-glass"
            style={{ borderColor: 'rgba(244, 63, 94, 0.4)', color: 'var(--accent-rose)' }}
     >
      Reset Exam Scores
     </button>

     <button
      onClick={handleFactoryReset}
      className="btn-glass"
            style={{ background: 'rgba(244, 63, 94, 0.15)', borderColor: 'var(--accent-rose)', color: '#fff' }}
     >
      Factory Reset App
     </button>
    </div>
   </div>

   {/* Xiaomi Time Picker Modal */}
   {activeTimePicker && (
    <TimePickerModal
     title={activeTimePicker === 'daily' ? 'Morning Routine Alert Time' : 'Evening Study Reminder Time'}
     initialTime={
      activeTimePicker === 'daily'
       ? profile.dailyRoutineAlertTime || '08:00'
       : profile.eveningReminderTime || '21:00'
     }
     onSave={(timeStr) => {
      if (activeTimePicker === 'daily') {
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
