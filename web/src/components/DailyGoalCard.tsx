import React, { useState } from 'react';
import { Flame, Play, Plus, Clock } from 'lucide-react';
import { storage } from '../services/storage';
import { sound } from '../services/audio';

interface DailyGoalCardProps {
 dailyGoalHours: number;
 todayMinutes: number;
 streak: number;
 onOpenTimer: () => void;
}

export const DailyGoalCard: React.FC<DailyGoalCardProps> = ({
 dailyGoalHours,
 todayMinutes,
 streak,
 onOpenTimer
}) => {
 const [showLogModal, setShowLogModal] = useState(false);
 const [logMinsInput, setLogMinsInput] = useState('30');
 const [subjectInput, setSubjectInput] = useState('Physics');

 const goalMinutes = Math.max(60, dailyGoalHours * 60);
 const progressPercent = Math.min(100, Math.round((todayMinutes / goalMinutes) * 100));

 const hoursLogged = (todayMinutes / 60).toFixed(1);
 const remainingHours = Math.max(0, (goalMinutes - todayMinutes) / 60).toFixed(1);

 const handleQuickLog = (e: React.FormEvent) => {
  e.preventDefault();
  const mins = parseInt(logMinsInput, 10);
  if (!isNaN(mins) && mins > 0) {
   storage.logStudyMinutes(mins, subjectInput);
   sound.playSuccess();
   setShowLogModal(false);
  }
 };

 // SVG Circular progress radius
 const radius = 42;
 const circumference = 2 * Math.PI * radius;
 const strokeDashoffset = circumference - (progressPercent / 100) * circumference;

 return (
  <>
      <div className="glass-card" style={{ padding: '24px', position: 'relative' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <div
              style={{
        width: 36,
        height: 36,
        borderRadius: 10,
        background: 'rgba(139, 92, 246, 0.15)',
        border: '1px solid rgba(139, 92, 246, 0.3)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'var(--accent-violet)'
       }}
      >
       <Clock size={20} />
      </div>
      <div>
              <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent-violet)', letterSpacing: 1 }}>
        TODAY'S STUDY GOAL
       </div>
              <h3 style={{ margin: 0, fontSize: 17, fontWeight: 700, color: '#fff' }}>
        Daily Target: {dailyGoalHours} Hours
       </h3>
      </div>
     </div>

     <div
            style={{
       display: 'flex',
       alignItems: 'center',
       gap: 6,
       padding: '6px 12px',
       borderRadius: 'var(--radius-full)',
       background: 'rgba(245, 158, 11, 0.12)',
       border: '1px solid rgba(245, 158, 11, 0.3)',
       color: 'var(--accent-amber)',
       fontSize: 12.5,
       fontWeight: 700
      }}
     >
      <Flame size={16} />
      <span>{streak} Days Streak</span>
     </div>
    </div>

    {/* Content Body with Circular Progress Meter */}
    <div
          style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 20,
      marginTop: 10
     }}
    >
     {/* Stats Description */}
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
       <span
                style={{
         fontFamily: 'var(--font-mono)',
         fontSize: 36,
         fontWeight: 800,
         color: '#fff',
         lineHeight: 1
        }}
       >
        {hoursLogged}h
       </span>
              <span style={{ fontSize: 15, color: 'var(--text-muted)' }}>/ {dailyGoalHours}.0h</span>
      </div>

            <div style={{ marginTop: 8, fontSize: 13, color: 'var(--text-secondary)' }}>
       {progressPercent >= 100 ? (
                <span style={{ color: 'var(--accent-emerald)', fontWeight: 700 }}>
         Daily target achieved! Excellent work!
        </span>
       ) : (
        <span>
         <strong>{remainingHours}h remaining</strong> to reach your daily goal.
        </span>
       )}
      </div>

      {/* Quick Action Buttons */}
            <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
       <button
        onClick={() => {
         sound.playTick();
         onOpenTimer();
        }}
        className="btn-primary"
                style={{ padding: '8px 16px', fontSize: 13 }}
       >
        <Play size={14} />
        <span>Start Timer</span>
       </button>

       <button
        onClick={() => {
         sound.playTick();
         setShowLogModal(true);
        }}
        className="btn-glass"
                style={{ padding: '8px 14px', fontSize: 13 }}
       >
        <Plus size={14} />
        <span>Quick Log</span>
       </button>
      </div>
     </div>

     {/* SVG Progress Circle */}
          <div style={{ position: 'relative', width: 100, height: 100, flexShrink: 0 }}>
      <svg width="100" height="100" viewBox="0 0 100 100">
       {/* Background Track */}
       <circle
        cx="50"
        cy="50"
        r={radius}
        fill="none"
        stroke="rgba(255, 255, 255, 0.08)"
        strokeWidth="8"
       />
       {/* Animated Glowing Progress Ring */}
       <circle
        className="progress-ring-circle"
        cx="50"
        cy="50"
        r={radius}
        fill="none"
        stroke="url(#cyanVioletGrad)"
        strokeWidth="8"
        strokeDasharray={circumference}
        strokeDashoffset={strokeDashoffset}
        strokeLinecap="round"
       />
       <defs>
        <linearGradient id="cyanVioletGrad" x1="0%" y1="0%" x2="100%" y2="100%">
         <stop offset="0%" stopColor="#00e5ff" />
         <stop offset="100%" stopColor="#8b5cf6" />
        </linearGradient>
       </defs>
      </svg>
      <div
              style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexDirection: 'column'
       }}
      >
       <span
                style={{
         fontFamily: 'var(--font-mono)',
         fontSize: 17,
         fontWeight: 800,
         color: '#fff'
        }}
       >
        {progressPercent}%
       </span>
      </div>
     </div>
    </div>
   </div>

   {/* Quick Log Modal */}
   {showLogModal && (
    <div
          style={{
      position: 'fixed',
      inset: 0,
      zIndex: 100,
      background: 'rgba(0, 0, 0, 0.75)',
      backdropFilter: 'blur(10px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 20
     }}
     onClick={() => setShowLogModal(false)}
    >
     <div
      className="glass-modal"
            style={{ width: '100%', maxWidth: 380, padding: 24 }}
      onClick={(e) => e.stopPropagation()}
     >
            <h3 style={{ margin: '0 0 16px 0', fontSize: 18, color: '#fff' }}>
       Log Study Session
      </h3>

            <form onSubmit={handleQuickLog} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
       <div>
                <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 6 }}>
         SUBJECT
        </label>
        <select
         value={subjectInput}
         onChange={(e) => setSubjectInput(e.target.value)}
                  style={{ width: '100%' }}
        >
         <option value="Physics">Physics (পদার্থবিজ্ঞান)</option>
         <option value="Chemistry">Chemistry (রসায়ন)</option>
         <option value="Higher Math">Higher Math (উচ্চতর গণিত)</option>
         <option value="Biology">Biology (জীববিজ্ঞান)</option>
         <option value="Question Bank">Question Bank Solve</option>
         <option value="Revision">Revision / Practice</option>
        </select>
       </div>

       <div>
                <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 6 }}>
         DURATION (MINUTES)
        </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 6, marginBottom: 8 }}>
         {['15', '30', '45', '60'].map((mins) => (
          <button
           type="button"
           key={mins}
           onClick={() => setLogMinsInput(mins)}
                      style={{
            padding: '6px 0',
            borderRadius: 'var(--radius-sm)',
            border: logMinsInput === mins ? '1px solid var(--accent-cyan)' : '1px solid var(--border-glass)',
            background: logMinsInput === mins ? 'rgba(0, 229, 255, 0.2)' : 'rgba(255, 255, 255, 0.05)',
            color: '#fff',
            fontSize: 12.5,
            fontWeight: 600,
            cursor: 'pointer'
           }}
          >
           {mins}m
          </button>
         ))}
        </div>
        <input
         type="number"
         min="1"
         max="720"
         value={logMinsInput}
         onChange={(e) => setLogMinsInput(e.target.value)}
                  style={{ width: '100%' }}
         placeholder="Minutes studied"
         required
        />
       </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
        <button
         type="button"
         onClick={() => setShowLogModal(false)}
         className="btn-glass"
                  style={{ flex: 1 }}
        >
         Cancel
        </button>
                <button type="submit" className="btn-primary" style={{ flex: 1 }}>
         Log Minutes
        </button>
       </div>
      </form>
     </div>
    </div>
   )}
  </>
 );
};
