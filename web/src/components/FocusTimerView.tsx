import React, { useState, useEffect, useRef } from 'react';
import { Play, Pause, RotateCcw, Save, CheckCircle2, Timer, Clock } from 'lucide-react';
import confetti from 'canvas-confetti';
import { storage } from '../services/storage';
import { sound } from '../services/audio';

export const FocusTimerView: React.FC = () => {
 const [timerMode, setTimerMode] = useState<'stopwatch' | 'pomodoro'>('stopwatch');
 const [pomodoroMinutes, setPomodoroMinutes] = useState<number>(25);

 const [secondsElapsed, setSecondsElapsed] = useState<number>(0);
 const [secondsRemaining, setSecondsRemaining] = useState<number>(25 * 60);
 const [isRunning, setIsRunning] = useState<boolean>(false);
 const [selectedSubject, setSelectedSubject] = useState<string>('Physics');
 const [sessionNotes, setSessionNotes] = useState<string>('');
 const [lastLoggedMessage, setLastLoggedMessage] = useState<string | null>(null);

 const intervalRef = useRef<number | null>(null);

 // Timer Tick Loop
 useEffect(() => {
  if (isRunning) {
   intervalRef.current = window.setInterval(() => {
    if (timerMode === 'stopwatch') {
     setSecondsElapsed((prev) => prev + 1);
    } else {
     setSecondsRemaining((prev) => {
      if (prev <= 1) {
       // Timer Finished
       setIsRunning(false);
       sound.playTimerDone();
       confetti({ particleCount: 80, spread: 80, origin: { y: 0.6 } });
       return 0;
      }
      return prev - 1;
     });
    }
   }, 1000);
  } else {
   if (intervalRef.current !== null) {
    clearInterval(intervalRef.current);
    intervalRef.current = null;
   }
  }
  return () => {
   if (intervalRef.current !== null) {
    clearInterval(intervalRef.current);
    intervalRef.current = null;
   }
  };
 }, [isRunning, timerMode]);

 const handleStartPause = () => {
  sound.playTick();
  setIsRunning((prev) => !prev);
 };

 const handleReset = () => {
  sound.playTick();
  setIsRunning(false);
  setSecondsElapsed(0);
  setSecondsRemaining(pomodoroMinutes * 60);
 };

 const handleSetPomodoroDuration = (mins: number) => {
  sound.playTick();
  setIsRunning(false);
  setPomodoroMinutes(mins);
  setSecondsRemaining(mins * 60);
 };

 const handleLogSession = () => {
  sound.playTick();
  const durationMins =
   timerMode === 'stopwatch'
    ? Math.max(1, Math.round(secondsElapsed / 60))
    : Math.max(1, Math.round((pomodoroMinutes * 60 - secondsRemaining) / 60));

  storage.logStudyMinutes(durationMins, selectedSubject, sessionNotes);
  sound.playSuccess();

  setLastLoggedMessage(`Logged +${durationMins}m of ${selectedSubject} to today's study goal!`);
  setTimeout(() => setLastLoggedMessage(null), 4000);

  handleReset();
 };

 // Time formatters
 const displaySeconds = timerMode === 'stopwatch' ? secondsElapsed : secondsRemaining;
 const mins = Math.floor(displaySeconds / 60);
 const secs = displaySeconds % 60;
 const formattedTime = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

 // SVG Circle calculations
 const radius = 110;
 const circumference = 2 * Math.PI * radius;
 const totalTargetSecs = timerMode === 'stopwatch' ? 3600 : pomodoroMinutes * 60;
 const progressRatio =
  timerMode === 'stopwatch'
   ? Math.min(1, secondsElapsed / totalTargetSecs)
   : Math.max(0, secondsRemaining / (pomodoroMinutes * 60));
 const strokeDashoffset = circumference - progressRatio * circumference;

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, alignItems: 'center' }}>
   {/* Timer Container Card */}
   <div
    className="glass-card"
        style={{
     width: '100%',
     maxWidth: 600,
     padding: '36px 28px',
     display: 'flex',
     flexDirection: 'column',
     alignItems: 'center',
     gap: 24,
     position: 'relative'
    }}
   >
    {/* Mode Selector */}
    <div
          style={{
      display: 'flex',
      background: 'rgba(255, 255, 255, 0.05)',
      padding: 4,
      borderRadius: 'var(--radius-full)',
      border: '1px solid var(--border-glass)'
     }}
    >
     <button
      onClick={() => {
       sound.playTick();
       setIsRunning(false);
       setTimerMode('stopwatch');
      }}
            style={{
       display: 'flex', alignItems: 'center', gap: 6,
       padding: '8px 20px',
       borderRadius: 'var(--radius-full)',
       border: 'none',
       background: timerMode === 'stopwatch' ? 'var(--accent-cyan)' : 'transparent',
       color: timerMode === 'stopwatch' ? '#06101e' : 'var(--text-secondary)',
       fontWeight: 700,
       fontSize: 13,
       cursor: 'pointer'
      }}
     >
      <Timer size={15} /> Stopwatch
     </button>

     <button
      onClick={() => {
       sound.playTick();
       setIsRunning(false);
       setTimerMode('pomodoro');
       setSecondsRemaining(pomodoroMinutes * 60);
      }}
            style={{
       display: 'flex', alignItems: 'center', gap: 6,
       padding: '8px 20px',
       borderRadius: 'var(--radius-full)',
       border: 'none',
       background: timerMode === 'pomodoro' ? 'var(--accent-violet)' : 'transparent',
       color: timerMode === 'pomodoro' ? '#fff' : 'var(--text-secondary)',
       fontWeight: 700,
       fontSize: 13,
       cursor: 'pointer'
      }}
     >
      <Clock size={15} /> Pomodoro
     </button>
    </div>

    {/* Pomodoro presets if in Pomodoro Mode */}
    {timerMode === 'pomodoro' && (
          <div style={{ display: 'flex', gap: 8 }}>
      {[25, 45, 60, 90].map((m) => (
       <button
        key={m}
        onClick={() => handleSetPomodoroDuration(m)}
                style={{
         padding: '4px 12px',
         borderRadius: 'var(--radius-sm)',
         border: pomodoroMinutes === m ? '1px solid var(--accent-violet)' : '1px solid var(--border-glass)',
         background: pomodoroMinutes === m ? 'rgba(139, 92, 246, 0.25)' : 'rgba(255, 255, 255, 0.04)',
         color: '#fff',
         fontSize: 12,
         fontWeight: 600,
         cursor: 'pointer'
        }}
       >
        {m}m
       </button>
      ))}
     </div>
    )}

    {/* Circular Neon Study Visualizer */}
        <div style={{ position: 'relative', width: 260, height: 260, margin: '10px 0' }}>
     <svg width="260" height="260" viewBox="0 0 260 260">
      {/* Background Track */}
      <circle
       cx="130"
       cy="130"
       r={radius}
       fill="none"
       stroke="rgba(255, 255, 255, 0.06)"
       strokeWidth="10"
      />
      {/* Glowing Active Ring */}
      <circle
       className="progress-ring-circle"
       cx="130"
       cy="130"
       r={radius}
       fill="none"
       stroke={timerMode === 'stopwatch' ? 'url(#cyanGrad)' : 'url(#violetGrad)'}
       strokeWidth="10"
       strokeDasharray={circumference}
       strokeDashoffset={strokeDashoffset}
       strokeLinecap="round"
      />
      <defs>
       <linearGradient id="cyanGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#00e5ff" />
        <stop offset="100%" stopColor="#3b82f6" />
       </linearGradient>
       <linearGradient id="violetGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#8b5cf6" />
        <stop offset="100%" stopColor="#ec4899" />
       </linearGradient>
      </defs>
     </svg>

     {/* Time Display Inside Ring */}
     <div
            style={{
       position: 'absolute',
       inset: 0,
       display: 'flex',
       flexDirection: 'column',
       alignItems: 'center',
       justifyContent: 'center'
      }}
     >
      <div
              style={{
        fontFamily: 'var(--font-mono)',
        fontSize: 48,
        fontWeight: 800,
        color: '#fff',
        letterSpacing: 2
       }}
      >
       {formattedTime}
      </div>
            <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-muted)', marginTop: 4 }}>
       {isRunning ? (
                <span style={{ color: 'var(--accent-emerald)' }}>● FOCUSING IN PROGRESS</span>
       ) : (
        'PAUSED'
       )}
      </div>
     </div>
    </div>

    {/* Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
     <button
      onClick={handleReset}
      title="Reset"
      className="btn-glass"
            style={{ width: 46, height: 46, borderRadius: '50%', padding: 0 }}
     >
      <RotateCcw size={18} />
     </button>

     <button
      onClick={handleStartPause}
      className="btn-primary"
            style={{
       width: 64,
       height: 64,
       borderRadius: '50%',
       padding: 0,
       fontSize: 20
      }}
     >
            {isRunning ? <Pause size={24} /> : <Play size={24} style={{ marginLeft: 2 }} />}
     </button>

     <button
      onClick={handleLogSession}
      title="Log to Daily Goal"
      className="btn-glass"
            style={{
       width: 46,
       height: 46,
       borderRadius: '50%',
       padding: 0,
       color: 'var(--accent-emerald)'
      }}
     >
      <Save size={18} />
     </button>
    </div>

    {/* Subject & Session Notes Selector */}
        <div style={{ width: '100%', maxWidth: 400, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div style={{ display: 'flex', gap: 10 }}>
      <select
       value={selectedSubject}
       onChange={(e) => setSelectedSubject(e.target.value)}
              style={{ flex: 1 }}
      >
       <option value="Physics">Physics (পদার্থবিজ্ঞান)</option>
       <option value="Chemistry">Chemistry (রসায়ন)</option>
       <option value="Higher Math">Higher Math (উচ্চতর গণিত)</option>
       <option value="Biology">Biology (জীববিজ্ঞান)</option>
       <option value="Question Bank">Question Bank Solve</option>
       <option value="General Study">General Revision</option>
      </select>
     </div>

     <input
      type="text"
      placeholder="Session topic (e.g. ভেক্টর ম্যাথ সলভ, কণিক সূত্র)..."
      value={sessionNotes}
      onChange={(e) => setSessionNotes(e.target.value)}
            style={{ width: '100%', fontSize: 13 }}
     />

     <button
      onClick={handleLogSession}
      className="btn-glass"
            style={{
       width: '100%',
       padding: '10px 16px',
       color: 'var(--accent-emerald)',
       borderColor: 'rgba(16, 185, 129, 0.4)',
       background: 'rgba(16, 185, 129, 0.08)'
      }}
     >
      <CheckCircle2 size={16} />
      <span>Save & Log Session to Daily Goal</span>
     </button>
    </div>

    {/* Success Alert */}
    {lastLoggedMessage && (
     <div
            style={{
       padding: '10px 16px',
       borderRadius: 'var(--radius-sm)',
       background: 'rgba(16, 185, 129, 0.2)',
       border: '1px solid var(--accent-emerald)',
       color: '#fff',
       fontSize: 13,
       fontWeight: 600
      }}
     >
      {lastLoggedMessage}
     </div>
    )}
   </div>
  </div>
 );
};
